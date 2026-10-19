package com.hrgenius.entity.payroll;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.hrgenius.entity.BaseEntity;
import com.hrgenius.entity.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "payslips")
@Getter
@Setter
@NoArgsConstructor
public class Payslip extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "payroll_year", nullable = false)
    private Integer year;

    @Column(name = "payroll_month", nullable = false)
    private Integer month;

    // Attendance
    @Column(name = "total_working_days", nullable = false)
    private Integer totalWorkingDays;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "present_days", nullable = false, precision = 4, scale = 1)
    private Double presentDays;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "unpaid_leave_days", nullable = false, precision = 4, scale = 1)
    private Double unpaidLeaveDays = 0.0;

    // Earnings
    @Column(name = "basic_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal basicSalary;

    @Column(name = "hra", nullable = false, precision = 15, scale = 2)
    private BigDecimal hra;

    @Column(name = "special_allowance", precision = 15, scale = 2)
    private BigDecimal specialAllowance = BigDecimal.ZERO;

    @Column(name = "other_allowances", precision = 15, scale = 2)
    private BigDecimal otherAllowances = BigDecimal.ZERO;

    @Column(name = "gross_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal grossSalary;

    // Deductions
    @Column(name = "lop_deduction", precision = 15, scale = 2)
    private BigDecimal lopDeduction = BigDecimal.ZERO;

    @Column(name = "pf_deduction", precision = 15, scale = 2)
    private BigDecimal pfDeduction = BigDecimal.ZERO;

    @Column(name = "esi_deduction", precision = 15, scale = 2)
    private BigDecimal esiDeduction = BigDecimal.ZERO;

    @Column(name = "pt_deduction", precision = 15, scale = 2)
    private BigDecimal ptDeduction = BigDecimal.ZERO;

    @Column(name = "tds_deduction", precision = 15, scale = 2)
    private BigDecimal tdsDeduction = BigDecimal.ZERO;

    @Column(name = "total_deductions", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDeductions;

    @Column(name = "net_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal netSalary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayrollRun.PayrollStatus status = PayrollRun.PayrollStatus.DRAFT;
}
