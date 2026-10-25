package com.aibi.product_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Filtre HTTP pour product-service propageant ou générant l'identifiant de corrélation
 * (X-Correlation-ID) et l'associant au contexte MDC pour la centralisation des logs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";
    private static final Pattern SAFE_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{8,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String incomingHeader = request.getHeader(CORRELATION_ID_HEADER);
        String correlationId;

        if (incomingHeader != null && SAFE_PATTERN.matcher(incomingHeader.trim()).matches()) {
            correlationId = incomingHeader.trim();
        } else {
            correlationId = UUID.randomUUID().toString();
            if (incomingHeader != null && !incomingHeader.isBlank()) {
                log.warn("Identifiant de corrélation non conforme ou suspect reçu. Nouveau généré : {}", correlationId);
            }
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
