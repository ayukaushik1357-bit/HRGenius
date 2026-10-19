-- ============================================================
-- V11: Self sign-up approval flag and password reset tokens
-- ============================================================

-- Self-registered and Google sign-up accounts wait for an administrator
ALTER TABLE users ADD pending_approval NUMBER(1) DEFAULT 0 NOT NULL;

CREATE TABLE password_reset_tokens (
    id            NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       NUMBER        NOT NULL,
    token_hash    VARCHAR2(64)  NOT NULL UNIQUE,
    expires_at    TIMESTAMP     NOT NULL,
    used          NUMBER(1)     DEFAULT 0 NOT NULL,
    created_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_prt_user ON password_reset_tokens(user_id);
