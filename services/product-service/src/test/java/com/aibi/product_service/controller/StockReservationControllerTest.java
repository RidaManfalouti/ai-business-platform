package com.aibi.product_service.controller;

import com.aibi.product_service.entity.Product;
import com.aibi.product_service.repository.ProductRepository;
import com.aibi.product_service.repository.StockReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class StockReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockReservationRepository reservationRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = productRepository.save(Product.builder()
                .name("Produit Test Stock " + UUID.randomUUID())
                .description("Test Description")
                .price(new BigDecimal("99.99"))
                .stockQuantity(10)
                .category("Test")
                .build());
    }

    @Test
    @DisplayName("POST /api/products/reservations : Réservation valide -> 201 Created et stock décrémenté")
    void shouldReserveStockSuccessfully() throws Exception {
        String resId = "RES-TEST-" + UUID.randomUUID();
        String payload = """
                {
                    "reservationId": "%s",
                    "items": [
                        {
                            "productId": %d,
                            "quantity": 3
                        }
                    ]
                }
                """.formatted(resId, testProduct.getId());

        mockMvc.perform(post("/api/products/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value(resId))
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].quantity").value(3));

        Product updated = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(updated.getStockQuantity()).isEqualTo(7); // 10 - 3 = 7
    }

    @Test
    @DisplayName("POST /api/products/reservations : Stock insuffisant -> 409 Conflict et stock inchangé")
    void shouldReturn409WhenStockIsInsufficient() throws Exception {
        String resId = "RES-OVER-" + UUID.randomUUID();
        String payload = """
                {
                    "reservationId": "%s",
                    "items": [
                        {
                            "productId": %d,
                            "quantity": 25
                        }
                    ]
                }
                """.formatted(resId, testProduct.getId());

        mockMvc.perform(post("/api/products/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message", containsString("Stock insuffisant")));

        Product updated = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(updated.getStockQuantity()).isEqualTo(10); // inchangé
    }

    @Test
    @DisplayName("POST /api/products/reservations : Idempotence -> répétition de la même requête ne réserve pas deux fois")
    void shouldBeIdempotentOnRepeatedReservationRequests() throws Exception {
        String resId = "RES-IDEM-" + UUID.randomUUID();
        String payload = """
                {
                    "reservationId": "%s",
                    "items": [
                        {
                            "productId": %d,
                            "quantity": 4
                        }
                    ]
                }
                """.formatted(resId, testProduct.getId());

        // Première requête
        mockMvc.perform(post("/api/products/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        Product afterFirst = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(afterFirst.getStockQuantity()).isEqualTo(6); // 10 - 4 = 6

        // Seconde requête avec le même reservationId
        mockMvc.perform(post("/api/products/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value(resId))
                .andExpect(jsonPath("$.status").value("RESERVED"));

        Product afterSecond = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(afterSecond.getStockQuantity()).isEqualTo(6); // Toujours 6, pas de double décrémentation !
    }

    @Test
    @DisplayName("POST /api/products/reservations/{id}/release : Libération réussie -> 200 OK et stock restitué")
    void shouldReleaseReservationAndRestoreStock() throws Exception {
        String resId = "RES-REL-" + UUID.randomUUID();
        String payload = """
                {
                    "reservationId": "%s",
                    "items": [
                        {
                            "productId": %d,
                            "quantity": 4
                        }
                    ]
                }
                """.formatted(resId, testProduct.getId());

        // Réserver 4 unités
        mockMvc.perform(post("/api/products/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        assertThat(productRepository.findById(testProduct.getId()).orElseThrow().getStockQuantity()).isEqualTo(6);

        // Libérer la réservation
        mockMvc.perform(post("/api/products/reservations/{id}/release", resId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(resId))
                .andExpect(jsonPath("$.status").value("RELEASED"));

        // Vérifier que le stock a été restitué
        Product restored = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(restored.getStockQuantity()).isEqualTo(10);

        // Seconde libération (idempotence)
        mockMvc.perform(post("/api/products/reservations/{id}/release", resId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASED"));

        assertThat(productRepository.findById(testProduct.getId()).orElseThrow().getStockQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("Concurrence : Requêtes concurrentes -> stock ne devient jamais négatif et décrémentation exacte")
    void shouldHandleConcurrentReservationsSafelyWithoutOverselling() throws Exception {
        // Produit avec stock initial de 10 unités
        Product concurrentProduct = productRepository.save(Product.builder()
                .name("Produit Concurrence " + UUID.randomUUID())
                .description("Concurrence Test")
                .price(new BigDecimal("10.00"))
                .stockQuantity(10)
                .category("Concurrence")
                .build());

        // On lance 20 threads tentant chacun de réserver 1 unité
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final String resId = "RES-CONC-" + i + "-" + UUID.randomUUID();
            executor.submit(() -> {
                try {
                    latch.await();
                    String payload = """
                            {
                                "reservationId": "%s",
                                "items": [
                                    {
                                        "productId": %d,
                                        "quantity": 1
                                    }
                                ]
                            }
                            """.formatted(resId, concurrentProduct.getId());

                    int statusCode = mockMvc.perform(post("/api/products/reservations")
                                    .with(user("concurrent-user"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(payload))
                            .andReturn()
                            .getResponse()
                            .getStatus();

                    if (statusCode == 201) {
                        successCount.incrementAndGet();
                    } else if (statusCode == 409) {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // handle
                }
            });
        }

        // Déclencher les threads simultanément
        latch.countDown();
        executor.shutdown();
        boolean finished = executor.awaitTermination(15, TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        // Exactement 10 doivent réussir (stock initial = 10) et 10 doivent être rejetés avec 409 Conflict
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(conflictCount.get()).isEqualTo(10);

        Product finalProduct = productRepository.findById(concurrentProduct.getId()).orElseThrow();
        assertThat(finalProduct.getStockQuantity()).isEqualTo(0);
        assertThat(finalProduct.getStockQuantity()).isNotNegative();
    }
}
