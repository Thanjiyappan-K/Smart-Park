package com.smartpark.parking.repository;

import com.smartpark.parking.entity.ParkingImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingImageRepository extends JpaRepository<ParkingImage, Long> {

    List<ParkingImage> findByParkingSpaceId(Long parkingSpaceId);

    Optional<ParkingImage> findByParkingSpaceIdAndIsPrimaryTrue(Long parkingSpaceId);

    void deleteByParkingSpaceId(Long parkingSpaceId);
}
