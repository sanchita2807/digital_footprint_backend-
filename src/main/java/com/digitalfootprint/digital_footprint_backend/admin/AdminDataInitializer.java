package com.digitalfootprint.digital_footprint_backend.admin;

import com.digitalfootprint.digital_footprint_backend.auth.User;
import com.digitalfootprint.digital_footprint_backend.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates a default admin account on first startup if none exists.
 * Credentials are configurable via application.properties.
 *
 * Default: admin@digitalfootprint.com / Admin@123
 * CHANGE THE PASSWORD IMMEDIATELY AFTER FIRST LOGIN.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default-email:admin@digitalfootprint.com}")
    private String defaultAdminEmail;

    @Value("${app.admin.default-password:Admin@123}")
    private String defaultAdminPassword;

    @Value("${app.admin.default-name:System Administrator}")
    private String defaultAdminName;

    @Override
    public void run(String... args) {
        // Check if any admin user exists
        boolean adminExists = userRepository.findAll().stream()
                .anyMatch(u -> u.getIsAdmin() != null && u.getIsAdmin() == 1);

        if (adminExists) {
            log.debug("Admin account already exists, skipping initialization.");
            return;
        }

        // Check if the email is already taken by a regular user
        if (userRepository.findByEmail(defaultAdminEmail).isPresent()) {
            log.warn("Email {} is already taken by a non-admin user. Skipping admin creation.", defaultAdminEmail);
            return;
        }

        User admin = User.builder()
                .fullName(defaultAdminName)
                .phoneNumber("+910000000000") // Placeholder phone for admin
                .email(defaultAdminEmail)
                .isVerified(true)
                .isActive(true)
                .isAdmin(1) // Admin role (0 = User, 1 = Admin)
                .password(passwordEncoder.encode(defaultAdminPassword))
                .build();

        userRepository.save(admin);
        log.info("════════════════════════════════════════════════════");
        log.info("  Default admin account created:");
        log.info("  Email    : {}", defaultAdminEmail);
        log.info("  Password : {}", defaultAdminPassword);
        log.info("  ⚠️  CHANGE THE PASSWORD IMMEDIATELY AFTER FIRST LOGIN!");
        log.info("════════════════════════════════════════════════════");
    }
}
