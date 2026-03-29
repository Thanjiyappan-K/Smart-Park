# JWT_SECRET Error - Troubleshooting & Solution

## 🔴 The Error
```
java.lang.IllegalArgumentException: Could not resolve placeholder 'JWT_SECRET' in value "${JWT_SECRET}"
```

## ✅ What We Fixed

We've implemented a **3-layer solution** to ensure environment variables load properly:

### 1. **Automated Configuration Loading** 
   - Created `EnvironmentConfig.java` that loads `.env` file automatically at startup
   - Converts `.env` variables to System properties
   - Runs before Spring processes `@Value` annotations

### 2. **Smart Fallback Values**
   - `application.yml` now includes development defaults for all sensitive config
   - Production values must be set via `.env` file or environment variables
   - Errors are clear and actionable if values are missing

### 3. **Validation & Logging**
   - `JwtService` validates the secret on startup
   - Clear error messages if configuration is missing
   - Helpful warnings if secret is too short
   - Status logs showing what was loaded: "✓ JWT_SECRET loaded successfully"

---

## 🚀 Steps to Get Running

### Step 1: Ensure `.env` File Exists
Your `.env` file should be in the project root directory with these values:

```env
# ===== Database Configuration =====
DB_URL=jdbc:mysql://localhost:3306/smartpark?createDatabaseIfNotExist=true
DB_USERNAME=root
DB_PASSWORD=thanji830

# ===== JWT Configuration =====
JWT_SECRET=smartpark-secret-key-change-in-production-to-a-very-long-and-secure-random-string
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# ===== Stripe Configuration =====
STRIPE_SECRET_KEY=sk_test_51RKuJsKQoUsslZXjTEi0GWre8CymbTpPWtuD8horlkPqtycvMsEr8xJ9ygLKr6rgUsimOfAXgFOa9ztsJ6SxclnO00qVRVGNxY
STRIPE_PUBLISHABLE_KEY=pk_test_51RKuJsKQoUsslZXj6vT63bkVWwrJgdwkbkIBKEq4sogm8DP4jAOVN8o1mzKS0fATBKlVwC8VqPGMd6PNvAfFXDzS00efE32tQ6
STRIPE_WEBHOOK_SECRET=whsec_769888980786ea4d71683a6b8146c068b46467a7317cb7b6ca040090b206655d
STRIPE_CONNECT_SUCCESS_URL=http://localhost:3000/owner/connect/success
STRIPE_CONNECT_REFRESH_URL=http://localhost:3000/owner/connect/refresh
STRIPE_CURRENCY=inr

# ===== Redis Configuration =====
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_TIMEOUT=2000
```

### Step 2: Clean Build (Important!)
```bash
mvn clean install
```

### Step 3: Run the Application
```bash
mvn spring-boot:run
```

or

```bash
./mvnw spring-boot:run
```

### Step 4: Verify Success
Look for these logs during startup:

```
✓ JWT_SECRET loaded successfully
✓ Stripe API configured and ready
```

---

## ⚠️ If You Still See Errors

### Error: "IllegalStateException: JWT_SECRET is not configured"
**Cause:** `.env` file not found or JWT_SECRET not defined

**Fix:**
1. Verify `.env` file exists in project root: `d:\SmartPark\.env`
2. Verify the file contains: `JWT_SECRET=your-secret-value`
3. Make sure there are **no spaces** around the `=` sign
4. Restart the application

### Error: "Cannot resolve placeholder"
**Cause:** EnvironmentConfig not registered or .env file being loaded too late

**Fix:**
1. Run `mvn clean install` (clears old build artifacts)
2. Make sure `EnvironmentConfig.java` is in the classpath
3. Check that `src/main/java/com/smartpark/config/EnvironmentConfig.java` exists

### Warning: "Spring Data Redis - Could not safely identify store assignment"
**Status:** ⚠️ This is NOT an error (see separate notes below)

---

## 📋 What Changed

### New Files
- `src/main/java/com/smartpark/config/EnvironmentConfig.java` - Loads `.env` at startup

### Updated Files
- `pom.xml` - Added `dotenv-java` dependency  
- `src/main/java/com/smartpark/auth/jwt/JwtService.java` - Added validation & logging
- `src/main/java/com/smartpark/payment/config/StripeConfig.java` - Added logging
- `src/main/resources/application.yml` - Added fallback values & warnings

---

## 🔐 Production Security Notes

**DO NOT:**
- Commit `.env` to version control (already in `.gitignore`)
- Use development secrets in production
- Share `.env` files via email or chat

**DO:**
- Use strong random values for all secrets
- Regenerate secrets periodically
- Use `.env.example` as template documentation only
- Store secrets in secure vaults (AWS Secrets Manager, HashiCorp Vault, etc.)

---

## 📝 Additional Resources

- [ENV_SETUP.md](../ENV_SETUP.md) - Complete environment variable guide
- `.env.example` - Template with all required variables
- `.gitignore` - Ensures sensitive files are never committed

