package com.hrgenius.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    @Test
    @DisplayName("In prod profile: missing secret throws IllegalStateException")
    void missingSecretInProdThrows() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        JwtProperties properties = new JwtProperties(env);
        properties.setSecret(null);

        assertThatThrownBy(properties::validateSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET must be configured in prod profile");
    }

    @Test
    @DisplayName("In prod profile: secret shorter than 256 bits throws IllegalStateException")
    void shortSecretInProdThrows() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        JwtProperties properties = new JwtProperties(env);
        properties.setSecret("short-secret-under-32-chars");

        assertThatThrownBy(properties::validateSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 256 bits");
    }

    @Test
    @DisplayName("In prod profile: valid 256-bit secret succeeds")
    void validSecretInProdSucceeds() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        JwtProperties properties = new JwtProperties(env);
        properties.setSecret("this-is-a-valid-prod-secret-with-at-least-32-characters-123456");

        assertThatCode(properties::validateSecret).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("In non-prod profile: short or missing secret does not fail fast in validateSecret")
    void nonProdAllowsDevDefaults() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");

        JwtProperties properties = new JwtProperties(env);
        properties.setSecret("short");

        assertThatCode(properties::validateSecret).doesNotThrowAnyException();
    }
}
