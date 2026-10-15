package com.hrgenius.dto.recruitment;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationRequest {

    @NotNull(message = "Job posting ID is required")
    private Long jobPostingId;

    @NotNull(message = "Candidate ID is required")
    private Long candidateId;

    private String notes;
}
