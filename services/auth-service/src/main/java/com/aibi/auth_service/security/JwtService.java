package com.aibi.auth_service.security;

import com.aibi.auth_service.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(User user) {
        String roleStr = (user.getRole() != null) ? user.getRole().name() : "USER";
        return generateToken(user.getEmail(), roleStr, user.getId(), null, null, null);
    }

    public String generateToken(User user, UUID tenantId, String tenantSlug, String tenantRole) {
        String roleStr = (user.getRole() != null) ? user.getRole().name() : "USER";
        return generateToken(user.getEmail(), roleStr, user.getId(), tenantId, tenantSlug, tenantRole);
    }

    public String generateToken(String email, String role) {
        return generateToken(email, role, null, null, null, null);
    }

    public String generateToken(String email, String role, Long userId, UUID tenantId, String tenantSlug, String tenantRole) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        List<String> authorities = new ArrayList<>();
        if (role != null && !role.isBlank()) {
            String r = role.trim();
            if (!r.startsWith("ROLE_")) {
                r = "ROLE_" + r;
            }
            authorities.add(r);
        }
        if (authorities.isEmpty()) {
            authorities.add("ROLE_USER");
        }

        if (tenantRole != null && !tenantRole.isBlank()) {
            String tr = tenantRole.trim().toUpperCase();
            String normalizedRole = tr.startsWith("ROLE_") ? tr : "ROLE_" + tr;
            if (!authorities.contains(normalizedRole)) {
                authorities.add(normalizedRole);
            }
            String tenantPrefixed = "ROLE_TENANT_" + normalizedRole.replace("ROLE_", "");
            if (!authorities.contains(tenantPrefixed)) {
                authorities.add(tenantPrefixed);
            }
        }

        var builder = Jwts.builder()
                .subject(email)
                .claim("roles", authorities)
                .issuedAt(now)
                .expiration(expiryDate);

        if (userId != null) {
            builder.claim("user_id", userId);
        }
        if (tenantId != null) {
            builder.claim("tenant_id", tenantId.toString());
        }
        if (tenantSlug != null) {
            builder.claim("tenant_slug", tenantSlug);
        }
        if (tenantRole != null) {
            builder.claim("tenant_role", tenantRole);
        }

        return builder.signWith(getSigningKey()).compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public UUID extractTenantId(String token) {
        try {
            Claims claims = getClaims(token);
            String tenantIdStr = claims.get("tenant_id", String.class);
            return tenantIdStr != null ? UUID.fromString(tenantIdStr) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public String extractTenantRole(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.get("tenant_role", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> extractRoles(String token) {
        try {
            Claims claims = getClaims(token);
            Object rolesObj = claims.get("roles");
            List<String> roles = new ArrayList<>();
            if (rolesObj instanceof Collection<?> collection) {
                for (Object item : collection) {
                    if (item != null) {
                        String r = item.toString().trim();
                        if (!r.isEmpty()) {
                            roles.add(r.startsWith("ROLE_") ? r : "ROLE_" + r);
                        }
                    }
                }
            } else if (rolesObj instanceof String str) {
                for (String part : str.split("[,\\s]+")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        roles.add(trimmed.startsWith("ROLE_") ? trimmed : "ROLE_" + trimmed);
                    }
                }
            }
            return Collections.unmodifiableList(roles);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
