package com.smartpark.booking.service;

import com.smartpark.booking.enums.BookingStatus;
import com.smartpark.booking.repository.BookingRepository;
import com.smartpark.parking.dto.BookingSummaryDto;
import com.smartpark.parking.dto.OwnerDashboardResponse;
import com.smartpark.parking.entity.ParkingSpace;
import com.smartpark.parking.repository.ParkingSpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides real booking/earnings data for owner dashboard.
 * Replaces StubDashboardProvider when booking module is active.
 */
@Service
@Primary
@RequiredArgsConstructor
public class BookingDashboardProvider implements com.smartpark.parking.service.ParkingDashboardProvider {

    private final BookingRepository bookingRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;

    @Override
    public OwnerDashboardResponse getDashboardForParking(Long parkingId, Long ownerId) {
        return parkingSpaceRepository.findByIdAndOwnerId(parkingId, ownerId)
                .map(p -> buildDashboard(p))
                .orElse(null);
    }

    @Override
    public List<OwnerDashboardResponse> getDashboardsForOwner(Long ownerId) {
        List<ParkingSpace> parkings = parkingSpaceRepository.findByOwnerIdOrderByCreatedAtDesc(
                ownerId, PageRequest.of(0, 100));
        return parkings.stream()
                .map(this::buildDashboard)
                .collect(Collectors.toList());
    }

    private OwnerDashboardResponse buildDashboard(ParkingSpace p) {
        int total = p.getTotalSlots();
        int available = p.getAvailableSlots();
        int used = total - available;
        BigDecimal utilization = total > 0
                ? BigDecimal.valueOf(used * 100.0 / total).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long upcoming = bookingRepository.countByParkingIdAndStatusIn(
                p.getId(), List.of(BookingStatus.PENDING_PAYMENT, BookingStatus.BOOKED));
        long completed = bookingRepository.countByParkingIdAndStatusIn(
                p.getId(), List.of(BookingStatus.COMPLETED));
        long cancelled = bookingRepository.countByParkingIdAndStatusIn(
                p.getId(), List.of(BookingStatus.CANCELLED, BookingStatus.REFUNDED, BookingStatus.NO_SHOW));

        List<BookingSummaryDto> recent = bookingRepository
                .findByParkingIdOrderByStartTimeDesc(p.getId(), PageRequest.of(0, 10))
                .stream()
                .map(b -> BookingSummaryDto.builder()
                        .bookingId(b.getId())
                        .driverName(null)
                        .startTime(b.getStartTime().toString())
                        .endTime(b.getEndTime().toString())
                        .status(b.getStatus().name())
                        .amount(b.getTotalAmount())
                        .build())
                .collect(Collectors.toList());

        BigDecimal grossEarnings = bookingRepository.findByParkingIdOrderByStartTimeDesc(p.getId(), PageRequest.of(0, 10000))
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .map(b -> b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return OwnerDashboardResponse.builder()
                .parkingId(p.getId())
                .parkingName(p.getName())
                .totalSlots(total)
                .availableSlots(available)
                .upcomingBookingsCount((int) upcoming)
                .completedBookingsCount((int) completed)
                .cancelledBookingsCount((int) cancelled)
                .utilizationPercent(utilization)
                .grossEarnings(grossEarnings)
                .fees(BigDecimal.ZERO)
                .netEarnings(grossEarnings)
                .recentBookings(recent)
                .build();
    }
}
