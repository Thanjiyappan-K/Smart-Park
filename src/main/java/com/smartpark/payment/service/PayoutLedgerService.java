package com.smartpark.payment.service;

import com.smartpark.payment.dto.PayoutLedgerResponse;
import com.smartpark.payment.entity.PayoutLedger;
import com.smartpark.payment.repository.PayoutLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PayoutLedgerService {

    private final PayoutLedgerRepository payoutLedgerRepository;

    public Page<PayoutLedgerResponse> findByOwnerId(Long ownerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return payoutLedgerRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId, pageable)
                .map(this::toResponse);
    }

    private PayoutLedgerResponse toResponse(PayoutLedger l) {
        return PayoutLedgerResponse.builder()
                .id(l.getId())
                .ownerId(l.getOwnerId())
                .parkingId(l.getParkingId())
                .stripePayoutId(l.getStripePayoutId())
                .stripeTransferId(l.getStripeTransferId())
                .periodStart(l.getPeriodStart())
                .periodEnd(l.getPeriodEnd())
                .grossAmount(l.getGrossAmount())
                .platformFees(l.getPlatformFees())
                .stripeFees(l.getStripeFees())
                .netAmount(l.getNetAmount())
                .status(l.getStatus())
                .createdAt(l.getCreatedAt())
                .paymentId(l.getPaymentId())
                .build();
    }
}
