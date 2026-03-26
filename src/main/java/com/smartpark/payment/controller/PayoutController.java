package com.smartpark.payment.controller;

import com.smartpark.common.response.ApiResponse;
import com.smartpark.payment.dto.PayoutLedgerResponse;
import com.smartpark.payment.service.PayoutLedgerService;
import com.smartpark.user.entity.User;
import com.smartpark.user.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment/owner/payouts")
@RequiredArgsConstructor
public class PayoutController {

    private final PayoutLedgerService payoutLedgerService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PayoutLedgerResponse>>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (user.getRole() != Role.PARKING_OWNER) {
            return ResponseEntity.status(403).body(ApiResponse.<Page<PayoutLedgerResponse>>builder()
                    .success(false)
                    .message("Forbidden")
                    .build());
        }
        Page<PayoutLedgerResponse> result = payoutLedgerService.findByOwnerId(user.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.<Page<PayoutLedgerResponse>>builder()
                .success(true)
                .data(result)
                .build());
    }
}
