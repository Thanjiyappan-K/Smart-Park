package com.smartpark.booking.controller;

import com.smartpark.booking.dto.PaymentCallbackRequest;
import com.smartpark.booking.service.BookingService;
import com.smartpark.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Called by Payment module (or mock) after payment success/failure.
 * In production, secure with shared secret or internal network.
 */
@RestController
@RequestMapping("/booking/internal")
@RequiredArgsConstructor
public class PaymentCallbackController {

    private final BookingService bookingService;

    @PostMapping("/payment-callback")
    public ResponseEntity<ApiResponse<Void>> paymentCallback(@Valid @RequestBody PaymentCallbackRequest request) {
        bookingService.handlePaymentCallback(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Callback processed")
                .build());
    }
}
