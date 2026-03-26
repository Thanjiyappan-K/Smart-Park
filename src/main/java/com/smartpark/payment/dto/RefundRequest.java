package com.smartpark.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    @NotNull(message = "Payment ID or Booking ID is required")
    private Long paymentId;

    /** Optional: partial refund amount. If null, full refund. */
    private BigDecimal amount;

    private String reason;
}
