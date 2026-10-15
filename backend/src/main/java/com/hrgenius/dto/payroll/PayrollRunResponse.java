package com.hrgenius.dto.payroll;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PayrollRunResponse {
    private Long id;
    private Integer year;
    private Integer month;
    private Integer totalEmployees;
    private BigDecimal totalGrossPay;
    private BigDecimal totalNetPay;
    private String status;
    private LocalDateTime processedAt;
    private LocalDateTime paidAt;
}
