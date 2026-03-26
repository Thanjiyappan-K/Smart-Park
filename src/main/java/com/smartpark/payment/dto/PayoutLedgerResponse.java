package com.smartpark.payment.dto;

import com.smartpark.payment.enums.PayoutLedgerStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutLedgerResponse {

    private Long id;
    private Long ownerId;
    private Long parkingId;
    private String stripePayoutId;
    private String stripeTransferId;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private BigDecimal grossAmount;
    private BigDecimal platformFees;
    private BigDecimal stripeFees;
    private BigDecimal netAmount;
    private PayoutLedgerStatus status;
    private LocalDateTime createdAt;
    private Long paymentId;
}
