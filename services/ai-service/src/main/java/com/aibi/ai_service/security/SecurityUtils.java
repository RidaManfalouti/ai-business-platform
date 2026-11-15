package com.aibi.ai_service.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<Jwt> getCurrentJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(jwt);
        }
        return Optional.empty();
    }

    public static Long getRequiredUserId() {
        return getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("Identifiant utilisateur (user_id) absent du contexte de sécurité"));
    }

    public static Optional<Long> getCurrentUserId() {
        return getCurrentJwt().map(jwt -> {
            Object claim = jwt.getClaim("user_id");
            if (claim instanceof Number num) {
                return num.longValue();
            } else if (claim instanceof String str) {
                try {
                    return Long.parseLong(str);
                } catch (NumberFormatException ignored) {
                }
            }
            return null;
        });
    }

    public static Optional<String> getCurrentUserEmail() {
        return getCurrentJwt().map(Jwt::getSubject);
    }

    public static Optional<String> getCurrentTenantRole() {
        return getCurrentJwt().map(jwt -> {
            Object claim = jwt.getClaim("tenant_role");
            if (claim != null) {
                String role = claim.toString().trim();
                if (!role.isEmpty()) {
                    return role;
                }
            }
            // Fallback de compatibilité vers la liste des rôles si tenant_role n'est pas séparé
            Object rolesClaim = jwt.getClaim("roles");
            if (rolesClaim == null) {
                rolesClaim = jwt.getClaim("authorities");
            }
            if (rolesClaim instanceof java.util.Collection<?> col) {
                for (Object item : col) {
                    if (item != null) {
                        String r = item.toString().trim();
                        if (r.startsWith("ROLE_")) {
                            r = r.substring(5);
                        }
                        if (java.util.Set.of("OWNER", "ADMIN", "MANAGER", "SELLER", "VIEWER").contains(r.toUpperCase())) {
                            return r.toUpperCase();
                        }
                    }
                }
            } else if (rolesClaim instanceof String str) {
                for (String part : str.split("[,\\s]+")) {
                    String r = part.trim();
                    if (r.startsWith("ROLE_")) {
                        r = r.substring(5);
                    }
                    if (java.util.Set.of("OWNER", "ADMIN", "MANAGER", "SELLER", "VIEWER").contains(r.toUpperCase())) {
                        return r.toUpperCase();
                    }
                }
            }
            return null;
        });
    }
}
