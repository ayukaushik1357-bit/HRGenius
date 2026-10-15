package com.hrgenius.repository;

import com.hrgenius.entity.performance.PerformanceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, Long> {
    Optional<PerformanceReview> findByReviewCycleIdAndEmployeeId(Long reviewCycleId, Long employeeId);
    List<PerformanceReview> findByEmployeeId(Long employeeId);
    List<PerformanceReview> findByManagerId(Long managerId);
    List<PerformanceReview> findByReviewCycleId(Long reviewCycleId);

    @Query("SELECT pr.finalRating, COUNT(pr) FROM PerformanceReview pr " +
           "WHERE pr.status = 'COMPLETED' AND pr.finalRating IS NOT NULL GROUP BY pr.finalRating")
    List<Object[]> getRatingDistribution();
}
