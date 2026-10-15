package com.aibi.api_gateway.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Gestionnaire d'erreurs réactif global pour l'API Gateway.
 * Intercepte les pannes de connexion (downstream indisponible, refus de connexion, timeouts)
 * et retourne un statut HTTP 503 SERVICE_UNAVAILABLE clair et standardisé en JSON.
 */
@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status;
        String error;
        String message;

        if (isConnectionException(ex)) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            error = "SERVICE_UNAVAILABLE";
            message = "Le service de destination est actuellement indisponible ou injoignable.";
            log.warn("Service indisponible lors de l'appel [{} {}] : {}",
                    exchange.getRequest().getMethod(), exchange.getRequest().getURI().getPath(), ex.getMessage());
        } else if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            error = status.name();
            message = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            error = "INTERNAL_SERVER_ERROR";
            message = "Une erreur inattendue est survenue au niveau de l'API Gateway.";
            log.error("Erreur passerelle non gérée", ex);
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String path = exchange.getRequest().getURI().getPath();
        String json = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                Instant.now().toString(),
                status.value(),
                error,
                escapeJson(message),
                escapeJson(path)
        );

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private boolean isConnectionException(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ConnectException
                    || current.getClass().getName().contains("AnnotatedConnectException")
                    || current instanceof java.util.concurrent.TimeoutException
                    || (current.getMessage() != null && current.getMessage().toLowerCase().contains("connection refused"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
