-- ============================================================
-- V1: Roles & Users
-- ============================================================

CREATE TABLE roles (
    id            NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR2(30)  NOT NULL UNIQUE,
    created_by    VARCHAR2(100),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by    VARCHAR2(100),
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id            NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      VARCHAR2(50)  NOT NULL UNIQUE,
    email         VARCHAR2(100) NOT NULL UNIQUE,
    password      VARCHAR2(255) NOT NULL,
    enabled       NUMBER(1)     DEFAULT 1 NOT NULL,
    created_by    VARCHAR2(100),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by    VARCHAR2(100),
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id       NUMBER NOT NULL,
    role_id       NUMBER NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id)
);
