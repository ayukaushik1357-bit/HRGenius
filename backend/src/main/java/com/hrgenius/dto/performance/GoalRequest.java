package com.hrgenius.dto.performance;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoalRequest {

    @NotNull(message = "Review cycle ID is required")
    private Long reviewCycleId;

    private Long employeeId;

    @NotBlank(message = "Goal title is required")
    private String title;

    private String description;

    @NotNull(message = "Weightage is required")
    @Min(value = 0, message = "Weightage cannot be negative")
    @Max(value = 100, message = "Weightage cannot exceed 100")
    private Double weightage;

    @Min(value = 0, message = "Progress cannot be negative")
    @Max(value = 100, message = "Progress cannot exceed 100")
    private Double progressPercentage = 0.0;
}
