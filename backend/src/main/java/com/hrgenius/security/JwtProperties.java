package com.hrgenius.security;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtProperties {
    @Autowired(required = false)
    private Environment environment;

    private String secret;
    private long accessTokenExpirationMs = 900000L; // 15 minutes
    private long refreshTokenExpirationMs = 604800000L; // 7 days

    public JwtProperties() {
    }

    public JwtProperties(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateSecret() {
        // Deployed profiles must not run with the development default secret
        boolean isProd = environment != null && Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equals("prod") || p.equals("cloud"));
        if (isProd) {
            if (secret == null || secret.trim().isEmpty()) {
                throw new IllegalStateException("JWT_SECRET must be configured when deployed (prod/cloud profile)");
            }
            if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalStateException("JWT_SECRET must be at least 256 bits (32 bytes) when deployed (prod/cloud profile)");
            }
        }
    }
}
