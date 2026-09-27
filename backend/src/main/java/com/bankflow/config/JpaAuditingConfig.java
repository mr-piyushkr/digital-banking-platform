package com.bankflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables @CreatedDate and @LastModifiedDate. Kept in its own class so tests
 * that need fixed timestamps can exclude it without disabling other config.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
