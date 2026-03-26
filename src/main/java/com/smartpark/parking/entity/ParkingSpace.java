package com.smartpark.parking.entity;

import com.smartpark.parking.enums.VerificationStatus;
import com.smartpark.parking.enums.VehicleType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "parking_space", indexes = {
    @Index(name = "idx_parking_lat_lon", columnList = "latitude, longitude"),
    @Index(name = "idx_parking_owner_id", columnList = "owner_id"),
    @Index(name = "idx_parking_published_active_verification", columnList = "is_published, is_active, verification_status"),
    @Index(name = "idx_parking_price", columnList = "price_per_hour"),
    @Index(name = "idx_parking_city", columnList = "city")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSpace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 11, scale = 7)
    private BigDecimal longitude;

    @Column(name = "total_slots", nullable = false)
    private Integer totalSlots;

    @Column(name = "available_slots", nullable = false)
    private Integer availableSlots;

    @Column(name = "price_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;

    @Version
    private Long version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "parkingSpace", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ParkingAvailability> availabilityRules = new ArrayList<>();

    @OneToMany(mappedBy = "parkingSpace", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ParkingImage> images = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
