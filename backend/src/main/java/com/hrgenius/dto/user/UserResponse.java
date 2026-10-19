package com.hrgenius.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private boolean enabled;
    private boolean mustChangePassword;
    private boolean pendingApproval;
    private int failedAttempts;
    private LocalDateTime lockoutTime;
    private boolean locked;
    private Set<String> roles;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private LocalDateTime createdAt;
}
