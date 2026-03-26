# API Testing Guide - SmartPark Booking Module

## Prerequisites

1. **Authentication**: You need valid JWT tokens for a DRIVER, OWNER, and ADMIN user
2. **Parking Setup**: At least one APPROVED and ACTIVE parking must exist
3. **Application Running**: `mvn spring-boot:run`

## Quick Setup

### 1. Create Test Users

#### Register a Driver
```http
POST http://localhost:8080/auth/register/driver
Content-Type: application/json

{
  "name": "John Driver",
  "email": "driver@test.com",
  "phone": "9876543210",
  "password": "password123"
}
```

#### Register a Parking Owner
```http
POST http://localhost:8080/auth/register/owner
Content-Type: application/json

{
  "name": "Jane Owner",
  "email": "owner@test.com",
  "phone": "9123456789",
  "password": "password123"
}
```

#### Create/Verify Admin
```sql
INSERT INTO users(name, email, phone, password, role, status, created_at, updated_at)
VALUES (
  'Admin User',
  'admin@smartpark.com',
  '9999999999',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
  'ADMIN',
  'ACTIVE',
  NOW(),
  NOW()
);
```
Password: `admin123`

### 2. Login and Save Tokens

#### Login as Driver
```http
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "emailOrPhone": "driver@test.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "role": "DRIVER"
  }
}
```

#### Login as Owner
```http
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "emailOrPhone": "owner@test.com",
  "password": "password123"
}
```

#### Login as Admin
```http
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "emailOrPhone": "admin@smartpark.com",
  "password": "admin123"
}
```

### 3. Get Parking ID

```sql
SELECT id, name, owner_id, status, is_published FROM parkings 
WHERE status = 'ACTIVE' AND is_published = true LIMIT 1;
```

**Note down the parking `id` and `owner_id` for testing.**

---

## Driver APIs - `/booking/driver/**`

### 1. Create Booking

Create a new parking slot booking as a driver.

```http
POST http://localhost:8080/booking/driver/create
Authorization: Bearer <DRIVER_JWT_TOKEN>
Content-Type: application/json

{
  "parkingId": 1,
  "startTime": "2026-02-10T10:00:00",
  "endTime": "2026-02-10T12:00:00"
}
```

**Sample Response (Success):**
```json
{
  "success": true,
  "message": "Booking created successfully",
  "data": {
    "id": 101,
    "driverId": 5,
    "parkingId": 1,
    "startTime": "2026-02-10T10:00:00",
    "endTime": "2026-02-10T12:00:00",
    "status": "PENDING_PAYMENT",
    "totalAmount": 200.00,
    "parkingVersionAtBooking": 1,
    "paymentReference": null,
    "failureReason": null,
    "cancelledAt": null,
    "cancelledBy": null,
    "pendingPaymentExpiresAt": "2026-02-10T10:05:00",
    "createdAt": "2026-02-06T14:30:00",
    "updatedAt": "2026-02-06T14:30:00"
  }
}
```

**Sample Response (Validation Errors):**
```json
{
  "success": false,
  "message": "SLOT_UNAVAILABLE",
  "errorCode": "SLOT_UNAVAILABLE",
  "details": "No available slots for the requested time window"
}
```

**Status Codes & Error Scenarios:**
| Status | Scenario | ErrorCode |
|--------|----------|-----------|
| 201 | Booking created successfully | - |
| 400 | Start time >= End time | INVALID_TIME_RANGE |
| 400 | Parking not found | PARKING_NOT_FOUND |
| 400 | Parking is inactive/unapproved | PARKING_INACTIVE |
| 400 | Start/End outside parking hours | INVALID_TIME_RANGE |
| 409 | No available slots | SLOT_UNAVAILABLE |
| 409 | Time overlap with existing bookings | TIME_OVERLAP |
| 409 | Concurrent booking (version conflict) | CONCURRENT_BOOKING |
| 401 | Unauthorized (missing/invalid token) | - |
| 403 | Forbidden (not a DRIVER) | - |

---

### 2. Get Booking Details

Retrieve details of a specific booking owned by the driver.

```http
GET http://localhost:8080/booking/driver/101
Authorization: Bearer <DRIVER_JWT_TOKEN>
```

**Sample Response:**
```json
{
  "success": true,
  "data": {
    "id": 101,
    "driverId": 5,
    "parkingId": 1,
    "startTime": "2026-02-10T10:00:00",
    "endTime": "2026-02-10T12:00:00",
    "status": "BOOKED",
    "totalAmount": 200.00,
    "parkingVersionAtBooking": 1,
    "paymentReference": "PAY_123456789",
    "failureReason": null,
    "cancelledAt": null,
    "cancelledBy": null,
    "pendingPaymentExpiresAt": null,
    "createdAt": "2026-02-06T14:30:00",
    "updatedAt": "2026-02-06T14:35:00"
  }
}
```

**Error Scenarios:**
| Status | Scenario | ErrorCode |
|--------|----------|-----------|
| 200 | Booking found | - |
| 404 | Booking not found | BOOKING_NOT_FOUND |
| 403 | Booking belongs to another driver | UNAUTHORIZED_BOOKING_ACCESS |
| 401 | Unauthorized | - |

---

### 3. Cancel Booking

Cancel a booking. Only allowed before the cancellation cutoff (default: 60 minutes before start).

```http
POST http://localhost:8080/booking/driver/cancel/101
Authorization: Bearer <DRIVER_JWT_TOKEN>
Content-Type: application/json

{
  "reason": "Change of plans"
}
```

**Sample Response (Success):**
```json
{
  "success": true,
  "message": "Booking cancelled successfully",
  "data": {
    "id": 101,
    "driverId": 5,
    "parkingId": 1,
    "startTime": "2026-02-10T10:00:00",
    "endTime": "2026-02-10T12:00:00",
    "status": "CANCELLED",
    "totalAmount": 200.00,
    "parkingVersionAtBooking": 1,
    "paymentReference": "PAY_123456789",
    "failureReason": null,
    "cancelledAt": "2026-02-06T14:45:00",
    "cancelledBy": "DRIVER",
    "pendingPaymentExpiresAt": null,
    "createdAt": "2026-02-06T14:30:00",
    "updatedAt": "2026-02-06T14:45:00"
  }
}
```

**Error Scenarios:**
| Status | Scenario | ErrorCode |
|--------|----------|-----------|
| 200 | Successfully cancelled | - |
| 404 | Booking not found | BOOKING_NOT_FOUND |
| 403 | Not booking owner | UNAUTHORIZED_BOOKING_ACCESS |
| 409 | Too close to start time | CANNOT_CANCEL_AFTER_CUTOFF |
| 409 | Invalid status transition (already cancelled/refunded) | INVALID_STATUS_TRANSITION |
| 401 | Unauthorized | - |

---

### 4. Get Booking History

Retrieve paginated list of all driver's bookings.

```http
GET http://localhost:8080/booking/driver/history?page=0&size=10&sort=createdAt,desc
Authorization: Bearer <DRIVER_JWT_TOKEN>
```

**Sample Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 101,
        "driverId": 5,
        "parkingId": 1,
        "startTime": "2026-02-10T10:00:00",
        "endTime": "2026-02-10T12:00:00",
        "status": "BOOKED",
        "totalAmount": 200.00,
        "parkingVersionAtBooking": 1,
        "paymentReference": "PAY_123456789",
        "createdAt": "2026-02-06T14:30:00"
      },
      {
        "id": 100,
        "driverId": 5,
        "parkingId": 2,
        "startTime": "2026-02-08T14:00:00",
        "endTime": "2026-02-08T16:00:00",
        "status": "CANCELLED",
        "totalAmount": 150.00,
        "parkingVersionAtBooking": 2,
        "paymentReference": null,
        "createdAt": "2026-02-05T10:00:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "totalElements": 2,
      "totalPages": 1
    }
  }
}
```

**Query Parameters:**
- `page`: Page number (0-indexed, default: 0)
- `size`: Page size (default: 10, max: 100)
- `sort`: Sort by field, direction (e.g., `createdAt,desc` or `startTime,asc`)

---

## Owner APIs - `/booking/owner/**`

### 1. List Bookings by Parking

Get all bookings for a specific parking owned by the owner.

```http
GET http://localhost:8080/booking/owner/parking/1?page=0&size=10&status=BOOKED
Authorization: Bearer <OWNER_JWT_TOKEN>
```

**Sample Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 101,
        "driverId": 5,
        "parkingId": 1,
        "startTime": "2026-02-10T10:00:00",
        "endTime": "2026-02-10T12:00:00",
        "status": "BOOKED",
        "totalAmount": 200.00,
        "paymentReference": "PAY_123456789",
        "createdAt": "2026-02-06T14:30:00"
      },
      {
        "id": 102,
        "driverId": 6,
        "parkingId": 1,
        "startTime": "2026-02-10T13:00:00",
        "endTime": "2026-02-10T15:00:00",
        "status": "BOOKED",
        "totalAmount": 200.00,
        "paymentReference": "PAY_123456790",
        "createdAt": "2026-02-06T14:45:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "totalElements": 2,
      "totalPages": 1
    }
  }
}
```

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)
- `status`: Filter by booking status (INITIATED, PENDING_PAYMENT, BOOKED, COMPLETED, CANCELLED, REFUNDED, NO_SHOW)

**Error Scenarios:**
| Status | Scenario | ErrorCode |
|--------|----------|-----------|
| 200 | Bookings retrieved | - |
| 404 | Parking not found | PARKING_NOT_FOUND |
| 403 | Parking owner mismatch | UNAUTHORIZED_BOOKING_ACCESS |
| 401 | Unauthorized | - |

---

### 2. List All Owner's Parkings' Bookings

Get all bookings across all parkings owned by the owner.

```http
GET http://localhost:8080/booking/owner/all?page=0&size=20&status=COMPLETED
Authorization: Bearer <OWNER_JWT_TOKEN>
```

**Sample Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 95,
        "driverId": 4,
        "parkingId": 1,
        "startTime": "2026-02-05T10:00:00",
        "endTime": "2026-02-05T12:00:00",
        "status": "COMPLETED",
        "totalAmount": 200.00,
        "paymentReference": "PAY_123456785",
        "createdAt": "2026-02-04T08:00:00"
      },
      {
        "id": 96,
        "driverId": 5,
        "parkingId": 2,
        "startTime": "2026-02-04T14:00:00",
        "endTime": "2026-02-04T16:00:00",
        "status": "COMPLETED",
        "totalAmount": 150.00,
        "paymentReference": "PAY_123456786",
        "createdAt": "2026-02-03T14:30:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "totalElements": 2,
      "totalPages": 1
    }
  }
}
```

---

### 3. Get Specific Booking (Owner View)

Get details of a specific booking (read-only, must own the associated parking).

```http
GET http://localhost:8080/booking/owner/101
Authorization: Bearer <OWNER_JWT_TOKEN>
```

**Sample Response:**
```json
{
  "success": true,
  "data": {
    "id": 101,
    "driverId": 5,
    "parkingId": 1,
    "startTime": "2026-02-10T10:00:00",
    "endTime": "2026-02-10T12:00:00",
    "status": "BOOKED",
    "totalAmount": 200.00,
    "parkingVersionAtBooking": 1,
    "paymentReference": "PAY_123456789",
    "createdAt": "2026-02-06T14:30:00",
    "updatedAt": "2026-02-06T14:35:00"
  }
}
```

**Error Scenarios:**
| Status | Scenario | ErrorCode |
|--------|----------|-----------|
| 200 | Booking retrieved | - |
| 404 | Booking not found | BOOKING_NOT_FOUND |
| 403 | Parking owner mismatch | UNAUTHORIZED_BOOKING_ACCESS |
| 401 | Unauthorized | - |

---

## Admin APIs - `/booking/admin/**`

### 1. List All Bookings

Get all bookings in the system (admin only).

```http
GET http://localhost:8080/booking/admin/all?page=0&size=50&status=PENDING_PAYMENT&sortBy=createdAt&order=DESC
Authorization: Bearer <ADMIN_JWT_TOKEN>
```

**Sample Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 105,
        "driverId": 7,
        "parkingId": 3,
        "startTime": "2026-02-11T09:00:00",
        "endTime": "2026-02-11T11:00:00",
        "status": "PENDING_PAYMENT",
        "totalAmount": 180.00,
        "parkingVersionAtBooking": 2,
        "paymentReference": null,
        "pendingPaymentExpiresAt": "2026-02-11T09:05:00",
        "createdAt": "2026-02-06T15:00:00"
      },
      {
        "id": 104,
        "driverId": 6,
        "parkingId": 2,
        "startTime": "2026-02-10T15:00:00",
        "endTime": "2026-02-10T17:00:00",
        "status": "PENDING_PAYMENT",
        "totalAmount": 220.00,
        "parkingVersionAtBooking": 1,
        "paymentReference": null,
        "pendingPaymentExpiresAt": "2026-02-10T15:05:00",
        "createdAt": "2026-02-06T14:50:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 50,
      "totalElements": 2,
      "totalPages": 1
    }
  }
}
```

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 50)
- `status`: Filter by status
- `sortBy`: Sort field (createdAt, startTime, totalAmount, etc.)
- `order`: ASC or DESC

---

### 2. Get Booking by ID (Admin View)

Get full details of any booking.

```http
GET http://localhost:8080/booking/admin/101
Authorization: Bearer <ADMIN_JWT_TOKEN>
```

**Sample Response:**
```json
{
  "success": true,
  "data": {
    "id": 101,
    "driverId": 5,
    "parkingId": 1,
    "startTime": "2026-02-10T10:00:00",
    "endTime": "2026-02-10T12:00:00",
    "status": "BOOKED",
    "totalAmount": 200.00,
    "parkingVersionAtBooking": 1,
    "paymentReference": "PAY_123456789",
    "failureReason": null,
    "cancelledAt": null,
    "cancelledBy": null,
    "pendingPaymentExpiresAt": null,
    "createdAt": "2026-02-06T14:30:00",
    "updatedAt": "2026-02-06T14:35:00"
  }
}
```

**Error Scenarios:**
| Status | Scenario | ErrorCode |
|--------|----------|-----------|
| 200 | Booking found | - |
| 404 | Booking not found | BOOKING_NOT_FOUND |
| 401 | Unauthorized | - |
| 403 | Not an admin | - |

---

### 3. Force Cancel Booking (Admin Only)

Cancel any booking and create an audit log entry.

```http
POST http://localhost:8080/booking/admin/force-cancel/101
Authorization: Bearer <ADMIN_JWT_TOKEN>
Content-Type: application/json

{
  "reason": "Dispute resolution - duplicate bookings"
}
```

**Sample Response (Success):**
```json
{
  "success": true,
  "message": "Booking force cancelled and slot released",
  "data": {
    "id": 101,
    "driverId": 5,
    "parkingId": 1,
    "startTime": "2026-02-10T10:00:00",
    "endTime": "2026-02-10T12:00:00",
    "status": "CANCELLED",
    "totalAmount": 200.00,
    "parkingVersionAtBooking": 1,
    "paymentReference": "PAY_123456789",
    "failureReason": null,
    "cancelledAt": "2026-02-06T14:50:00",
    "cancelledBy": "ADMIN",
    "createdAt": "2026-02-06T14:30:00",
    "updatedAt": "2026-02-06T14:50:00"
  }
}
```

**Audit Log Created:**
The action is logged in `booking_admin_audit` table:
```sql
SELECT id, booking_id, admin_id, action, reason, created_at FROM booking_admin_audit
WHERE booking_id = 101 AND action = 'FORCE_CANCEL';
```

**Sample Audit Log Response:**
```json
{
  "id": 1,
  "bookingId": 101,
  "adminId": 3,
  "action": "FORCE_CANCEL",
  "reason": "Dispute resolution - duplicate bookings",
  "createdAt": "2026-02-06T14:50:00"
}
```

**Error Scenarios:**
| Status | Scenario | ErrorCode |
|--------|----------|-----------|
| 200 | Force cancelled | - |
| 404 | Booking not found | BOOKING_NOT_FOUND |
| 409 | Invalid status transition | INVALID_STATUS_TRANSITION |
| 401 | Unauthorized | - |
| 403 | Not an admin | - |

---

## Internal APIs - `/booking/internal/**`

### Payment Callback

Handle payment confirmation/failure from payment gateway. This endpoint is for internal use only.

```http
POST http://localhost:8080/booking/internal/payment-callback
Content-Type: application/json
X-Internal-Key: <INTERNAL_SECRET_KEY>

{
  "bookingId": 101,
  "success": true,
  "paymentReference": "PAY_123456789",
  "failureReason": null
}
```

**Success Case:**
```json
{
  "success": true,
  "message": "Payment processed successfully",
  "data": {
    "id": 101,
    "status": "BOOKED",
    "paymentReference": "PAY_123456789",
    "totalAmount": 200.00,
    "updatedAt": "2026-02-06T14:35:00"
  }
}
```

**Failure Case:**
```http
POST http://localhost:8080/booking/internal/payment-callback
Content-Type: application/json
X-Internal-Key: <INTERNAL_SECRET_KEY>

{
  "bookingId": 101,
  "success": false,
  "paymentReference": null,
  "failureReason": "Insufficient funds"
}
```

**Failure Response:**
```json
{
  "success": true,
  "message": "Payment failure processed",
  "data": {
    "id": 101,
    "status": "CANCELLED",
    "paymentReference": null,
    "failureReason": "Insufficient funds",
    "cancelledBy": "SYSTEM",
    "updatedAt": "2026-02-06T14:40:00"
  }
}
```

**Properties:**
- `bookingId`: ID of the booking to update
- `success`: true if payment succeeded, false if failed/timeout
- `paymentReference`: Payment transaction ID (required if success=true)
- `failureReason`: Reason for failure (optional, used when success=false)

**Error Scenarios:**
| Status | Scenario |
|--------|----------|
| 200 | Callback processed (idempotent) |
| 400 | Invalid booking ID or missing fields |
| 404 | Booking not found |
| 401 | Missing/invalid internal key |

---

## Complete End-to-End Testing Flow

### Scenario 1: Successful Booking and Payment

1. **Driver creates booking** (Status: PENDING_PAYMENT)
   ```http
   POST http://localhost:8080/booking/driver
   ```

2. **External payment gateway calls callback** (simulate success)
   ```http
   POST http://localhost:8080/booking/internal/payment-callback
   ```

3. **Status becomes BOOKED**
   - Verify by calling GET `/booking/driver/{id}`

4. **Booking auto-completes after end time** (via job)
   - Status becomes COMPLETED
   - Slot is released

### Scenario 2: Payment Timeout

1. **Driver creates booking** (Status: PENDING_PAYMENT)
   - Expiry set: current_time + 5 minutes (default)

2. **Wait for expiry or PendingPaymentCleanupJob triggers**
   - Booking status → CANCELLED
   - Slot is released automatically
   - cancelledBy: SYSTEM

3. **Verify via GET** `/booking/driver/{id}`

### Scenario 3: Driver Cancellation

1. **Driver creates booking** (Status: BOOKED after payment)

2. **Driver cancels within cutoff** (60 min before start)
   ```http
   POST http://localhost:8080/booking/driver/cancel/{id}
   ```

3. **Status becomes CANCELLED**
   - Slot is released
   - Owner can see this via listings

4. **Owner cannot cancel via driver API** (only admin can force-cancel)

### Scenario 4: Admin Force Cancel

1. **Booking exists in any status** (except already CANCELLED/REFUNDED)

2. **Admin force cancels with reason**
   ```http
   POST http://localhost:8080/booking/admin/force-cancel/{id}
   ```

3. **Audit log created**
   - Booking status → CANCELLED
   - Slot released
   - cancelledBy: ADMIN

4. **Query audit logs**
   ```sql
   SELECT * FROM booking_admin_audit WHERE booking_id = 101;
   ```

---

## Configuration

Add these to `application.yml`:

```yaml
booking:
  pending-payment-timeout-minutes: 5
  cancellation-cutoff-minutes: 60
  job:
    pending-cleanup-cron: "0 */2 * * * ?"       # Every 2 minutes
    completion-cron: "0 */5 * * * ?"            # Every 5 minutes
    reconciliation-cron: "0 0 * * * ?"          # Every hour
```

---

## Database Queries for Testing

### All Bookings
```sql
SELECT id, driver_id, parking_id, status, total_amount, created_at 
FROM bookings 
ORDER BY created_at DESC;
```

### Pending Payments (expiring soon)
```sql
SELECT id, driver_id, parking_id, pending_payment_expires_at 
FROM bookings 
WHERE status = 'PENDING_PAYMENT' 
  AND pending_payment_expires_at < NOW();
```

### Booking Admin Audit
```sql
SELECT id, booking_id, admin_id, action, reason, created_at 
FROM booking_admin_audit 
ORDER BY created_at DESC;
```

### Active Bookings by Parking
```sql
SELECT id, driver_id, parking_id, status, start_time, end_time 
FROM bookings 
WHERE parking_id = 1 
  AND status IN ('BOOKED', 'PENDING_PAYMENT') 
  AND end_time > NOW()
ORDER BY start_time ASC;
```

### Completed Revenue by Owner
```sql
SELECT p.id, p.name, p.owner_id, 
       COUNT(b.id) as completed_bookings,
       SUM(b.total_amount) as total_revenue
FROM parkings p
LEFT JOIN bookings b ON p.id = b.parking_id 
  AND b.status = 'COMPLETED'
WHERE p.owner_id = <OWNER_ID>
GROUP BY p.id, p.name, p.owner_id
ORDER BY total_revenue DESC;
```

---

## Postman Environment Variables

Create a Postman environment with:

```json
{
  "name": "SmartPark Booking",
  "values": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080",
      "enabled": true
    },
    {
      "key": "driverToken",
      "value": "",
      "enabled": true
    },
    {
      "key": "ownerToken",
      "value": "",
      "enabled": true
    },
    {
      "key": "adminToken",
      "value": "",
      "enabled": true
    },
    {
      "key": "parkingId",
      "value": "1",
      "enabled": true
    },
    {
      "key": "bookingId",
      "value": "",
      "enabled": true
    },
    {
      "key": "driverId",
      "value": "",
      "enabled": true
    }
  ]
}
```

**Post-Login Scripts (for Postman):**

```javascript
// In the Tests tab of login request
if (pm.response.code === 200) {
  const response = pm.response.json();
  if (response.data.role === 'DRIVER') {
    pm.environment.set('driverToken', response.data.token);
    pm.environment.set('driverId', response.data.userId);
  } else if (response.data.role === 'PARKING_OWNER') {
    pm.environment.set('ownerToken', response.data.token);
  } else if (response.data.role === 'ADMIN') {
    pm.environment.set('adminToken', response.data.token);
  }
}
```

---

## Troubleshooting

### Issue: "SLOT_UNAVAILABLE"
- **Cause**: All parking slots are booked for the requested time
- **Solution**: 
  - Check parking capacity
  - Choose different time window
  - Increase parking capacity

### Issue: "TIME_OVERLAP"
- **Cause**: Requested time overlaps with existing bookings
- **Solution**: Choose non-overlapping time slots
- **Note**: Only BOOKED and PENDING_PAYMENT bookings are counted

### Issue: "PARKING_INACTIVE"
- **Cause**: Parking is not APPROVED or not ACTIVE
- **Solution**: 
  - Check parking status in database
  - Admin must activate parking
  - Owner must have APPROVED status

### Issue: "CANNOT_CANCEL_AFTER_CUTOFF"
- **Cause**: Trying to cancel too close to start time
- **Default**: Must cancel 60+ minutes before start
- **Solution**: 
  - Admin can always force-cancel
  - Contact support if urgent

### Issue: "CONCURRENT_BOOKING"
- **Cause**: Version conflict during slot reservation
- **Solution**: Retry the booking request immediately

### Issue: "UNAUTHORIZED_BOOKING_ACCESS"
- **Cause**: Trying to access another user's booking
- **Solution**: Use bookings owned by logged-in user

### Issue: 401 Unauthorized
- **Cause**: Token missing, expired, or invalid
- **Solution**: 
  - Get new token via login
  - Check Authorization header: `Bearer <token>`
  - Regenerate token if expired

### Issue: 403 Forbidden
- **Cause**: User role doesn't have permission
- **Solution**: 
  - DRIVER can only access `/booking/driver/**`
  - OWNER can only access `/booking/owner/**` with their parkings
  - ADMIN can access `/booking/admin/**`

---

## Performance & Testing Tips

1. **Use H2 In-Memory Database for Testing** (see application.yml)
   - Faster test execution
   - Automatic rollback per test

2. **Batch Create Bookings with Different Times**
   - Test overlap detection
   - Test capacity limits

3. **Simulate Payment Delays**
   - Create booking, wait, then callback
   - Test pending payment expiry

4. **Stress Test Concurrent Bookings**
   - Create multiple bookings simultaneously
   - Verify optimistic locking prevents overbooking

5. **Monitor Job Execution**
   - Check logs for job execution times
   - Verify bookings auto-complete and slots release

---

## Summary of Key Features Tested

✅ Create Booking with validation
✅ Payment callback handling (success & failure)
✅ Slot reservation with optimistic locking
✅ Cancellation by driver & admin
✅ Auto-completion via job
✅ Pending payment timeout cleanup
✅ Booking reconciliation
✅ Audit logging for admin actions
✅ Role-based access control
✅ Pagination and filtering
