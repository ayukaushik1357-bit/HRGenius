package com.hrgenius.controller;

import com.hrgenius.dto.auth.AuthResponse;
import com.hrgenius.dto.auth.LoginRequest;
import com.hrgenius.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for login, logout, and token refresh")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login with username and password, returns access token and sets refresh token in HttpOnly cookie")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response) {
        return ResponseEntity.ok(authService.login(loginRequest, response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using HttpOnly refresh token cookie")
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        return ResponseEntity.ok(authService.refreshToken(request, response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user, revoke refresh token in DB, and clear refresh token cookie")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout from all devices, revoking all user refresh tokens")
    public ResponseEntity<Map<String, String>> logoutAll(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        String username = authentication != null ? authentication.getName() : null;
        if (username == null && org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null) {
            username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        }
        if (username == null) {
            String refreshToken = authService.extractRefreshTokenFromCookie(request);
            if (org.springframework.util.StringUtils.hasText(refreshToken)) {
                String tokenHash = AuthService.hashToken(refreshToken);
                username = authService.getUsernameFromTokenHash(tokenHash);
            }
        }
        if (username == null || "anonymousUser".equals(username)) {
            return ResponseEntity.status(401).build();
        }
        authService.logoutAll(username, response);
        return ResponseEntity.ok(Map.of("message", "Logged out from all devices successfully"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user info")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        }
        if (authentication == null || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(401).build();
        }
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return ResponseEntity.ok(Map.of(
                "username", authentication.getName(),
                "roles", roles
        ));
    }
}
