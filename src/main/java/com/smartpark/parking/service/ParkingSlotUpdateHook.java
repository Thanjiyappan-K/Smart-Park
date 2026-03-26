package com.smartpark.parking.service;

/**
 * Hook for booking module: decrement/increment available_slots.
 * Parking module exposes this; booking module calls it on book/cancel/complete.
 */
public interface ParkingSlotUpdateHook {

    /**
     * Reserve one slot (decrement available_slots). Call on booking success.
     * @return true if slot was reserved, false if no slot available or concurrent update
     */
    boolean reserveSlot(Long parkingId, Long version);

    /**
     * Release one slot (increment available_slots). Call on cancel or complete.
     */
    void releaseSlot(Long parkingId);
}
