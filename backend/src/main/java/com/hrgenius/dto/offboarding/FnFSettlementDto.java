package com.hrgenius.dto.offboarding;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FnFSettlementDto {

    @Getter
    @Setter
    public static class Request {
        private Long exitRequestId;
        private LocalDate settlementDate;
        private Integer payableDays;
        private BigDecimal salaryPayable;
        private Double leaveEncashmentDays;
        private BigDecimal leaveEncashmentAmount;
        private BigDecimal gratuityAmount;
        private BigDecimal deductionsAmount;
        private String remarks;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private Long exitRequestId;
        private Long employeeId;
        private String employeeName;
        private String employeeCode;
        private LocalDate settlementDate;
        private Integer payableDays;
        private BigDecimal salaryPayable;
        private Double leaveEncashmentDays;
        private BigDecimal leaveEncashmentAmount;
        private BigDecimal gratuityAmount;
        private BigDecimal deductionsAmount;
        private BigDecimal netPayableAmount;
        private String status;
        private String remarks;
    }
}
