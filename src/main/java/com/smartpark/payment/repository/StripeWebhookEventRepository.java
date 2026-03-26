package com.smartpark.payment.repository;

import com.smartpark.payment.entity.StripeWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StripeWebhookEventRepository extends JpaRepository<StripeWebhookEvent, Long> {

    boolean existsByStripeEventId(String stripeEventId);
}
