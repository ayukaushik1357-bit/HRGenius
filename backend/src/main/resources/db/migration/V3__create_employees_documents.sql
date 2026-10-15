-- ============================================================
-- V3: Employees, Addresses, Emergency Contacts, Documents
-- ============================================================

CREATE TABLE employees (
    id                    NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_code         VARCHAR2(20)   NOT NULL UNIQUE,
    first_name            VARCHAR2(50)   NOT NULL,
    last_name             VARCHAR2(50)   NOT NULL,
    email                 VARCHAR2(100)  NOT NULL UNIQUE,
    phone                 VARCHAR2(20),
    date_of_birth         DATE,
    gender                VARCHAR2(10),
    joining_date          DATE           NOT NULL,
    department_id         NUMBER,
    designation_id        NUMBER,
    reporting_manager_id  NUMBER,
    employment_type       VARCHAR2(20)   DEFAULT 'FULL_TIME' NOT NULL,
    status                VARCHAR2(20)   DEFAULT 'ACTIVE' NOT NULL,
    salary_ctc            NUMBER(15,2),
    user_id               NUMBER,
    created_by            VARCHAR2(100),
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by            VARCHAR2(100),
    updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_emp_dept      FOREIGN KEY (department_id)        REFERENCES departments(id),
    CONSTRAINT fk_emp_desig     FOREIGN KEY (designation_id)       REFERENCES designations(id),
    CONSTRAINT fk_emp_manager   FOREIGN KEY (reporting_manager_id) REFERENCES employees(id),
    CONSTRAINT fk_emp_user      FOREIGN KEY (user_id)              REFERENCES users(id)
);

CREATE TABLE addresses (
    id            NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    address_type  VARCHAR2(20)  NOT NULL,
    line1         VARCHAR2(200) NOT NULL,
    line2         VARCHAR2(200),
    city          VARCHAR2(100) NOT NULL,
    state         VARCHAR2(100) NOT NULL,
    country       VARCHAR2(100) NOT NULL,
    zip_code      VARCHAR2(20)  NOT NULL,
    employee_id   NUMBER        NOT NULL,
    created_by    VARCHAR2(100),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by    VARCHAR2(100),
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_addr_emp FOREIGN KEY (employee_id) REFERENCES employees(id)
);

CREATE TABLE emergency_contacts (
    id            NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR2(100) NOT NULL,
    relationship  VARCHAR2(50)  NOT NULL,
    phone         VARCHAR2(20)  NOT NULL,
    employee_id   NUMBER        NOT NULL,
    created_by    VARCHAR2(100),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by    VARCHAR2(100),
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ec_emp FOREIGN KEY (employee_id) REFERENCES employees(id)
);

CREATE TABLE documents (
    id                 NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    file_name          VARCHAR2(255) NOT NULL,
    original_file_name VARCHAR2(255) NOT NULL,
    content_type       VARCHAR2(100) NOT NULL,
    file_size          NUMBER,
    file_path          VARCHAR2(500) NOT NULL,
    document_type      VARCHAR2(30)  NOT NULL,
    employee_id        NUMBER        NOT NULL,
    created_by         VARCHAR2(100),
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by         VARCHAR2(100),
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_doc_emp FOREIGN KEY (employee_id) REFERENCES employees(id)
);
