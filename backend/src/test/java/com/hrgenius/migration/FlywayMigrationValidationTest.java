package com.hrgenius.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("migration-test")
class FlywayMigrationValidationTest {

    @Test
    @DisplayName("Verify Flyway migrations execute and Hibernate ddl-auto=validate passes with zero mismatches")
    void contextLoadsAndValidatesEntitiesAgainstFlyway() {
        // Successful context startup proves Flyway ran all scripts and Hibernate validate matched all entities
    }
}
