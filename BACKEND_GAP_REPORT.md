# Backend Gap Report

Date: 2026-03-24
Scope: Backend modules under `src/main/java/com/smartpark`

## 1) Implemented

### Auth and Security
- JWT authentication flow is implemented (register/login/refresh/logout).
- Role-based authorization and JWT request filtering are wired in security config.
- Admin user moderation flows are implemented (verify owners, list pending, block/unblock).
- Token blacklist cleanup support exists.

### Parking
- Public parking endpoints are implemented (search/list/details).
- Owner parking management endpoints are implemented (create/update/list/get/activate/deactivate).
- Admin parking moderation endpoints are implemented (pending/approve/reject/force-disable).
- Core parking service includes geo search and slot reserve/release hooks.
- Parking scheduled jobs exist for stats/reconciliation routines.

### Booking
- Booking controllers exist for owner/admin/internal callback flows.
- Booking business logic is implemented for create/cancel/complete and payment state transitions.
- Booking cleanup and reconciliation scheduled jobs are present.
- Booking dashboard provider integration is present and set as primary for owner dashboard usage.

### Payment
- Payment APIs are implemented (create intent, fetch by booking/id, refund).
- Stripe webhook verification and event handling are implemented.
- Stripe Connect onboarding and payout listing endpoints are implemented.
- Payout ledger and transfer flow is integrated with payment success handling.

## 2) Missing / Pending

### High Priority Gaps
- Driver booking API controller/routes are missing:
  - Security/docs reference `/booking/driver/**`.
  - Driver operations exist in service layer but are not exposed as API routes.
- Booking API documentation is out of sync with actual implementation:
  - Some documented paths/verbs/params do not match current controllers.
- Automated backend test coverage is missing:
  - No meaningful `src/test` coverage found for auth, booking, payment critical paths.

### Medium Priority Gaps
- Payment reconciliation job is a placeholder:
  - Current behavior logs counts; full Stripe-vs-local reconciliation is not implemented.
- Internal callback hardening is incomplete:
  - Docs mention internal key header validation, but callback route does not enforce it.
- Schema evolution process is weak:
  - Structured DB migrations (Flyway/Liquibase) are not evident; environment appears to rely on `ddl-auto: update`.

### Low Priority Gaps
- Stub dashboard provider class still exists (overridden at runtime, but adds maintenance ambiguity).
- README/module docs contain drift (some sections still imply backend modules are pending).

## 3) Recommended Next 10 Tasks (Closure Plan)

1. Implement booking driver controller (`/booking/driver`) and wire to existing service methods.
2. Align booking API contracts: update code or docs so route definitions and payload contracts match exactly.
3. Add auth integration tests: register/login/refresh/logout + role authorization checks.
4. Add booking integration tests: create/cancel/complete/payment callback + timeout behavior.
5. Add payment integration tests: intent/refund/webhook success-failure-dispute flows.
6. Implement real payment reconciliation job against Stripe data (with mismatch reporting and alerting).
7. Enforce internal callback authentication (`X-Internal-Key`) and add negative tests.
8. Introduce DB migration tooling (Flyway or Liquibase) and create baseline migration scripts.
9. Remove or clearly deprecate unused stub dashboard provider to reduce confusion.
10. Refresh `README` and testing guides to reflect current backend reality and exact endpoint contracts.

## 4) Suggested Tracking Fields (Optional)

Use these fields per item in your tracker:
- Owner
- Priority (H/M/L)
- Status (`todo` / `in-progress` / `done`)
- ETA
- PR link
- Test evidence
- Release target

