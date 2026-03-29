# Startup Verification Checklist

## Pre-Startup Checks ✓

Before running the application, verify:

### 1. File Verification
- [ ] `.env` file exists in project root: `d:\SmartPark\.env`
- [ ] `EnvironmentConfig.java` exists in: `src/main/java/com/smartpark/config/EnvironmentConfig.java`
- [ ] `application.yml` updated with environment variable placeholders
- [ ] `.gitignore` includes `.env` (should show in git status)

### 2. Configuration Files
Run this command to check your `.env` file:
```bash
cd d:\SmartPark
Get-Content .env
```

Expected output (values will differ):
```
DB_URL=jdbc:mysql://localhost:3306/smartpark?createDatabaseIfNotExist=true
DB_USERNAME=root
DB_PASSWORD=thanji830
JWT_SECRET=smartpark-secret-key-...
...
```

### 3. Maven Dependencies
Check that new dependency was added to `pom.xml`:
```bash
mvn dependency:tree | findstr "dotenv"
```

Should show: `io.github.cdimascio:dotenv-java:jar:3.0.0`

---

## Startup Steps

### Step 1: Clean Build
```powershell
cd d:\SmartPark
mvn clean install
```

Wait for: `BUILD SUCCESS`

### Step 2: Run Application
```powershell
mvn spring-boot:run
```

or 

```powershell
./mvnw spring-boot:run
```

### Step 3: Watch the Logs
Look for these **SUCCESS indicators**:

```
✓ JWT_SECRET loaded successfully
✓ Stripe API configured and ready
o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http)
o.s.b.SpringApplication : Started SmartParkApplication in X.XXX seconds
```

### Step 4: Test the API
```bash
curl http://localhost:8080/api/health
```

---

## Expected Warnings (NOT Errors)

These warnings are SAFE to ignore:

### 1. Redis Warning
```
Caused by: java.lang.ClassNotFoundException: redis.clients.jedis.commands.JedisCommands
```
**Why harmless:** Redis is optional; app uses fallback

### 2. Spring Data Redis Store Warning  
```
Could not safely identify store assignment for repository
```
**Why harmless:** Spring is confused about JPA vs Redis; not critical for auth

---

## Common Issues & Fixes

### 🔴 Issue: "Could not resolve placeholder 'JWT_SECRET'"
```
java.lang.IllegalArgumentException: Could not resolve placeholder 'JWT_SECRET' in value "${JWT_SECRET}"
```

**Cause:** .env file not loaded

**Solution:**
```powershell
# 1. Verify .env exists
Test-Path ".\\.env"  # Should return True

# 2. Clean and rebuild
mvn clean install
```

---

### 🔴 Issue: "JWT_SECRET is too short"
```
⚠️ JWT_SECRET is too short (10 chars). Recommended minimum: 32 characters.
```

**Solution:** Update `.env` file:
```env
JWT_SECRET=your-very-long-secure-random-string-at-least-32-chars-here
```

---

### 🟡 Issue: Cannot Find Database
```
java.sql.SQLException: Cannot load driver class: com.mysql.cj.jdbc.Driver
```

**Solution:**
1. Ensure MySQL is running
2. Verify DB_URL in `.env` is correct
3. Verify DB_USERNAME and DB_PASSWORD are correct

```powershell
# Windows: Check MySQL service
Get-Service MySQL80  # or your MySQL service name
```

---

## After Successful Startup

### 1. Database Auto-Setup
- Tables are created automatically (Hibernate `ddl-auto: update`)
- Check your MySQL database: should have tables like `user`, `booking`, `parking_spot`, etc.

### 2. Test Authentication
```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test@1234",
    "phone": "+1234567890",
    "role": "DRIVER"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "emailOrPhone": "test@example.com",
    "password": "Test@1234"
  }'
```

### 3. View Logs
```powershell
# Real-time logs
Get-Content -Path "logs/smartpark.log" -Wait

# Or in Docker
docker logs <container-id> -f
```

---

## Next Steps

1. ✅ Complete the checklist above
2. ✅ Run the startup steps
3. ✅ Verify all SUCCESS indicators appear
4. ✅ Test the API endpoints
5. ✅ Commit `.env` to local git (it's in .gitignore, so won't push)

---

## Still Having Issues?

1. Check [JWT_SECRET_FIX.md](JWT_SECRET_FIX.md) for detailed troubleshooting
2. Check [ENV_SETUP.md](ENV_SETUP.md) for environment configuration help
3. Review application logs: `logs/smartpark.log`

