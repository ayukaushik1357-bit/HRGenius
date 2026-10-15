package com.hrgenius.dto.onboarding;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OnboardingProgressResponse {
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private long totalTasks;
    private long completedTasks;
    private double progressPercentage;
    private List<OnboardingTaskResponse> tasks;
}
