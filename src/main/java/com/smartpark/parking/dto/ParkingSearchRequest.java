package com.smartpark.parking.dto;

import com.smartpark.parking.enums.VehicleType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSearchRequest {

    @NotNull
    @DecimalMin(value = "-90")
    @DecimalMax(value = "90")
    private BigDecimal latitude;

    @NotNull
    @DecimalMin(value = "-180")
    @DecimalMax(value = "180")
    private BigDecimal longitude;

    @NotNull
    @DecimalMin("0")
    private BigDecimal radiusKm;

    private VehicleType vehicleType;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    private String city;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;
}
