package com.smartpark.payment.dto;

import com.smartpark.payment.enums.PaymentGateway;
import com.smartpark.payment.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private Long bookingId;
    private PaymentGateway paymentGateway;
    private BigDecimal amount;
    private String currency;
    private String stripePaymentIntentId;
    private PaymentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer attempts;
    private String failureReason;
    private BigDecimal refundAmount;
    private Map<String, String> metadata;
}
