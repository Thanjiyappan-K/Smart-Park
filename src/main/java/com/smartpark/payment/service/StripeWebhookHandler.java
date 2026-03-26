package com.smartpark.payment.service;

import com.smartpark.booking.dto.PaymentCallbackRequest;
import com.smartpark.booking.service.BookingService;
import com.smartpark.payment.entity.Payment;
import com.smartpark.payment.entity.StripeWebhookEvent;
import com.smartpark.payment.enums.PaymentStatus;
import com.smartpark.payment.exception.PaymentErrorCode;
import com.smartpark.payment.exception.PaymentException;
import com.smartpark.payment.repository.PaymentRepository;
import com.smartpark.payment.repository.StripeWebhookEventRepository;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Dispute;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles Stripe webhook events. Idempotent: deduplicates by event id.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookHandler {

    private final StripeWebhookEventRepository webhookEventRepository;
    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final PaymentService paymentService;

    /**
     * Returns true if event was already processed (idempotent).
     */
    @Transactional
    public boolean isDuplicate(String stripeEventId) {
        return webhookEventRepository.existsByStripeEventId(stripeEventId);
    }

    /**
     * Record event as processed (call after successful handling).
     */
    @Transactional
    public void recordProcessed(String stripeEventId, String eventType) {
        if (webhookEventRepository.existsByStripeEventId(stripeEventId)) {
            return;
        }
        StripeWebhookEvent event = StripeWebhookEvent.builder()
                .stripeEventId(stripeEventId)
                .eventType(eventType)
                .processedAt(java.time.LocalDateTime.now())
                .build();
        webhookEventRepository.save(event);
    }

    /**
     * Handle event payload. Call after signature verification and dedupe check.
     */
    @Transactional
    public void handleEvent(Event event) {
        String eventId = event.getId();
        String type = event.getType();
        recordProcessed(eventId, type);

        switch (type) {
            case "payment_intent.succeeded" -> handlePaymentIntentSucceeded(event);
            case "payment_intent.payment_failed" -> handlePaymentIntentFailed(event);
            case "charge.refunded" -> handleChargeRefunded(event);
            case "charge.dispute.created" -> handleDisputeCreated(event);
            default -> log.debug("Unhandled Stripe event type: {}", type);
        }
    }

    private void handlePaymentIntentSucceeded(Event event) {
        StripeObject obj = deserialize(event, "payment_intent");
        if (!(obj instanceof PaymentIntent pi)) {
            return;
        }
        String piId = pi.getId();
        Payment payment = paymentRepository.findByStripePaymentIntentId(piId).orElse(null);
        if (payment == null) {
            log.warn("Payment not found for PaymentIntent {}", piId);
            return;
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.debug("Payment {} already SUCCESS (idempotent)", payment.getId());
            notifyBookingSuccess(payment.getBookingId(), piId);
            return;
        }
        payment.setStatus(PaymentStatus.SUCCESS);
        try {
            Object latestCharge = pi.getLatestCharge();
            if (latestCharge != null) payment.setStripeChargeId(String.valueOf(latestCharge));
        } catch (Exception ignored) {}
        paymentRepository.save(payment);
        notifyBookingSuccess(payment.getBookingId(), piId);
        paymentService.onPaymentSuccess(payment);
        log.info("Payment {} succeeded for booking {}", payment.getId(), payment.getBookingId());
    }

    private void handlePaymentIntentFailed(Event event) {
        StripeObject obj = deserialize(event, "payment_intent");
        if (!(obj instanceof PaymentIntent pi)) {
            return;
        }
        String reason = "Payment failed";
        try {
            if (pi.getLastPaymentError() != null) reason = pi.getLastPaymentError().getMessage();
        } catch (Exception ignored) {}
        String piId = pi.getId();
        Payment payment = paymentRepository.findByStripePaymentIntentId(piId).orElse(null);
        if (payment != null) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(reason);
            paymentRepository.save(payment);
        }
        String bookingIdStr = null;
        try {
            if (pi.getMetadata() != null) bookingIdStr = pi.getMetadata().get("booking_id");
        } catch (Exception ignored) {}
        if (bookingIdStr != null) {
            try {
                Long bookingId = Long.parseLong(bookingIdStr);
                bookingService.onPaymentFailure(bookingId, reason);
            } catch (NumberFormatException e) {
                log.warn("Invalid booking_id in PaymentIntent metadata: {}", bookingIdStr);
            }
        }
    }

    private void handleChargeRefunded(Event event) {
        // Optional: update payment refund_amount from charge. For now we rely on refund API updating our DB.
        log.debug("charge.refunded received: {}", event.getId());
    }

    private void handleDisputeCreated(Event event) {
        StripeObject obj = deserialize(event, "dispute");
        if (!(obj instanceof Dispute dispute)) return;
        String chargeId = dispute.getCharge();
        if (chargeId == null) return;
        paymentRepository.findByStripeChargeId(chargeId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.DISPUTED);
            paymentRepository.save(payment);
            log.info("Payment {} marked DISPUTED (charge {})", payment.getId(), chargeId);
        });
    }

    private void notifyBookingSuccess(Long bookingId, String paymentReference) {
        try {
            bookingService.handlePaymentCallback(PaymentCallbackRequest.builder()
                    .bookingId(bookingId)
                    .success(true)
                    .paymentReference(paymentReference)
                    .build());
        } catch (Exception e) {
            log.error("Failed to notify booking success for booking {}", bookingId, e);
            throw new PaymentException(PaymentErrorCode.STRIPE_ERROR, "Booking update failed: " + e.getMessage());
        }
    }

    private StripeObject deserialize(Event event, String objectType) {
        EventDataObjectDeserializer data = event.getDataObjectDeserializer();
        if (data.getObject().isPresent()) {
            return data.getObject().get();
        }
        log.warn("Could not deserialize event {} object type {}", event.getId(), objectType);
        return null;
    }
}
