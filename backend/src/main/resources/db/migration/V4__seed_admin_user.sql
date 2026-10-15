-- ============================================================
-- V4: Seed roles and ADMIN user
-- Password is BCrypt hash of "Admin@123"
-- ============================================================

INSERT INTO roles (name, created_by, created_at) VALUES ('ADMIN',      'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO roles (name, created_by, created_at) VALUES ('HR_MANAGER', 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO roles (name, created_by, created_at) VALUES ('MANAGER',    'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO roles (name, created_by, created_at) VALUES ('EMPLOYEE',   'SYSTEM', CURRENT_TIMESTAMP);

INSERT INTO users (username, email, password, enabled, created_by, created_at)
VALUES ('admin', 'admin@hrgenius.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        1, 'SYSTEM', CURRENT_TIMESTAMP);

-- Link admin user to ADMIN role
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ADMIN';
