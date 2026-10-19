-- Deleting an employee hides the record but keeps their payslips, leave and attendance history
ALTER TABLE employees ADD is_deleted NUMBER(1) DEFAULT 0 NOT NULL;
