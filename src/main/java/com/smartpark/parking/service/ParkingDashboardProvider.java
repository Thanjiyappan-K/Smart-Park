package com.smartpark.parking.service;

import com.smartpark.parking.dto.OwnerDashboardResponse;

import java.util.List;

/**
 * Provides booking/earnings data for owner dashboard.
 * Booking module can implement this; parking module uses a stub until then.
 */
public interface ParkingDashboardProvider {

    /**
     * Get dashboard data for a parking (bookings count, earnings).
     * Returns stub (zeros, empty list) if booking module not integrated.
     */
    OwnerDashboardResponse getDashboardForParking(Long parkingId, Long ownerId);

    /**
     * Get dashboards for all parkings of an owner.
     */
    List<OwnerDashboardResponse> getDashboardsForOwner(Long ownerId);
}
