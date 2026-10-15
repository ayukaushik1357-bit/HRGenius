package com.hrgenius.dto.onboarding;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingTemplateTaskRequest {

    @NotBlank(message = "Task title is required")
    private String title;

    private String description;
    private String category; // DOCUMENTS, IT_SETUP, INDUCTION, TRAINING

    @Min(value = 1, message = "Days to complete must be at least 1")
    private Integer defaultDaysToComplete = 3;
}
