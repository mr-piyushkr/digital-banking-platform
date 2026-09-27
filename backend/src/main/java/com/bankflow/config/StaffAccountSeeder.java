package com.bankflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.bankflow.entity.Role;
import com.bankflow.entity.User;
import com.bankflow.entity.enums.KycStatus;
import com.bankflow.entity.enums.RoleName;
import com.bankflow.repository.RoleRepository;
import com.bankflow.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Creates the admin and auditor logins on startup if they are missing.
 *
 * Deliberately not a Flyway migration: that would mean committing a BCrypt hash
 * to the repository, and the same hash would then exist in every environment.
 * Here the password comes from the environment and is hashed by the application
 * with the configured encoder.
 *
 * Idempotent, and restricted to non-production profiles.
 */
@Slf4j
@Configuration
@Profile({"dev", "docker", "local"})
@RequiredArgsConstructor
public class StaffAccountSeeder {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL:admin@bankflow.local}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Value("${AUDITOR_EMAIL:auditor@bankflow.local}")
    private String auditorEmail;

    @Value("${AUDITOR_PASSWORD:}")
    private String auditorPassword;

    @Bean
    ApplicationRunner seedStaffAccounts() {
        return args -> {
            seed(adminEmail, adminPassword, "Bank", "Admin", RoleName.ROLE_ADMIN, "9000000001");
            seed(auditorEmail, auditorPassword, "Bank", "Auditor", RoleName.ROLE_AUDITOR, "9000000002");
        };
    }

    @Transactional
    void seed(
            String email,
            String rawPassword,
            String firstName,
            String lastName,
            RoleName roleName,
            String phone) {

        if (rawPassword == null || rawPassword.isBlank()) {
            log.warn("No password configured for {} — skipping seed", roleName);
            return;
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        Role role = roleRepository
                .findByName(roleName)
                .orElseThrow(() -> new IllegalStateException(roleName + " missing — apply migration V2"));

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email.toLowerCase());
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setKycStatus(KycStatus.VERIFIED);
        user.addRole(role);

        userRepository.save(user);
        log.info("Seeded {} account {}", roleName, email);
    }
}
