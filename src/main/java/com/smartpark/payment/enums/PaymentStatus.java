package com.smartpark.payment.enums;

/**
 * Payment lifecycle status.
 */
public enum PaymentStatus {
    INITIATED,
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED,
    DISPUTED
}
