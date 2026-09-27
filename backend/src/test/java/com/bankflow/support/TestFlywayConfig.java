package com.bankflow.support;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Drops and re-applies every migration when a test context starts, so a run is
 * never affected by rows or schema left behind by a previous one. Only ever
 * active against the bankflow_test schema.
 */
@TestConfiguration
public class TestFlywayConfig {

    @Bean
    FlywayMigrationStrategy cleanMigrateStrategy() {
        return flyway -> {
            flyway.clean();
            flyway.migrate();
        };
    }
}
