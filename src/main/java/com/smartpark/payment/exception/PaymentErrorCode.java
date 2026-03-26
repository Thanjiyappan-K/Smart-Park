package com.smartpark.payment.exception;

import lombok.Getter;

@Getter
public enum PaymentErrorCode {
    PAYMENT_NOT_FOUND("Payment not found"),
    BOOKING_NOT_FOUND("Booking not found"),
    BOOKING_NOT_PENDING_PAYMENT("Booking is not in PENDING_PAYMENT status"),
    STRIPE_ERROR("Stripe API error"),
    WEBHOOK_SIGNATURE_INVALID("Invalid webhook signature"),
    DUPLICATE_PAYMENT_INTENT("Payment intent already created for this booking"),
    REFUND_AMOUNT_EXCEEDED("Refund amount exceeds paid amount"),
    REFUND_NOT_ALLOWED("Refund not allowed for this payment"),
    OWNER_NOT_CONNECTED("Owner has not completed Stripe Connect onboarding"),
    UNAUTHORIZED_PAYMENT_ACCESS("Access denied to this payment"),
    INVALID_AMOUNT("Invalid amount");

    private final String message;

    PaymentErrorCode(String message) {
        this.message = message;
    }
}
