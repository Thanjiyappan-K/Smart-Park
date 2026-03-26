package com.smartpark.payment.entity;

import com.smartpark.payment.enums.PaymentGateway;
import com.smartpark.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "payment", indexes = {
    @Index(name = "idx_payment_booking_id", columnList = "booking_id"),
    @Index(name = "idx_payment_stripe_pi", columnList = "stripe_payment_intent_id"),
    @Index(name = "idx_payment_status", columnList = "status"),
    @Index(name = "idx_payment_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_gateway", nullable = false, length = 20)
    @Builder.Default
    private PaymentGateway paymentGateway = PaymentGateway.STRIPE;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "stripe_payment_intent_id", length = 255)
    private String stripePaymentIntentId;

    @Column(name = "stripe_charge_id", length = 255)
    private String stripeChargeId;

    @Column(name = "stripe_account_id", length = 255)
    private String stripeAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.INITIATED;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "attempts")
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "refund_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Column(name = "metadata", columnDefinition = "JSON")
    @Convert(converter = PaymentMetadataConverter.class)
    private Map<String, String> metadata;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
