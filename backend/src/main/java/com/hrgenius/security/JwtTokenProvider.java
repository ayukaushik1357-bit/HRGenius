package com.hrgenius.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        // Derive HMAC key from the Base64-encoded secret
        byte[] keyBytes = Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(
                        jwtProperties.getSecret().getBytes()));
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate a short-lived access token.
     */
    public String generateAccessToken(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return buildToken(principal.getUsername(), principal.getAuthorities(), jwtProperties.getAccessTokenExpirationMs(), "access");
    }

    /**
     * Generate a longer-lived refresh token.
     */
    public String generateRefreshToken(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return buildToken(principal.getUsername(), principal.getAuthorities(), jwtProperties.getRefreshTokenExpirationMs(), "refresh");
    }

    public String generateAccessTokenFromUsername(String username, java.util.Collection<? extends GrantedAuthority> authorities) {
        return buildToken(username, authorities, jwtProperties.getAccessTokenExpirationMs(), "access");
    }

    public String generateRefreshTokenFromUsername(String username, java.util.Collection<? extends GrantedAuthority> authorities) {
        return buildToken(username, authorities, jwtProperties.getRefreshTokenExpirationMs(), "refresh");
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String getTokenType(String token) {
        Claims claims = parseClaims(token);
        return claims.get("type", String.class);
    }

    public String getJtiFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.getId();
    }

    public boolean isAccessToken(String token) {
        return "access".equals(getTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(getTokenType(token));
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token");
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token");
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token");
        } catch (SecurityException e) {
            log.warn("Invalid JWT signature");
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty");
        }
        return false;
    }

    public long getRefreshTokenExpirationMs() {
        return jwtProperties.getRefreshTokenExpirationMs();
    }

    public long getAccessTokenExpirationMs() {
        return jwtProperties.getAccessTokenExpirationMs();
    }

    // ── private helpers ──────────────────────────────────────

    private String buildToken(String username, java.util.Collection<? extends GrantedAuthority> authorities, long expirationMs, String type) {
        String roles = authorities == null ? "" : authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .id(java.util.UUID.randomUUID().toString())
                .subject(username)
                .claim("type", type)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
