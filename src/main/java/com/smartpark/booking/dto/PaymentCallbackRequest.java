package com.smartpark.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCallbackRequest {

    @NotNull
    private Long bookingId;

    @NotNull
    private Boolean success;

    private String paymentReference;

    private String failureReason;
}
