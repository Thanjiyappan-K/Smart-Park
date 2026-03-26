package com.smartpark.parking.exception;

import lombok.Getter;

@Getter
public enum ParkingErrorCode {
    PARKING_NOT_FOUND("Parking not found"),
    PARKING_NOT_ACTIVE("Parking is not active"),
    NOT_APPROVED("Parking is not approved"),
    FORBIDDEN_ACCESS("Access denied to this parking"),
    INVALID_SLOT_COUNT("Invalid slot count"),
    OVERBOOKING("No available slots"),
    CONCURRENT_UPDATE_FAILED("Update conflict, please retry"),
    CANNOT_ACTIVATE_UNAPPROVED("Cannot activate parking until admin approval"),
    CANNOT_REDUCE_SLOTS_BELOW_BOOKINGS("Cannot reduce total slots below active future bookings"),
    INVALID_COORDINATES("Invalid coordinates"),
    REJECT_REASON_REQUIRED("Rejection reason is required");

    private final String message;

    ParkingErrorCode(String message) {
        this.message = message;
    }
}
