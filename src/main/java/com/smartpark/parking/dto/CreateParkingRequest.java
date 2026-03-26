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
public class CreateParkingRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 200)
    private String name;

    @Size(max = 1000)
    private String address;

    @Size(max = 100)
    private String city;

    @NotNull
    @DecimalMin(value = "-90", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90", message = "Latitude must be between -90 and 90")
    private BigDecimal latitude;

    @NotNull
    @DecimalMin(value = "-180", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180", message = "Longitude must be between -180 and 180")
    private BigDecimal longitude;

    @NotNull
    @Min(1)
    private Integer totalSlots;

    @NotNull
    @DecimalMin("0")
    private BigDecimal pricePerHour;

    @NotNull
    private VehicleType vehicleType;

    private List<AvailabilitySlotDto> availabilityRules;

    private List<String> imageUrls; // first one can be primary if needed
}
