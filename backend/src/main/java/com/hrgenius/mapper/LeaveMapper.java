package com.hrgenius.mapper;

import com.hrgenius.dto.leave.LeaveApplicationResponse;
import com.hrgenius.dto.leave.LeaveBalanceResponse;
import com.hrgenius.entity.leave.LeaveApplication;
import com.hrgenius.entity.leave.LeaveBalance;
import org.springframework.stereotype.Component;

@Component
public class LeaveMapper {

    public LeaveApplicationResponse toApplicationResponse(LeaveApplication entity) {
        if (entity == null) return null;
        String employeeName = null;
        String employeeCode = null;
        Long employeeId = null;
        if (entity.getEmployee() != null) {
            employeeId = entity.getEmployee().getId();
            employeeName = entity.getEmployee().getFirstName() + " " + entity.getEmployee().getLastName();
            employeeCode = entity.getEmployee().getEmployeeCode();
        }

        String approvedByName = null;
        Long approvedById = null;
        if (entity.getApprovedBy() != null) {
            approvedById = entity.getApprovedBy().getId();
            approvedByName = entity.getApprovedBy().getFirstName() + " " + entity.getApprovedBy().getLastName();
        }

        return LeaveApplicationResponse.builder()
                .id(entity.getId())
                .employeeId(employeeId)
                .employeeName(employeeName)
                .employeeCode(employeeCode)
                .leaveType(entity.getLeaveType() != null ? entity.getLeaveType().name() : null)
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .numberOfDays(entity.getNumberOfDays())
                .reason(entity.getReason())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .approvedById(approvedById)
                .approvedByName(approvedByName)
                .rejectionReason(entity.getRejectionReason())
                .actionedAt(entity.getActionedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public LeaveBalanceResponse toBalanceResponse(LeaveBalance entity) {
        if (entity == null) return null;
        String employeeName = null;
        Long employeeId = null;
        if (entity.getEmployee() != null) {
            employeeId = entity.getEmployee().getId();
            employeeName = entity.getEmployee().getFirstName() + " " + entity.getEmployee().getLastName();
        }

        return LeaveBalanceResponse.builder()
                .id(entity.getId())
                .employeeId(employeeId)
                .employeeName(employeeName)
                .year(entity.getYear())
                .casualQuota(entity.getCasualQuota())
                .casualUsed(entity.getCasualUsed())
                .casualRemaining(entity.getCasualRemaining())
                .sickQuota(entity.getSickQuota())
                .sickUsed(entity.getSickUsed())
                .sickRemaining(entity.getSickRemaining())
                .earnedQuota(entity.getEarnedQuota())
                .earnedUsed(entity.getEarnedUsed())
                .earnedRemaining(entity.getEarnedRemaining())
                .build();
    }
}
