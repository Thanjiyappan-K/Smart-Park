package com.smartpark.common.exception;

import com.smartpark.booking.exception.BookingErrorCode;
import com.smartpark.booking.exception.BookingException;
import com.smartpark.common.response.ApiResponse;
import com.smartpark.parking.exception.ParkingErrorCode;
import com.smartpark.parking.exception.ParkingException;
import com.smartpark.payment.exception.PaymentErrorCode;
import com.smartpark.payment.exception.PaymentException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ParkingException.class)
    public ResponseEntity<ApiResponse<String>> handleParkingException(ParkingException ex) {
        HttpStatus status = mapParkingErrorToStatus(ex.getErrorCode());
        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(false)
                .message(ex.getMessage())
                .data(ex.getErrorCode().name())
                .build();
        return ResponseEntity.status(status).body(response);
    }

    private HttpStatus mapParkingErrorToStatus(ParkingErrorCode code) {
        return switch (code) {
            case PARKING_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case FORBIDDEN_ACCESS -> HttpStatus.FORBIDDEN;
            case PARKING_NOT_ACTIVE, NOT_APPROVED, OVERBOOKING, CANNOT_ACTIVATE_UNAPPROVED,
                 CANNOT_REDUCE_SLOTS_BELOW_BOOKINGS, INVALID_SLOT_COUNT, INVALID_COORDINATES,
                 REJECT_REASON_REQUIRED -> HttpStatus.BAD_REQUEST;
            case CONCURRENT_UPDATE_FAILED -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    @ExceptionHandler(BookingException.class)
    public ResponseEntity<ApiResponse<String>> handleBookingException(BookingException ex) {
        HttpStatus status = mapBookingErrorToStatus(ex.getErrorCode());
        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(false)
                .message(ex.getMessage())
                .data(ex.getErrorCode().name())
                .build();
        return ResponseEntity.status(status).body(response);
    }

    private HttpStatus mapBookingErrorToStatus(BookingErrorCode code) {
        return switch (code) {
            case BOOKING_NOT_FOUND, PARKING_NOT_FOUND, DRIVER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UNAUTHORIZED_BOOKING_ACCESS -> HttpStatus.FORBIDDEN;
            case SLOT_UNAVAILABLE, PARKING_INACTIVE, TIME_OVERLAP, INVALID_TIME_RANGE,
                 CANNOT_CANCEL_AFTER_CUTOFF, CANNOT_MODIFY_BOOKED, INVALID_STATUS_TRANSITION,
                 PAYMENT_TIMEOUT -> HttpStatus.BAD_REQUEST;
            case CONCURRENT_BOOKING -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiResponse<String>> handlePaymentException(PaymentException ex) {
        HttpStatus status = mapPaymentErrorToStatus(ex.getErrorCode());
        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(false)
                .message(ex.getMessage())
                .data(ex.getErrorCode().name())
                .build();
        return ResponseEntity.status(status).body(response);
    }

    private HttpStatus mapPaymentErrorToStatus(PaymentErrorCode code) {
        return switch (code) {
            case PAYMENT_NOT_FOUND, BOOKING_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UNAUTHORIZED_PAYMENT_ACCESS, WEBHOOK_SIGNATURE_INVALID -> HttpStatus.FORBIDDEN;
            case BOOKING_NOT_PENDING_PAYMENT, REFUND_AMOUNT_EXCEEDED, REFUND_NOT_ALLOWED,
                 DUPLICATE_PAYMENT_INTENT, INVALID_AMOUNT, OWNER_NOT_CONNECTED -> HttpStatus.BAD_REQUEST;
            case STRIPE_ERROR -> HttpStatus.BAD_GATEWAY;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<String>> handleRuntimeException(RuntimeException ex) {
        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .build();
        // Use 409 Conflict for business logic errors like duplicate email/phone
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .message("Validation failed")
                .data(errors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
