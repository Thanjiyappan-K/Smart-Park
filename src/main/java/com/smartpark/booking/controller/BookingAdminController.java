package com.smartpark.booking.controller;

import com.smartpark.booking.dto.AdminForceCancelRequest;
import com.smartpark.booking.dto.BookingResponse;
import com.smartpark.booking.service.BookingService;
import com.smartpark.common.response.ApiResponse;
import com.smartpark.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/booking/admin")
@RequiredArgsConstructor
public class BookingAdminController {

    private final BookingService bookingService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<BookingResponse> bookings = bookingService.getAllBookingsAdmin(page, size);
        return ResponseEntity.ok(ApiResponse.<Page<BookingResponse>>builder()
                .success(true)
                .data(bookings)
                .build());
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getById(@PathVariable Long bookingId) {
        BookingResponse booking = bookingService.getBookingAdmin(bookingId);
        return ResponseEntity.ok(ApiResponse.<BookingResponse>builder()
                .success(true)
                .data(booking)
                .build());
    }

    @PostMapping("/force-cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> forceCancel(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AdminForceCancelRequest request) {
        BookingResponse cancelled = bookingService.adminForceCancel(user, request);
        return ResponseEntity.ok(ApiResponse.<BookingResponse>builder()
                .success(true)
                .message("Booking force-cancelled (audited)")
                .data(cancelled)
                .build());
    }
}
