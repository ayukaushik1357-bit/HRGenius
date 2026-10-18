-- ============================================================
-- V7: Payroll and Notifications
-- ============================================================

CREATE TABLE salary_structures (
    id                   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id          NUMBER         NOT NULL UNIQUE,
    basic_salary         NUMBER(15,2)   NOT NULL,
    hra                  NUMBER(15,2)   NOT NULL,
    special_allowance    NUMBER(15,2)   DEFAULT 0.00,
    other_allowances     NUMBER(15,2)   DEFAULT 0.00,
    effective_from       DATE           NOT NULL,
    created_by           VARCHAR2(100),
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by           VARCHAR2(100),
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ss_emp FOREIGN KEY (employee_id) REFERENCES employees(id)
);

CREATE TABLE payroll_runs (
    id                   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payroll_year         NUMBER(4)      NOT NULL,
    payroll_month        NUMBER(2)      NOT NULL,
    total_employees      NUMBER(6)      DEFAULT 0 NOT NULL,
    total_gross_pay      NUMBER(15,2)   DEFAULT 0.00,
    total_net_pay        NUMBER(15,2)   DEFAULT 0.00,
    status               VARCHAR2(20)   DEFAULT 'DRAFT' NOT NULL,
    processed_at         TIMESTAMP,
    paid_at              TIMESTAMP,
    created_by           VARCHAR2(100),
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by           VARCHAR2(100),
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_pr_year_month UNIQUE (payroll_year, payroll_month)
);

CREATE TABLE payslips (
    id                   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payroll_run_id       NUMBER         NOT NULL,
    employee_id          NUMBER         NOT NULL,
    payroll_year         NUMBER(4)      NOT NULL,
    payroll_month        NUMBER(2)      NOT NULL,
    total_working_days   NUMBER(2)      NOT NULL,
    present_days         NUMBER(4,1)    NOT NULL,
    unpaid_leave_days    NUMBER(4,1)    DEFAULT 0.0 NOT NULL,
    basic_salary         NUMBER(15,2)   NOT NULL,
    hra                  NUMBER(15,2)   NOT NULL,
    special_allowance    NUMBER(15,2)   DEFAULT 0.00,
    other_allowances     NUMBER(15,2)   DEFAULT 0.00,
    gross_salary         NUMBER(15,2)   NOT NULL,
    lop_deduction        NUMBER(15,2)   DEFAULT 0.00,
    pf_deduction         NUMBER(15,2)   DEFAULT 0.00,
    esi_deduction        NUMBER(15,2)   DEFAULT 0.00,
    pt_deduction         NUMBER(15,2)   DEFAULT 0.00,
    tds_deduction        NUMBER(15,2)   DEFAULT 0.00,
    total_deductions     NUMBER(15,2)   NOT NULL,
    net_salary           NUMBER(15,2)   NOT NULL,
    status               VARCHAR2(20)   DEFAULT 'DRAFT' NOT NULL,
    created_by           VARCHAR2(100),
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by           VARCHAR2(100),
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ps_run FOREIGN KEY (payroll_run_id) REFERENCES payroll_runs(id),
    CONSTRAINT fk_ps_emp FOREIGN KEY (employee_id) REFERENCES employees(id)
);

CREATE TABLE notifications (
    id                   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id              NUMBER         NOT NULL,
    title                VARCHAR2(150)  NOT NULL,
    message              VARCHAR2(1000) NOT NULL,
    type                 VARCHAR2(30)   DEFAULT 'GENERAL' NOT NULL,
    is_read              NUMBER(1)      DEFAULT 0 NOT NULL,
    read_at              TIMESTAMP,
    link_url             VARCHAR2(255),
    created_by           VARCHAR2(100),
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by           VARCHAR2(100),
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id)
);
