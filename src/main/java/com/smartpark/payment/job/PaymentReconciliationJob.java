package com.smartpark.payment.job;

import com.smartpark.payment.repository.PaymentRepository;
import com.smartpark.payment.repository.PayoutLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily reconciliation: compare our payment/payout ledger with Stripe (placeholder).
 * In production: fetch Stripe Balance Transactions / Payouts and report mismatches.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationJob {

    private final PaymentRepository paymentRepository;
    private final PayoutLedgerRepository payoutLedgerRepository;

    @Scheduled(cron = "${payment.job.reconciliation-cron:0 0 2 * * *}")
    public void runReconciliation() {
        try {
            long paymentCount = paymentRepository.count();
            long payoutCount = payoutLedgerRepository.count();
            log.info("Payment reconciliation: {} payments, {} payout ledger entries", paymentCount, payoutCount);
            // TODO: call Stripe API for balance transactions and compare with local records;
            // report differences and create alerts
        } catch (Exception e) {
            log.error("Payment reconciliation failed", e);
        }
    }
}
