package com.hrgenius.dto.performance;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GoalResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long reviewCycleId;
    private String reviewCycleName;
    private String title;
    private String description;
    private Double weightage;
    private Double progressPercentage;
}
