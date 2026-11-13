package com.aibi.auth_service.security;

import com.aibi.auth_service.entity.Membership;
import com.aibi.auth_service.entity.Tenant;
import com.aibi.auth_service.entity.User;
import com.aibi.auth_service.repository.MembershipRepository;
import com.aibi.auth_service.repository.TenantRepository;
import com.aibi.auth_service.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AdminUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);
    private static final UUID DEFAULT_LEGACY_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.initial-admin.enabled:false}")
    private boolean adminEnabled;

    @Value("${app.security.initial-admin.username:admin}")
    private String adminUsername;

    @Value("${app.security.initial-admin.email:admin@platform.local}")
    private String adminEmail;

    @Value("${app.security.initial-admin.password:}")
    private String adminPassword;

    public AdminUserInitializer(
            UserRepository userRepository,
            MembershipRepository membershipRepository,
            TenantRepository tenantRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.tenantRepository = tenantRepository;
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
        User admin = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (admin == null && !userRepository.existsByUsername(adminUsername.trim())) {
            admin = User.builder()
                    .username(adminUsername.trim())
                    .email(normalizedEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(User.Role.ADMIN)
                    .build();
            admin = userRepository.save(admin);
            log.info("Initial administrator account bootstrapped successfully for: {}", normalizedEmail);
        }

        if (admin != null && tenantRepository.existsById(DEFAULT_LEGACY_TENANT_ID)) {
            if (!membershipRepository.existsByTenantIdAndUserId(DEFAULT_LEGACY_TENANT_ID, admin.getId())) {
                Tenant legacyTenant = tenantRepository.findById(DEFAULT_LEGACY_TENANT_ID).orElse(null);
                Membership membership = Membership.builder()
                        .id(UUID.randomUUID())
                        .tenantId(DEFAULT_LEGACY_TENANT_ID)
                        .tenant(legacyTenant)
                        .userId(admin.getId())
                        .user(admin)
                        .role(Membership.TenantRole.OWNER)
                        .status(Membership.MembershipStatus.ACTIVE)
                        .build();
                membershipRepository.save(membership);
                log.info("Initial administrator membership assigned to legacy tenant {} as OWNER", DEFAULT_LEGACY_TENANT_ID);
            }
        }
    }
}
