package com.smartpark.parking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilitySlotDto {

    @NotNull
    @Min(1)
    @Max(7)
    private Integer dayOfWeek; // 1=Monday, 7=Sunday

    @NotNull
    private LocalTime openTime;

    @NotNull
    private LocalTime closeTime;

    private Boolean isAvailable = true;
}
