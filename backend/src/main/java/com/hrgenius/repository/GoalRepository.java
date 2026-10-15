package com.hrgenius.repository;

import com.hrgenius.entity.performance.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByEmployeeIdAndReviewCycleId(Long employeeId, Long reviewCycleId);
    List<Goal> findByEmployeeId(Long employeeId);
}
