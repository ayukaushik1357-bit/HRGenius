package com.hrgenius.mapper;

import com.hrgenius.dto.audit.AuditLogResponse;
import com.hrgenius.dto.selfservice.ProfileUpdateRequestDto;
import com.hrgenius.entity.audit.AuditLog;
import com.hrgenius.entity.selfservice.ProfileUpdateRequest;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLogResponse toResponse(AuditLog entity) {
        if (entity == null) return null;
        return AuditLogResponse.builder()
                .id(entity.getId())
                .entityName(entity.getEntityName())
                .entityId(entity.getEntityId())
                .action(entity.getAction())
                .changedBy(entity.getChangedBy())
                .changedAt(entity.getChangedAt())
                .oldValue(entity.getOldValue())
                .newValue(entity.getNewValue())
                .ipAddress(entity.getIpAddress())
                .build();
    }

    public ProfileUpdateRequestDto.Response toProfileUpdateResponse(ProfileUpdateRequest entity) {
        if (entity == null) return null;
        return ProfileUpdateRequestDto.Response.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployee() != null ? entity.getEmployee().getId() : null)
                .employeeName(entity.getEmployee() != null ? entity.getEmployee().getFirstName() + " " + entity.getEmployee().getLastName() : null)
                .employeeCode(entity.getEmployee() != null ? entity.getEmployee().getEmployeeCode() : null)
                .requestedChanges(entity.getRequestedChanges())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .reviewedById(entity.getReviewedBy() != null ? entity.getReviewedBy().getId() : null)
                .reviewedByName(entity.getReviewedBy() != null ? entity.getReviewedBy().getFirstName() + " " + entity.getReviewedBy().getLastName() : null)
                .reviewedAt(entity.getReviewedAt())
                .remarks(entity.getRemarks())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
