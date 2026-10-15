package com.hrgenius.dto.leave;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaveApprovalRequest {
    private boolean approved;
    private String rejectionReason;
}
