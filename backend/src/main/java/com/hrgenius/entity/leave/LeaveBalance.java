package com.hrgenius.entity.leave;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.hrgenius.entity.BaseEntity;
import com.hrgenius.entity.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "leave_balances", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"employee_id", "leave_year"})
})
@Getter
@Setter
@NoArgsConstructor
public class LeaveBalance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "leave_year", nullable = false)
    private Integer year;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "casual_quota", nullable = false, precision = 5, scale = 2)
    private Double casualQuota = 12.0;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "casual_used", nullable = false, precision = 5, scale = 2)
    private Double casualUsed = 0.0;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "sick_quota", nullable = false, precision = 5, scale = 2)
    private Double sickQuota = 10.0;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "sick_used", nullable = false, precision = 5, scale = 2)
    private Double sickUsed = 0.0;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "earned_quota", nullable = false, precision = 5, scale = 2)
    private Double earnedQuota = 15.0;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "earned_used", nullable = false, precision = 5, scale = 2)
    private Double earnedUsed = 0.0;

    public Double getCasualRemaining() {
        return Math.max(0, casualQuota - casualUsed);
    }

    public Double getSickRemaining() {
        return Math.max(0, sickQuota - sickUsed);
    }

    public Double getEarnedRemaining() {
        return Math.max(0, earnedQuota - earnedUsed);
    }
}
