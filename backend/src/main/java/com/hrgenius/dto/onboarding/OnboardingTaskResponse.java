package com.hrgenius.dto.onboarding;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class OnboardingTaskResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String title;
    private String description;
    private String category;
    private String status;
    private Long ownerId;
    private String ownerName;
    private LocalDate dueDate;
    private LocalDateTime completedAt;
}
