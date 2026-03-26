package com.smartpark.parking.repository;

import com.smartpark.parking.entity.ParkingSpace;
import com.smartpark.parking.enums.VerificationStatus;
import com.smartpark.parking.enums.VehicleType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSpaceRepository extends JpaRepository<ParkingSpace, Long> {

    List<ParkingSpace> findByOwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);

    long countByOwnerId(Long ownerId);

    Optional<ParkingSpace> findByIdAndOwnerId(Long id, Long ownerId);

    @Query(value = """
        SELECT p.id, (
            6371000 * acos(
                LEAST(1, GREATEST(-1,
                    cos(radians(:lat)) * cos(radians(p.latitude)) * cos(radians(p.longitude) - radians(:lon))
                    + sin(radians(:lat)) * sin(radians(p.latitude))
                ))
            )
        ) AS distance
        FROM parking_space p
        WHERE p.verification_status = 'APPROVED'
          AND p.is_published = true
          AND p.is_active = true
          AND p.available_slots > 0
          AND (:vehicleType IS NULL OR :vehicleType = '' OR p.vehicle_type = :vehicleType OR p.vehicle_type = 'ANY')
          AND (:priceMin IS NULL OR p.price_per_hour >= :priceMin)
          AND (:priceMax IS NULL OR p.price_per_hour <= :priceMax)
          AND (:city IS NULL OR :city = '' OR LOWER(p.city) LIKE LOWER(CONCAT('%', :city, '%')))
          AND (
            6371000 * acos(
                LEAST(1, GREATEST(-1,
                    cos(radians(:lat)) * cos(radians(p.latitude)) * cos(radians(p.longitude) - radians(:lon))
                    + sin(radians(:lat)) * sin(radians(p.latitude))
                ))
            )
          ) <= :radiusMeters
        ORDER BY distance
        """, nativeQuery = true)
    List<Object[]> findIdsAndDistanceByLocation(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusMeters") double radiusMeters,
            @Param("vehicleType") String vehicleType,
            @Param("priceMin") BigDecimal priceMin,
            @Param("priceMax") BigDecimal priceMax,
            @Param("city") String city,
            Pageable pageable);

    @Query(value = """
        SELECT COUNT(*) FROM parking_space p
        WHERE p.verification_status = 'APPROVED'
          AND p.is_published = true
          AND p.is_active = true
          AND p.available_slots > 0
          AND (:vehicleType IS NULL OR :vehicleType = '' OR p.vehicle_type = :vehicleType OR p.vehicle_type = 'ANY')
          AND (:priceMin IS NULL OR p.price_per_hour >= :priceMin)
          AND (:priceMax IS NULL OR p.price_per_hour <= :priceMax)
          AND (:city IS NULL OR :city = '' OR LOWER(p.city) LIKE LOWER(CONCAT('%', :city, '%')))
          AND (
            6371000 * acos(
                LEAST(1, GREATEST(-1,
                    cos(radians(:lat)) * cos(radians(p.latitude)) * cos(radians(p.longitude) - radians(:lon))
                    + sin(radians(:lat)) * sin(radians(p.latitude))
                ))
            )
          ) <= :radiusMeters
        """, nativeQuery = true)
    long countSearchByLocationAndFilters(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusMeters") double radiusMeters,
            @Param("vehicleType") String vehicleType,
            @Param("priceMin") BigDecimal priceMin,
            @Param("priceMax") BigDecimal priceMax,
            @Param("city") String city);

    List<ParkingSpace> findByVerificationStatusOrderByCreatedAtAsc(
            VerificationStatus status, Pageable pageable);

    long countByVerificationStatus(VerificationStatus status);
}
