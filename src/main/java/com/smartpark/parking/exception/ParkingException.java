package com.smartpark.parking.exception;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ParkingException extends RuntimeException {

    private final ParkingErrorCode errorCode;

    public ParkingException(ParkingErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ParkingException(ParkingErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + (detail != null ? ": " + detail : ""));
        this.errorCode = errorCode;
    }
}
