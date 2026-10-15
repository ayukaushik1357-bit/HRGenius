package com.hrgenius.dto.onboarding;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OnboardingTemplateRequest {

    @NotBlank(message = "Template name is required")
    private String name;

    private String description;

    private List<OnboardingTemplateTaskRequest> tasks;
}
