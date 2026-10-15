package com.hrgenius.dto.performance;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PerformanceReviewResponse {
    private Long id;
    private Long reviewCycleId;
    private String reviewCycleName;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private Long managerId;
    private String managerName;
    private String status;
    private Double selfRating;
    private String selfComments;
    private Double managerRating;
    private String managerComments;
    private Double finalRating;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private List<PeerFeedbackResponse> peerFeedbacks;
}
