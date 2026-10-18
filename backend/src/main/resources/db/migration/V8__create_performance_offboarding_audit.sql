-- V8: Performance Management, Offboarding/FnF, Audit Logs, and Profile Update Requests

-- 1. Review Cycles
CREATE TABLE review_cycles (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR2(100) NOT NULL UNIQUE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by VARCHAR2(50),
    updated_at TIMESTAMP,
    updated_by VARCHAR2(50)
);

-- 2. Goals / KRAs
CREATE TABLE goals (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    review_cycle_id BIGINT NOT NULL,
    title VARCHAR2(150) NOT NULL,
    description VARCHAR2(1000),
    weightage NUMBER(5, 2) DEFAULT 0.0 NOT NULL,
    progress_percentage NUMBER(5, 2) DEFAULT 0.0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by VARCHAR2(50),
    updated_at TIMESTAMP,
    updated_by VARCHAR2(50),
    CONSTRAINT fk_goal_emp FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_goal_cycle FOREIGN KEY (review_cycle_id) REFERENCES review_cycles(id)
);

-- 3. Performance Reviews
CREATE TABLE performance_reviews (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    review_cycle_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    manager_id BIGINT,
    status VARCHAR2(30) DEFAULT 'NOT_STARTED' NOT NULL,
    self_rating NUMBER(3, 1),
    self_comments VARCHAR2(2000),
    manager_rating NUMBER(3, 1),
    manager_comments VARCHAR2(2000),
    final_rating NUMBER(3, 2),
    submitted_at TIMESTAMP,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by VARCHAR2(50),
    updated_at TIMESTAMP,
    updated_by VARCHAR2(50),
    CONSTRAINT uq_rev_emp_cycle UNIQUE (review_cycle_id, employee_id),
    CONSTRAINT fk_pr_cycle FOREIGN KEY (review_cycle_id) REFERENCES review_cycles(id),
    CONSTRAINT fk_pr_emp FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_pr_mgr FOREIGN KEY (manager_id) REFERENCES employees(id)
);

-- 4. 360 Peer Feedback
CREATE TABLE peer_feedbacks (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    performance_review_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    feedback VARCHAR2(2000) NOT NULL,
    rating NUMBER(3, 1),
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by VARCHAR2(50),
    updated_at TIMESTAMP,
    updated_by VARCHAR2(50),
    CONSTRAINT fk_pf_review FOREIGN KEY (performance_review_id) REFERENCES performance_reviews(id) ON DELETE CASCADE,
    CONSTRAINT fk_pf_reviewer FOREIGN KEY (reviewer_id) REFERENCES employees(id)
);

-- 5. Exit Requests (Resignation / Offboarding)
CREATE TABLE exit_requests (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    resignation_date DATE NOT NULL,
    notice_period_days INTEGER DEFAULT 30 NOT NULL,
    last_working_day DATE NOT NULL,
    reason VARCHAR2(1000) NOT NULL,
    status VARCHAR2(20) DEFAULT 'PENDING' NOT NULL,
    approved_by_id BIGINT,
    remarks VARCHAR2(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by VARCHAR2(50),
    updated_at TIMESTAMP,
    updated_by VARCHAR2(50),
    CONSTRAINT fk_exit_emp FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_exit_appr FOREIGN KEY (approved_by_id) REFERENCES employees(id)
);

-- 6. Clearance Items
CREATE TABLE clearance_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    exit_request_id BIGINT NOT NULL,
    department_type VARCHAR2(30) NOT NULL,
    title VARCHAR2(150) NOT NULL,
    status VARCHAR2(20) DEFAULT 'PENDING' NOT NULL,
    cleared_by_id BIGINT,
    cleared_at TIMESTAMP,
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by VARCHAR2(50),
    updated_at TIMESTAMP,
    updated_by VARCHAR2(50),
    CONSTRAINT fk_cl_exit FOREIGN KEY (exit_request_id) REFERENCES exit_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_cl_emp FOREIGN KEY (cleared_by_id) REFERENCES employees(id)
);

-- 7. Full & Final Settlement (FnF)
CREATE TABLE fnf_settlements (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    exit_request_id BIGINT NOT NULL UNIQUE,
    employee_id BIGINT NOT NULL,
    settlement_date DATE NOT NULL,
    payable_days INTEGER DEFAULT 0 NOT NULL,
    salary_payable NUMBER(15, 2) DEFAULT 0.0 NOT NULL,
    leave_encashment_days NUMBER(5, 2) DEFAULT 0.0 NOT NULL,
    leave_encashment_amount NUMBER(15, 2) DEFAULT 0.0 NOT NULL,
    gratuity_amount NUMBER(15, 2) DEFAULT 0.0 NOT NULL,
    deductions_amount NUMBER(15, 2) DEFAULT 0.0 NOT NULL,
    net_payable_amount NUMBER(15, 2) DEFAULT 0.0 NOT NULL,
    status VARCHAR2(20) DEFAULT 'DRAFT' NOT NULL,
    remarks VARCHAR2(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by VARCHAR2(50),
    updated_at TIMESTAMP,
    updated_by VARCHAR2(50),
    CONSTRAINT fk_fnf_exit FOREIGN KEY (exit_request_id) REFERENCES exit_requests(id),
    CONSTRAINT fk_fnf_emp FOREIGN KEY (employee_id) REFERENCES employees(id)
);

-- 8. Audit Logs
CREATE TABLE audit_logs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entity_name VARCHAR2(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR2(30) NOT NULL,
    changed_by VARCHAR2(50) NOT NULL,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    old_value CLOB,
    new_value CLOB,
    ip_address VARCHAR2(50)
);

-- 9. Profile Update Requests (Self-service with HR approval)
CREATE TABLE profile_update_requests (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    requested_changes CLOB NOT NULL,
    status VARCHAR2(20) DEFAULT 'PENDING' NOT NULL,
    reviewed_by_id BIGINT,
    reviewed_at TIMESTAMP,
    remarks VARCHAR2(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by VARCHAR2(50),
    updated_at TIMESTAMP,
    updated_by VARCHAR2(50),
    CONSTRAINT fk_pur_emp FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_pur_rev FOREIGN KEY (reviewed_by_id) REFERENCES employees(id)
);

-- 10. Security: Account Lockout columns on users
ALTER TABLE users ADD failed_attempts INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE users ADD lockout_time TIMESTAMP;

-- 11. Sensitive fields on employees (masked for non-HR)
ALTER TABLE employees ADD pan_number VARCHAR2(20);
ALTER TABLE employees ADD aadhaar_number VARCHAR2(20);
ALTER TABLE employees ADD bank_account_number VARCHAR2(30);
ALTER TABLE employees ADD bank_ifsc_code VARCHAR2(20);
ALTER TABLE employees ADD bank_name VARCHAR2(100);

