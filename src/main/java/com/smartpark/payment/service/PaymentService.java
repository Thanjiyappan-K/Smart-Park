package com.smartpark.payment.service;

import com.smartpark.booking.entity.Booking;
import com.smartpark.booking.repository.BookingRepository;
import com.smartpark.booking.service.BookingService;
import com.smartpark.payment.dto.*;
import com.smartpark.payment.entity.Payment;
import com.smartpark.payment.entity.PayoutLedger;
import com.smartpark.payment.enums.PaymentGateway;
import com.smartpark.payment.enums.PaymentStatus;
import com.smartpark.payment.enums.PayoutLedgerStatus;
import com.smartpark.payment.exception.PaymentErrorCode;
import com.smartpark.payment.exception.PaymentException;
import com.smartpark.payment.repository.PaymentRepository;
import com.smartpark.payment.repository.PayoutLedgerRepository;
import com.smartpark.parking.entity.ParkingSpace;
import com.smartpark.parking.repository.ParkingSpaceRepository;
import com.smartpark.user.entity.User;
import com.smartpark.user.enums.Role;
import com.smartpark.user.repository.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final UserRepository userRepository;
    private final BookingService bookingService;
    private final StripeService stripeService;
    private final PayoutLedgerRepository payoutLedgerRepository;

    @Value("${payment.platform-fee-percent:10}")
    private int platformFeePercent;

    @Value("${payment.default-currency:inr}")
    private String defaultCurrency;

    /**
     * Create a Stripe PaymentIntent for a booking (driver). Booking must be PENDING_PAYMENT.
     * Idempotent: if a payment record with same booking already has a PaymentIntent, return it.
     */
    @Transactional
    public CreatePaymentIntentResponse createPaymentIntentForBooking(User user, CreatePaymentIntentRequest request) {
        if (user.getRole() != Role.DRIVER) {
            throw new PaymentException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
        }
        Long bookingId = request.getBookingId();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.BOOKING_NOT_FOUND));
        if (!booking.getDriverId().equals(user.getId())) {
            throw new PaymentException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
        }
        if (booking.getStatus() != com.smartpark.booking.enums.BookingStatus.PENDING_PAYMENT) {
            throw new PaymentException(PaymentErrorCode.BOOKING_NOT_PENDING_PAYMENT);
        }

        Optional<Payment> existing = paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(bookingId);
        if (existing.isPresent() && existing.get().getStripePaymentIntentId() != null
                && (existing.get().getStatus() == PaymentStatus.PENDING || existing.get().getStatus() == PaymentStatus.INITIATED)) {
            Payment p = existing.get();
            if (p.getStatus() == PaymentStatus.SUCCESS) {
                throw new PaymentException(PaymentErrorCode.DUPLICATE_PAYMENT_INTENT, "Payment already completed");
            }
            return CreatePaymentIntentResponse.builder()
                    .clientSecret(retrieveClientSecret(p.getStripePaymentIntentId()))
                    .paymentIntentId(p.getStripePaymentIntentId())
                    .publishableKey(stripeService.getPublishableKey())
                    .currency(p.getCurrency())
                    .amountInSmallestUnit(stripeService.toStripeAmount(p.getAmount(), p.getCurrency()))
                    .build();
        }

        ParkingSpace parking = parkingSpaceRepository.findById(booking.getParkingId())
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.BOOKING_NOT_FOUND));
        String currency = defaultCurrency;
        long amountSmallest = stripeService.toStripeAmount(booking.getTotalAmount(), currency);
        if (amountSmallest < 50) {
            throw new PaymentException(PaymentErrorCode.INVALID_AMOUNT, "Minimum charge is 0.50 " + currency.toUpperCase());
        }

        Map<String, String> metadata = new HashMap<>();
        metadata.put("booking_id", String.valueOf(bookingId));
        metadata.put("owner_id", String.valueOf(parking.getOwnerId()));
        metadata.put("parking_id", String.valueOf(parking.getId()));
        String idempotencyKey = request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()
                ? request.getIdempotencyKey()
                : "booking-" + bookingId + "-" + UUID.randomUUID();

        PaymentIntent pi;
        try {
            pi = stripeService.createPaymentIntent(amountSmallest, currency, metadata, idempotencyKey);
        } catch (StripeException e) {
            log.error("Stripe createPaymentIntent failed for booking {}", bookingId, e);
            throw new PaymentException(PaymentErrorCode.STRIPE_ERROR, e.getMessage());
        }

        Payment payment = Payment.builder()
                .bookingId(bookingId)
                .paymentGateway(PaymentGateway.STRIPE)
                .amount(booking.getTotalAmount())
                .currency(currency)
                .stripePaymentIntentId(pi.getId())
                .status(PaymentStatus.PENDING)
                .attempts(1)
                .stripeAccountId(null)
                .metadata(metadata)
                .build();
        payment = paymentRepository.save(payment);
        log.info("PaymentIntent created for booking {} -> payment {}", bookingId, payment.getId());

        String clientSecret = pi.getClientSecret();
        return CreatePaymentIntentResponse.builder()
                .clientSecret(clientSecret)
                .paymentIntentId(pi.getId())
                .publishableKey(stripeService.getPublishableKey())
                .currency(currency)
                .amountInSmallestUnit(amountSmallest)
                .build();
    }

    private String retrieveClientSecret(String paymentIntentId) {
        try {
            PaymentIntent pi = PaymentIntent.retrieve(paymentIntentId);
            return pi.getClientSecret();
        } catch (Exception e) {
            log.warn("Could not retrieve client secret for {}", paymentIntentId);
            return null;
        }
    }

    /**
     * Process refund (admin or owner of the parking).
     */
    @Transactional
    public RefundResponse refund(RefundRequest request, User requester) {
        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new PaymentException(PaymentErrorCode.REFUND_NOT_ALLOWED);
        }
        if (requester.getRole() != Role.ADMIN) {
            Booking booking = bookingRepository.findById(payment.getBookingId())
                    .orElseThrow(() -> new PaymentException(PaymentErrorCode.BOOKING_NOT_FOUND));
            ParkingSpace parking = parkingSpaceRepository.findById(booking.getParkingId()).orElse(null);
            if (parking == null || !parking.getOwnerId().equals(requester.getId())) {
                throw new PaymentException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
            }
        }
        BigDecimal maxRefund = payment.getAmount().subtract(payment.getRefundAmount() != null ? payment.getRefundAmount() : BigDecimal.ZERO);
        BigDecimal refundAmount = request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) > 0
                ? request.getAmount()
                : maxRefund;
        if (refundAmount.compareTo(maxRefund) > 0) {
            throw new PaymentException(PaymentErrorCode.REFUND_AMOUNT_EXCEEDED);
        }
        long refundSmallest = stripeService.toStripeAmount(refundAmount, payment.getCurrency());
        if (refundSmallest <= 0) {
            throw new PaymentException(PaymentErrorCode.REFUND_AMOUNT_EXCEEDED);
        }
        try {
            var refund = stripeService.createRefund(payment.getStripePaymentIntentId(), refundSmallest, request.getReason());
            payment.setRefundAmount((payment.getRefundAmount() != null ? payment.getRefundAmount() : BigDecimal.ZERO).add(refundAmount));
            if (payment.getRefundAmount().compareTo(payment.getAmount()) >= 0) {
                payment.setStatus(PaymentStatus.REFUNDED);
            }
            paymentRepository.save(payment);
            return RefundResponse.builder()
                    .paymentId(payment.getId())
                    .stripeRefundId(refund.getId())
                    .refundAmount(refundAmount)
                    .status(refund.getStatus())
                    .build();
        } catch (com.stripe.exception.StripeException e) {
            log.error("Refund failed for payment {}", payment.getId(), e);
            throw new PaymentException(PaymentErrorCode.STRIPE_ERROR, e.getMessage());
        }
    }

    /**
     * Create payout ledger entry and optionally transfer to owner (Connect).
     */
    @Transactional
    public void onPaymentSuccess(Payment payment) {
        BigDecimal gross = payment.getAmount();
        BigDecimal platformFee = gross.multiply(BigDecimal.valueOf(platformFeePercent)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal net = gross.subtract(platformFee);
        Long ownerId = null;
        Long parkingId = null;
        if (payment.getMetadata() != null) {
            String o = payment.getMetadata().get("owner_id");
            String p = payment.getMetadata().get("parking_id");
            if (o != null) ownerId = Long.parseLong(o);
            if (p != null) parkingId = Long.parseLong(p);
        }
        PayoutLedger ledger = PayoutLedger.builder()
                .ownerId(ownerId != null ? ownerId : 0L)
                .parkingId(parkingId)
                .grossAmount(gross)
                .platformFees(platformFee)
                .stripeFees(BigDecimal.ZERO)
                .netAmount(net)
                .status(PayoutLedgerStatus.PENDING)
                .periodStart(LocalDateTime.now())
                .periodEnd(LocalDateTime.now())
                .paymentId(payment.getId())
                .build();
        ledger = payoutLedgerRepository.save(ledger);

        if (ownerId != null) {
            User owner = userRepository.findById(ownerId).orElse(null);
            if (owner != null && owner.getStripeAccountId() != null && !owner.getStripeAccountId().isBlank()) {
                long netSmallest = stripeService.toStripeAmount(net, payment.getCurrency());
                if (netSmallest > 0) {
                    try {
                        var transfer = stripeService.createTransfer(
                                owner.getStripeAccountId(),
                                netSmallest,
                                payment.getCurrency(),
                                "SmartPark booking payout #" + payment.getBookingId(),
                                "payout-payment-" + payment.getId());
                        ledger.setStripeTransferId(transfer.getId());
                        ledger.setStatus(PayoutLedgerStatus.SENT);
                        payoutLedgerRepository.save(ledger);
                        log.info("Transfer created for owner {} payment {}", ownerId, payment.getId());
                    } catch (Exception e) {
                        log.error("Transfer to owner {} failed for payment {}", ownerId, payment.getId(), e);
                        ledger.setStatus(PayoutLedgerStatus.FAILED);
                        payoutLedgerRepository.save(ledger);
                    }
                }
            }
        }
    }

    public PaymentResponse getPaymentByBookingId(Long bookingId, User user) {
        Payment payment = paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(bookingId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new PaymentException(PaymentErrorCode.BOOKING_NOT_FOUND));
        if (user.getRole() == Role.DRIVER && !booking.getDriverId().equals(user.getId())) {
            throw new PaymentException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
        }
        if (user.getRole() == Role.PARKING_OWNER) {
            ParkingSpace parking = parkingSpaceRepository.findById(booking.getParkingId()).orElse(null);
            if (parking == null || !parking.getOwnerId().equals(user.getId())) {
                throw new PaymentException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
            }
        }
        return toPaymentResponse(payment);
    }

    public PaymentResponse getPayment(Long paymentId, User user) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));
        Booking booking = bookingRepository.findById(payment.getBookingId()).orElseThrow(() -> new PaymentException(PaymentErrorCode.BOOKING_NOT_FOUND));
        if (user.getRole() == Role.DRIVER && !booking.getDriverId().equals(user.getId())) {
            throw new PaymentException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
        }
        if (user.getRole() == Role.PARKING_OWNER) {
            ParkingSpace parking = parkingSpaceRepository.findById(booking.getParkingId()).orElse(null);
            if (parking == null || !parking.getOwnerId().equals(user.getId())) {
                throw new PaymentException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
            }
        }
        return toPaymentResponse(payment);
    }

    private PaymentResponse toPaymentResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .bookingId(p.getBookingId())
                .paymentGateway(p.getPaymentGateway())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .stripePaymentIntentId(p.getStripePaymentIntentId())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .attempts(p.getAttempts())
                .failureReason(p.getFailureReason())
                .refundAmount(p.getRefundAmount())
                .metadata(p.getMetadata())
                .build();
    }
}
