package com.smartpark.booking.entity;

import com.smartpark.booking.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking", indexes = {
    @Index(name = "idx_booking_driver_id", columnList = "driver_id"),
    @Index(name = "idx_booking_parking_id", columnList = "parking_id"),
    @Index(name = "idx_booking_status", columnList = "status"),
    @Index(name = "idx_booking_start_end", columnList = "start_time, end_time"),
    @Index(name = "idx_booking_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "parking_id", nullable = false)
    private Long parkingId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BookingStatus status = BookingStatus.INITIATED;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "parking_version_at_booking")
    private Long parkingVersionAtBooking;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by", length = 20)
    private String cancelledBy; // DRIVER, ADMIN, SYSTEM

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "pending_payment_expires_at")
    private LocalDateTime pendingPaymentExpiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isSlotOccupying() {
        return status == BookingStatus.BOOKED || status == BookingStatus.PENDING_PAYMENT;
    }

    public boolean isTerminal() {
        return status == BookingStatus.COMPLETED || status == BookingStatus.CANCELLED
                || status == BookingStatus.REFUNDED || status == BookingStatus.NO_SHOW;
    }
}
