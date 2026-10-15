package com.aibi.api_gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Contrôleur de secours local pour les services en attente de développement.
 * Permet de router les requêtes vers un point de terminaison interne retournant HTTP 503
 * sans émettre d'appels réseau vers des hôtes ou ports inexistants.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping(value = "/service-unavailable", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> handleUnavailableService(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", "SERVICE_UNAVAILABLE");
        body.put("message", "Le service demandé n'est pas encore disponible ou est en cours de développement.");
        body.put("path", path);

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
}
