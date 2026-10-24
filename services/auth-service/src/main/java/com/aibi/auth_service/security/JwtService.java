package com.aibi.auth_service.security;

import com.aibi.auth_service.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
        return generateToken(user.getEmail(), roleStr);
    }

    public String generateToken(String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        String normalizedRole = (role != null && !role.isBlank()) ? role.trim() : "USER";
        if (!normalizedRole.startsWith("ROLE_")) {
            normalizedRole = "ROLE_" + normalizedRole;
        }

        return Jwts.builder()
                .subject(email)
                .claim("roles", List.of(normalizedRole))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateToken(String email) {
        return generateToken(email, "USER");
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
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
