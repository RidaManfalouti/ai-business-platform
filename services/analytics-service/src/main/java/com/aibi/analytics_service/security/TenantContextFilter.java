package com.aibi.analytics_service.security;

import com.aibi.analytics_service.exception.MissingTenantContextException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Endpoints publics ou techniques exemptés
        if (path.startsWith("/actuator") || path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                Object claim = jwt.getClaim("tenant_id");
                if (claim != null) {
                    try {
                        UUID tenantId = UUID.fromString(claim.toString());
                        TenantContext.setTenantId(tenantId);
                    } catch (IllegalArgumentException e) {
                        handleForbidden(response, "Identifiant de locataire (tenant_id) malformé.");
                        return;
                    }
                } else if (path.startsWith("/api/")) {
                    handleForbidden(response, "Revendication de locataire (tenant_id) requise mais absente.");
                    return;
                }
            } else if (path.startsWith("/api/") && auth != null && auth.isAuthenticated()) {
                // Autre type d'authentification sans JWT tenant_id
                handleForbidden(response, "Authentification sans contexte de locataire.");
                return;
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void handleForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"error\":\"FORBIDDEN\",\"message\":\"" + message + "\"}"
        );
    }
}
