package com.hrgenius.dto.performance;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PeerFeedbackRequest {

    @NotNull(message = "Performance review ID is required")
    private Long performanceReviewId;

    @NotBlank(message = "Feedback text is required")
    private String feedback;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Double rating;
}
