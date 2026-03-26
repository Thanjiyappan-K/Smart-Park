package com.smartpark.parking.job;

import com.smartpark.parking.entity.ParkingSpace;
import com.smartpark.parking.repository.ParkingSpaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Daily parking stats (occupancy, etc.).
 * Idempotent. Actual aggregation can be extended when booking/stats tables exist.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ParkingDailyStatsJob {

    private final ParkingSpaceRepository parkingSpaceRepository;

    @Scheduled(cron = "${parking.job.daily-stats-cron:0 0 1 * * *}") // 1 AM daily
    @Transactional(readOnly = true)
    public void runDailyStats() {
        try {
            List<ParkingSpace> all = parkingSpaceRepository.findAll();
            int activeCount = 0;
            for (ParkingSpace p : all) {
                if (Boolean.TRUE.equals(p.getIsActive())) {
                    activeCount++;
                    int total = p.getTotalSlots();
                    int available = p.getAvailableSlots();
                    if (total > 0) {
                        double occupancy = (total - available) * 100.0 / total;
                        log.debug("Parking {} occupancy: {}%", p.getId(), String.format("%.1f", occupancy));
                    }
                }
            }
            log.info("Daily stats: {} active parkings", activeCount);
        } catch (Exception e) {
            log.warn("Daily stats job failed: {}", e.getMessage());
        }
    }
}
