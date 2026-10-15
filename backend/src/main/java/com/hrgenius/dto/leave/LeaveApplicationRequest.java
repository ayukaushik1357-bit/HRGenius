package com.hrgenius.dto.leave;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class LeaveApplicationRequest {

    @NotBlank(message = "Leave type is required")
    private String leaveType; // CASUAL, SICK, EARNED, UNPAID

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Number of days is required")
    @DecimalMin(value = "0.5", message = "Leave duration must be at least 0.5 days")
    private Double numberOfDays;

    @NotBlank(message = "Reason is required")
    private String reason;
}
