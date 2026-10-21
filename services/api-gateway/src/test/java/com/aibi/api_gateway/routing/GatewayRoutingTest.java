package com.aibi.api_gateway.routing;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    private static DisposableServer mockBackend;
    private static final AtomicReference<String> receivedAuthHeader = new AtomicReference<>();

    @BeforeEach
    void setUpClient() {
        this.webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @BeforeAll
    static void startMockBackend() {
        mockBackend = HttpServer.create()
                .port(0)
                .route(routes -> routes
                        .post("/api/auth/register", (req, res) ->
                                res.status(HttpResponseStatus.CREATED)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .sendString(Mono.just("{\"token\":\"test-jwt\",\"userId\":1}")))
                        .post("/api/auth/login", (req, res) ->
                                res.status(HttpResponseStatus.OK)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .sendString(Mono.just("{\"token\":\"login-jwt\"}")))
                        .get("/api/users/me", (req, res) -> {
                            receivedAuthHeader.set(req.requestHeaders().get(HttpHeaders.AUTHORIZATION));
                            return res.status(HttpResponseStatus.OK)
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .sendString(Mono.just("{\"email\":\"test@example.com\",\"message\":\"Authentification JWT réussie\"}"));
                        })
                        .get("/api/products/1", (req, res) ->
                                res.status(HttpResponseStatus.OK)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .sendString(Mono.just("{\"id\":1,\"name\":\"Laptop\"}")))
                        .get("/api/customers/1", (req, res) ->
                                res.status(HttpResponseStatus.OK)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .sendString(Mono.just("{\"id\":1,\"firstName\":\"John\"}")))
                        .get("/api/sales/1", (req, res) ->
                                res.status(HttpResponseStatus.OK)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .sendString(Mono.just("{\"id\":1,\"reference\":\"SAL-TEST-001\"}")))
                )
                .bindNow();
    }

    @AfterAll
    static void stopMockBackend() {
        if (mockBackend != null) {
            mockBackend.disposeNow();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("AUTH_SERVICE_URL", () -> "http://localhost:" + mockBackend.port());
        registry.add("PRODUCT_SERVICE_URL", () -> "http://localhost:" + mockBackend.port());
        registry.add("CUSTOMER_SERVICE_URL", () -> "http://localhost:" + mockBackend.port());
        registry.add("SALES_SERVICE_URL", () -> "http://localhost:" + mockBackend.port());
    }

    @Test
    @DisplayName("Vérifie que la route /api/auth/login est correctement relayée vers le backend")
    void shouldRouteAuthLoginSuccessfully() {
        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"test@example.com\",\"password\":\"pwd\"}")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.token").isEqualTo("login-jwt");
    }

    @Test
    @DisplayName("Vérifie la transmission de l'en-tête Authorization vers le backend sans altération")
    void shouldForwardAuthorizationHeaderForJwtRequests() {
        String testJwt = "Bearer eyJhbGciOiJIUzI1NiJ9.sample.token";

        webTestClient.get()
                .uri("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, testJwt)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo("test@example.com");

        assertThat(receivedAuthHeader.get()).isEqualTo(testJwt);
    }

    @Test
    @DisplayName("Vérifie que l'endpoint Actuator Health de la Gateway répond UP sur le port local")
    void shouldReturnActuatorHealthUp() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    @DisplayName("Vérifie que la route /api/products/** est correctement relayée vers le Product Service")
    void shouldRouteProductServiceSuccessfully() {
        webTestClient.get()
                .uri("/api/products/1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("Laptop");
    }

    @Test
    @DisplayName("Vérifie que la route /api/customers/** est correctement relayée vers le Customer Service")
    void shouldRouteCustomerServiceSuccessfully() {
        webTestClient.get()
                .uri("/api/customers/1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.firstName").isEqualTo("John");
    }

    @Test
    @DisplayName("Vérifie que la route /api/sales/** est correctement relayée vers le Sales Service")
    void shouldRouteSalesServiceSuccessfully() {
        webTestClient.get()
                .uri("/api/sales/1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.reference").isEqualTo("SAL-TEST-001");
    }

    @Test
    @DisplayName("Vérifie que les services non encore implémentés renvoient 503 sans appel externe")
    void shouldReturn503ForFutureServices() {
        webTestClient.get()
                .uri("/api/analytics")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.error").isEqualTo("SERVICE_UNAVAILABLE");
    }
}
