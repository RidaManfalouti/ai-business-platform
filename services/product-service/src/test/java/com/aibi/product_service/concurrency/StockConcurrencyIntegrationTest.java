package com.aibi.product_service.concurrency;

import com.aibi.product_service.dto.StockReservationItemRequest;
import com.aibi.product_service.dto.StockReservationRequest;
import com.aibi.product_service.dto.StockReservationResponse;
import com.aibi.product_service.entity.Product;
import com.aibi.product_service.exception.InsufficientStockException;
import com.aibi.product_service.repository.ProductRepository;
import com.aibi.product_service.repository.StockReservationRepository;
import com.aibi.product_service.service.StockReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class StockConcurrencyIntegrationTest {

    @Autowired
    private StockReservationService stockReservationService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockReservationRepository reservationRepository;

    @Test
    @DisplayName("Concurrence réelle : 6 demandes concurrentes pour 1 unité sur un stock initial de 5 -> exactement 5 succès et 1 rejet 409")
    void testConcurrentOversellPrevention_5UnitsVs6Requests() throws InterruptedException {
        // 1. Création d'un produit avec un stock strictement égal à 5
        Product product = Product.builder()
                .name("Test Concurrency Laptop " + UUID.randomUUID())
                .description("Produit de test de concurrence")
                .price(new BigDecimal("999.99"))
                .stockQuantity(5)
                .category("Informatique")
                .build();
        Product savedProduct = productRepository.save(product);
        Long productId = savedProduct.getId();

        int numberOfThreads = 6;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startGate.await(); // Attente du coup d'envoi simultané
                    String reservationId = "CONC-5VS6-" + index + "-" + UUID.randomUUID();
                    StockReservationRequest request = new StockReservationRequest(
                            reservationId,
                            List.of(new StockReservationItemRequest(productId, 1))
                    );
                    stockReservationService.reserveStock(request);
                    successCount.incrementAndGet();
                } catch (InsufficientStockException ex) {
                    failureCount.incrementAndGet();
                } catch (Exception ex) {
                    // Erreur inattendue
                    ex.printStackTrace();
                } finally {
                    endGate.countDown();
                }
            });
        }

        // Lancement simultané des 6 threads
        startGate.countDown();
        boolean completed = endGate.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get())
                .as("Exactement 5 réservations doivent réussir car le stock disponible initial est de 5")
                .isEqualTo(5);
        assertThat(failureCount.get())
                .as("Exactement 1 réservation doit être rejetée pour stock insuffisant")
                .isEqualTo(1);

        // Vérification de l'état final en base : le stock résiduel doit être exactement 0 (jamais négatif)
        Product finalProduct = productRepository.findById(productId).orElseThrow();
        assertThat(finalProduct.getStockQuantity())
                .as("Le stock résiduel en base doit être exactement 0")
                .isEqualTo(0);
    }

    @Test
    @DisplayName("Concurrence réelle : 2 demandes concurrentes de 3 unités sur un stock de 5 -> 1 succès, 1 échec, solde 2")
    void testConcurrentOversellPrevention_2RequestsOf3UnitsOnStock5() throws InterruptedException {
        Product product = Product.builder()
                .name("Test Concurrency Screen " + UUID.randomUUID())
                .description("Écran de test")
                .price(new BigDecimal("350.00"))
                .stockQuantity(5)
                .category("Écrans")
                .build();
        Product savedProduct = productRepository.save(product);
        Long productId = savedProduct.getId();

        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startGate.await();
                    String reservationId = "CONC-2X3-" + index + "-" + UUID.randomUUID();
                    StockReservationRequest request = new StockReservationRequest(
                            reservationId,
                            List.of(new StockReservationItemRequest(productId, 3))
                    );
                    stockReservationService.reserveStock(request);
                    successCount.incrementAndGet();
                } catch (InsufficientStockException ex) {
                    failureCount.incrementAndGet();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        boolean completed = endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);

        Product finalProduct = productRepository.findById(productId).orElseThrow();
        assertThat(finalProduct.getStockQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("Idempotence concurrente : 4 threads tentent la même réservation -> stock décrémenté une seule fois")
    void testConcurrentIdempotentReservation_SameReservationId() throws InterruptedException {
        Product product = Product.builder()
                .name("Test Concurrency Mouse " + UUID.randomUUID())
                .description("Souris de test")
                .price(new BigDecimal("25.00"))
                .stockQuantity(10)
                .category("Accessoires")
                .build();
        Product savedProduct = productRepository.save(product);
        Long productId = savedProduct.getId();

        String sameReservationId = "IDEMP-CONC-" + UUID.randomUUID();
        int numberOfThreads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numberOfThreads);

        ConcurrentLinkedQueue<StockReservationResponse> responses = new ConcurrentLinkedQueue<>();
        AtomicInteger errors = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    StockReservationRequest request = new StockReservationRequest(
                            sameReservationId,
                            List.of(new StockReservationItemRequest(productId, 2))
                    );
                    StockReservationResponse resp = stockReservationService.reserveStock(request);
                    responses.add(resp);
                } catch (Exception ex) {
                    errors.incrementAndGet();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        boolean completed = endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        // Le stock ne doit avoir été décrémenté que de 2 (10 -> 8), et JAMAIS de 8 (10 -> 2)
        Product finalProduct = productRepository.findById(productId).orElseThrow();
        assertThat(finalProduct.getStockQuantity()).isEqualTo(8);
        assertThat(responses.size()).isGreaterThanOrEqualTo(1);
    }
}
