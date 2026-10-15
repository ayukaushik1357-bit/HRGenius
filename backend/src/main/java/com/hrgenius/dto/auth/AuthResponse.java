package com.hrgenius.dto.auth;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private String username;
    private String email;
    private Set<String> roles;
}
