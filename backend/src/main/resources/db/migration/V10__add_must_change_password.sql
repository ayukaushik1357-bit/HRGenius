-- ============================================================
-- V10: Forced password change flag on users
-- ============================================================

ALTER TABLE users ADD must_change_password INTEGER DEFAULT 0 NOT NULL;
