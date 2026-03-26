package com.smartpark.payment.controller;

import com.smartpark.common.response.ApiResponse;
import com.smartpark.payment.dto.ConnectOnboardResponse;
import com.smartpark.payment.exception.PaymentErrorCode;
import com.smartpark.payment.exception.PaymentException;
import com.smartpark.payment.service.ConnectOnboardingService;
import com.smartpark.user.entity.User;
import com.smartpark.user.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Stripe Connect Express onboarding for parking owners.
 */
@RestController
@RequestMapping("/payment/owner/connect")
@RequiredArgsConstructor
public class ConnectController {

    private final ConnectOnboardingService connectOnboardingService;

    /** Start Connect onboarding: create Express account if needed, return redirect URL. */
    @GetMapping("/onboard")
    public ResponseEntity<ApiResponse<ConnectOnboardResponse>> startOnboarding(
            @AuthenticationPrincipal User user) {
        if (user.getRole() != Role.PARKING_OWNER) {
            throw new PaymentException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
        }
        ConnectOnboardResponse response = connectOnboardingService.getOrCreateOnboardingUrl(user);
        return ResponseEntity.ok(ApiResponse.<ConnectOnboardResponse>builder()
                .success(true)
                .data(response)
                .build());
    }

    /** Return URL after onboarding (frontend redirects here; we just return success). */
    @GetMapping("/success")
    public ResponseEntity<ApiResponse<String>> onboardingSuccess(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Stripe Connect onboarding complete")
                .data("OK")
                .build());
    }
}
