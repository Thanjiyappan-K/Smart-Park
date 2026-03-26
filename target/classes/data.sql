-- Admin user creation script
-- Password: admin123 (BCrypt encoded)
-- You can generate your own BCrypt hash or use this one


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
)
ON DUPLICATE KEY UPDATE email = email;
