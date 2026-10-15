package com.hrgenius.dto.recruitment;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class JobPostingResponse {
    private Long id;
    private String title;
    private String description;
    private String requirements;
    private String location;
    private Integer minExperienceYears;
    private Integer maxExperienceYears;
    private Integer openings;
    private String status;
    private Long departmentId;
    private String departmentName;
    private Long designationId;
    private String designationTitle;
    private String createdBy;
    private LocalDateTime createdAt;
}
