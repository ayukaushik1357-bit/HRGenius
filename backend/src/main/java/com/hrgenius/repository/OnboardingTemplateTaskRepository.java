package com.hrgenius.repository;

import com.hrgenius.entity.onboarding.OnboardingTemplateTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnboardingTemplateTaskRepository extends JpaRepository<OnboardingTemplateTask, Long> {
    List<OnboardingTemplateTask> findByTemplateId(Long templateId);
}
