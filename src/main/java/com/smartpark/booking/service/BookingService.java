package com.smartpark.booking.service;

import com.smartpark.booking.dto.*;
import com.smartpark.booking.entity.Booking;
import com.smartpark.booking.entity.BookingAdminAudit;
import com.smartpark.booking.enums.BookingStatus;
import com.smartpark.booking.exception.BookingErrorCode;
import com.smartpark.booking.exception.BookingException;
import com.smartpark.booking.repository.BookingAdminAuditRepository;
import com.smartpark.booking.repository.BookingRepository;
import com.smartpark.parking.entity.ParkingAvailability;
import com.smartpark.parking.entity.ParkingSpace;
import com.smartpark.parking.enums.VerificationStatus;
import com.smartpark.parking.repository.ParkingAvailabilityRepository;
import com.smartpark.parking.repository.ParkingSpaceRepository;
import com.smartpark.parking.service.ParkingSlotUpdateHook;
import com.smartpark.user.entity.User;
import com.smartpark.user.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private static final List<BookingStatus> OCCUPYING_STATUSES = List.of(
            BookingStatus.BOOKED, BookingStatus.PENDING_PAYMENT);

    private final BookingRepository bookingRepository;
    private final BookingAdminAuditRepository bookingAdminAuditRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final ParkingAvailabilityRepository parkingAvailabilityRepository;
    private final ParkingSlotUpdateHook parkingSlotUpdateHook;

    @Value("${booking.pending-payment-timeout-minutes:10}")
    private int pendingPaymentTimeoutMinutes;

    @Value("${booking.cancellation-cutoff-minutes:60}")
    private int cancellationCutoffMinutes;

    // --- Validation: parking exists, approved+active, availability window, times, overlap, capacity ---
    private void validateParkingForBooking(ParkingSpace parking) {
        if (parking == null) {
            throw new BookingException(BookingErrorCode.PARKING_NOT_FOUND);
        }
        if (parking.getVerificationStatus() != VerificationStatus.APPROVED
                || !Boolean.TRUE.equals(parking.getIsPublished())
                || !Boolean.TRUE.equals(parking.getIsActive())) {
            throw new BookingException(BookingErrorCode.PARKING_INACTIVE);
        }
        if (parking.getAvailableSlots() == null || parking.getAvailableSlots() <= 0) {
            throw new BookingException(BookingErrorCode.SLOT_UNAVAILABLE);
        }
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (!start.isBefore(end)) {
            throw new BookingException(BookingErrorCode.INVALID_TIME_RANGE);
        }
    }

    private void validateAvailabilityWindow(Long parkingId, LocalDateTime startTime, LocalDateTime endTime) {
        List<ParkingAvailability> rules = parkingAvailabilityRepository.findByParkingSpaceIdOrderByDayOfWeek(parkingId);
        if (rules == null || rules.isEmpty()) {
            return; // no rules = treat as 24/7
        }
        int startDay = startTime.getDayOfWeek().getValue();
        int endDay = endTime.getDayOfWeek().getValue();
        LocalTime startLocal = startTime.toLocalTime();
        LocalTime endLocal = endTime.toLocalTime();
        boolean startDayOk = false;
        boolean endDayOk = false;
        for (ParkingAvailability av : rules) {
            if (!Boolean.TRUE.equals(av.getIsAvailable())) continue;
            if (av.getDayOfWeek() == startDay) {
                if (!startLocal.isBefore(av.getOpenTime()) && !startLocal.isAfter(av.getCloseTime())) {
                    startDayOk = true;
                }
            }
            if (av.getDayOfWeek() == endDay) {
                if (!endLocal.isAfter(av.getCloseTime()) && !endLocal.isBefore(av.getOpenTime())) {
                    endDayOk = true;
                }
            }
        }
        if (!startDayOk || !endDayOk) {
            throw new BookingException(
                    BookingErrorCode.INVALID_TIME_RANGE,
                    "Time outside parking availability window for day " + startDay
                            + " and/or day " + endDay + ". Please choose a slot within configured open/close hours.");
        }
    }

    private void validateNoOverbooking(Long parkingId, LocalDateTime startTime, LocalDateTime endTime, int totalSlots) {
        long overlapping = bookingRepository.countOverlappingBookings(
                parkingId, startTime, endTime, OCCUPYING_STATUSES);
        if (overlapping >= totalSlots) {
            throw new BookingException(BookingErrorCode.SLOT_UNAVAILABLE);
        }
    }

    // --- Create booking (transaction boundary) ---
    @Transactional
    public BookingResponse createBooking(User user, CreateBookingRequest req) {
        if (user.getRole() != Role.DRIVER) {
            throw new BookingException(BookingErrorCode.UNAUTHORIZED_BOOKING_ACCESS);
        }
        validateTimeRange(req.getStartTime(), req.getEndTime());

        ParkingSpace parking = parkingSpaceRepository.findById(req.getParkingId())
                .orElseThrow(() -> new BookingException(BookingErrorCode.PARKING_NOT_FOUND));
        validateParkingForBooking(parking);
        validateAvailabilityWindow(parking.getId(), req.getStartTime(), req.getEndTime());
        validateNoOverbooking(parking.getId(), req.getStartTime(), req.getEndTime(), parking.getTotalSlots());

        BigDecimal totalAmount = calculateAmount(parking.getPricePerHour(), req.getStartTime(), req.getEndTime());
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(pendingPaymentTimeoutMinutes);

        boolean reserved;
        try {
            reserved = parkingSlotUpdateHook.reserveSlot(parking.getId(), parking.getVersion());
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BookingException(BookingErrorCode.CONCURRENT_BOOKING);
        }
        if (!reserved) {
            throw new BookingException(BookingErrorCode.SLOT_UNAVAILABLE);
        }

        Booking booking = Booking.builder()
                .driverId(user.getId())
                .parkingId(parking.getId())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .status(BookingStatus.PENDING_PAYMENT)
                .totalAmount(totalAmount)
                .parkingVersionAtBooking(parking.getVersion())
                .pendingPaymentExpiresAt(expiresAt)
                .build();
        booking = bookingRepository.save(booking);
        log.info("Booking {} created PENDING_PAYMENT for parking {}", booking.getId(), parking.getId());
        return toResponse(booking, null, null);
    }

    private BigDecimal calculateAmount(BigDecimal pricePerHour, LocalDateTime start, LocalDateTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        return pricePerHour.multiply(BigDecimal.valueOf(minutes)).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    // --- Payment callback (idempotent) ---
    @Transactional
    public void onPaymentSuccess(Long bookingId, String paymentReference) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException(BookingErrorCode.BOOKING_NOT_FOUND));
        if (booking.getStatus() == BookingStatus.BOOKED) {
            log.debug("Booking {} already BOOKED (idempotent)", bookingId);
            return;
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            // Late callback after timeout/cancel: keep terminal state and return idempotently.
            log.warn("Ignoring late payment success callback for CANCELLED booking {}", bookingId);
            return;
        }
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BookingException(BookingErrorCode.INVALID_STATUS_TRANSITION,
                    "Expected PENDING_PAYMENT, got " + booking.getStatus());
        }
        booking.setStatus(BookingStatus.BOOKED);
        booking.setPaymentReference(paymentReference);
        booking.setPendingPaymentExpiresAt(null);
        bookingRepository.save(booking);
        log.info("Booking {} confirmed BOOKED", bookingId);
    }

    @Transactional
    public void onPaymentFailure(Long bookingId, String failureReason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException(BookingErrorCode.BOOKING_NOT_FOUND));
        if (booking.isTerminal() || booking.getStatus() == BookingStatus.BOOKED) {
            return;
        }
        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setFailureReason(failureReason);
            booking.setCancelledBy("SYSTEM");
            booking.setCancelledAt(LocalDateTime.now());
            booking.setPendingPaymentExpiresAt(null);
            bookingRepository.save(booking);
            parkingSlotUpdateHook.releaseSlot(booking.getParkingId());
            log.info("Booking {} cancelled due to payment failure", bookingId);
        }
    }

    // --- Driver: cancel (before cutoff) ---
    @Transactional
    public BookingResponse cancelByDriver(User user, Long bookingId) {
        Booking booking = bookingRepository.findByIdAndDriverId(bookingId, user.getId())
                .orElseThrow(() -> new BookingException(BookingErrorCode.BOOKING_NOT_FOUND));
        if (user.getRole() != Role.DRIVER) {
            throw new BookingException(BookingErrorCode.UNAUTHORIZED_BOOKING_ACCESS);
        }
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT && booking.getStatus() != BookingStatus.BOOKED) {
            throw new BookingException(BookingErrorCode.INVALID_STATUS_TRANSITION);
        }
        LocalDateTime cutoff = booking.getStartTime().minusMinutes(cancellationCutoffMinutes);
        if (LocalDateTime.now().isAfter(cutoff)) {
            throw new BookingException(BookingErrorCode.CANNOT_CANCEL_AFTER_CUTOFF);
        }
        return doCancel(booking, "DRIVER");
    }

    private BookingResponse doCancel(Booking booking, String cancelledBy) {
        if (booking.getStatus().isTerminal()) {
            return toResponse(booking, null, null);
        }
        boolean wasOccupying = booking.isSlotOccupying();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledBy(cancelledBy);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setPendingPaymentExpiresAt(null);
        bookingRepository.save(booking);
        if (wasOccupying) {
            parkingSlotUpdateHook.releaseSlot(booking.getParkingId());
        }
        return toResponse(booking, null, null);
    }

    // --- Admin: force cancel (audited) ---
    @Transactional
    public BookingResponse adminForceCancel(User admin, AdminForceCancelRequest req) {
        if (admin.getRole() != Role.ADMIN) {
            throw new BookingException(BookingErrorCode.UNAUTHORIZED_BOOKING_ACCESS);
        }
        Booking booking = bookingRepository.findById(req.getBookingId())
                .orElseThrow(() -> new BookingException(BookingErrorCode.BOOKING_NOT_FOUND));
        BookingResponse resp = doCancel(booking, "ADMIN");
        BookingAdminAudit audit = BookingAdminAudit.builder()
                .bookingId(booking.getId())
                .adminId(admin.getId())
                .action("FORCE_CANCEL")
                .reason(req.getReason())
                .build();
        bookingAdminAuditRepository.save(audit);
        return resp;
    }

    // --- Completion (called by job or manual) ---
    @Transactional
    public void completeBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException(BookingErrorCode.BOOKING_NOT_FOUND));
        if (booking.getStatus() != BookingStatus.BOOKED) {
            return;
        }
        if (LocalDateTime.now().isBefore(booking.getEndTime())) {
            return;
        }
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);
        parkingSlotUpdateHook.releaseSlot(booking.getParkingId());
        log.info("Booking {} completed", bookingId);
    }

    // --- Driver: get own booking / history ---
    public BookingResponse getBookingForDriver(User user, Long bookingId) {
        Booking booking = bookingRepository.findByIdAndDriverId(bookingId, user.getId())
                .orElseThrow(() -> new BookingException(BookingErrorCode.BOOKING_NOT_FOUND));
        return toResponse(booking, null, user.getName());
    }

    public Page<BookingResponse> getDriverHistory(User user, int page, int size) {
        if (user.getRole() != Role.DRIVER) {
            throw new BookingException(BookingErrorCode.UNAUTHORIZED_BOOKING_ACCESS);
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Booking> bookings = bookingRepository.findByDriverIdOrderByCreatedAtDesc(user.getId(), pageable);
        return bookings.map(b -> toResponse(b, null, user.getName()));
    }

    public Page<BookingResponse> getDriverPendingPayments(User user, int page, int size) {
        if (user.getRole() != Role.DRIVER) {
            throw new BookingException(BookingErrorCode.UNAUTHORIZED_BOOKING_ACCESS);
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Booking> bookings = bookingRepository.findByDriverIdAndStatusOrderByCreatedAtDesc(
                user.getId(), BookingStatus.PENDING_PAYMENT, pageable);
        return bookings.map(b -> toResponse(b, null, user.getName()));
    }

    // --- Owner: read-only bookings for their parking ---
    public Page<BookingResponse> getBookingsForOwnerParking(User user, Long parkingId, int page, int size) {
        if (user.getRole() != Role.PARKING_OWNER) {
            throw new BookingException(BookingErrorCode.UNAUTHORIZED_BOOKING_ACCESS);
        }
        ParkingSpace parking = parkingSpaceRepository.findByIdAndOwnerId(parkingId, user.getId())
                .orElseThrow(() -> new BookingException(BookingErrorCode.PARKING_NOT_FOUND));
        Pageable pageable = PageRequest.of(page, size);
        Page<Booking> bookings = bookingRepository.findByParkingIdOrderByStartTimeDesc(parking.getId(), pageable);
        return bookings.map(b -> toResponse(b, parking.getName(), null));
    }

    public Page<BookingResponse> getBookingsForOwner(User user, int page, int size) {
        if (user.getRole() != Role.PARKING_OWNER) {
            throw new BookingException(BookingErrorCode.UNAUTHORIZED_BOOKING_ACCESS);
        }
        Pageable pageable = PageRequest.of(page, size);
        List<Long> parkingIds = parkingSpaceRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId(), Pageable.unpaged())
                .stream().map(ParkingSpace::getId).collect(Collectors.toList());
        if (parkingIds.isEmpty()) {
            return Page.empty(pageable);
        }
        Page<Booking> bookings = bookingRepository.findByParkingIdInOrderByStartTimeDesc(parkingIds, pageable);
        return bookings.map(b -> toResponse(b, null, null));
    }

    // --- Admin: all bookings, force cancel (audited above) ---
    public Page<BookingResponse> getAllBookingsAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return bookingRepository.findAll(pageable).map(b -> toResponse(b, null, null));
    }

    public BookingResponse getBookingAdmin(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException(BookingErrorCode.BOOKING_NOT_FOUND));
        return toResponse(booking, null, null);
    }

    // --- Callback endpoint for payment module ---
    @Transactional
    public void handlePaymentCallback(PaymentCallbackRequest req) {
        if (Boolean.TRUE.equals(req.getSuccess())) {
            onPaymentSuccess(req.getBookingId(), req.getPaymentReference() != null ? req.getPaymentReference() : "");
        } else {
            onPaymentFailure(req.getBookingId(), req.getFailureReason() != null ? req.getFailureReason() : "Payment failed");
        }
    }

    // --- Internal: pending cleanup job ---
    @Transactional
    public int cleanupPendingPaymentTimeouts() {
        LocalDateTime cutoff = LocalDateTime.now();
        List<Booking> stuck = bookingRepository.findByStatusAndPendingPaymentExpiresAtBefore(
                BookingStatus.PENDING_PAYMENT, cutoff);
        for (Booking b : stuck) {
            onPaymentFailure(b.getId(), "Payment timeout");
        }
        return stuck.size();
    }

    // --- Internal: completion job ---
    @Transactional
    public int completeExpiredBookings() {
        List<Booking> expired = bookingRepository.findByStatusAndEndTimeBefore(BookingStatus.BOOKED, LocalDateTime.now());
        for (Booking b : expired) {
            completeBooking(b.getId());
        }
        return expired.size();
    }

    private BookingResponse toResponse(Booking b, String parkingName, String driverName) {
        return BookingResponse.builder()
                .id(b.getId())
                .driverId(b.getDriverId())
                .parkingId(b.getParkingId())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .status(b.getStatus())
                .totalAmount(b.getTotalAmount())
                .paymentReference(b.getPaymentReference())
                .failureReason(b.getFailureReason())
                .cancelledAt(b.getCancelledAt())
                .cancelledBy(b.getCancelledBy())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .pendingPaymentExpiresAt(b.getPendingPaymentExpiresAt())
                .parkingName(parkingName)
                .driverName(driverName)
                .build();
    }
}
