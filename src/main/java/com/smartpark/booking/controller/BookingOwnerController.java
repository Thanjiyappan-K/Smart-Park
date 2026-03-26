package com.smartpark.booking.controller;

import com.smartpark.booking.dto.BookingResponse;
import com.smartpark.booking.service.BookingService;
import com.smartpark.common.response.ApiResponse;
import com.smartpark.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/booking/owner")
@RequiredArgsConstructor
public class BookingOwnerController {

    private final BookingService bookingService;

    @GetMapping("/parking/{parkingId}")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getByParking(
            @AuthenticationPrincipal User user,
            @PathVariable Long parkingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<BookingResponse> bookings = bookingService.getBookingsForOwnerParking(user, parkingId, page, size);
        return ResponseEntity.ok(ApiResponse.<Page<BookingResponse>>builder()
                .success(true)
                .data(bookings)
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getAllForOwner(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<BookingResponse> bookings = bookingService.getBookingsForOwner(user, page, size);
        return ResponseEntity.ok(ApiResponse.<Page<BookingResponse>>builder()
                .success(true)
                .data(bookings)
                .build());
    }
}
