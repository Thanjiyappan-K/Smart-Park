package com.smartpark.booking.job;

import com.smartpark.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PendingPaymentCleanupJob {

    private final BookingService bookingService;

    @Scheduled(cron = "${booking.job.pending-cleanup-cron:0 */2 * * * *}")
    public void cleanupPendingPayments() {
        try {
            int count = bookingService.cleanupPendingPaymentTimeouts();
            if (count > 0) {
                log.info("Pending payment cleanup: {} bookings cancelled", count);
            }
        } catch (Exception e) {
            log.error("Pending payment cleanup failed", e);
        }
    }
}
