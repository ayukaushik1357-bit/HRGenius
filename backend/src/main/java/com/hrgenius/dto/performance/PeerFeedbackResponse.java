package com.hrgenius.dto.performance;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PeerFeedbackResponse {
    private Long id;
    private Long performanceReviewId;
    private Long reviewerId;
    private String reviewerName;
    private String feedback;
    private Double rating;
    private LocalDateTime submittedAt;
}
