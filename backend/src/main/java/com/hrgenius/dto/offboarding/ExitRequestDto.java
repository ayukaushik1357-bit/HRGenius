package com.hrgenius.dto.offboarding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ExitRequestDto {

    @Getter
    @Setter
    public static class Request {
        private Long employeeId;

        @NotNull(message = "Resignation date is required")
        private LocalDate resignationDate;

        private Integer noticePeriodDays = 30;

        @NotNull(message = "Last working day is required")
        private LocalDate lastWorkingDay;

        @NotBlank(message = "Reason is required")
        private String reason;
    }

    @Getter
    @Setter
    public static class ActionRequest {
        private boolean approved;
        private String remarks;
        private LocalDate agreedLastWorkingDay;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private Long employeeId;
        private String employeeName;
        private String employeeCode;
        private String departmentName;
        private String designationTitle;
        private LocalDate resignationDate;
        private Integer noticePeriodDays;
        private LocalDate lastWorkingDay;
        private String reason;
        private String status;
        private Long approvedById;
        private String approvedByName;
        private String remarks;
        private LocalDateTime createdAt;
        private List<ClearanceItemDto.Response> clearanceItems;
        private FnFSettlementDto.Response settlement;
    }
}
