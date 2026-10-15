package com.hrgenius.dto.recruitment;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ApplicationResponse {
    private Long id;
    private Long jobPostingId;
    private String jobPostingTitle;
    private Long candidateId;
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;
    private String resumeFileName;
    private String stage;
    private LocalDateTime appliedAt;
    private String notes;
}
