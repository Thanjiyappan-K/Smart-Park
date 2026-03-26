package com.smartpark.booking.exception;

import lombok.Getter;

@Getter
public class BookingException extends RuntimeException {

    private final BookingErrorCode errorCode;

    public BookingException(BookingErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BookingException(BookingErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + (detail != null ? ": " + detail : ""));
        this.errorCode = errorCode;
    }
}
