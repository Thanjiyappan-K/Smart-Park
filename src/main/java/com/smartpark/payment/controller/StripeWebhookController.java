package com.smartpark.payment.controller;

import com.smartpark.payment.service.StripeService;
import com.smartpark.payment.service.StripeWebhookHandler;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Stripe webhook endpoint. Must be publicly reachable by Stripe (no JWT).
 * Verify Stripe-Signature and process events idempotently.
 */
@RestController
@RequestMapping("/webhooks/stripe")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final StripeService stripeService;
    private final StripeWebhookHandler webhookHandler;

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature) {
        String signingSecret = stripeService.getWebhookSigningSecret();
        if (signingSecret == null || signingSecret.isBlank()) {
            log.warn("Stripe webhook signing secret not configured");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook not configured");
        }
        if (stripeSignature == null || stripeSignature.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing Stripe-Signature");
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, stripeSignature, signingSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid webhook signature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }
        if (webhookHandler.isDuplicate(event.getId())) {
            return ResponseEntity.ok("OK");
        }
        try {
            webhookHandler.handleEvent(event);
        } catch (Exception e) {
            log.error("Webhook processing failed for event {}", event.getId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Processing failed");
        }
        return ResponseEntity.ok("OK");
    }
}
