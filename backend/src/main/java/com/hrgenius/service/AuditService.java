package com.hrgenius.service;

import com.hrgenius.dto.audit.AuditLogResponse;
import com.hrgenius.dto.common.PagedResponse;
import com.hrgenius.entity.audit.AuditLog;
import com.hrgenius.mapper.AuditLogMapper;
import com.hrgenius.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logChange(String entityName, Long entityId, String action, String oldValue, String newValue) {
        logChange(entityName, entityId, action, oldValue, newValue, "127.0.0.1");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logChange(String entityName, Long entityId, String action, String oldValue, String newValue, String ipAddress) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String changedBy = (auth != null && auth.getName() != null) ? auth.getName() : "SYSTEM";

            AuditLog auditLog = new AuditLog();
            auditLog.setEntityName(entityName);
            auditLog.setEntityId(entityId);
            auditLog.setAction(action);
            auditLog.setChangedBy(changedBy);
            auditLog.setChangedAt(LocalDateTime.now());
            auditLog.setOldValue(oldValue);
            auditLog.setNewValue(newValue);
            auditLog.setIpAddress(ipAddress != null ? ipAddress : "127.0.0.1");

            auditLogRepository.save(auditLog);
        } catch (Exception ex) {
            log.warn("Failed to write audit log for entity {}: {}", entityName, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> getAuditLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> logPage = auditLogRepository.findByOrderByChangedAtDesc(pageable);

        List<AuditLogResponse> content = logPage.getContent().stream()
                .map(auditLogMapper::toResponse)
                .collect(Collectors.toList());

        return PagedResponse.<AuditLogResponse>builder()
                .content(content)
                .page(logPage.getNumber())
                .size(logPage.getSize())
                .totalElements(logPage.getTotalElements())
                .totalPages(logPage.getTotalPages())
                .last(logPage.isLast())
                .build();
    }
}
