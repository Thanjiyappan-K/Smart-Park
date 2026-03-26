package com.smartpark.parking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminApproveRejectRequest {

    @NotNull
    private Long parkingId;

    /** Required when action is REJECT */
    private String reason;
}
