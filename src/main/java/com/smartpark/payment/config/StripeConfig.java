package com.smartpark.payment.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Slf4j
public class StripeConfig {

    @Value("${stripe.secret-key:}")
    private String secretKey;

    @Value("${stripe.publishable-key:}")
    private String publishableKey;

    @Value("${stripe.webhook-signing-secret:}")
    private String webhookSigningSecret;

    @Value("${stripe.connect.success-url:}")
    private String connectSuccessUrl;

    @Value("${stripe.connect.refresh-url:}")
    private String connectRefreshUrl;

    @Value("${payment.default-currency:inr}")
    private String defaultCurrency;

    @PostConstruct
    public void init() {
        if (secretKey != null && !secretKey.isBlank() && !secretKey.startsWith("sk_")) {
            // Placeholder in config - do not set Stripe key (allows app to start without key)
            log.warn("⚠️  Stripe is not configured. Payment features will not work. Set STRIPE_SECRET_KEY in .env file for production.");
            return;
        }
        if (secretKey != null && !secretKey.isBlank()) {
            Stripe.apiKey = secretKey;
            log.info("✓ Stripe API configured and ready");
        }
    }

    public boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank() && secretKey.startsWith("sk_");
    }
}
