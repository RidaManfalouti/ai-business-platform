package com.aibi.api_gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;
    private GatewayFilterChain chain;
    private AtomicReference<ServerWebExchange> capturedExchange;
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F-]{36}$");

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        chain = mock(GatewayFilterChain.class);
        capturedExchange = new AtomicReference<>();

        given(chain.filter(any(ServerWebExchange.class))).willAnswer(invocation -> {
            capturedExchange.set(invocation.getArgument(0));
            return Mono.empty();
        });
    }

    @Test
    @DisplayName("Génère un UUID valide lorsque l'en-tête X-Correlation-ID est absent")
    void shouldGenerateCorrelationIdWhenMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/products").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        // Vérification de la réponse
        String responseHeader = exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(responseHeader).isNotNull();
        assertThat(UUID_PATTERN.matcher(responseHeader).matches()).isTrue();

        // Vérification de la transmission en aval
        ServerWebExchange downstreamExchange = capturedExchange.get();
        String downstreamHeader = downstreamExchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(downstreamHeader).isEqualTo(responseHeader);
    }

    @Test
    @DisplayName("Conserve un identifiant de corrélation valide fourni par le client")
    void shouldPreserveValidClientCorrelationId() {
        String validId = "custom-corr-id-12345";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/sales")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, validId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        String responseHeader = exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(responseHeader).isEqualTo(validId);

        String downstreamHeader = capturedExchange.get().getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(downstreamHeader).isEqualTo(validId);
    }

    @Test
    @DisplayName("Rejette et remplace un en-tête de corrélation client suspect ou avec injection")
    void shouldSanitizeMaliciousClientCorrelationId() {
        String maliciousId = "invalid\r\nInjected-Header: value";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/sales")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, maliciousId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        String responseHeader = exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(responseHeader).isNotNull();
        assertThat(responseHeader).isNotEqualTo(maliciousId);
        assertThat(UUID_PATTERN.matcher(responseHeader).matches()).isTrue();
    }
}
