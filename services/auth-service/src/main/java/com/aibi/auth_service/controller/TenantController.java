package com.aibi.auth_service.controller;

import com.aibi.auth_service.dto.*;
import com.aibi.auth_service.entity.User;
import com.aibi.auth_service.repository.UserRepository;
import com.aibi.auth_service.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth/tenants")
public class TenantController {

    private final TenantService tenantService;
    private final UserRepository userRepository;

    public TenantController(TenantService tenantService, UserRepository userRepository) {
        this.tenantService = tenantService;
        this.userRepository = userRepository;
    }

    private Long getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié.");
        }
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable."));
        return user.getId();
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthResponse> createTenant(
            Authentication authentication,
            @Valid @RequestBody CreateTenantRequest request) {
        Long userId = getAuthenticatedUserId(authentication);
        AuthResponse response = tenantService.createTenant(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TenantSummaryResponse>> getMyTenants(Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(tenantService.getUserTenants(userId));
    }

    @PostMapping("/{tenantId}/switch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthResponse> switchTenant(
            Authentication authentication,
            @PathVariable UUID tenantId) {
        Long userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(tenantService.switchTenant(userId, tenantId));
    }

    @GetMapping("/{tenantId}/members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MembershipResponse>> getTenantMembers(
            Authentication authentication,
            @PathVariable UUID tenantId) {
        Long userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(tenantService.getTenantMembers(userId, tenantId));
    }

    @PostMapping("/{tenantId}/members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MembershipResponse> addMember(
            Authentication authentication,
            @PathVariable UUID tenantId,
            @Valid @RequestBody AddMemberRequest request) {
        Long userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(tenantService.addMember(userId, tenantId, request));
    }

    @DeleteMapping("/{tenantId}/members/{membershipId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeMember(
            Authentication authentication,
            @PathVariable UUID tenantId,
            @PathVariable UUID membershipId) {
        Long userId = getAuthenticatedUserId(authentication);
        tenantService.removeMember(userId, tenantId, membershipId);
        return ResponseEntity.noContent().build();
    }
}
