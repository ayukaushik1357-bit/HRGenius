package com.hrgenius.dto.leave;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LeaveBalanceResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Integer year;
    private Double casualQuota;
    private Double casualUsed;
    private Double casualRemaining;
    private Double sickQuota;
    private Double sickUsed;
    private Double sickRemaining;
    private Double earnedQuota;
    private Double earnedUsed;
    private Double earnedRemaining;
}
