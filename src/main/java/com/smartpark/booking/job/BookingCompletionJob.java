package com.smartpark.booking.job;

import com.smartpark.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCompletionJob {

    private final BookingService bookingService;

    @Scheduled(cron = "${booking.job.completion-cron:0 */5 * * * *}")
    public void completeExpiredBookings() {
        try {
            int count = bookingService.completeExpiredBookings();
            if (count > 0) {
                log.info("Booking completion job: {} bookings completed", count);
            }
        } catch (Exception e) {
            log.error("Booking completion job failed", e);
        }
    }
}
