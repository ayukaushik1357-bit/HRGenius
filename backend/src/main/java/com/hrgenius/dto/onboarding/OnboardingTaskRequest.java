package com.hrgenius.dto.onboarding;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class OnboardingTaskRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private String category;
    private String status; // PENDING, IN_PROGRESS, COMPLETED
    private Long ownerId;
    private LocalDate dueDate;
}
