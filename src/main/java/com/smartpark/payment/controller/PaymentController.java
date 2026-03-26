package com.smartpark.payment.controller;

import com.smartpark.common.response.ApiResponse;
import com.smartpark.payment.dto.*;
import com.smartpark.payment.service.PaymentService;
import com.smartpark.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** Driver: create PaymentIntent for a booking (booking must be PENDING_PAYMENT). */
    @PostMapping("/driver/create-intent")
    public ResponseEntity<ApiResponse<CreatePaymentIntentResponse>> createIntent(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreatePaymentIntentRequest request) {
        CreatePaymentIntentResponse response = paymentService.createPaymentIntentForBooking(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<CreatePaymentIntentResponse>builder()
                        .success(true)
                        .message("Payment intent created")
                        .data(response)
                        .build());
    }

    /** Get payment by booking id (driver or owner of parking). */
    @GetMapping("/by-booking/{bookingId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getByBooking(
            @AuthenticationPrincipal User user,
            @PathVariable Long bookingId) {
        PaymentResponse response = paymentService.getPaymentByBookingId(bookingId, user);
        return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder()
                .success(true)
                .data(response)
                .build());
    }

    /** Get payment by payment id. */
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getById(
            @AuthenticationPrincipal User user,
            @PathVariable Long paymentId) {
        PaymentResponse response = paymentService.getPayment(paymentId, user);
        return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder()
                .success(true)
                .data(response)
                .build());
    }

    /** Admin or parking owner: request refund. */
    @PostMapping("/refund")
    public ResponseEntity<ApiResponse<RefundResponse>> refund(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody RefundRequest request) {
        RefundResponse response = paymentService.refund(request, user);
        return ResponseEntity.ok(ApiResponse.<RefundResponse>builder()
                .success(true)
                .message("Refund initiated")
                .data(response)
                .build());
    }
}
