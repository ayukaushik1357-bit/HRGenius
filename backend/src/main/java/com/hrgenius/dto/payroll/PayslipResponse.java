package com.hrgenius.dto.payroll;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PayslipResponse {
    private Long id;
    private Long payrollRunId;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private String departmentName;
    private String designationTitle;
    private Integer year;
    private Integer month;
    private Integer totalWorkingDays;
    private Double presentDays;
    private Double unpaidLeaveDays;
    private BigDecimal basicSalary;
    private BigDecimal hra;
    private BigDecimal specialAllowance;
    private BigDecimal otherAllowances;
    private BigDecimal grossSalary;
    private BigDecimal lopDeduction;
    private BigDecimal pfDeduction;
    private BigDecimal esiDeduction;
    private BigDecimal ptDeduction;
    private BigDecimal tdsDeduction;
    private BigDecimal totalDeductions;
    private BigDecimal netSalary;
    private String status;
}
