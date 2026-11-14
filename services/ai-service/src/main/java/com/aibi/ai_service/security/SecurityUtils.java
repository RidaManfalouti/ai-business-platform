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
}
