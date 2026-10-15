package com.hrgenius.dto.leave;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class LeaveApplicationResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double numberOfDays;
    private String reason;
    private String status;
    private Long approvedById;
    private String approvedByName;
    private String rejectionReason;
    private LocalDateTime actionedAt;
    private LocalDateTime createdAt;
}
