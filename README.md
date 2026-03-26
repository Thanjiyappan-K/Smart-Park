# SmartPark - Authentication Module

Complete authentication and authorization system for SmartPark parking management application.

## Features

✅ **User Registration**
- Register as DRIVER (automatically ACTIVE)
- Register as PARKING_OWNER (PENDING_VERIFICATION status)

✅ **Authentication**
- Login with email or phone number
- JWT-based token generation
- Refresh token support (7 days expiry)

✅ **Authorization**
- Role-based access control (DRIVER, PARKING_OWNER, ADMIN)
- JWT filter for automatic token validation
- Method-level security with `@PreAuthorize`

✅ **Admin Features**
- Verify parking owners
- View pending owners
- Block/unblock users

✅ **Security**
- BCrypt password encoding
- Token blacklist for logout
- Automatic token cleanup

## Tech Stack

- **Spring Boot 3.2.0**
- **Spring Security** - Authentication & Authorization
- **Spring Data JPA** - Database operations
- **MySQL** - Database
- **JWT (JJWT 0.12.3)** - Token management
- **Lombok** - Boilerplate reduction
- **Validation** - Request validation

## Project Structure

```
com.smartpark
 ├── auth
 │   ├── controller
 │   │   ├── AuthController.java
 │   │   └── AdminController.java
 │   ├── service
 │   │   ├── AuthService.java
 │   │   ├── AdminService.java
 │   │   └── BlacklistService.java
 │   ├── dto
 │   │   ├── RegisterRequest.java
 │   │   ├── LoginRequest.java
 │   │   ├── AuthResponse.java
 │   │   └── RefreshTokenRequest.java
 │   ├── jwt
 │   │   └── JwtService.java
 │   ├── entity
 │   │   ├── RefreshToken.java
 │   │   └── TokenBlacklist.java
 │   └── repository
 │       ├── RefreshTokenRepository.java
 │       └── TokenBlacklistRepository.java
 ├── user
 │   ├── entity
 │   │   └── User.java
 │   ├── repository
 │   │   └── UserRepository.java
 │   └── enums
 │       ├── Role.java
 │       └── UserStatus.java
 ├── common
 │   ├── exception
 │   │   └── GlobalExceptionHandler.java
 │   ├── response
 │   │   └── ApiResponse.java
 │   └── security
 │       ├── SecurityConfig.java
 │       └── JwtAuthFilter.java
 └── SmartParkApplication.java
```

## Database Setup

1. Create MySQL database:
```sql
CREATE DATABASE smartpark;
```

2. Update `application.yml` with your MySQL credentials:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/smartpark
    username: root
    password: root
```

3. Tables will be auto-created by Hibernate (`ddl-auto: update`)

## Configuration

### JWT Configuration
Update `application.yml`:
```yaml
jwt:
  secret: your-secret-key-here (use a long, secure random string in production)
  expiration: 86400000 # 1 day in milliseconds
```

## API Endpoints

### Public Endpoints (No Authentication Required)

#### 1. Register Driver
```http
POST /auth/register/driver
Content-Type: application/json

{
  "name": "John Doe",
  "email": "driver@example.com",
  "phone": "1234567890",
  "password": "password123"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Driver registered successfully",
  "data": null
}
```

#### 2. Register Owner
```http
POST /auth/register/owner
Content-Type: application/json

{
  "name": "Jane Smith",
  "email": "owner@example.com",
  "phone": "9876543210",
  "password": "password123"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Owner registered, pending verification",
  "data": null
}
```

#### 3. Login
```http
POST /auth/login
Content-Type: application/json

{
  "emailOrPhone": "driver@example.com",
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
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "role": "DRIVER"
  }
}
```

#### 4. Refresh Token
```http
POST /auth/refresh
Content-Type: application/json

{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "new-refresh-token-uuid",
    "role": "DRIVER"
  }
}
```

#### 5. Logout
```http
POST /auth/logout
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
```json
{
  "success": true,
  "message": "Logged out successfully",
  "data": null
}
```

### Admin Endpoints (Requires ADMIN Role)

All admin endpoints require JWT token with ADMIN role in header:
```http
Authorization: Bearer <JWT_TOKEN>
```

#### 1. Verify Owner
```http
POST /admin/verify-owner/{id}
```

**Response:**
```json
{
  "success": true,
  "message": "Owner verified successfully",
  "data": null
}
```

#### 2. Get Pending Owners
```http
GET /admin/pending-owners
```

**Response:**
```json
{
  "success": true,
  "message": "Pending owners retrieved successfully",
  "data": [
    {
      "id": 2,
      "name": "Jane Smith",
      "email": "owner@example.com",
      "phone": "9876543210",
      "role": "PARKING_OWNER",
      "status": "PENDING_VERIFICATION",
      ...
    }
  ]
}
```

#### 3. Block User
```http
POST /admin/block-user/{id}
```

#### 4. Unblock User
```http
POST /admin/unblock-user/{id}
```

## User Roles

- **DRIVER**: Can book parking spots
- **PARKING_OWNER**: Can manage parking lots (requires admin verification)
- **ADMIN**: Can verify owners and manage users

## User Status

- **ACTIVE**: User can login and use the system
- **PENDING_VERIFICATION**: Owner waiting for admin approval
- **BLOCKED**: User cannot login

## Security Features

1. **Password Encryption**: BCrypt with strength 10
2. **JWT Tokens**: 
   - Access token: 1 day expiry
   - Refresh token: 7 days expiry
3. **Token Blacklist**: Logged out tokens are invalidated
4. **Role-Based Access**: Method-level security with `@PreAuthorize`
5. **Automatic Token Validation**: JWT filter validates every request

## Creating Admin User

To create an admin user, you can either:

1. **Use SQL directly:**
```sql
INSERT INTO users(name, email, phone, password, role, status, created_at, updated_at)
VALUES (
  'Admin',
  'admin@smartpark.com',
  '9999999999',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- password: admin123
  'ADMIN',
  'ACTIVE',
  NOW(),
  NOW()
);
```

2. **Or create a data initialization script** (recommended for production)

## Running the Application

1. Ensure MySQL is running
2. Update database credentials in `application.yml`
3. Run:
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Testing with Postman

1. **Register a driver:**
   - POST `/auth/register/driver`
   - Body: JSON with name, email, phone, password

2. **Login:**
   - POST `/auth/login`
   - Body: JSON with emailOrPhone and password
   - Copy the `token` from response

3. **Access protected endpoint:**
   - Add header: `Authorization: Bearer <token>`
   - Example: GET `/admin/pending-owners` (requires ADMIN role)

## Next Steps

After authentication module is complete, you can build:
- Parking module
- Booking module
- Payment module
- Review module

All these modules will use the JWT authentication and role-based authorization already implemented.
