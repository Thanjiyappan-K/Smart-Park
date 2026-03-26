package com.smartpark.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores processed Stripe webhook event IDs for idempotency (deduplication).
 */
@Entity
@Table(name = "stripe_webhook_event", indexes = {
    @Index(name = "idx_stripe_webhook_event_id", columnList = "stripe_event_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StripeWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stripe_event_id", nullable = false, unique = true, length = 255)
    private String stripeEventId;

    @Column(name = "event_type", length = 100)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        if (processedAt == null) processedAt = LocalDateTime.now();
    }
}
