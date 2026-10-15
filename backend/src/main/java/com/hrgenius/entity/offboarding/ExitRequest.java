package com.hrgenius.entity.offboarding;

import com.hrgenius.entity.BaseEntity;
import com.hrgenius.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exit_requests")
@Getter
@Setter
@NoArgsConstructor
public class ExitRequest extends BaseEntity {

    public enum ExitStatus {
        PENDING, APPROVED, REJECTED, COMPLETED
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "resignation_date", nullable = false)
    private LocalDate resignationDate;

    @Column(name = "notice_period_days", nullable = false)
    private Integer noticePeriodDays = 30;

    @Column(name = "last_working_day", nullable = false)
    private LocalDate lastWorkingDay;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExitStatus status = ExitStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private Employee approvedBy;

    @Column(length = 1000)
    private String remarks;

    @OneToMany(mappedBy = "exitRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClearanceItem> clearanceItems = new ArrayList<>();

    @OneToOne(mappedBy = "exitRequest", cascade = CascadeType.ALL)
    private FnFSettlement settlement;
}
