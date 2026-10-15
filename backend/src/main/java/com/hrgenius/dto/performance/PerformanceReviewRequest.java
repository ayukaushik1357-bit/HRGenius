package com.hrgenius.dto.performance;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformanceReviewRequest {

    private Long reviewCycleId;
    private Long employeeId;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Double rating;

    private String comments;

    private String actionType; // "SELF_SUBMIT" or "MANAGER_SUBMIT"
}
