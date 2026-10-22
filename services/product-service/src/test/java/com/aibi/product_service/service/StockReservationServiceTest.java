package com.aibi.product_service.service;

import com.aibi.product_service.dto.*;
import com.aibi.product_service.entity.Product;
import com.aibi.product_service.entity.StockReservation;
import com.aibi.product_service.entity.StockReservationItem;
import com.aibi.product_service.exception.InsufficientStockException;
import com.aibi.product_service.exception.ProductNotFoundException;
import com.aibi.product_service.exception.ReservationNotFoundException;
import com.aibi.product_service.model.StockReservationStatus;
import com.aibi.product_service.repository.ProductRepository;
import com.aibi.product_service.repository.StockReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockReservationServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockReservationRepository reservationRepository;

    @InjectMocks
    private StockReservationService reservationService;

    private Product mockProduct1;
    private Product mockProduct2;

    @BeforeEach
    void setUp() {
        mockProduct1 = Product.builder()
                .id(1L)
                .name("MacBook Pro")
                .description("Ordinateur portable Apple")
                .price(new BigDecimal("1999.99"))
                .stockQuantity(10)
                .category("Informatique")
                .build();

        mockProduct2 = Product.builder()
                .id(2L)
                .name("Souris Sans Fil")
                .description("Souris ergonomique")
                .price(new BigDecimal("49.99"))
                .stockQuantity(5)
                .category("Accessoires")
                .build();
    }

    @Test
    @DisplayName("reserveStock : Réservation valide mono-article -> stock décrémenté et statut RESERVED")
    void reserveStock_Success_SingleItem() {
        when(reservationRepository.findByReservationId("RES-001")).thenReturn(Optional.empty());
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct1));
        when(productRepository.decrementStock(1L, 3)).thenReturn(1);
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(invocation -> {
            StockReservation r = invocation.getArgument(0);
            r.setId(100L);
            return r;
        });

        StockReservationRequest request = new StockReservationRequest(
                "RES-001",
                List.of(new StockReservationItemRequest(1L, 3))
        );

        StockReservationResponse response = reservationService.reserveStock(request);

        assertThat(response).isNotNull();
        assertThat(response.reservationId()).isEqualTo("RES-001");
        assertThat(response.status()).isEqualTo("RESERVED");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(3);

        verify(productRepository).decrementStock(1L, 3);
        verify(reservationRepository).save(any(StockReservation.class));
    }

    @Test
    @DisplayName("reserveStock : Réservation multi-articles avec agrégation des doublons et tri anti-deadlock")
    void reserveStock_Success_MultiItems_Aggregated() {
        when(reservationRepository.findByReservationId("RES-MULTI")).thenReturn(Optional.empty());
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(mockProduct2));
        when(productRepository.decrementStock(1L, 4)).thenReturn(1);
        when(productRepository.decrementStock(2L, 2)).thenReturn(1);
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Envoi dans le désordre (2 puis 1) avec 1 en doublon (1+3 = 4)
        StockReservationRequest request = new StockReservationRequest(
                "RES-MULTI",
                List.of(
                        new StockReservationItemRequest(2L, 2),
                        new StockReservationItemRequest(1L, 1),
                        new StockReservationItemRequest(1L, 3)
                )
        );

        StockReservationResponse response = reservationService.reserveStock(request);

        assertThat(response.reservationId()).isEqualTo("RES-MULTI");
        assertThat(response.items()).hasSize(2);
        // Doit décrémenter 1L de 4 et 2L de 2
        verify(productRepository).decrementStock(1L, 4);
        verify(productRepository).decrementStock(2L, 2);
    }

    @Test
    @DisplayName("reserveStock : Stock insuffisant -> InsufficientStockException et rollback")
    void reserveStock_InsufficientStock() {
        when(reservationRepository.findByReservationId("RES-FAIL")).thenReturn(Optional.empty());
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct1));
        // decrementStock retourne 0 quand le stock en base < demandé
        when(productRepository.decrementStock(1L, 15)).thenReturn(0);

        StockReservationRequest request = new StockReservationRequest(
                "RES-FAIL",
                List.of(new StockReservationItemRequest(1L, 15))
        );

        assertThatThrownBy(() -> reservationService.reserveStock(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Stock insuffisant pour le produit 'MacBook Pro'");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("reserveStock : Produit inconnu -> ProductNotFoundException")
    void reserveStock_ProductNotFound() {
        when(reservationRepository.findByReservationId("RES-404")).thenReturn(Optional.empty());
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        StockReservationRequest request = new StockReservationRequest(
                "RES-404",
                List.of(new StockReservationItemRequest(999L, 1))
        );

        assertThatThrownBy(() -> reservationService.reserveStock(request))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("reserveStock : Idempotence -> renvoie la réservation existante sans décrémenter le stock")
    void reserveStock_IdempotentReplay() {
        StockReservation existingReservation = StockReservation.builder()
                .id(50L)
                .reservationId("RES-IDEMP")
                .status(StockReservationStatus.RESERVED)
                .build();
        existingReservation.addItem(StockReservationItem.builder().productId(1L).quantity(2).build());

        when(reservationRepository.findByReservationId("RES-IDEMP")).thenReturn(Optional.of(existingReservation));

        StockReservationRequest request = new StockReservationRequest(
                "RES-IDEMP",
                List.of(new StockReservationItemRequest(1L, 2))
        );

        StockReservationResponse response = reservationService.reserveStock(request);

        assertThat(response.reservationId()).isEqualTo("RES-IDEMP");
        assertThat(response.status()).isEqualTo("RESERVED");

        verify(productRepository, never()).decrementStock(any(), any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("reserveStock : Requête de réservation pour une réservation déjà libérée -> IllegalStateException")
    void reserveStock_AlreadyReleased() {
        StockReservation existingReservation = StockReservation.builder()
                .id(50L)
                .reservationId("RES-REL")
                .status(StockReservationStatus.RELEASED)
                .build();

        when(reservationRepository.findByReservationId("RES-REL")).thenReturn(Optional.of(existingReservation));

        StockReservationRequest request = new StockReservationRequest(
                "RES-REL",
                List.of(new StockReservationItemRequest(1L, 2))
        );

        assertThatThrownBy(() -> reservationService.reserveStock(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("déjà dans l'état RELEASED");
    }

    @Test
    @DisplayName("releaseStock : Libération réussie (compensation) -> stock réincrémenté et statut RELEASED")
    void releaseStock_Success() {
        StockReservation reservation = StockReservation.builder()
                .id(1L)
                .reservationId("RES-COMP")
                .status(StockReservationStatus.RESERVED)
                .build();
        reservation.addItem(StockReservationItem.builder().productId(1L).quantity(3).build());
        reservation.addItem(StockReservationItem.builder().productId(2L).quantity(1).build());

        when(reservationRepository.findByReservationId("RES-COMP")).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockReleaseResponse response = reservationService.releaseStock("RES-COMP");

        assertThat(response.reservationId()).isEqualTo("RES-COMP");
        assertThat(response.status()).isEqualTo("RELEASED");

        verify(productRepository).incrementStock(1L, 3);
        verify(productRepository).incrementStock(2L, 1);
        assertThat(reservation.getStatus()).isEqualTo(StockReservationStatus.RELEASED);
        verify(reservationRepository).save(reservation);
    }

    @Test
    @DisplayName("releaseStock : Libération idempotente si déjà libérée -> aucun double réincrément")
    void releaseStock_AlreadyReleased_Idempotent() {
        StockReservation reservation = StockReservation.builder()
                .id(1L)
                .reservationId("RES-COMP-AGAIN")
                .status(StockReservationStatus.RELEASED)
                .build();

        when(reservationRepository.findByReservationId("RES-COMP-AGAIN")).thenReturn(Optional.of(reservation));

        StockReleaseResponse response = reservationService.releaseStock("RES-COMP-AGAIN");

        assertThat(response.status()).isEqualTo("RELEASED");
        verify(productRepository, never()).incrementStock(any(), any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("releaseStock : Réservation inexistante -> ReservationNotFoundException")
    void releaseStock_NotFound() {
        when(reservationRepository.findByReservationId("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.releaseStock("UNKNOWN"))
                .isInstanceOf(ReservationNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }
}
