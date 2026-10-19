-- ============================================================
-- V9: Refresh Tokens table
-- ============================================================

CREATE TABLE refresh_tokens (
    id            NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token_hash    VARCHAR2(64)  NOT NULL UNIQUE,
    jti           VARCHAR2(100),
    user_id       NUMBER        NOT NULL,
    expires_at    TIMESTAMP     NOT NULL,
    revoked       NUMBER(1)     DEFAULT 0 NOT NULL,
    created_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    replaced_by   VARCHAR2(64),
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_rt_user ON refresh_tokens(user_id);
-- token_hash is UNIQUE, which already creates an index (a second index fails on Oracle with ORA-01408)
