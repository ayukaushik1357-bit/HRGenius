package com.hrgenius.service;

import com.hrgenius.dto.auth.AuthResponse;
import com.hrgenius.dto.auth.LoginRequest;
import com.hrgenius.exception.BadRequestException;
import com.hrgenius.security.JwtTokenProvider;
import com.hrgenius.security.UserPrincipal;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "hrgenius_refresh_token";

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    public AuthResponse login(LoginRequest loginRequest, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        addRefreshTokenCookie(response, refreshToken);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
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

    public AuthResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null || !tokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        String username = tokenProvider.getUsernameFromToken(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        String newAccessToken = tokenProvider.generateAccessTokenFromUsername(username, userDetails.getAuthorities());
        // Rotate refresh token optionally or re-set
        String newRefreshToken = tokenProvider.generateAccessTokenFromUsername(username, userDetails.getAuthorities()); // generate new refresh
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        String rotatedRefreshToken = tokenProvider.generateRefreshToken(auth);
        addRefreshTokenCookie(response, rotatedRefreshToken);

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

    public void logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false) // set to true in production HTTPS
                .path("/api/auth")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        long maxAgeSec = tokenProvider.getRefreshTokenExpirationMs() / 1000;
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(false) // in local dev (HTTP), secure=true would block browser from sending cookie over localhost
                .path("/api/auth")
                .maxAge(maxAgeSec)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_TOKEN_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
