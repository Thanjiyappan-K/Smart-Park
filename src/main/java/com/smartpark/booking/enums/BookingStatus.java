package com.smartpark.booking.enums;

/**
 * Booking status state machine.
 * INITIATED → PENDING_PAYMENT → BOOKED → COMPLETED
 * Exceptional: PENDING_PAYMENT/BOOKED → CANCELLED, BOOKED → REFUNDED, (optional NO_SHOW).
 */
public enum BookingStatus {
    INITIATED,
    PENDING_PAYMENT,
    BOOKED,
    COMPLETED,
    CANCELLED,
    REFUNDED,
    NO_SHOW
    ;

    /**
     * Returns true if this status is a terminal state (no further transitions expected).
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == REFUNDED || this == NO_SHOW;
    }
}
