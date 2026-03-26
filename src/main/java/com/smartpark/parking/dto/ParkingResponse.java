package com.smartpark.parking.dto;

import com.smartpark.parking.enums.VerificationStatus;
import com.smartpark.parking.enums.VehicleType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingResponse {

    private Long id;
    private Long ownerId;
    private String name;
    private String address;
    private String city;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer totalSlots;
    private Integer availableSlots;
    private BigDecimal pricePerHour;
    private VehicleType vehicleType;
    private VerificationStatus verificationStatus;
    private Boolean isPublished;
    private Boolean isActive;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Distance in meters (only for public search). */
    private Double distanceMeters;

    private List<AvailabilitySlotDto> availabilityRules;
    private List<String> imageUrls;
    private String primaryImageUrl;
}
