package com.smartpark.booking.exception;

import lombok.Getter;

@Getter
public enum BookingErrorCode {
    BOOKING_NOT_FOUND("Booking not found"),
    SLOT_UNAVAILABLE("No slots available"),
    PARKING_INACTIVE("Parking is not approved or active"),
    TIME_OVERLAP("Time overlap with existing bookings or outside availability"),
    PAYMENT_TIMEOUT("Payment timed out"),
    CONCURRENT_BOOKING("Concurrent booking conflict, please retry"),
    UNAUTHORIZED_BOOKING_ACCESS("Access denied to this booking"),
    CANNOT_CANCEL_AFTER_CUTOFF("Cancellation not allowed after cutoff time"),
    CANNOT_MODIFY_BOOKED("Cannot modify booking after confirmation"),
    INVALID_TIME_RANGE("Start time must be before end time"),
    PARKING_NOT_FOUND("Parking not found"),
    DRIVER_NOT_FOUND("Driver not found"),
    INVALID_STATUS_TRANSITION("Invalid status transition");

    private final String message;

    BookingErrorCode(String message) {
        this.message = message;
    }
}
