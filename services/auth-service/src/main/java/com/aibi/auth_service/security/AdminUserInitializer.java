package com.aibi.auth_service.security;

import com.aibi.auth_service.entity.User;
import com.aibi.auth_service.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.initial-admin.enabled:false}")
    private boolean adminEnabled;

    @Value("${app.security.initial-admin.username:admin}")
    private String adminUsername;

    @Value("${app.security.initial-admin.email:admin@platform.local}")
    private String adminEmail;

    @Value("${app.security.initial-admin.password:}")
    private String adminPassword;

    public AdminUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!adminEnabled) {
            return;
        }

        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("Initial admin bootstrap is enabled but no password was provided. Skipping admin creation.");
            return;
        }

        String normalizedEmail = adminEmail.trim().toLowerCase();
        if (userRepository.findByEmail(normalizedEmail).isEmpty()
                && !userRepository.existsByUsername(adminUsername.trim())) {
            User admin = User.builder()
                    .username(adminUsername.trim())
                    .email(normalizedEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(User.Role.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("Initial administrator account bootstrapped successfully for: {}", normalizedEmail);
        }
    }
}
