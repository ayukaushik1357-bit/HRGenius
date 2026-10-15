package com.hrgenius.repository;

import com.hrgenius.entity.onboarding.OnboardingTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OnboardingTemplateRepository extends JpaRepository<OnboardingTemplate, Long> {
    Optional<OnboardingTemplate> findByName(String name);
}
