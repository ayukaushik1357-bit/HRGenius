-- ============================================================
-- V6: Attendance and Leave Management
-- ============================================================

CREATE TABLE attendances (
    id                   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id          NUMBER         NOT NULL,
    attendance_date      DATE           NOT NULL,
    check_in_time        VARCHAR2(10),
    check_out_time       VARCHAR2(10),
    working_hours        NUMBER(5,2),
    status               VARCHAR2(20)   DEFAULT 'PRESENT' NOT NULL,
    notes                VARCHAR2(255),
    created_by           VARCHAR2(100),
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by           VARCHAR2(100),
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_att_emp_date UNIQUE (employee_id, attendance_date),
    CONSTRAINT fk_att_emp FOREIGN KEY (employee_id) REFERENCES employees(id)
);

CREATE TABLE holidays (
    id                   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    holiday_date         DATE           NOT NULL UNIQUE,
    name                 VARCHAR2(100)  NOT NULL,
    description          VARCHAR2(500),
    is_optional          NUMBER(1)      DEFAULT 0 NOT NULL,
    created_by           VARCHAR2(100),
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by           VARCHAR2(100),
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE leave_balances (
    id                   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id          NUMBER         NOT NULL,
    leave_year           NUMBER(4)      NOT NULL,
    casual_quota         NUMBER(5,2)    DEFAULT 12.0 NOT NULL,
    casual_used          NUMBER(5,2)    DEFAULT 0.0 NOT NULL,
    sick_quota           NUMBER(5,2)    DEFAULT 10.0 NOT NULL,
    sick_used            NUMBER(5,2)    DEFAULT 0.0 NOT NULL,
    earned_quota         NUMBER(5,2)    DEFAULT 15.0 NOT NULL,
    earned_used          NUMBER(5,2)    DEFAULT 0.0 NOT NULL,
    created_by           VARCHAR2(100),
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by           VARCHAR2(100),
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_lb_emp_year UNIQUE (employee_id, leave_year),
    CONSTRAINT fk_lb_emp FOREIGN KEY (employee_id) REFERENCES employees(id)
);

CREATE TABLE leave_applications (
    id                   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id          NUMBER         NOT NULL,
    leave_type           VARCHAR2(20)   NOT NULL,
    start_date           DATE           NOT NULL,
    end_date             DATE           NOT NULL,
    number_of_days       NUMBER(5,2)    NOT NULL,
    reason               VARCHAR2(1000) NOT NULL,
    status               VARCHAR2(20)   DEFAULT 'PENDING' NOT NULL,
    approved_by_id       NUMBER,
    rejection_reason     VARCHAR2(1000),
    actioned_at          TIMESTAMP,
    created_by           VARCHAR2(100),
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by           VARCHAR2(100),
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_la_emp FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_la_approver FOREIGN KEY (approved_by_id) REFERENCES employees(id)
);
