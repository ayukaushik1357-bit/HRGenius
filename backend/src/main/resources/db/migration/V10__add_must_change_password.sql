-- ============================================================
-- V10: Forced password change flag on users
-- ============================================================

ALTER TABLE users ADD must_change_password NUMBER(1) DEFAULT 0 NOT NULL;
