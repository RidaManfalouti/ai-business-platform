package com.aibi.auth_service.service;

import com.aibi.auth_service.dto.AuthResponse;
import com.aibi.auth_service.dto.LoginRequest;
import com.aibi.auth_service.dto.RegisterRequest;
import com.aibi.auth_service.dto.TenantSummaryResponse;
import com.aibi.auth_service.entity.Membership;
import com.aibi.auth_service.entity.Tenant;
import com.aibi.auth_service.entity.User;
import com.aibi.auth_service.repository.MembershipRepository;
import com.aibi.auth_service.repository.UserRepository;
import com.aibi.auth_service.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            MembershipRepository membershipRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé.");
        }

        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Ce nom d'utilisateur est déjà utilisé.");
        }

        User user = User.builder()
                .username(request.username().trim())
                .email(request.email().trim().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .role(User.Role.USER)
                .build();

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser);

        return new AuthResponse(
                token,
                "Bearer",
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                null,
                null,
                null,
                Collections.emptyList()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(
                request.email().trim().toLowerCase()
        ).orElseThrow(() ->
                new BadCredentialsException("Email ou mot de passe incorrect.")
        );

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Email ou mot de passe incorrect.");
        }

        List<Membership> activeMemberships = membershipRepository.findByUserIdAndStatus(
                user.getId(),
                Membership.MembershipStatus.ACTIVE
        );

        List<TenantSummaryResponse> availableTenants = activeMemberships.stream()
                .filter(m -> m.getTenant() != null && m.getTenant().getStatus() == Tenant.TenantStatus.ACTIVE)
                .map(m -> new TenantSummaryResponse(
                        m.getTenant().getId(),
                        m.getTenant().getName(),
                        m.getTenant().getSlug(),
                        m.getTenant().getStatus().name(),
                        m.getRole().name()
                ))
                .toList();

        UUID activeTenantId = null;
        String activeTenantSlug = null;
        String activeTenantRole = null;
        String token;

        if (!availableTenants.isEmpty()) {
            TenantSummaryResponse selected = availableTenants.stream()
                    .filter(t -> "00000000-0000-0000-0000-000000000001".equals(t.id().toString()))
                    .findFirst()
                    .orElse(availableTenants.get(0));

            activeTenantId = selected.id();
            activeTenantSlug = selected.slug();
            activeTenantRole = selected.role();

            token = jwtService.generateToken(user, activeTenantId, activeTenantSlug, activeTenantRole);
        } else {
            token = jwtService.generateToken(user);
        }

        return new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                activeTenantId,
                activeTenantSlug,
                activeTenantRole,
                availableTenants
        );
    }
}
