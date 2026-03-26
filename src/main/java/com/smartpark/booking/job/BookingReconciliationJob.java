package com.smartpark.booking.job;

import com.smartpark.booking.enums.BookingStatus;
import com.smartpark.booking.repository.BookingRepository;
import com.smartpark.parking.entity.ParkingSpace;
import com.smartpark.parking.repository.ParkingSpaceRepository;
import com.smartpark.parking.service.ParkingSlotUpdateHook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Compares active bookings count per parking with available_slots and fixes mismatches.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingReconciliationJob {

    private final BookingRepository bookingRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final ParkingSlotUpdateHook parkingSlotUpdateHook;

    private static final List<BookingStatus> OCCUPYING = List.of(BookingStatus.BOOKED, BookingStatus.PENDING_PAYMENT);

    @Scheduled(cron = "${booking.job.reconciliation-cron:0 0 */1 * * *}")
    @Transactional
    public void reconcile() {
        List<ParkingSpace> all = parkingSpaceRepository.findAll();
        for (ParkingSpace p : all) {
            long occupying = bookingRepository.countByParkingIdAndStatusIn(p.getId(), OCCUPYING);
            int expectedAvailable = Math.max(0, p.getTotalSlots() - (int) occupying);
            int currentAvailable = p.getAvailableSlots();
            if (currentAvailable != expectedAvailable) {
                log.warn("Reconciliation: parking {} available_slots {} vs expected {}", p.getId(), currentAvailable, expectedAvailable);
                int diff = expectedAvailable - currentAvailable;
                if (diff > 0) {
                    for (int i = 0; i < diff; i++) {
                        parkingSlotUpdateHook.releaseSlot(p.getId());
                    }
                }
            }
        }
    }
}
