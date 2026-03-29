# Environment Variables Setup Guide

## Overview
This project uses environment variables for managing sensitive configuration data (database credentials, API keys, etc.). The `.env` file is used for local development and **should never be committed to version control**.

## Files

- **`.env`** - Contains actual environment variables for local development (EXCLUDED from git)
- **`.env.example`** - Template file showing required variables with placeholder values (INCLUDED in git for reference)

## Setup Instructions

### 1. Copy the Example File
```bash
cp .env.example .env
```

### 2. Update `.env` with Your Values
Edit the `.env` file and replace placeholder values with your actual credentials:

```env
# Database
DB_URL=jdbc:mysql://localhost:3306/smartpark?createDatabaseIfNotExist=true
DB_USERNAME=root
DB_PASSWORD=your_actual_password

# JWT
JWT_SECRET=your-very-long-secure-random-jwt-secret-key
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Stripe API Keys (from Stripe Dashboard)
STRIPE_SECRET_KEY=sk_test_your_actual_key
STRIPE_PUBLISHABLE_KEY=pk_test_your_actual_key
STRIPE_WEBHOOK_SECRET=whsec_your_actual_key
STRIPE_CONNECT_SUCCESS_URL=http://localhost:3000/owner/connect/success
STRIPE_CONNECT_REFRESH_URL=http://localhost:3000/owner/connect/refresh

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_TIMEOUT=2000
```

### 3. How It Works
- The `spring-dotenv` dependency automatically loads variables from `.env` on application startup
- In `application.yml`, variables are referenced using `${VARIABLE_NAME}` syntax
- If a variable is not found, the default value after the colon is used (e.g., `${DB_HOST:localhost}`)

### 4. Security Best Practices

✅ **DO:**
- Keep `.env` file locally and never commit it to git
- Use strong, random values for secrets (especially JWT_SECRET and Stripe keys)
- Regenerate secrets periodically in production
- Use `.env.example` as documentation for required variables

❌ **DON'T:**
- Commit `.env` to version control
- Share `.env` files via email or version control
- Use development keys in production
- Store plain-text passwords in code

### 5. Production Deployment

For production environments:

**Option A: Environment Variables (Recommended)**
```bash
export DB_USERNAME="prod_user"
export DB_PASSWORD="prod_secure_password"
export JWT_SECRET="prod_long_secure_secret"
# ... set all variables before starting the app
```

**Option B: Docker Secrets or Configuration Management**
- Use Docker secrets or Kubernetes ConfigMaps/Secrets
- Use AWS Secrets Manager, HashiCorp Vault, etc.

### 6. Accessing Variables in Java Code

Variables are loaded automatically by Spring. Access them using:

```java
@Value("${STRIPE_SECRET_KEY}")
private String stripeSecretKey;

@Value("${DB_PASSWORD}")
private String dbPassword;
```

### 7. Troubleshooting

**Issue:** Variables not loading
- Ensure `.env` file is in the project root directory
- Restart the application after changes
- Check console logs for errors during startup

**Issue:** Default values being used despite .env file existing
- Verify variable names in `.env` match exactly (case-sensitive)
- Ensure no extra spaces around variable names

## Variable Reference

| Variable | Purpose | Required | Default |
|----------|---------|----------|---------|
| `DB_URL` | MySQL connection string | Yes | localhost |
| `DB_USERNAME` | Database user | Yes | root |
| `DB_PASSWORD` | Database password | Yes | - |
| `JWT_SECRET` | JWT signing secret | Yes | - |
| `JWT_EXPIRATION` | JWT token lifetime (ms) | No | 86400000 |
| `STRIPE_SECRET_KEY` | Stripe API secret key | Yes | - |
| `STRIPE_PUBLISHABLE_KEY` | Stripe public key | Yes | - |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook signing key | Yes | - |
| `REDIS_HOST` | Redis server host | No | localhost |
| `REDIS_PORT` | Redis server port | No | 6379 |

