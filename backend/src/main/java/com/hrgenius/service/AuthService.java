package com.hrgenius.service;

import com.hrgenius.dto.auth.AuthResponse;
import com.hrgenius.dto.auth.ChangePasswordRequest;
import com.hrgenius.dto.auth.LoginRequest;
import com.hrgenius.dto.auth.ResetPasswordResponse;
import com.hrgenius.entity.RefreshToken;
import com.hrgenius.entity.User;
import com.hrgenius.exception.AccountPendingApprovalException;
import com.hrgenius.exception.BadRequestException;
import com.hrgenius.exception.ResourceNotFoundException;
import com.hrgenius.repository.RefreshTokenRepository;
import com.hrgenius.repository.UserRepository;
import com.hrgenius.security.JwtTokenProvider;
import com.hrgenius.security.UserPrincipal;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "hrgenius_refresh_token";

    /** How long after a rotation the previous refresh token may still be replayed (page reload / two tabs). */
    static final Duration REFRESH_REUSE_GRACE = Duration.ofSeconds(10);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final com.hrgenius.security.CookieProperties cookieProperties;
    private final PasswordEncoder passwordEncoder;

    /**
     * True when a revoked token was rotated moments ago and its replacement is still unused:
     * that is a client race, not a stolen token.
     */
    private boolean isBenignConcurrentReuse(RefreshToken revokedToken) {
        if (revokedToken.getReplacedBy() == null) {
            return false; // revoked by logout / logout-all, never valid again
        }
        return refreshTokenRepository.findByTokenHash(revokedToken.getReplacedBy())
                .map(next -> !next.isRevoked()
                        && next.getCreatedAt().isAfter(LocalDateTime.now().minus(REFRESH_REUSE_GRACE)))
                .orElse(false);
    }

    public static String hashToken(String token) {
        if (token == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    // noRollbackFor: the failed-attempt counter and lockout are saved right before these exceptions
    // are thrown; without it the exception would roll the counter back and lockout would never trigger.
    @Transactional(noRollbackFor = {BadCredentialsException.class, LockedException.class,
            AccountPendingApprovalException.class})
    public AuthResponse login(LoginRequest loginRequest, HttpServletResponse response) {
        // The "username" field accepts either a username or an email address
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(loginRequest.getUsername());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getLockoutTime() != null) {
                if (user.getLockoutTime().isAfter(LocalDateTime.now())) {
                    throw new LockedException("Account is temporarily locked. Try again later.");
                } else {
                    user.setLockoutTime(null);
                    user.setFailedAttempts(0);
                    userRepository.save(user);
                }
            }
            // Only reveal "pending approval" to someone who knows the password
            if (user.isPendingApproval()) {
                if (passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                    throw new AccountPendingApprovalException();
                }
                throw new BadCredentialsException("Invalid username or password");
            }
        }

        String principalName = userOpt.map(User::getUsername).orElse(loginRequest.getUsername());
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(principalName, loginRequest.getPassword()));
        } catch (BadCredentialsException ex) {
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                int attempts = user.getFailedAttempts() + 1;
                user.setFailedAttempts(attempts);
                if (attempts >= 5) {
                    user.setLockoutTime(LocalDateTime.now().plusMinutes(15));
                }
                userRepository.save(user);
            }
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = userOpt.orElseGet(() -> userRepository.findByUsername(principalName)
                .orElseThrow(() -> new BadRequestException("User not found")));

        // Reset failed attempts on successful login
        if (user.getFailedAttempts() > 0 || user.getLockoutTime() != null) {
            user.setFailedAttempts(0);
            user.setLockoutTime(null);
            userRepository.save(user);
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        return issueSession(user, response);
    }

    /**
     * Creates a new access token and a rotating refresh token (stored as a SHA-256 hash and sent as an
     * HttpOnly cookie) for an already authenticated user. Used by password login and Google sign-in.
     */
    public AuthResponse issueSession(User user, HttpServletResponse response) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        String accessToken = tokenProvider.generateAccessToken(auth);
        String refreshToken = tokenProvider.generateRefreshToken(auth);

        RefreshToken dbRefreshToken = RefreshToken.builder()
                .tokenHash(hashToken(refreshToken))
                .jti(tokenProvider.getJtiFromToken(refreshToken))
                .user(user)
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(tokenProvider.getRefreshTokenExpirationMs())))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();
        refreshTokenRepository.save(dbRefreshToken);

        addRefreshTokenCookie(response, refreshToken);

        Set<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .mustChangePassword(user.isMustChangePassword())
                .build();
    }

    // noRollbackFor: the reuse-detection branch revokes all tokens and then throws;
    // without this the exception would roll the revocation back.
    @Transactional(noRollbackFor = BadRequestException.class)
    public AuthResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (!StringUtils.hasText(refreshToken)) {
            String bearer = request.getHeader("Authorization");
            if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
                refreshToken = bearer.substring(7);
            }
        }
        if (refreshToken == null || !tokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid or expired refresh token");
        }
        if (!"refresh".equals(tokenProvider.getTokenType(refreshToken))) {
            throw new BadRequestException("Invalid token type. Expected refresh token");
        }

        String tokenHash = hashToken(refreshToken);
        RefreshToken dbToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired refresh token"));

        User user = dbToken.getUser();

        // Reuse detection: If a revoked token is reused, revoke ALL tokens of that user.
        // Exception (benign race): a fast page reload or two tabs can send the same cookie twice.
        // If the token was rotated only seconds ago and its replacement has not been used yet,
        // just issue a fresh pair instead of treating it as theft.
        if (dbToken.isRevoked() && isBenignConcurrentReuse(dbToken)) {
            log.info("Refresh token for user '{}' reused within grace window; issuing a new pair.", user.getUsername());
        } else if (dbToken.isRevoked()) {
            log.warn("Security Alert: Revoked refresh token reused for user '{}'. Revoking all active tokens.", user.getUsername());
            refreshTokenRepository.revokeAllByUser(user);
            throw new BadRequestException("Invalid refresh token (reused revoked token). All sessions have been terminated.");
        }

        if (dbToken.isExpired()) {
            dbToken.setRevoked(true);
            refreshTokenRepository.save(dbToken);
            throw new BadRequestException("Refresh token is expired");
        }

        if (!user.isEnabled()) {
            throw new org.springframework.security.authentication.DisabledException("User account is disabled");
        }

        // Token rotation: generate new access & refresh tokens
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String newAccessToken = tokenProvider.generateAccessTokenFromUsername(user.getUsername(), userDetails.getAuthorities());
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        String newRefreshToken = tokenProvider.generateRefreshToken(auth);
        String newRefreshTokenHash = hashToken(newRefreshToken);

        // Revoke old token and link to replacement
        dbToken.setRevoked(true);
        dbToken.setReplacedBy(newRefreshTokenHash);
        refreshTokenRepository.save(dbToken);

        // Store new refresh token hash in DB
        RefreshToken newDbToken = RefreshToken.builder()
                .tokenHash(newRefreshTokenHash)
                .jti(tokenProvider.getJtiFromToken(newRefreshToken))
                .user(user)
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(tokenProvider.getRefreshTokenExpirationMs())))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();
        refreshTokenRepository.save(newDbToken);

        addRefreshTokenCookie(response, newRefreshToken);

        Set<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        String email = "";
        if (userDetails instanceof UserPrincipal up) {
            email = up.getEmail();
        }

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .username(userDetails.getUsername())
                .email(email)
                .roles(roles)
                .mustChangePassword(user.isMustChangePassword())
                .build();
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (!StringUtils.hasText(refreshToken)) {
            String bearer = request.getHeader("Authorization");
            if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
                refreshToken = bearer.substring(7);
            }
        }
        if (StringUtils.hasText(refreshToken)) {
            String tokenHash = hashToken(refreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(rt -> {
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
            });
        }
        clearRefreshTokenCookie(response);
    }

    @Transactional
    public void logoutAll(String username, HttpServletResponse response) {
        if (StringUtils.hasText(username)) {
            userRepository.findByUsername(username).ifPresent(refreshTokenRepository::revokeAllByUser);
        }
        clearRefreshTokenCookie(response);
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieProperties != null && cookieProperties.isSecure())
                .path(cookieProperties != null ? cookieProperties.getPath() : "/api/auth")
                .maxAge(0)
                .sameSite(cookieProperties != null ? cookieProperties.getSameSite() : "Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        long maxAgeSec = tokenProvider.getRefreshTokenExpirationMs() / 1000;
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(cookieProperties != null && cookieProperties.isSecure())
                .path(cookieProperties != null ? cookieProperties.getPath() : "/api/auth")
                .maxAge(maxAgeSec)
                .sameSite(cookieProperties != null ? cookieProperties.getSameSite() : "Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_TOKEN_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    public String getUsernameFromTokenHash(String tokenHash) {
        return refreshTokenRepository.findByTokenHash(tokenHash)
                .map(rt -> rt.getUser().getUsername())
                .orElse(null);
    }

    public static void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters long");
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }

        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            throw new BadRequestException("Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character");
        }
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        validatePasswordStrength(request.getNewPassword());

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password cannot be the same as current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    @Transactional
    public ResetPasswordResponse adminResetPassword(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String tempPassword = "Temp@" + java.util.UUID.randomUUID().toString().substring(0, 8) + "1A";

        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setMustChangePassword(true);
        user.setFailedAttempts(0);
        user.setLockoutTime(null);
        userRepository.save(user);

        // Revoke all existing sessions
        refreshTokenRepository.revokeAllByUser(user);

        return ResetPasswordResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .temporaryPassword(tempPassword)
                .mustChangePassword(true)
                .message("Password reset successfully. User must change password on next login.")
                .build();
    }
}
