package com.hrgenius.entity.selfservice;

import com.hrgenius.entity.BaseEntity;
import com.hrgenius.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "profile_update_requests")
@Getter
@Setter
@NoArgsConstructor
public class ProfileUpdateRequest extends BaseEntity {

    public enum RequestStatus {
        PENDING, APPROVED, REJECTED
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Lob
    @Column(name = "requested_changes", nullable = false)
    private String requestedChanges; // JSON string containing changed fields

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status = RequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private Employee reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(length = 500)
    private String remarks;
}
