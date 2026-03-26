package com.smartpark.payment.repository;

import com.smartpark.payment.entity.Payment;
import com.smartpark.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findTopByBookingIdOrderByCreatedAtDesc(Long bookingId);

    Optional<Payment> findByBookingId(Long bookingId);

    List<Payment> findByBookingIdOrderByCreatedAtDesc(Long bookingId, org.springframework.data.domain.Pageable pageable);

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    Optional<Payment> findByStripeChargeId(String stripeChargeId);

    boolean existsByStripePaymentIntentId(String stripePaymentIntentId);

    List<Payment> findByStatus(PaymentStatus status);
}
