package com.hrgenius.repository;

import com.hrgenius.entity.audit.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByOrderByChangedAtDesc(Pageable pageable);
    List<AuditLog> findByEntityNameAndEntityIdOrderByChangedAtDesc(String entityName, Long entityId);
}
