package com.hrgenius.dto.recruitment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class OfferRequest {

    @NotNull(message = "Application ID is required")
    private Long applicationId;

    @NotNull(message = "Offer date is required")
    private LocalDate offerDate;

    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;

    @NotNull(message = "Offered salary is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Offered salary must be positive")
    private BigDecimal offeredSalaryCTC;

    private String status; // PENDING, ACCEPTED, REJECTED
    private String remarks;
}
