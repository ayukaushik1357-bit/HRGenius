package com.hrgenius.entity.payroll;

import com.hrgenius.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payroll_runs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"payroll_year", "payroll_month"})
})
@Getter
@Setter
@NoArgsConstructor
public class PayrollRun extends BaseEntity {

    public enum PayrollStatus {
        DRAFT, PROCESSED, PAID
    }

    @Column(name = "payroll_year", nullable = false)
    private Integer year;

    @Column(name = "payroll_month", nullable = false)
    private Integer month; // 1 to 12

    @Column(name = "total_employees", nullable = false)
    private Integer totalEmployees = 0;

    @Column(name = "total_gross_pay", precision = 15, scale = 2)
    private BigDecimal totalGrossPay = BigDecimal.ZERO;

    @Column(name = "total_net_pay", precision = 15, scale = 2)
    private BigDecimal totalNetPay = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayrollStatus status = PayrollStatus.DRAFT;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @OneToMany(mappedBy = "payrollRun", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payslip> payslips = new ArrayList<>();
}
