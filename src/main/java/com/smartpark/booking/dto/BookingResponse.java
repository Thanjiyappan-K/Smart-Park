package com.smartpark.booking.dto;

import com.smartpark.booking.enums.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long id;
    private Long driverId;
    private Long parkingId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private String paymentReference;
    private String failureReason;
    private LocalDateTime cancelledAt;
    private String cancelledBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime pendingPaymentExpiresAt;

    private String parkingName;
    private String driverName;
}
