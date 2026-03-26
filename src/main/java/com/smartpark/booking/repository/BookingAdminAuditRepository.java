package com.smartpark.booking.repository;

import com.smartpark.booking.entity.BookingAdminAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingAdminAuditRepository extends JpaRepository<BookingAdminAudit, Long> {

    List<BookingAdminAudit> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
