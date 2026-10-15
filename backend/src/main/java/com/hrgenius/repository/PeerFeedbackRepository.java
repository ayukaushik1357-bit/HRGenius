package com.hrgenius.repository;

import com.hrgenius.entity.performance.PeerFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PeerFeedbackRepository extends JpaRepository<PeerFeedback, Long> {
    List<PeerFeedback> findByPerformanceReviewId(Long performanceReviewId);
    List<PeerFeedback> findByReviewerId(Long reviewerId);
}
