# API Testing Guide - SmartPark Authentication

## Quick Start Testing

### 1. Start the Application
```bash
mvn spring-boot:run
```

### 2. Create Admin User (First Time Only)

**Option A: Using SQL**
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
Password: `admin123`

**Option B: Using Postman**
1. Register a user first
2. Manually update the database to change role to ADMIN

### 3. Test Flow

#### Step 1: Register a Driver
```http
POST http://localhost:8080/auth/register/driver
Content-Type: application/json

{
  "name": "John Driver",
  "email": "driver@test.com",
  "phone": "1234567890",
  "password": "password123"
}
```

#### Step 2: Register an Owner
```http
POST http://localhost:8080/auth/register/owner
Content-Type: application/json

{
  "name": "Jane Owner",
  "email": "owner@test.com",
  "phone": "9876543210",
  "password": "password123"
}
```

#### Step 3: Login as Driver
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
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "uuid-here",
    "role": "DRIVER"
  }
}
```

**Save the token for next requests!**

#### Step 4: Login as Admin
```http
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "emailOrPhone": "admin@smartpark.com",
  "password": "admin123"
}
```

#### Step 5: Verify Owner (Admin Only)
```http
POST http://localhost:8080/admin/verify-owner/2
Authorization: Bearer <ADMIN_TOKEN>
```

#### Step 6: Get Pending Owners (Admin Only)
```http
GET http://localhost:8080/admin/pending-owners
Authorization: Bearer <ADMIN_TOKEN>
```

#### Step 7: Refresh Token
```http
POST http://localhost:8080/auth/refresh
Content-Type: application/json

{
  "refreshToken": "<REFRESH_TOKEN_FROM_LOGIN>"
}
```

#### Step 8: Logout
```http
POST http://localhost:8080/auth/logout
Authorization: Bearer <JWT_TOKEN>
```

## Postman Collection Structure

Create a Postman collection with these folders:

1. **Auth - Public**
   - Register Driver
   - Register Owner
   - Login
   - Refresh Token
   - Logout

2. **Admin - Protected**
   - Verify Owner
   - Get Pending Owners
   - Block User
   - Unblock User

## Environment Variables (Postman)

Create a Postman environment with:
- `baseUrl`: `http://localhost:8080`
- `jwtToken`: (set automatically from login response)
- `refreshToken`: (set automatically from login response)

## Testing Scenarios

### ✅ Happy Path
1. Register driver → Login → Get token
2. Register owner → Admin verifies → Owner can login

### ❌ Error Cases
1. Register with duplicate email → Should fail
2. Login with wrong password → Should fail
3. Access admin endpoint without token → Should fail (401)
4. Access admin endpoint with DRIVER token → Should fail (403)
5. Use expired token → Should fail
6. Use blacklisted token → Should fail

### 🔄 Token Flow
1. Login → Get JWT + Refresh Token
2. Use JWT for API calls
3. When JWT expires → Use refresh token to get new JWT
4. Logout → Token blacklisted

## Common Issues

### Issue: "Invalid credentials"
- Check email/phone exists
- Verify password is correct
- Check user status is not BLOCKED

### Issue: "User is blocked"
- Admin needs to unblock user
- Or check database status

### Issue: "Owner not found"
- Verify owner ID exists
- Check owner role is PARKING_OWNER

### Issue: 401 Unauthorized
- Token missing in Authorization header
- Token format: `Bearer <token>`
- Token expired or invalid

### Issue: 403 Forbidden
- User role doesn't have permission
- Check role in token matches required role

## Database Queries for Testing

### Check all users
```sql
SELECT id, name, email, role, status FROM users;
```

### Check refresh tokens
```sql
SELECT * FROM refresh_tokens;
```

### Check blacklisted tokens
```sql
SELECT * FROM token_blacklist;
```

### Manually block a user
```sql
UPDATE users SET status = 'BLOCKED' WHERE id = 1;
```

### Manually activate an owner
```sql
UPDATE users SET status = 'ACTIVE' WHERE id = 2;
```

---

## Parking Module APIs

**Base paths:** `/parking/public/**` (no auth), `/parking/owner/**` (PARKING_OWNER), `/parking/admin/**` (ADMIN).

### Public (Driver / Discovery) – no auth

**Search by location (Haversine radius)**
```http
POST http://localhost:8080/parking/public/search
Content-Type: application/json

{
  "latitude": 12.9716,
  "longitude": 77.5946,
  "radiusKm": 5,
  "vehicleType": "CAR",
  "priceMin": null,
  "priceMax": null,
  "city": null,
  "page": 0,
  "size": 20
}
```

**Get parking details**
```http
GET http://localhost:8080/parking/public/{parkingId}
```

### Owner (PARKING_OWNER only)

**Create parking**
```http
POST http://localhost:8080/parking/owner
Authorization: Bearer <owner_token>
Content-Type: application/json

{
  "name": "Downtown Parking",
  "address": "123 Main St",
  "city": "Bangalore",
  "latitude": 12.9716,
  "longitude": 77.5946,
  "totalSlots": 50,
  "pricePerHour": 30,
  "vehicleType": "CAR",
  "availabilityRules": [
    { "dayOfWeek": 1, "openTime": "08:00", "closeTime": "20:00", "isAvailable": true }
  ],
  "imageUrls": ["https://example.com/img1.jpg"]
}
```

**Update parking**
```http
PUT http://localhost:8080/parking/owner/{parkingId}
Authorization: Bearer <owner_token>
Content-Type: application/json

{ "name": "Updated Name", "pricePerHour": 35 }
```

**List my parkings**
```http
GET http://localhost:8080/parking/owner?page=0&size=20
Authorization: Bearer <owner_token>
```

**Activate / Deactivate**
```http
POST http://localhost:8080/parking/owner/{parkingId}/activate
POST http://localhost:8080/parking/owner/{parkingId}/deactivate
Authorization: Bearer <owner_token>
```

**Dashboard (per parking or all)**
```http
GET http://localhost:8080/parking/owner/{parkingId}/dashboard
GET http://localhost:8080/parking/owner/dashboards
Authorization: Bearer <owner_token>
```

### Admin (ADMIN only)

**List pending parkings**
```http
GET http://localhost:8080/parking/admin/pending?page=0&size=20
Authorization: Bearer <admin_token>
```

**Approve parking**
```http
POST http://localhost:8080/parking/admin/approve
Authorization: Bearer <admin_token>
Content-Type: application/json

{ "parkingId": 1, "reason": null }
```

**Reject parking (reason required)**
**completed till now**
```http
POST http://localhost:8080/parking/admin/reject
Authorization: Bearer <admin_token>
Content-Type: application/json

{ "parkingId": 1, "reason": "Invalid documents" }
```
**skipped**
**Force disable**
```http
POST http://localhost:8080/parking/admin/force-disable
Authorization: Bearer <admin_token>
Content-Type: application/json

{ "parkingId": 1, "reason": "Policy violation" }
```

### Parking lifecycle

1. Owner creates parking → `verification_status=PENDING`, `is_published=false`, `is_active=false`.
2. Admin approves → `verification_status=APPROVED`, `is_published=true`, `is_active=false`.
3. Owner activates → `is_active=true` (visible to drivers).
4. Drivers see only parkings where approved + published + active.
