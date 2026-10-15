package com.hrgenius.dto.audit;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuditLogResponse {
    private Long id;
    private String entityName;
    private Long entityId;
    private String action;
    private String changedBy;
    private LocalDateTime changedAt;
    private String oldValue;
    private String newValue;
    private String ipAddress;
}
