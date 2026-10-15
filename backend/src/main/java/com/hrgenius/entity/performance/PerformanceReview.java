package com.hrgenius.entity.performance;

import com.hrgenius.entity.BaseEntity;
import com.hrgenius.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "performance_reviews", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"review_cycle_id", "employee_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class PerformanceReview extends BaseEntity {

    public enum ReviewStatus {
        NOT_STARTED, SELF_REVIEW, MANAGER_REVIEW, COMPLETED
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_cycle_id", nullable = false)
    private ReviewCycle reviewCycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewStatus status = ReviewStatus.NOT_STARTED;

    @Column(name = "self_rating")
    private Double selfRating; // 1.0 to 5.0

    @Column(name = "self_comments", length = 2000)
    private String selfComments;

    @Column(name = "manager_rating")
    private Double managerRating; // 1.0 to 5.0

    @Column(name = "manager_comments", length = 2000)
    private String managerComments;

    @Column(name = "final_rating")
    private Double finalRating; // Calculated or consensus

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @OneToMany(mappedBy = "performanceReview", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PeerFeedback> peerFeedbacks = new ArrayList<>();
}
