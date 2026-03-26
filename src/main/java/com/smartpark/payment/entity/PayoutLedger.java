package com.smartpark.payment.entity;

import com.smartpark.payment.enums.PayoutLedgerStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payout_ledger", indexes = {
    @Index(name = "idx_payout_owner_id", columnList = "owner_id"),
    @Index(name = "idx_payout_stripe_id", columnList = "stripe_payout_id"),
    @Index(name = "idx_payout_status", columnList = "status"),
    @Index(name = "idx_payout_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "parking_id")
    private Long parkingId;

    @Column(name = "stripe_payout_id", length = 255)
    private String stripePayoutId;

    @Column(name = "stripe_transfer_id", length = 255)
    private String stripeTransferId;

    @Column(name = "period_start")
    private LocalDateTime periodStart;

    @Column(name = "period_end")
    private LocalDateTime periodEnd;

    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "platform_fees", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal platformFees = BigDecimal.ZERO;

    @Column(name = "stripe_fees", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal stripeFees = BigDecimal.ZERO;

    @Column(name = "net_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PayoutLedgerStatus status = PayoutLedgerStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "payment_id")
    private Long paymentId;

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
