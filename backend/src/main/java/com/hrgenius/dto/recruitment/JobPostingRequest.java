package com.hrgenius.dto.recruitment;

import com.hrgenius.entity.recruitment.JobPosting;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobPostingRequest {

    @NotBlank(message = "Job title is required")
    private String title;

    private String description;
    private String requirements;
    private String location;
    private Integer minExperienceYears;
    private Integer maxExperienceYears;

    @NotNull(message = "Openings count is required")
    @Min(value = 1, message = "Openings must be at least 1")
    private Integer openings = 1;

    private JobPosting.JobPostingStatus status = JobPosting.JobPostingStatus.DRAFT;

    private Long departmentId;
    private Long designationId;
}
