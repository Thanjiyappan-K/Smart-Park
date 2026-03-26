package com.smartpark.payment.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {

    private Long paymentId;
    private String stripeRefundId;
    private BigDecimal refundAmount;
    private String status;
}
