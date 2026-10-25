package com.aibi.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Filtre global à la frontière du système assurant la gestion et la propagation
 * d'un identifiant de corrélation distribuée (X-Correlation-ID).
 *
 * Évite toute confiance aveugle dans les en-têtes clients :
 * - Si l'en-tête est absent ou non conforme au format sécurisé, un UUID v4 est généré.
 * - Si l'en-tête est valide, il est conservé et propagé aux services en aval.
 * - L'identifiant est également retourné dans les en-têtes de réponse pour le traçage client.
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final Pattern SAFE_CORRELATION_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{8,64}$");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientCorrelationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        String correlationId;

        if (clientCorrelationId != null && SAFE_CORRELATION_ID_PATTERN.matcher(clientCorrelationId.trim()).matches()) {
            correlationId = clientCorrelationId.trim();
        } else {
            correlationId = UUID.randomUUID().toString();
            if (clientCorrelationId != null && !clientCorrelationId.isBlank()) {
                log.warn("Identifiant de corrélation client non conforme ou suspect ignoré. Nouveau généré : {}", correlationId);
            }
        }

        // Ajoute l'en-tête dans la réponse client
        exchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, correlationId);

        // Propage l'en-tête vers les microservices cibles
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(r -> r.header(CORRELATION_ID_HEADER, correlationId))
                .build();

        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
