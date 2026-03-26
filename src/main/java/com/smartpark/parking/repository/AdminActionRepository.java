package com.smartpark.parking.repository;

import com.smartpark.parking.entity.AdminAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminActionRepository extends JpaRepository<AdminAction, Long> {

    List<AdminAction> findByParkingSpaceIdOrderByCreatedAtDesc(Long parkingSpaceId, Pageable pageable);

    Page<AdminAction> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
