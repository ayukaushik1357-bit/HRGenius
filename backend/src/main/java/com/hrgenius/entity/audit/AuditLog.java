package com.hrgenius.entity.audit;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_name", nullable = false, length = 100)
    private String entityName;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(nullable = false, length = 30)
    private String action; // CREATE, UPDATE, DELETE

    @Column(name = "changed_by", nullable = false, length = 50)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "old_value")
    private String oldValue;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "new_value")
    private String newValue;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;
}
