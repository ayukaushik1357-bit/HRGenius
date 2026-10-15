package com.hrgenius.entity.offboarding;

import com.hrgenius.entity.BaseEntity;
import com.hrgenius.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "clearance_items")
@Getter
@Setter
@NoArgsConstructor
public class ClearanceItem extends BaseEntity {

    public enum DepartmentType {
        IT, FINANCE, HR, ADMIN
    }

    public enum ClearanceStatus {
        PENDING, CLEARED, WAIVED
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exit_request_id", nullable = false)
    private ExitRequest exitRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "department_type", nullable = false, length = 30)
    private DepartmentType departmentType;

    @Column(nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClearanceStatus status = ClearanceStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cleared_by_id")
    private Employee clearedBy;

    @Column(name = "cleared_at")
    private LocalDateTime clearedAt;

    @Column(length = 500)
    private String remarks;
}
