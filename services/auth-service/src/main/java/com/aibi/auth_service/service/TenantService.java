package com.aibi.auth_service.service;

import com.aibi.auth_service.dto.*;
import com.aibi.auth_service.entity.Membership;
import com.aibi.auth_service.entity.Tenant;
import com.aibi.auth_service.entity.User;
import com.aibi.auth_service.repository.MembershipRepository;
import com.aibi.auth_service.repository.TenantRepository;
import com.aibi.auth_service.repository.UserRepository;
import com.aibi.auth_service.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class TenantService {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGESDHASHES = Pattern.compile("(^-+|-+$)");

    private final TenantRepository tenantRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public TenantService(
            TenantRepository tenantRepository,
            MembershipRepository membershipRepository,
            UserRepository userRepository,
            JwtService jwtService) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse createTenant(Long userId, CreateTenantRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));

        String rawSlug = (request.slug() != null && !request.slug().isBlank())
                ? request.slug().trim()
                : toSlug(request.name());

        if (rawSlug.isBlank()) {
            rawSlug = "company-" + UUID.randomUUID().toString().substring(0, 8);
        }

        String slug = rawSlug;
        if (tenantRepository.existsBySlug(slug)) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 6);
        }

        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Tenant.builder()
                .id(tenantId)
                .name(request.name().trim())
                .slug(slug)
                .status(Tenant.TenantStatus.ACTIVE)
                .build();

        Tenant savedTenant = tenantRepository.save(tenant);

        Membership membership = Membership.builder()
                .id(UUID.randomUUID())
                .tenantId(savedTenant.getId())
                .tenant(savedTenant)
                .userId(user.getId())
                .user(user)
                .role(Membership.TenantRole.OWNER)
                .status(Membership.MembershipStatus.ACTIVE)
                .build();

        membershipRepository.save(membership);

        String token = jwtService.generateToken(
                user,
                savedTenant.getId(),
                savedTenant.getSlug(),
                Membership.TenantRole.OWNER.name()
        );

        List<TenantSummaryResponse> available = getUserTenants(user.getId());

        return new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                savedTenant.getId(),
                savedTenant.getSlug(),
                Membership.TenantRole.OWNER.name(),
                available
        );
    }

    @Transactional(readOnly = true)
    public List<TenantSummaryResponse> getUserTenants(Long userId) {
        List<Membership> memberships = membershipRepository.findByUserIdAndStatus(
                userId,
                Membership.MembershipStatus.ACTIVE
        );

        return memberships.stream()
                .filter(m -> m.getTenant() != null && m.getTenant().getStatus() == Tenant.TenantStatus.ACTIVE)
                .map(m -> new TenantSummaryResponse(
                        m.getTenant().getId(),
                        m.getTenant().getName(),
                        m.getTenant().getSlug(),
                        m.getTenant().getStatus().name(),
                        m.getRole().name()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public AuthResponse switchTenant(Long userId, UUID targetTenantId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));

        Membership membership = membershipRepository.findByTenantIdAndUserIdAndStatus(
                targetTenantId,
                userId,
                Membership.MembershipStatus.ACTIVE
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Accès refusé : Vous n'êtes pas membre actif de cette entreprise."
        ));

        Tenant tenant = tenantRepository.findById(targetTenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entreprise introuvable."));

        if (tenant.getStatus() != Tenant.TenantStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette entreprise est actuellement suspendue.");
        }

        String token = jwtService.generateToken(
                user,
                tenant.getId(),
                tenant.getSlug(),
                membership.getRole().name()
        );

        List<TenantSummaryResponse> available = getUserTenants(user.getId());

        return new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                tenant.getId(),
                tenant.getSlug(),
                membership.getRole().name(),
                available
        );
    }

    @Transactional(readOnly = true)
    public List<MembershipResponse> getTenantMembers(Long currentUserId, UUID tenantId) {
        Membership currentMembership = membershipRepository.findByTenantIdAndUserIdAndStatus(
                tenantId,
                currentUserId,
                Membership.MembershipStatus.ACTIVE
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès interdit à ce locataire."));

        if (currentMembership.getRole() != Membership.TenantRole.OWNER &&
            currentMembership.getRole() != Membership.TenantRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Droits insuffisants pour administrer les membres.");
        }

        return membershipRepository.findByTenantId(tenantId).stream()
                .map(m -> new MembershipResponse(
                        m.getId(),
                        m.getTenantId(),
                        m.getUserId(),
                        m.getUser() != null ? m.getUser().getUsername() : null,
                        m.getUser() != null ? m.getUser().getEmail() : null,
                        m.getRole().name(),
                        m.getStatus().name(),
                        m.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public MembershipResponse addMember(Long currentUserId, UUID tenantId, AddMemberRequest request) {
        Membership currentMembership = membershipRepository.findByTenantIdAndUserIdAndStatus(
                tenantId,
                currentUserId,
                Membership.MembershipStatus.ACTIVE
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès interdit à ce locataire."));

        if (currentMembership.getRole() != Membership.TenantRole.OWNER &&
            currentMembership.getRole() != Membership.TenantRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Droits insuffisants pour inviter des membres.");
        }

        User targetUser = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun utilisateur trouvé avec cet email."));

        if (membershipRepository.existsByTenantIdAndUserId(tenantId, targetUser.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet utilisateur est déjà membre de cette entreprise.");
        }

        Membership.TenantRole assignedRole;
        try {
            assignedRole = Membership.TenantRole.valueOf(request.role().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle d'adhésion invalide.");
        }

        if (assignedRole == Membership.TenantRole.OWNER && currentMembership.getRole() != Membership.TenantRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul le propriétaire actuel peut nommer un autre propriétaire.");
        }

        Membership newMembership = Membership.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .userId(targetUser.getId())
                .role(assignedRole)
                .status(Membership.MembershipStatus.ACTIVE)
                .build();

        Membership saved = membershipRepository.save(newMembership);

        return new MembershipResponse(
                saved.getId(),
                saved.getTenantId(),
                targetUser.getId(),
                targetUser.getUsername(),
                targetUser.getEmail(),
                saved.getRole().name(),
                saved.getStatus().name(),
                saved.getCreatedAt()
        );
    }

    @Transactional
    public void removeMember(Long currentUserId, UUID tenantId, UUID membershipId) {
        Membership currentMembership = membershipRepository.findByTenantIdAndUserIdAndStatus(
                tenantId,
                currentUserId,
                Membership.MembershipStatus.ACTIVE
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès interdit."));

        if (currentMembership.getRole() != Membership.TenantRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul le propriétaire peut révoquer des membres.");
        }

        Membership targetMembership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adhésion introuvable."));

        if (!targetMembership.getTenantId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'adhésion ne correspond pas à cette entreprise.");
        }

        if (targetMembership.getUserId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le propriétaire ne peut pas se révoquer lui-même.");
        }

        membershipRepository.delete(targetMembership);
    }

    public static String toSlug(String input) {
        if (input == null) return "";
        String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        slug = EDGESDHASHES.matcher(slug).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
