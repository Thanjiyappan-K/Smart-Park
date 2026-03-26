package com.smartpark.booking.repository;

import com.smartpark.booking.entity.Booking;
import com.smartpark.booking.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByDriverIdOrderByCreatedAtDesc(Long driverId, Pageable pageable);

    List<Booking> findByDriverIdAndStatus(Long driverId, BookingStatus status);

    Page<Booking> findByDriverIdAndStatusOrderByCreatedAtDesc(Long driverId, BookingStatus status, Pageable pageable);

    Page<Booking> findByParkingIdOrderByStartTimeDesc(Long parkingId, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.parkingId = :parkingId AND b.status IN :statuses " +
           "AND (b.startTime < :endTime AND b.endTime > :startTime)")
    List<Booking> findOverlappingBookings(
            @Param("parkingId") Long parkingId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("statuses") List<BookingStatus> statuses);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.parkingId = :parkingId AND b.status IN :statuses " +
           "AND (b.startTime < :endTime AND b.endTime > :startTime)")
    long countOverlappingBookings(
            @Param("parkingId") Long parkingId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("statuses") List<BookingStatus> statuses);

    List<Booking> findByStatusAndPendingPaymentExpiresAtBefore(
            BookingStatus status, LocalDateTime expiresAt);

    List<Booking> findByStatusAndEndTimeBefore(BookingStatus status, LocalDateTime endTime);

    Page<Booking> findByParkingIdInOrderByStartTimeDesc(List<Long> parkingIds, Pageable pageable);

    long countByParkingIdAndStatusIn(Long parkingId, List<BookingStatus> statuses);

    Optional<Booking> findByIdAndDriverId(Long id, Long driverId);
}
