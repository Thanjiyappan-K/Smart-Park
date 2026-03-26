package com.smartpark.parking.controller;

import com.smartpark.common.response.ApiResponse;
import com.smartpark.parking.dto.ParkingResponse;
import com.smartpark.parking.dto.ParkingSearchRequest;
import com.smartpark.parking.dto.ParkingSearchResponse;
import com.smartpark.parking.service.ParkingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/parking/public")
@RequiredArgsConstructor
public class ParkingPublicController {

    private final ParkingService parkingService;

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<ParkingSearchResponse>> search(
            @Valid @RequestBody ParkingSearchRequest request) {
        ParkingSearchResponse result = parkingService.search(request);
        return ResponseEntity.ok(ApiResponse.<ParkingSearchResponse>builder()
                .success(true)
                .message("OK")
                .data(result)
                .build());
    }

    @GetMapping("/{parkingId}")
    public ResponseEntity<ApiResponse<ParkingResponse>> getById(@PathVariable Long parkingId) {
        ParkingResponse parking = parkingService.getByIdPublic(parkingId);
        return ResponseEntity.ok(ApiResponse.<ParkingResponse>builder()
                .success(true)
                .message("OK")
                .data(parking)
                .build());
    }
}
