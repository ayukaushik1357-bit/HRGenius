-- ============================================================
-- V5: Recruitment and Onboarding
-- ============================================================

CREATE TABLE job_postings (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title                VARCHAR2(150)  NOT NULL,
    description          VARCHAR2(2000),
    requirements         VARCHAR2(2000),
    location             VARCHAR2(100),
    min_exp_years        INTEGER,
    max_exp_years        INTEGER,
    openings             INTEGER        DEFAULT 1 NOT NULL,
    status               VARCHAR2(20)   DEFAULT 'DRAFT' NOT NULL,
    department_id        BIGINT,
    designation_id       BIGINT,
    created_by           VARCHAR2(100),
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by           VARCHAR2(100),
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_jp_dept FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_jp_desig FOREIGN KEY (designation_id) REFERENCES designations(id)
);

CREATE TABLE candidates (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name           VARCHAR2(50)   NOT NULL,
    last_name            VARCHAR2(50)   NOT NULL,
    email                VARCHAR2(100)  NOT NULL UNIQUE,
    phone                VARCHAR2(20),
    resume_file_name     VARCHAR2(255),
    resume_file_path     VARCHAR2(500),
    experience_years     NUMBER(5,2),
    current_company      VARCHAR2(100),
    expected_salary_ctc  NUMBER(15,2),
    created_by           VARCHAR2(100),
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by           VARCHAR2(100),
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE job_applications (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    job_posting_id       BIGINT         NOT NULL,
    candidate_id         BIGINT         NOT NULL,
    stage                VARCHAR2(20)   DEFAULT 'APPLIED' NOT NULL,
    applied_at           TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    notes                VARCHAR2(2000),
    created_by           VARCHAR2(100),
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by           VARCHAR2(100),
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ja_jp FOREIGN KEY (job_posting_id) REFERENCES job_postings(id),
    CONSTRAINT fk_ja_cand FOREIGN KEY (candidate_id) REFERENCES candidates(id)
);

CREATE TABLE interviews (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    application_id       BIGINT         NOT NULL,
    interviewer_id       BIGINT         NOT NULL,
    scheduled_at         TIMESTAMP      NOT NULL,
    round_name           VARCHAR2(100)  NOT NULL,
    rating               INTEGER,
    feedback             VARCHAR2(2000),
    status               VARCHAR2(20)   DEFAULT 'SCHEDULED' NOT NULL,
    created_by           VARCHAR2(100),
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by           VARCHAR2(100),
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_int_app FOREIGN KEY (application_id) REFERENCES job_applications(id),
    CONSTRAINT fk_int_emp FOREIGN KEY (interviewer_id) REFERENCES employees(id)
);

CREATE TABLE job_offers (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    application_id       BIGINT         NOT NULL UNIQUE,
    offer_date           DATE           NOT NULL,
    joining_date         DATE           NOT NULL,
    offered_salary_ctc   NUMBER(15,2)   NOT NULL,
    status               VARCHAR2(20)   DEFAULT 'PENDING' NOT NULL,
    remarks              VARCHAR2(1000),
    created_by           VARCHAR2(100),
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by           VARCHAR2(100),
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_offer_app FOREIGN KEY (application_id) REFERENCES job_applications(id)
);

CREATE TABLE onboarding_templates (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                 VARCHAR2(100)  NOT NULL UNIQUE,
    description          VARCHAR2(500),
    created_by           VARCHAR2(100),
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by           VARCHAR2(100),
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE onboarding_template_tasks (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_id          BIGINT         NOT NULL,
    title                VARCHAR2(150)  NOT NULL,
    description          VARCHAR2(500),
    category             VARCHAR2(30)   DEFAULT 'DOCUMENTS' NOT NULL,
    default_days_to_complete INTEGER    DEFAULT 3 NOT NULL,
    created_by           VARCHAR2(100),
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by           VARCHAR2(100),
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ott_tpl FOREIGN KEY (template_id) REFERENCES onboarding_templates(id)
);

CREATE TABLE onboarding_tasks (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id          BIGINT         NOT NULL,
    title                VARCHAR2(150)  NOT NULL,
    description          VARCHAR2(500),
    category             VARCHAR2(30)   NOT NULL,
    status               VARCHAR2(20)   DEFAULT 'PENDING' NOT NULL,
    owner_id             BIGINT,
    due_date             DATE,
    completed_at         TIMESTAMP,
    created_by           VARCHAR2(100),
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by           VARCHAR2(100),
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ot_emp FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_ot_owner FOREIGN KEY (owner_id) REFERENCES employees(id)
);
