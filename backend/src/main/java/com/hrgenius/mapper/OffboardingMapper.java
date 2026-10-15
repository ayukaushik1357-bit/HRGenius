package com.hrgenius.mapper;

import com.hrgenius.dto.offboarding.ClearanceItemDto;
import com.hrgenius.dto.offboarding.ExitRequestDto;
import com.hrgenius.dto.offboarding.FnFSettlementDto;
import com.hrgenius.entity.offboarding.ClearanceItem;
import com.hrgenius.entity.offboarding.ExitRequest;
import com.hrgenius.entity.offboarding.FnFSettlement;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Collectors;

@Component
public class OffboardingMapper {

    public ExitRequestDto.Response toExitResponse(ExitRequest entity) {
        if (entity == null) return null;
        return ExitRequestDto.Response.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployee() != null ? entity.getEmployee().getId() : null)
                .employeeName(entity.getEmployee() != null ? entity.getEmployee().getFirstName() + " " + entity.getEmployee().getLastName() : null)
                .employeeCode(entity.getEmployee() != null ? entity.getEmployee().getEmployeeCode() : null)
                .departmentName(entity.getEmployee() != null && entity.getEmployee().getDepartment() != null ? entity.getEmployee().getDepartment().getName() : null)
                .designationTitle(entity.getEmployee() != null && entity.getEmployee().getDesignation() != null ? entity.getEmployee().getDesignation().getTitle() : null)
                .resignationDate(entity.getResignationDate())
                .noticePeriodDays(entity.getNoticePeriodDays())
                .lastWorkingDay(entity.getLastWorkingDay())
                .reason(entity.getReason())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .approvedById(entity.getApprovedBy() != null ? entity.getApprovedBy().getId() : null)
                .approvedByName(entity.getApprovedBy() != null ? entity.getApprovedBy().getFirstName() + " " + entity.getApprovedBy().getLastName() : null)
                .remarks(entity.getRemarks())
                .createdAt(entity.getCreatedAt())
                .clearanceItems(entity.getClearanceItems() == null ? Collections.emptyList() :
                        entity.getClearanceItems().stream().map(this::toClearanceResponse).collect(Collectors.toList()))
                .settlement(entity.getSettlement() != null ? toSettlementResponse(entity.getSettlement()) : null)
                .build();
    }

    public ClearanceItemDto.Response toClearanceResponse(ClearanceItem entity) {
        if (entity == null) return null;
        return ClearanceItemDto.Response.builder()
                .id(entity.getId())
                .exitRequestId(entity.getExitRequest() != null ? entity.getExitRequest().getId() : null)
                .departmentType(entity.getDepartmentType() != null ? entity.getDepartmentType().name() : null)
                .title(entity.getTitle())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .clearedById(entity.getClearedBy() != null ? entity.getClearedBy().getId() : null)
                .clearedByName(entity.getClearedBy() != null ? entity.getClearedBy().getFirstName() + " " + entity.getClearedBy().getLastName() : null)
                .clearedAt(entity.getClearedAt())
                .remarks(entity.getRemarks())
                .build();
    }

    public FnFSettlementDto.Response toSettlementResponse(FnFSettlement entity) {
        if (entity == null) return null;
        return FnFSettlementDto.Response.builder()
                .id(entity.getId())
                .exitRequestId(entity.getExitRequest() != null ? entity.getExitRequest().getId() : null)
                .employeeId(entity.getEmployee() != null ? entity.getEmployee().getId() : null)
                .employeeName(entity.getEmployee() != null ? entity.getEmployee().getFirstName() + " " + entity.getEmployee().getLastName() : null)
                .employeeCode(entity.getEmployee() != null ? entity.getEmployee().getEmployeeCode() : null)
                .settlementDate(entity.getSettlementDate())
                .payableDays(entity.getPayableDays())
                .salaryPayable(entity.getSalaryPayable())
                .leaveEncashmentDays(entity.getLeaveEncashmentDays())
                .leaveEncashmentAmount(entity.getLeaveEncashmentAmount())
                .gratuityAmount(entity.getGratuityAmount())
                .deductionsAmount(entity.getDeductionsAmount())
                .netPayableAmount(entity.getNetPayableAmount())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .remarks(entity.getRemarks())
                .build();
    }
}
