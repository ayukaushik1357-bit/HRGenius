package com.hrgenius.controller;

import com.hrgenius.dto.auth.AuthConfigResponse;
import com.hrgenius.dto.auth.AuthResponse;
import com.hrgenius.dto.auth.GoogleLoginRequest;
import com.hrgenius.dto.auth.LoginRequest;
import com.hrgenius.dto.auth.PasswordResetConfirmRequest;
import com.hrgenius.dto.auth.PasswordResetRequest;
import com.hrgenius.dto.auth.RegisterRequest;
import com.hrgenius.dto.auth.SignInResult;
import com.hrgenius.service.AccountService;
import com.hrgenius.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final AccountService accountService;

    @GetMapping("/config")
    @Operation(summary = "Public login-page settings: is sign-up enabled, is Google sign-in configured")
    public ResponseEntity<AuthConfigResponse> config() {
        return ResponseEntity.ok(accountService.publicConfig());
    }

    @PostMapping("/register")
    @Operation(summary = "Create an account and sign in (or wait for approval if that is switched on)")
    public ResponseEntity<SignInResult> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletResponse response) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.register(request, response));
    }

    @PostMapping("/password-reset/request")
    @Operation(summary = "Email a one-time password reset link (same response whether or not the email exists)")
    public ResponseEntity<Map<String, String>> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        return ResponseEntity.ok(Map.of("message", accountService.requestPasswordReset(request.getEmail())));
    }

    @PostMapping("/password-reset/confirm")
    @Operation(summary = "Set a new password using the token from the reset link")
    public ResponseEntity<Map<String, String>> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        accountService.confirmPasswordReset(request);
        return ResponseEntity.ok(Map.of("message", "Your password has been changed. You can now sign in."));
    }

    @PostMapping("/google")
    @Operation(summary = "Sign in with a Google ID token; a new Google user gets an account")
    public ResponseEntity<SignInResult> google(@Valid @RequestBody GoogleLoginRequest request,
                                               HttpServletResponse response) {
        SignInResult result = accountService.googleLogin(request.getCredential(), response);
        return ResponseEntity.status("PENDING_APPROVAL".equals(result.status()) ? HttpStatus.ACCEPTED : HttpStatus.OK)
                .body(result);
    }

    @PostMapping("/login")
    @Operation(summary = "Login with username or email and password; returns an access token and sets the refresh token as an HttpOnly cookie")
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

    @PostMapping("/change-password")
    @Operation(summary = "Change password for the logged-in user")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody com.hrgenius.dto.auth.ChangePasswordRequest request,
            Authentication authentication) {
        if (authentication == null) {
            authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        }
        if (authentication == null || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(401).build();
        }
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PostMapping("/reset-password")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin endpoint to reset a user's password and set must-change flag")
    public ResponseEntity<com.hrgenius.dto.auth.ResetPasswordResponse> adminResetPasswordBody(@RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") != null ? Long.valueOf(body.get("userId").toString()) : null;
        if (userId == null) {
            throw new com.hrgenius.exception.BadRequestException("userId is required");
        }
        com.hrgenius.dto.auth.ResetPasswordResponse response = authService.adminResetPassword(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password/{userId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin endpoint to reset a user's password and set must-change flag")
    public ResponseEntity<com.hrgenius.dto.auth.ResetPasswordResponse> adminResetPasswordPath(@PathVariable Long userId) {
        com.hrgenius.dto.auth.ResetPasswordResponse response = authService.adminResetPassword(userId);
        return ResponseEntity.ok(response);
    }
}
