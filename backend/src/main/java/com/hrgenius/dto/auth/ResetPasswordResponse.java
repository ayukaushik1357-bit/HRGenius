package com.hrgenius.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordResponse {
    private Long userId;
    private String username;
    private String temporaryPassword;
    private boolean mustChangePassword;
    private String message;
}
