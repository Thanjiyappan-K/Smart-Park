package com.smartpark.parking.controller;

import com.smartpark.common.response.ApiResponse;
import com.smartpark.parking.dto.AdminApproveRejectRequest;
import com.smartpark.parking.dto.ParkingResponse;
import com.smartpark.parking.service.ParkingService;
import com.smartpark.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/parking/admin")
@RequiredArgsConstructor
public class ParkingAdminController {

    private final ParkingService parkingService;

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listPending(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<ParkingResponse> list = parkingService.listPending(user, page, size);
        long total = parkingService.countPending(user);
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("OK")
                .data(Map.of(
                        "parkings", list,
                        "totalElements", total,
                        "page", page,
                        "size", size))
                .build());
    }

    @PostMapping("/approve")
    public ResponseEntity<ApiResponse<ParkingResponse>> approve(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AdminApproveRejectRequest request) {
        ParkingResponse parking = parkingService.approve(user, request.getParkingId());
        return ResponseEntity.ok(ApiResponse.<ParkingResponse>builder()
                .success(true)
                .message("Parking approved and published")
                .data(parking)
                .build());
    }

    @PostMapping("/reject")
    public ResponseEntity<ApiResponse<ParkingResponse>> reject(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AdminApproveRejectRequest request) {
        if (request.getReason() == null || request.getReason().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.<ParkingResponse>builder()
                    .success(false)
                    .message("Rejection reason is required")
                    .data(null)
                    .build());
        }
        ParkingResponse parking = parkingService.reject(user, request.getParkingId(), request.getReason());
        return ResponseEntity.ok(ApiResponse.<ParkingResponse>builder()
                .success(true)
                .message("Parking rejected")
                .data(parking)
                .build());
    }

    @PostMapping("/force-disable")
    public ResponseEntity<ApiResponse<ParkingResponse>> forceDisable(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AdminApproveRejectRequest request) {
        ParkingResponse parking = parkingService.forceDisable(user, request.getParkingId(), request.getReason());
        return ResponseEntity.ok(ApiResponse.<ParkingResponse>builder()
                .success(true)
                .message("Parking force disabled")
                .data(parking)
                .build());
    }
}
