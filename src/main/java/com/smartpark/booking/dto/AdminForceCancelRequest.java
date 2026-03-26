package com.smartpark.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminForceCancelRequest {

    @NotNull
    private Long bookingId;

    private String reason;
}
