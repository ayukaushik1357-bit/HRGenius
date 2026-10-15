package com.hrgenius.dto.recruitment;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InterviewResponse {
    private Long id;
    private Long applicationId;
    private Long candidateId;
    private String candidateName;
    private String jobPostingTitle;
    private Long interviewerId;
    private String interviewerName;
    private LocalDateTime scheduledAt;
    private String roundName;
    private Integer rating;
    private String feedback;
    private String status;
}
