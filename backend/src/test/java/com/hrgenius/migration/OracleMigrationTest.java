package com.hrgenius.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs every Flyway migration against a real Oracle XE (same image as docker-compose.yml),
 * then starts the full Spring context with ddl-auto=validate and the demo data seeder.
 *
 * If any migration uses a type Oracle does not support, or any entity does not match its
 * Oracle column, the context fails to start and this test fails.
 *
 * Skipped automatically when Docker is not available (e.g. a laptop without Docker);
 * always runs in GitHub Actions, where Docker is installed.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Testcontainers(disabledWithoutDocker = true)
class OracleMigrationTest {

    @Container
    static final OracleContainer ORACLE =
            new OracleContainer(DockerImageName.parse("gvenzl/oracle-xe:21-slim-faststart"));

    @DynamicPropertySource
    static void oracleProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", ORACLE::getJdbcUrl);
        registry.add("spring.datasource.username", ORACLE::getUsername);
        registry.add("spring.datasource.password", ORACLE::getPassword);
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("app.seed-demo-data", () -> "true");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Flyway migrations run on Oracle, Hibernate validate passes and demo data seeds")
    void migrationsValidateAndSeedOnOracle() {
        // Reaching this point means Flyway, Hibernate validate and the seeder all succeeded on Oracle.
        Integer users = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertThat(users).isPositive();
    }
}
