package com.smartpark.booking.controller;

import com.smartpark.booking.dto.BookingResponse;
import com.smartpark.booking.dto.CreateBookingRequest;
import com.smartpark.booking.service.BookingService;
import com.smartpark.common.response.ApiResponse;
import com.smartpark.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/booking/driver")
@RequiredArgsConstructor
public class BookingDriverController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateBookingRequest request) {
        BookingResponse booking = bookingService.createBooking(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<BookingResponse>builder()
                        .success(true)
                        .message("Booking created and waiting for payment")
                        .data(booking)
                        .build());
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @AuthenticationPrincipal User user,
            @PathVariable Long bookingId) {
        BookingResponse booking = bookingService.cancelByDriver(user, bookingId);
        return ResponseEntity.ok(ApiResponse.<BookingResponse>builder()
                .success(true)
                .message("Booking cancelled")
                .data(booking)
                .build());
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @AuthenticationPrincipal User user,
            @PathVariable Long bookingId) {
        BookingResponse booking = bookingService.getBookingForDriver(user, bookingId);
        return ResponseEntity.ok(ApiResponse.<BookingResponse>builder()
                .success(true)
                .message("Booking fetched")
                .data(booking)
                .build());
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getBookingHistory(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<BookingResponse> bookings = bookingService.getDriverHistory(user, page, size);
        return ResponseEntity.ok(ApiResponse.<Page<BookingResponse>>builder()
                .success(true)
                .message("Booking history fetched")
                .data(bookings)
                .build());
    }

    @GetMapping("/pending-payment")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getPendingPaymentBookings(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<BookingResponse> bookings = bookingService.getDriverPendingPayments(user, page, size);
        return ResponseEntity.ok(ApiResponse.<Page<BookingResponse>>builder()
                .success(true)
                .message("Pending payment bookings fetched")
                .data(bookings)
                .build());
    }
}
