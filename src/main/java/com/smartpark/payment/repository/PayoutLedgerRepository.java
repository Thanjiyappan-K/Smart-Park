package com.smartpark.payment.repository;

import com.smartpark.payment.entity.PayoutLedger;
import com.smartpark.payment.enums.PayoutLedgerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayoutLedgerRepository extends JpaRepository<PayoutLedger, Long> {

    Page<PayoutLedger> findByOwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);

    Optional<PayoutLedger> findByStripeTransferId(String stripeTransferId);

    Optional<PayoutLedger> findByStripePayoutId(String stripePayoutId);

    List<PayoutLedger> findByStatus(PayoutLedgerStatus status);

    List<PayoutLedger> findByPaymentId(Long paymentId);
}
