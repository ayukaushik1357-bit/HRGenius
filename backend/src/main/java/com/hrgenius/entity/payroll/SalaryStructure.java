package com.hrgenius.entity.payroll;

import com.hrgenius.entity.BaseEntity;
import com.hrgenius.entity.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "salary_structures")
@Getter
@Setter
@NoArgsConstructor
public class SalaryStructure extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee;

    @Column(name = "basic_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal basicSalary;

    @Column(name = "hra", nullable = false, precision = 15, scale = 2)
    private BigDecimal hra;

    @Column(name = "special_allowance", precision = 15, scale = 2)
    private BigDecimal specialAllowance = BigDecimal.ZERO;

    @Column(name = "other_allowances", precision = 15, scale = 2)
    private BigDecimal otherAllowances = BigDecimal.ZERO;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    public BigDecimal getMonthlyGross() {
        BigDecimal b = basicSalary != null ? basicSalary : BigDecimal.ZERO;
        BigDecimal h = hra != null ? hra : BigDecimal.ZERO;
        BigDecimal s = specialAllowance != null ? specialAllowance : BigDecimal.ZERO;
        BigDecimal o = otherAllowances != null ? otherAllowances : BigDecimal.ZERO;
        return b.add(h).add(s).add(o);
    }
}
