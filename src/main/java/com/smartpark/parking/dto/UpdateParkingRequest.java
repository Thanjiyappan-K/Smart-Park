package com.smartpark.parking.dto;

import com.smartpark.parking.enums.VehicleType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateParkingRequest {

    @Size(max = 200)
    private String name;

    @Size(max = 1000)
    private String address;

    @Size(max = 100)
    private String city;

    @DecimalMin(value = "-90")
    @DecimalMax(value = "90")
    private BigDecimal latitude;

    @DecimalMin(value = "-180")
    @DecimalMax(value = "180")
    private BigDecimal longitude;

    @Min(1)
    private Integer totalSlots;

    @DecimalMin("0")
    private BigDecimal pricePerHour;

    private VehicleType vehicleType;

    private List<AvailabilitySlotDto> availabilityRules;

    private List<String> imageUrls;
}
