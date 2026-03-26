# Payment Module (Stripe Connect Express)

## Design decisions (Phase 0)

- **Connect mode**: Stripe Connect **Express** (Stripe handles onboarding & KYC).
- **Flow**: Driver pays the platform; platform charges and transfers net to owner (via Connect). Platform fee is configurable (`payment.platform-fee-percent`).
- **Currency**: Single currency (default `inr`), configurable via `payment.default-currency` / `stripe.currency`.

## Data model

- **payment**: One row per payment attempt; links to `booking_id`, stores `stripe_payment_intent_id`, `stripe_charge_id`, `stripe_account_id` (owner Connect account), `status` (INITIATED / PENDING / SUCCESS / FAILED / REFUNDED / DISPUTED), `refund_amount`, `metadata`, etc.
- **payout_ledger**: Bookkeeping per payout (owner_id, gross_amount, platform_fees, stripe_fees, net_amount, stripe_transfer_id, status).
- **stripe_webhook_event**: Idempotency; stores processed Stripe event IDs.

## API & flow

1. **Driver creates booking** → `POST /booking/driver` → booking status `PENDING_PAYMENT`, slot reserved.
2. **Driver gets PaymentIntent** → `POST /payment/driver/create-intent` with `{ "bookingId": <id> }` → returns `clientSecret`, `paymentIntentId`, `publishableKey`. Optional `idempotencyKey`.
3. **Client confirms payment** (Stripe Elements / Checkout / mobile SDK). SCA/3DS handled by Stripe.
4. **Stripe webhook** `payment_intent.succeeded` → backend verifies signature, dedupes by event id, updates `payment.status=SUCCESS`, calls `BookingService.handlePaymentCallback` → booking → `BOOKED`, creates payout ledger entry and (if owner has Connect account) creates Transfer to owner.
5. **Payment failed** → `payment_intent.payment_failed` webhook → `payment.status=FAILED`, booking → `CANCELLED`, slot released.

## Endpoints

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | `/payment/driver/create-intent` | DRIVER | Create PaymentIntent for a booking (body: `bookingId`, optional `idempotencyKey`). |
| GET | `/payment/by-booking/{bookingId}` | DRIVER / OWNER | Get payment for a booking. |
| GET | `/payment/{paymentId}` | DRIVER / OWNER / ADMIN | Get payment by id. |
| POST | `/payment/refund` | ADMIN / PARKING_OWNER | Refund (body: `paymentId`, optional `amount`, `reason`). |
| GET | `/payment/owner/connect/onboard` | PARKING_OWNER | Start Stripe Connect onboarding; returns redirect URL. |
| GET | `/payment/owner/connect/success` | PARKING_OWNER | Success redirect (no-op). |
| GET | `/payment/owner/payouts` | PARKING_OWNER | List payout ledger (paginated). |
| POST | `/webhooks/stripe` | (public) | Stripe webhook; **no JWT**; verify `Stripe-Signature`. |

## Configuration (application.yml / env)

```yaml
stripe:
  secret-key: ${STRIPE_SECRET_KEY}
  publishable-key: ${STRIPE_PUBLISHABLE_KEY}
  webhook-signing-secret: ${STRIPE_WEBHOOK_SECRET}
  connect:
    success-url: ${STRIPE_CONNECT_SUCCESS_URL}
    refresh-url: ${STRIPE_CONNECT_REFRESH_URL}
  currency: inr

payment:
  platform-fee-percent: 10
  default-currency: inr
  job:
    reconciliation-cron: "0 0 2 * * *"
```

Set env vars (or use Stripe test keys in dev):

- `STRIPE_SECRET_KEY` (e.g. `sk_test_...`)
- `STRIPE_PUBLISHABLE_KEY` (e.g. `pk_test_...`)
- `STRIPE_WEBHOOK_SECRET` (from Stripe Dashboard → Webhooks → endpoint signing secret)
- `STRIPE_CONNECT_SUCCESS_URL` / `STRIPE_CONNECT_REFRESH_URL` (e.g. frontend URLs for owner post-onboarding)

## Webhook events handled

- `payment_intent.succeeded` → payment SUCCESS, booking BOOKED, payout ledger + transfer (if owner connected).
- `payment_intent.payment_failed` → payment FAILED, booking CANCELLED, slot released.
- `charge.refunded` → logged (refund amount updated via refund API).
- `charge.dispute.created` → payment status DISPUTED.

Webhook handler: verifies `Stripe-Signature`, deduplicates by event id, then processes. Return 200 quickly; processing is synchronous but idempotent.

## Refunds

- **Full or partial**: `POST /payment/refund` with `paymentId` and optional `amount`, `reason`.
- Allowed for ADMIN or the parking owner. Refund is created in Stripe; `payment.refund_amount` and `payment.status` (REFUNDED when full) updated.

## Connect onboarding (owners)

1. Owner calls `GET /payment/owner/connect/onboard`.
2. Backend creates Express account (if not already) and Account Link, returns `url`.
3. Frontend redirects owner to `url`; Stripe hosts onboarding/KYC.
4. On return, owner’s `stripe_account_id` is already stored; future payouts use Transfers to this account.

## Reconciliation & jobs

- **Pending payment cleanup**: Existing `PendingPaymentCleanupJob` (booking module) cancels stale `PENDING_PAYMENT` bookings and releases slots.
- **Payment reconciliation job**: `PaymentReconciliationJob` runs daily (cron); currently logs counts; can be extended to compare with Stripe Balance Transactions and alert on mismatches.

## Testing

- Use Stripe **test** keys and **test** webhook secret.
- Stripe CLI: `stripe listen --forward-to localhost:8080/webhooks/stripe` and `stripe trigger payment_intent.succeeded` (etc.).
- Test cards: `4242 4242 4242 4242` (success), `4000 0000 0000 3220` (3DS), `4000 0000 0000 9995` (decline).

## Security

- Store keys in env or secret manager; never commit.
- Webhook endpoint: only trust events with valid `Stripe-Signature`.
- Idempotency: use idempotency keys for PaymentIntent/Refund/Transfer creation; webhook dedupe by event id.
- Do not log full PaymentIntent/Charge objects (may contain sensitive data).
