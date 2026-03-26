package com.smartpark.parking.job;

import com.smartpark.parking.entity.ParkingSpace;
import com.smartpark.parking.enums.VerificationStatus;
import com.smartpark.parking.repository.ParkingSpaceRepository;
import com.smartpark.parking.service.ParkingAvailabilityCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reconciles Redis availability cache with DB (source of truth).
 * Idempotent and safe to rerun.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ParkingReconciliationJob {

    private final ParkingSpaceRepository parkingSpaceRepository;
    private final ParkingAvailabilityCache availabilityCache;

    @Scheduled(cron = "${parking.job.reconciliation-cron:0 */5 * * * *}") // every 5 minutes
    @Transactional(readOnly = true)
    public void reconcileAvailabilityCache() {
        try {
            List<ParkingSpace> active = parkingSpaceRepository.findAll().stream()
                    .filter(p -> VerificationStatus.APPROVED.equals(p.getVerificationStatus())
                            && Boolean.TRUE.equals(p.getIsPublished())
                            && Boolean.TRUE.equals(p.getIsActive()))
                    .toList();
            for (ParkingSpace p : active) {
                availabilityCache.setAvailableSlots(p.getId(), p.getAvailableSlots());
            }
            log.debug("Reconciled availability cache for {} parkings", active.size());
        } catch (Exception e) {
            log.warn("Reconciliation job failed: {}", e.getMessage());
        }
    }
}
