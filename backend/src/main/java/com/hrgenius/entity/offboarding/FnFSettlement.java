package com.hrgenius.entity.offboarding;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.hrgenius.entity.BaseEntity;
import com.hrgenius.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fnf_settlements")
@Getter
@Setter
@NoArgsConstructor
public class FnFSettlement extends BaseEntity {

    public enum SettlementStatus {
        DRAFT, PROCESSED, PAID
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exit_request_id", nullable = false, unique = true)
    private ExitRequest exitRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "payable_days", nullable = false)
    private Integer payableDays = 0;

    @Column(name = "salary_payable", nullable = false, precision = 15, scale = 2)
    private BigDecimal salaryPayable = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "leave_encashment_days", nullable = false, precision = 5, scale = 2)
    private Double leaveEncashmentDays = 0.0;

    @Column(name = "leave_encashment_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal leaveEncashmentAmount = BigDecimal.ZERO;

    @Column(name = "gratuity_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal gratuityAmount = BigDecimal.ZERO;

    @Column(name = "deductions_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal deductionsAmount = BigDecimal.ZERO;

    @Column(name = "net_payable_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal netPayableAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status = SettlementStatus.DRAFT;

    @Column(length = 1000)
    private String remarks;
}
