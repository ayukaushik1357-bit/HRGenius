package com.hrgenius.dto.recruitment;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class CandidateResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String resumeFileName;
    private String resumeFilePath;
    private Double experienceYears;
    private String currentCompany;
    private BigDecimal expectedSalaryCTC;
    private LocalDateTime createdAt;
}
