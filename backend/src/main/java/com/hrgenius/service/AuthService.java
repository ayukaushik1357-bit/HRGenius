package com.hrgenius.service;

import com.hrgenius.dto.auth.AuthResponse;
import com.hrgenius.dto.auth.LoginRequest;
import com.hrgenius.entity.RefreshToken;
import com.hrgenius.entity.User;
import com.hrgenius.exception.BadRequestException;
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

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final com.hrgenius.security.CookieProperties cookieProperties;

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

    @Transactional
    public AuthResponse login(LoginRequest loginRequest, HttpServletResponse response) {
        Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsername());
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
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
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

        // Reset failed attempts on successful login
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getFailedAttempts() > 0 || user.getLockoutTime() != null) {
                user.setFailedAttempts(0);
                user.setLockoutTime(null);
                userRepository.save(user);
            }
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userOpt.orElseGet(() -> userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new BadRequestException("User not found")));

        // Save server-side refresh token hash
        String tokenHash = hashToken(refreshToken);
        String jti = tokenProvider.getJtiFromToken(refreshToken);
        LocalDateTime expiresAt = LocalDateTime.now().plus(Duration.ofMillis(tokenProvider.getRefreshTokenExpirationMs()));

        RefreshToken dbRefreshToken = RefreshToken.builder()
                .tokenHash(tokenHash)
                .jti(jti)
                .user(user)
                .expiresAt(expiresAt)
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();
        refreshTokenRepository.save(dbRefreshToken);

        addRefreshTokenCookie(response, refreshToken);

        Set<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .username(principal.getUsername())
                .email(principal.getEmail())
                .roles(roles)
                .build();
    }

    @Transactional
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

        // Reuse detection: If a revoked token is reused, revoke ALL tokens of that user
        if (dbToken.isRevoked()) {
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
            throw new BadRequestException("User account is disabled");
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
}
