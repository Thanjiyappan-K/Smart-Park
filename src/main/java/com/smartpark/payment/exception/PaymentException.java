package com.smartpark.payment.exception;

import lombok.Getter;

@Getter
public class PaymentException extends RuntimeException {

    private final PaymentErrorCode errorCode;

    public PaymentException(PaymentErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public PaymentException(PaymentErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + (detail != null ? ": " + detail : ""));
        this.errorCode = errorCode;
    }
}
