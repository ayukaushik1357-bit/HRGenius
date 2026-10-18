-- ============================================================
-- V2: Departments & Designations
-- ============================================================

CREATE TABLE departments (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR2(100)  NOT NULL UNIQUE,
    code          VARCHAR2(20)   NOT NULL UNIQUE,
    description   VARCHAR2(500),
    created_by    VARCHAR2(100),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by    VARCHAR2(100),
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE designations (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title             VARCHAR2(100) NOT NULL UNIQUE,
    designation_level INTEGER,
    description       VARCHAR2(500),
    created_by        VARCHAR2(100),
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by        VARCHAR2(100),
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
