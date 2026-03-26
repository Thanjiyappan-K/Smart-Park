package com.smartpark.parking.controller;

import com.smartpark.common.response.ApiResponse;
import com.smartpark.parking.dto.CreateParkingRequest;
import com.smartpark.parking.dto.OwnerDashboardResponse;
import com.smartpark.parking.dto.ParkingResponse;
import com.smartpark.parking.dto.UpdateParkingRequest;
import com.smartpark.parking.service.ParkingService;
import com.smartpark.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/parking/owner")
@RequiredArgsConstructor
public class ParkingOwnerController {

    private final ParkingService parkingService;

    @PostMapping
    public ResponseEntity<ApiResponse<ParkingResponse>> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateParkingRequest request) {
        ParkingResponse created = parkingService.create(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<ParkingResponse>builder()
                        .success(true)
                        .message("Parking created successfully")
                        .data(created)
                        .build());
    }

    @PutMapping("/{parkingId}")
    public ResponseEntity<ApiResponse<ParkingResponse>> update(
            @AuthenticationPrincipal User user,
            @PathVariable Long parkingId,
            @Valid @RequestBody UpdateParkingRequest request) {
        ParkingResponse updated = parkingService.update(user, parkingId, request);
        return ResponseEntity.ok(ApiResponse.<ParkingResponse>builder()
                .success(true)
                .message("Parking updated")
                .data(updated)
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<ParkingResponse> list = parkingService.listByOwner(user, page, size);
        long total = parkingService.countByOwner(user);
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

    @GetMapping("/{parkingId}")
    public ResponseEntity<ApiResponse<ParkingResponse>> getById(
            @AuthenticationPrincipal User user,
            @PathVariable Long parkingId) {
        ParkingResponse parking = parkingService.getByIdForOwner(user, parkingId);
        return ResponseEntity.ok(ApiResponse.<ParkingResponse>builder()
                .success(true)
                .message("OK")
                .data(parking)
                .build());
    }

    @PostMapping("/{parkingId}/activate")
    public ResponseEntity<ApiResponse<ParkingResponse>> activate(
            @AuthenticationPrincipal User user,
            @PathVariable Long parkingId) {
        ParkingResponse parking = parkingService.activate(user, parkingId);
        return ResponseEntity.ok(ApiResponse.<ParkingResponse>builder()
                .success(true)
                .message("Parking activated")
                .data(parking)
                .build());
    }

    @PostMapping("/{parkingId}/deactivate")
    public ResponseEntity<ApiResponse<ParkingResponse>> deactivate(
            @AuthenticationPrincipal User user,
            @PathVariable Long parkingId) {
        ParkingResponse parking = parkingService.deactivate(user, parkingId);
        return ResponseEntity.ok(ApiResponse.<ParkingResponse>builder()
                .success(true)
                .message("Parking deactivated")
                .data(parking)
                .build());
    }

    @GetMapping("/{parkingId}/dashboard")
    public ResponseEntity<ApiResponse<OwnerDashboardResponse>> dashboard(
            @AuthenticationPrincipal User user,
            @PathVariable Long parkingId) {
        OwnerDashboardResponse d = parkingService.getDashboardForParking(user, parkingId);
        return ResponseEntity.ok(ApiResponse.<OwnerDashboardResponse>builder()
                .success(true)
                .message("OK")
                .data(d)
                .build());
    }

    @GetMapping("/dashboards")
    public ResponseEntity<ApiResponse<List<OwnerDashboardResponse>>> dashboards(
            @AuthenticationPrincipal User user) {
        List<OwnerDashboardResponse> list = parkingService.getDashboardsForOwner(user);
        return ResponseEntity.ok(ApiResponse.<List<OwnerDashboardResponse>>builder()
                .success(true)
                .message("OK")
                .data(list)
                .build());
    }
}
