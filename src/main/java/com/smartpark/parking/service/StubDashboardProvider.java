package com.smartpark.parking.service;

import com.smartpark.parking.dto.OwnerDashboardResponse;
import com.smartpark.parking.entity.ParkingSpace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StubDashboardProvider implements ParkingDashboardProvider {

    private final com.smartpark.parking.repository.ParkingSpaceRepository parkingSpaceRepository;

    @Override
    public OwnerDashboardResponse getDashboardForParking(Long parkingId, Long ownerId) {
        return parkingSpaceRepository.findByIdAndOwnerId(parkingId, ownerId)
                .map(this::buildStubDashboard)
                .orElse(null);
    }

    @Override
    public List<OwnerDashboardResponse> getDashboardsForOwner(Long ownerId) {
        List<ParkingSpace> list = parkingSpaceRepository.findByOwnerIdOrderByCreatedAtDesc(
                ownerId, org.springframework.data.domain.PageRequest.of(0, 100));
        List<OwnerDashboardResponse> result = new ArrayList<>();
        for (ParkingSpace p : list) {
            result.add(buildStubDashboard(p));
        }
        return result;
    }

    private OwnerDashboardResponse buildStubDashboard(ParkingSpace p) {
        int total = p.getTotalSlots();
        int available = p.getAvailableSlots();
        int used = total - available;
        BigDecimal utilization = total > 0
                ? BigDecimal.valueOf(used * 100.0 / total).setScale(2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return OwnerDashboardResponse.builder()
                .parkingId(p.getId())
                .parkingName(p.getName())
                .totalSlots(total)
                .availableSlots(available)
                .upcomingBookingsCount(0)
                .completedBookingsCount(0)
                .cancelledBookingsCount(0)
                .utilizationPercent(utilization)
                .grossEarnings(BigDecimal.ZERO)
                .fees(BigDecimal.ZERO)
                .netEarnings(BigDecimal.ZERO)
                .recentBookings(Collections.emptyList())
                .build();
    }
}
