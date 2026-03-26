package com.smartpark.parking.repository;

import com.smartpark.parking.entity.ParkingAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingAvailabilityRepository extends JpaRepository<ParkingAvailability, Long> {

    List<ParkingAvailability> findByParkingSpaceIdOrderByDayOfWeek(Long parkingSpaceId);

    void deleteByParkingSpaceId(Long parkingSpaceId);
}
