package com.hrgenius.dto.recruitment;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class OfferResponse {
    private Long id;
    private Long applicationId;
    private Long candidateId;
    private String candidateName;
    private String jobPostingTitle;
    private LocalDate offerDate;
    private LocalDate joiningDate;
    private BigDecimal offeredSalaryCTC;
    private String status;
    private String remarks;
}
