package com.hrgenius.repository;

import com.hrgenius.entity.onboarding.OnboardingTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnboardingTaskRepository extends JpaRepository<OnboardingTask, Long> {
    List<OnboardingTask> findByEmployeeId(Long employeeId);
    List<OnboardingTask> findByOwnerId(Long ownerId);
    long countByEmployeeId(Long employeeId);
    long countByEmployeeIdAndStatus(Long employeeId, OnboardingTask.TaskStatus status);
}
