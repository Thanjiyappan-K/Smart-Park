package com.smartpark.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentIntentRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    /** Idempotency key for creating PaymentIntent (optional but recommended). */
    private String idempotencyKey;
}
