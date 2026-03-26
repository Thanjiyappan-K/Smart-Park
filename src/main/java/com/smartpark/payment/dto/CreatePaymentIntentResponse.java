package com.smartpark.payment.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentIntentResponse {

    private String clientSecret;
    private String paymentIntentId;
    private String publishableKey;
    private String currency;
    private Long amountInSmallestUnit;
}
