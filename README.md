# SmartPark

Parking management backend: registration and login, parking discovery and owner workflows, bookings with scheduled lifecycle jobs, and **Stripe Connect** payments (intents, webhooks, refunds, owner payouts). All APIs are served under a single context path.

## Features

### Authentication and users

- **User registration**
  - Register as **DRIVER** (status **ACTIVE** immediately).
  - Register as **PARKING_OWNER** (status **PENDING_VERIFICATION** until an admin verifies the account).

- **Authentication**
  - Login with **email or phone** and password.
  - **JWT** access tokens and **refresh tokens** (configurable expiry).
  - **Logout** with optional `Authorization: Bearer` header to blacklist the access token.

- **Authorization**
  - Roles: **DRIVER**, **PARKING_OWNER**, **ADMIN**.
  - Stateless **JWT filter** on protected routes.
  - **Method-level security** with `@PreAuthorize` where used (e.g. admin user APIs).

- **Admin (users)**
  - Verify parking owners, list pending owners, block/unblock users.

- **Security**
  - **BCrypt** password encoding.
  - **Token blacklist** for logout.
  - Scheduled cleanup of expired blacklist entries (existing auth behavior).

### Parking

- **Public discovery** (no JWT)
  - **Geographic search**: latitude, longitude, radius (km), optional filters (vehicle type, price range, city), pagination.
  - **Details by id** for published, active, approved listings.

- **Owner workspace** (JWT, **PARKING_OWNER**)
  - Create and update **parking spaces** (location, slots, pricing, vehicle type, images metadata as modeled).
  - List own parkings (paginated); get single parking for owner.
  - **Activate / deactivate** a parking (owner-controlled availability).
  - **Per-parking dashboard** and **aggregate dashboards** (revenue/booking-oriented summaries via dashboard provider).

- **Admin parking moderation** (JWT, **ADMIN**)
  - List **pending verification** listings.
  - **Approve** (publish) or **reject** (reason required for reject).
  - **Force-disable** a parking (with audit trail via admin actions).

- **Operational extras**
  - **Redis-backed** caching for availability-related reads (TTL configurable).
  - **Scheduled jobs**: parking reconciliation (e.g. slot consistency), daily stats rollups (cron in `application.yml`).

### Booking

- **Driver** (JWT, **DRIVER**)
  - **Create booking** for a time window (`parkingId`, `startTime`, `endTime`); transitions through **PENDING_PAYMENT** after creation.
  - **Cancel** booking (rules respect cancellation cutoff configuration).
  - **Get by id**, **paginated history**, list **pending payment** bookings.

- **Owner** (JWT, **PARKING_OWNER**)
  - List bookings for **one parking** or **all parkings** owned (paginated).

- **Admin** (JWT, **ADMIN**)
  - Paginated **all bookings**, **get by id**, **force-cancel** with audited reason.

- **Internal integration**
  - **Payment callback** (`/booking/internal/payment-callback`) for transitioning booking state after payment success/failure (authenticated; intended for trusted callers / payment module).

- **Scheduled jobs**
  - Cleanup of stale **pending payment** bookings, **completion** of elapsed sessions, **reconciliation** (cron values in `application.yml`).

### Payments (Stripe)

- **Driver**
  - **Create PaymentIntent** for a booking in **PENDING_PAYMENT** (`clientSecret` / publishable key flow for the client).

- **Shared**
  - **Get payment** by id or **by booking id** (access enforced: driver or owning parking owner as applicable).
  - **Refund** (admin or owner flow as implemented in `PaymentService`).

- **Parking owners — Stripe Connect Express**
  - **Onboarding URL** (`/payment/owner/connect/onboard`) to create/link Connect account; **success** return URL handler.
  - **Payout ledger**: paginated list of payout records for the authenticated owner.

- **Platform**
  - **Stripe webhooks** at `/webhooks/stripe` (signature verification, idempotent processing).
  - **Platform fee** percentage and default currency configurable.
  - **Payment reconciliation** scheduled job.

### Observability and configuration

- **Spring Boot Actuator**: health, info, metrics, prometheus (see `management.endpoints` in `application.yml`).
- **Structured logging** to console and rolling file (`logs/smartpark.log`).
- **`.env` support**: project-root `.env` is loaded at startup into system properties (does not override OS environment variables).

## Tech stack

| Layer | Technology |
|--------|------------|
| Runtime | Java 17, Spring Boot 3.2.x |
| Web & validation | Spring Web, Bean Validation |
| Security | Spring Security, JWT (JJWT 0.12.3) |
| Persistence | Spring Data JPA, Hibernate, MySQL 8 |
| Cache | Spring Data **Redis** |
| Payments | **Stripe Java** SDK (Connect, PaymentIntents, Webhooks) |
| Config | `application.yml`, environment variables, **dotenv-java** |
| Observability | **Spring Boot Actuator** |
| Utilities | Lombok |

## Base URL

All REST paths below are relative to:

`http://localhost:8080/api`

Example: login is `POST http://localhost:8080/api/auth/login`.

## Project structure (high level)

```
com.smartpark
├── SmartParkApplication.java
├── config
│   └── SchedulerConfig.java
├── auth
│   ├── controller      (AuthController, AdminController)
│   ├── dto
│   ├── entity          (RefreshToken, TokenBlacklist)
│   ├── jwt             (JwtService)
│   ├── repository
│   └── service         (Auth, Admin, Blacklist)
├── user
│   ├── entity          (User)
│   ├── enums           (Role, UserStatus)
│   └── repository
├── parking
│   ├── controller      (Public, Owner, Admin)
│   ├── dto
│   ├── entity
│   ├── enums
│   ├── exception
│   ├── job
│   ├── repository
│   └── service
├── booking
│   ├── controller      (Driver, Owner, Admin, Internal callback)
│   ├── dto
│   ├── entity
│   ├── enums
│   ├── job
│   ├── repository
│   └── service
├── payment
│   ├── config          (StripeConfig)
│   ├── controller      (Payment, Connect, Payout, StripeWebhook)
│   ├── dto
│   ├── entity
│   ├── enums
│   ├── exception
│   ├── job
│   ├── repository
│   └── service
└── common
    ├── exception       (GlobalExceptionHandler)
    ├── response        (ApiResponse)
    └── security        (SecurityConfig, JwtAuthFilter)
```

## Database setup

1. **Create database** (or rely on `createDatabaseIfNotExist` in JDBC URL):

```sql
CREATE DATABASE smartpark;
```

2. **Credentials** via environment or `.env` (see [Configuration](#configuration)). Hibernate uses `ddl-auto: update` for schema evolution in dev.

## Configuration

### Environment variables (common)

| Variable | Purpose |
|----------|---------|
| `DB_URL` | JDBC URL (default in yml points at local MySQL `smartpark`) |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password (**required** unless set elsewhere) |
| `JWT_SECRET` | Signing secret for JWT (use a long random string in production) |
| `JWT_EXPIRATION` | Access token TTL (ms) |
| `JWT_REFRESH_EXPIRATION` | Refresh token TTL (ms) |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_TIMEOUT` | Redis connection |
| `STRIPE_SECRET_KEY`, `STRIPE_PUBLISHABLE_KEY`, `STRIPE_WEBHOOK_SECRET` | Stripe API and webhook verification |
| `STRIPE_CONNECT_SUCCESS_URL`, `STRIPE_CONNECT_REFRESH_URL` | Connect onboarding return URLs |

Module-specific knobs (platform fee %, booking timeouts, cron expressions, cache TTL) live under `payment`, `booking`, `parking` in `application.yml`.

## API reference

Standard envelope:

```json
{
  "success": true,
  "message": "optional",
  "data": { }
}
```

### Public (no JWT)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/auth/register/driver` | Register driver |
| POST | `/auth/register/owner` | Register owner (pending verification) |
| POST | `/auth/login` | Login; returns `token`, `refreshToken`, `role` |
| POST | `/auth/refresh` | New access token from refresh token body |
| POST | `/auth/logout` | Optionally send `Authorization: Bearer` to blacklist token |
| POST | `/parking/public/search` | Geo search (JSON body: lat, lon, radiusKm, optional filters, page, size) |
| GET | `/parking/public/{parkingId}` | Public parking details |
| POST | `/webhooks/stripe` | Stripe webhook (raw body + `Stripe-Signature` header) |

### Admin — users (`Authorization: Bearer`, role **ADMIN**)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/admin/verify-owner/{id}` | Approve owner account |
| GET | `/admin/pending-owners` | List owners awaiting verification |
| POST | `/admin/block-user/{id}` | Block user |
| POST | `/admin/unblock-user/{id}` | Unblock user |

### Admin — parkings (JWT, **ADMIN**)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/parking/admin/pending` | Pending parkings (`page`, `size`) |
| POST | `/parking/admin/approve` | Body: `parkingId` |
| POST | `/parking/admin/reject` | Body: `parkingId`, `reason` (required) |
| POST | `/parking/admin/force-disable` | Body: `parkingId`, optional `reason` |

### Owner — parkings (JWT, **PARKING_OWNER**)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/parking/owner` | Create parking |
| GET | `/parking/owner` | List my parkings |
| GET | `/parking/owner/{parkingId}` | Get my parking |
| PUT | `/parking/owner/{parkingId}` | Update |
| POST | `/parking/owner/{parkingId}/activate` | Activate |
| POST | `/parking/owner/{parkingId}/deactivate` | Deactivate |
| GET | `/parking/owner/{parkingId}/dashboard` | Dashboard for one parking |
| GET | `/parking/owner/dashboards` | All my dashboards |

### Driver — bookings (JWT, **DRIVER**)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/booking/driver` | Create booking (`parkingId`, `startTime`, `endTime`) |
| POST | `/booking/driver/{bookingId}/cancel` | Cancel |
| GET | `/booking/driver/{bookingId}` | Detail |
| GET | `/booking/driver/history` | Paginated history |
| GET | `/booking/driver/pending-payment` | Bookings awaiting payment |

### Owner — bookings (JWT, **PARKING_OWNER**)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/booking/owner/parking/{parkingId}` | Bookings for a parking |
| GET | `/booking/owner` | All bookings for owner’s parkings |

### Admin — bookings (JWT, **ADMIN**)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/booking/admin` | All bookings (paged) |
| GET | `/booking/admin/{bookingId}` | Detail |
| POST | `/booking/admin/force-cancel` | Body: booking id + reason (audited) |

### Internal — booking (JWT, any authenticated principal)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/booking/internal/payment-callback` | Payment outcome callback for booking state transitions |

### Payments (JWT; roles enforced per endpoint)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/payment/driver/create-intent` | **DRIVER** — body: `bookingId`, optional `idempotencyKey` |
| GET | `/payment/by-booking/{bookingId}` | Payment for booking (authorized parties) |
| GET | `/payment/{paymentId}` | Payment by id |
| POST | `/payment/refund` | Refund request (owner/admin per service rules) |
| GET | `/payment/owner/connect/onboard` | **PARKING_OWNER** — Stripe Connect onboarding URL |
| GET | `/payment/owner/connect/success` | Post-onboarding landing (success message) |
| GET | `/payment/owner/payouts` | **PARKING_OWNER** — paged payout ledger |

## User roles and statuses

**Roles**

- **DRIVER**: Search parkings, book, pay, cancel within rules.
- **PARKING_OWNER**: Manage listings, see bookings, Connect onboarding, payouts, refunds where allowed.
- **ADMIN**: Verify owners, moderate parkings, manage users, view all bookings, force-cancel.

**User status**

- **ACTIVE**: Can sign in.
- **PENDING_VERIFICATION**: Owner awaiting admin verification.
- **BLOCKED**: Cannot sign in.

**Parking verification** (`VerificationStatus`): **PENDING**, **APPROVED**, **REJECTED**.

**Booking status** (simplified flow): **INITIATED** → **PENDING_PAYMENT** → **BOOKED** → **COMPLETED**; terminal states include **CANCELLED**, **REFUNDED**, **NO_SHOW** (see `BookingStatus` in code).

## Security notes

1. **Passwords**: BCrypt.
2. **JWT**: Access + refresh; blacklist on logout when token is sent.
3. **Stripe webhooks**: Verify `Stripe-Signature`; events processed idempotently.
4. **Production**: Set strong `JWT_SECRET`, real Stripe keys, restrict `/booking/internal` to internal networks or shared secrets if you harden beyond default JWT-only auth.

## Creating an admin user

**SQL example** (hash below is illustrative; generate your own BCrypt hash for production):

```sql
INSERT INTO users(name, email, phone, password, role, status, created_at, updated_at)
VALUES (
  'Admin',
  'admin@smartpark.com',
  '9999999999',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
  'ADMIN',
  'ACTIVE',
  NOW(),
  NOW()
);
```

## Running the application

1. Start **MySQL** and **Redis** (Redis used for parking availability cache).
2. Set `DB_PASSWORD` and other env vars (or `.env`).
3. From project root:

```bash
mvn spring-boot:run
```

4. App listens on port **8080** with context path **`/api`**.

## Postman / client tips

1. Register and login; copy `token` from `data.token`.
2. Send header `Authorization: Bearer <token>` on protected routes.
3. Driver flow: search → create booking → `POST /payment/driver/create-intent` → complete payment on client with Stripe → webhooks update backend state.
4. Owner flow: create parking → wait for admin approval → activate listing → connect Stripe via `/payment/owner/connect/onboard`.

## Roadmap / not in this repo

- **Review / ratings** module for parkings or owners is not implemented yet; JWT and roles are ready for future endpoints.

