package com.smartpark.parking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "parking_availability", indexes = {
    @Index(name = "idx_availability_parking_id", columnList = "parking_space_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_space_id", nullable = false)
    private ParkingSpace parkingSpace;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek; // 1 = Monday, 7 = Sunday (Java DayOfWeek convention)

    @Column(name = "open_time", nullable = false)
    private LocalTime openTime;

    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;
}
