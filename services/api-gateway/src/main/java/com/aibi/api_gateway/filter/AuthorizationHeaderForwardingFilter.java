package com.aibi.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filtre global assurant la transmission systématique de l'en-tête Authorization
 * (tokens JWT) vers les microservices cibles sans créer de seconde authentification
 * au niveau de la passerelle.
 *
 * La validation sémantique et la vérification cryptographique du token relèvent
 * des services sous-jacents (ex: auth-service).
 */
@Component
public class AuthorizationHeaderForwardingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationHeaderForwardingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(r -> {
                    if (authHeader != null && !authHeader.isBlank()) {
                        log.debug("Transmission de l'en-tête Authorization pour la requête [{} {}]",
                                exchange.getRequest().getMethod(), exchange.getRequest().getURI().getPath());
                        r.header(HttpHeaders.AUTHORIZATION, authHeader);
                    }
                    // Strip any client-supplied role headers to prevent header injection/spoofing
                    r.headers(h -> {
                        h.remove("X-Role");
                        h.remove("X-Roles");
                        h.remove("X-User-Role");
                        h.remove("X-Authorities");
                    });
                })
                .build();

        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
