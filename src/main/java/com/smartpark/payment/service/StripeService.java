package com.smartpark.payment.service;

import com.smartpark.payment.config.StripeConfig;
import com.smartpark.payment.exception.PaymentErrorCode;
import com.smartpark.payment.exception.PaymentException;
import com.stripe.exception.StripeException;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.TransferCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.AccountCreateParams;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.Transfer;
import com.stripe.model.AccountLink;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Low-level Stripe API operations. Uses platform secret key.
 * For Connect: we charge on platform then transfer to connected account.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService {

    private final StripeConfig stripeConfig;

    /**
     * Convert amount to Stripe smallest currency unit (e.g. paise for INR, cents for USD).
     */
    public long toStripeAmount(BigDecimal amount, String currency) {
        if (amount == null) return 0L;
        String c = (currency == null || currency.isBlank()) ? stripeConfig.getDefaultCurrency() : currency.toLowerCase();
        // Zero-decimal currencies (e.g. JPY): no multiplication
        if ("jpy".equals(c) || "krw".equals(c)) {
            return amount.longValue();
        }
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }

    /**
     * Create a PaymentIntent on the platform account.
     *
     * @param amountInSmallestUnit amount in paise/cents
     * @param currency             e.g. "inr", "usd"
     * @param metadata             booking_id, owner_id, etc.
     * @param idempotencyKey       optional idempotency key
     */
    public PaymentIntent createPaymentIntent(
            long amountInSmallestUnit,
            String currency,
            Map<String, String> metadata,
            String idempotencyKey) throws StripeException {
        if (!stripeConfig.isConfigured()) {
            throw new PaymentException(PaymentErrorCode.STRIPE_ERROR, "Stripe is not configured");
        }
        PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                .setAmount(amountInSmallestUnit)
                .setCurrency(currency)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build())
                .putAllMetadata(metadata != null ? metadata : new HashMap<>());
        PaymentIntentCreateParams params = builder.build();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return PaymentIntent.create(params,
                    com.stripe.net.RequestOptions.builder().setIdempotencyKey(idempotencyKey).build());
        }
        return PaymentIntent.create(params);
    }

    /**
     * Create a Refund for a charge (by payment intent id).
     */
    public Refund createRefund(String paymentIntentId, Long amountInSmallestUnit, String reason) throws StripeException {
        if (!stripeConfig.isConfigured()) {
            throw new PaymentException(PaymentErrorCode.STRIPE_ERROR, "Stripe is not configured");
        }
        RefundCreateParams.Builder b = RefundCreateParams.builder().setPaymentIntent(paymentIntentId);
        if (amountInSmallestUnit != null && amountInSmallestUnit > 0) {
            b.setAmount(amountInSmallestUnit);
        }
        if (reason != null && !reason.isBlank()) {
            b.setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER);
        }
        return Refund.create(b.build());
    }

    /**
     * Transfer funds from platform to a connected account (owner).
     * Use after charge is captured on platform.
     */
    public Transfer createTransfer(String destinationStripeAccountId, long amountInSmallestUnit, String currency,
                                   String description, String idempotencyKey) throws StripeException {
        if (!stripeConfig.isConfigured()) {
            throw new PaymentException(PaymentErrorCode.STRIPE_ERROR, "Stripe is not configured");
        }
        TransferCreateParams params = TransferCreateParams.builder()
                .setAmount(amountInSmallestUnit)
                .setCurrency(currency)
                .setDestination(destinationStripeAccountId)
                .setDescription(description != null ? description : "SmartPark payout")
                .build();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return Transfer.create(params,
                    com.stripe.net.RequestOptions.builder().setIdempotencyKey(idempotencyKey).build());
        }
        return Transfer.create(params);
    }

    /**
     * Create Express account link for Connect onboarding (redirect owner to Stripe).
     */
    public String createAccountLink(String stripeAccountId) throws StripeException {
        if (!stripeConfig.isConfigured()) {
            throw new PaymentException(PaymentErrorCode.STRIPE_ERROR, "Stripe is not configured");
        }
        AccountLinkCreateParams params = AccountLinkCreateParams.builder()
                .setAccount(stripeAccountId)
                .setRefreshUrl(stripeConfig.getConnectRefreshUrl())
                .setReturnUrl(stripeConfig.getConnectSuccessUrl())
                .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                .build();
        AccountLink link = AccountLink.create(params);
        return link.getUrl();
    }

    /**
     * Create a Connect Express account (for platform).
     */
    public com.stripe.model.Account createConnectExpressAccount(String email, String country) throws StripeException {
        if (!stripeConfig.isConfigured()) {
            throw new PaymentException(PaymentErrorCode.STRIPE_ERROR, "Stripe is not configured");
        }
        AccountCreateParams.Builder b = AccountCreateParams.builder()
                .setType(AccountCreateParams.Type.EXPRESS)
                .setEmail(email);
        if (country != null && !country.isBlank()) {
            b.setCountry(country);
        }
        return com.stripe.model.Account.create(b.build());
    }

    public String getPublishableKey() {
        return stripeConfig.getPublishableKey();
    }

    public String getWebhookSigningSecret() {
        return stripeConfig.getWebhookSigningSecret();
    }

    public boolean isConfigured() {
        return stripeConfig.isConfigured();
    }
}
