package com.hrgenius.entity.performance;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.hrgenius.entity.BaseEntity;
import com.hrgenius.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "peer_feedbacks")
@Getter
@Setter
@NoArgsConstructor
public class PeerFeedback extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_review_id", nullable = false)
    private PerformanceReview performanceReview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private Employee reviewer;

    @Column(nullable = false, length = 2000)
    private String feedback;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(precision = 3, scale = 1)
    private Double rating; // 1.0 to 5.0

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();
}
