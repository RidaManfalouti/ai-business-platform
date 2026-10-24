package com.aibi.sales_service.service;

import com.aibi.sales_service.client.CustomerServiceClient;
import com.aibi.sales_service.client.ProductServiceClient;
import com.aibi.sales_service.client.dto.CustomerDto;
import com.aibi.sales_service.client.dto.ProductDto;
import com.aibi.sales_service.dto.CreateSaleRequest;
import com.aibi.sales_service.dto.SaleItemRequest;
import com.aibi.sales_service.dto.SaleResponse;
import com.aibi.sales_service.dto.UpdateSaleStatusRequest;
import com.aibi.sales_service.entity.Sale;
import com.aibi.sales_service.exception.*;
import com.aibi.sales_service.model.SaleStatus;
import com.aibi.sales_service.repository.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private CustomerServiceClient customerServiceClient;

    @Mock
    private com.aibi.sales_service.saga.service.SagaService sagaService;

    @InjectMocks
    private SalesService salesService;

    private CustomerDto mockCustomer;
    private ProductDto mockProduct1;
    private ProductDto mockProduct2;

    @BeforeEach
    void setUp() {
        mockCustomer = new CustomerDto(1L, "Alice", "Smith", "alice@example.com", "0123456789", "Acme");
        mockProduct1 = new ProductDto(10L, "Laptop", "Description", new BigDecimal("1200.50"), 15, "Tech");
        mockProduct2 = new ProductDto(20L, "Souris", "Description", new BigDecimal("25.00"), 50, "Tech");
    }

    @Test
    @DisplayName("createSale : Création valide avec calcul serveur strict des prix et totaux")
    void createSale_Success() {
        when(customerServiceClient.getCustomerById(1L)).thenReturn(mockCustomer);
        when(productServiceClient.getProductById(10L)).thenReturn(mockProduct1);
        when(productServiceClient.getProductById(20L)).thenReturn(mockProduct2);

        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale s = invocation.getArgument(0);
            s.setId(100L);
            s.setCreatedAt(LocalDateTime.now());
            return s;
        });

        CreateSaleRequest request = new CreateSaleRequest(
                1L,
                "REF-TEST-001",
                List.of(
                        new SaleItemRequest(10L, 2),
                        new SaleItemRequest(20L, 3)
                )
        );

        SaleResponse response = salesService.createSale(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.reference()).isEqualTo("REF-TEST-001");
        assertThat(response.customerId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(SaleStatus.PENDING);

        // Vérification du recalcul côté serveur :
        // Ligne 1 : 1200.50 * 2 = 2401.00
        // Ligne 2 : 25.00 * 3 = 75.00
        // Total : 2476.00
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("2476.00"));
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).unitPrice()).isEqualByComparingTo(new BigDecimal("1200.50"));
        assertThat(response.items().get(0).lineTotal()).isEqualByComparingTo(new BigDecimal("2401.00"));
        assertThat(response.items().get(0).productNameSnapshot()).isEqualTo("Laptop");

        assertThat(response.items().get(1).unitPrice()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(response.items().get(1).lineTotal()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(response.items().get(1).productNameSnapshot()).isEqualTo("Souris");
    }

    @Test
    @DisplayName("createSale : Génération automatique d'une référence unique si non fournie")
    void createSale_GeneratesReferenceWhenMissing() {
        when(customerServiceClient.getCustomerById(1L)).thenReturn(mockCustomer);
        when(productServiceClient.getProductById(10L)).thenReturn(mockProduct1);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale s = invocation.getArgument(0);
            s.setId(101L);
            return s;
        });

        CreateSaleRequest request = new CreateSaleRequest(
                1L,
                null,
                List.of(new SaleItemRequest(10L, 1))
        );

        SaleResponse response = salesService.createSale(request);
        assertThat(response.reference()).isNotNull();
        assertThat(response.reference()).startsWith("SAL-");
    }

    @Test
    @DisplayName("createSale : Client introuvable -> CustomerNotFoundException")
    void createSale_CustomerNotFound() {
        when(customerServiceClient.getCustomerById(99L))
                .thenThrow(new CustomerNotFoundException("Client introuvable"));

        CreateSaleRequest request = new CreateSaleRequest(
                99L,
                null,
                List.of(new SaleItemRequest(10L, 1))
        );

        assertThatThrownBy(() -> salesService.createSale(request))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("Client introuvable");

        verify(saleRepository, never()).save(any());
    }

    @Test
    @DisplayName("createSale : Produit introuvable -> ProductNotFoundException")
    void createSale_ProductNotFound() {
        when(customerServiceClient.getCustomerById(1L)).thenReturn(mockCustomer);
        when(productServiceClient.getProductById(999L))
                .thenThrow(new ProductNotFoundException("Produit introuvable"));

        CreateSaleRequest request = new CreateSaleRequest(
                1L,
                null,
                List.of(new SaleItemRequest(999L, 1))
        );

        assertThatThrownBy(() -> salesService.createSale(request))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("Produit introuvable");

        verify(saleRepository, never()).save(any());
    }

    @Test
    @DisplayName("createSale : Stock insuffisant -> InsufficientStockException (409)")
    void createSale_InsufficientStock() {
        when(customerServiceClient.getCustomerById(1L)).thenReturn(mockCustomer);
        // Stock disponible : 15, demandé : 20
        when(productServiceClient.getProductById(10L)).thenReturn(mockProduct1);

        CreateSaleRequest request = new CreateSaleRequest(
                1L,
                null,
                List.of(new SaleItemRequest(10L, 20))
        );

        assertThatThrownBy(() -> salesService.createSale(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Stock insuffisant pour le produit 'Laptop'");

        verify(saleRepository, never()).save(any());
    }

    @Test
    @DisplayName("createSale : Référence existante envoyée par le client -> DuplicateSaleReferenceException (409)")
    void createSale_DuplicateReferenceFromClient() {
        when(saleRepository.existsByReference("REF-EXISTANTE")).thenReturn(true);

        CreateSaleRequest request = new CreateSaleRequest(
                1L,
                "REF-EXISTANTE",
                List.of(new SaleItemRequest(10L, 1))
        );

        assertThatThrownBy(() -> salesService.createSale(request))
                .isInstanceOf(DuplicateSaleReferenceException.class)
                .hasMessageContaining("existe déjà");

        verify(customerServiceClient, never()).getCustomerById(any());
    }

    @Test
    @DisplayName("createSale : Violation de contrainte UNIQUE concurrente -> DuplicateSaleReferenceException")
    void createSale_DataIntegrityViolationOnSave() {
        when(customerServiceClient.getCustomerById(1L)).thenReturn(mockCustomer);
        when(productServiceClient.getProductById(10L)).thenReturn(mockProduct1);
        when(saleRepository.save(any(Sale.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value"));

        CreateSaleRequest request = new CreateSaleRequest(
                1L,
                "REF-RACE-001",
                List.of(new SaleItemRequest(10L, 1))
        );

        assertThatThrownBy(() -> salesService.createSale(request))
                .isInstanceOf(DuplicateSaleReferenceException.class);
    }

    @Test
    @DisplayName("createSale : Lignes avec produit en doublon -> agrégation des quantités")
    void createSale_AggregatesDuplicateProducts() {
        when(customerServiceClient.getCustomerById(1L)).thenReturn(mockCustomer);
        when(productServiceClient.getProductById(10L)).thenReturn(mockProduct1);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale s = invocation.getArgument(0);
            s.setId(102L);
            return s;
        });

        // Même produit demandé deux fois : 2 + 3 = 5 unités
        CreateSaleRequest request = new CreateSaleRequest(
                1L,
                null,
                List.of(
                        new SaleItemRequest(10L, 2),
                        new SaleItemRequest(10L, 3)
                )
        );

        SaleResponse response = salesService.createSale(request);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(5);
        assertThat(response.items().get(0).lineTotal()).isEqualByComparingTo(new BigDecimal("6002.50"));
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("6002.50"));
    }

    @Test
    @DisplayName("createSale : Quantité négative ou nulle -> IllegalArgumentException")
    void createSale_InvalidQuantity() {
        CreateSaleRequest request = new CreateSaleRequest(
                1L,
                null,
                List.of(new SaleItemRequest(10L, 0))
        );

        assertThatThrownBy(() -> salesService.createSale(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("createSale : Liste d'articles vide -> IllegalArgumentException")
    void createSale_EmptyItems() {
        CreateSaleRequest request = new CreateSaleRequest(
                1L,
                null,
                Collections.emptyList()
        );

        assertThatThrownBy(() -> salesService.createSale(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("createSale : Indisponibilité du service distant -> RemoteServiceUnavailableException")
    void createSale_RemoteServiceUnavailable() {
        when(customerServiceClient.getCustomerById(1L))
                .thenThrow(new RemoteServiceUnavailableException("Customer service down"));

        CreateSaleRequest request = new CreateSaleRequest(
                1L,
                null,
                List.of(new SaleItemRequest(10L, 1))
        );

        assertThatThrownBy(() -> salesService.createSale(request))
                .isInstanceOf(RemoteServiceUnavailableException.class);
    }

    @Test
    @DisplayName("getSaleById : Vente trouvée -> SaleResponse")
    void getSaleById_Found() {
        Sale sale = Sale.builder()
                .id(1L)
                .reference("SAL-123")
                .customerId(5L)
                .status(SaleStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .build();

        when(saleRepository.findByIdWithItems(1L)).thenReturn(Optional.of(sale));

        SaleResponse response = salesService.getSaleById(1L);
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.reference()).isEqualTo("SAL-123");
    }

    @Test
    @DisplayName("getSaleById : Vente introuvable -> SaleNotFoundException")
    void getSaleById_NotFound() {
        when(saleRepository.findByIdWithItems(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salesService.getSaleById(999L))
                .isInstanceOf(SaleNotFoundException.class)
                .hasMessageContaining("Vente introuvable");
    }

    @Test
    @DisplayName("getAllSales : Retourne une page de ventes")
    void getAllSales_ReturnsPage() {
        Sale sale = Sale.builder()
                .id(1L)
                .reference("SAL-123")
                .customerId(5L)
                .status(SaleStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        when(saleRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(sale)));

        Page<SaleResponse> result = salesService.getAllSales(pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getSalesByCustomerId : Retourne les ventes d'un client spécifique")
    void getSalesByCustomerId_ReturnsPage() {
        Sale sale = Sale.builder()
                .id(2L)
                .reference("SAL-456")
                .customerId(5L)
                .status(SaleStatus.CONFIRMED)
                .totalAmount(new BigDecimal("250.00"))
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        when(saleRepository.findByCustomerId(5L, pageable)).thenReturn(new PageImpl<>(List.of(sale)));

        Page<SaleResponse> result = salesService.getSalesByCustomerId(5L, pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).customerId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("updateSaleStatus : Transition PENDING -> CONFIRMED autorisée")
    void updateSaleStatus_PendingToConfirmed() {
        Sale sale = Sale.builder()
                .id(1L)
                .status(SaleStatus.PENDING)
                .reference("SAL-001")
                .customerId(1L)
                .totalAmount(BigDecimal.TEN)
                .build();

        when(saleRepository.findByIdWithItems(1L)).thenReturn(Optional.of(sale));
        when(saleRepository.save(any(Sale.class))).thenAnswer(i -> i.getArgument(0));

        SaleResponse response = salesService.updateSaleStatus(1L, new UpdateSaleStatusRequest(SaleStatus.CONFIRMED));
        assertThat(response.status()).isEqualTo(SaleStatus.CONFIRMED);
    }

    @Test
    @DisplayName("updateSaleStatus : Transition CONFIRMED -> COMPLETED autorisée")
    void updateSaleStatus_ConfirmedToCompleted() {
        Sale sale = Sale.builder()
                .id(1L)
                .status(SaleStatus.CONFIRMED)
                .reference("SAL-001")
                .customerId(1L)
                .totalAmount(BigDecimal.TEN)
                .build();

        when(saleRepository.findByIdWithItems(1L)).thenReturn(Optional.of(sale));
        when(saleRepository.save(any(Sale.class))).thenAnswer(i -> i.getArgument(0));

        SaleResponse response = salesService.updateSaleStatus(1L, new UpdateSaleStatusRequest(SaleStatus.COMPLETED));
        assertThat(response.status()).isEqualTo(SaleStatus.COMPLETED);
    }

    @Test
    @DisplayName("updateSaleStatus : Transition COMPLETED -> CANCELLED interdite (état terminal) -> 409")
    void updateSaleStatus_CompletedToCancelledForbidden() {
        Sale sale = Sale.builder()
                .id(1L)
                .status(SaleStatus.COMPLETED)
                .reference("SAL-001")
                .customerId(1L)
                .totalAmount(BigDecimal.TEN)
                .build();

        when(saleRepository.findByIdWithItems(1L)).thenReturn(Optional.of(sale));

        assertThatThrownBy(() -> salesService.updateSaleStatus(1L, new UpdateSaleStatusRequest(SaleStatus.CANCELLED)))
                .isInstanceOf(InvalidSaleStatusTransitionException.class)
                .hasMessageContaining("Transition de statut interdite");

        verify(saleRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateSaleStatus : Transition CANCELLED -> CONFIRMED interdite (état terminal) -> 409")
    void updateSaleStatus_CancelledToConfirmedForbidden() {
        Sale sale = Sale.builder()
                .id(1L)
                .status(SaleStatus.CANCELLED)
                .reference("SAL-001")
                .customerId(1L)
                .totalAmount(BigDecimal.TEN)
                .build();

        when(saleRepository.findByIdWithItems(1L)).thenReturn(Optional.of(sale));

        assertThatThrownBy(() -> salesService.updateSaleStatus(1L, new UpdateSaleStatusRequest(SaleStatus.CONFIRMED)))
                .isInstanceOf(InvalidSaleStatusTransitionException.class)
                .hasMessageContaining("Transition de statut interdite");
    }

    @Test
    @DisplayName("updateSaleStatus : Même statut -> retour immédiat sans modification")
    void updateSaleStatus_SameStatusNoOp() {
        Sale sale = Sale.builder()
                .id(1L)
                .status(SaleStatus.PENDING)
                .reference("SAL-001")
                .customerId(1L)
                .totalAmount(BigDecimal.TEN)
                .build();

        when(saleRepository.findByIdWithItems(1L)).thenReturn(Optional.of(sale));

        SaleResponse response = salesService.updateSaleStatus(1L, new UpdateSaleStatusRequest(SaleStatus.PENDING));
        assertThat(response.status()).isEqualTo(SaleStatus.PENDING);
        verify(saleRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateSaleStatus : Vente introuvable -> SaleNotFoundException")
    void updateSaleStatus_NotFound() {
        when(saleRepository.findByIdWithItems(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salesService.updateSaleStatus(999L, new UpdateSaleStatusRequest(SaleStatus.CONFIRMED)))
                .isInstanceOf(SaleNotFoundException.class);
    }

    @Test
    @DisplayName("createSale : Échec de la réservation atomique distante -> InsufficientStockException et pas d'enregistrement")
    void createSale_ReservationFailsRemotely() {
        when(customerServiceClient.getCustomerById(1L)).thenReturn(mockCustomer);
        when(productServiceClient.getProductById(10L)).thenReturn(mockProduct1);
        when(productServiceClient.reserveStock(any())).thenThrow(new InsufficientStockException("Stock insuffisant"));

        CreateSaleRequest request = new CreateSaleRequest(
                1L,
                "REF-FAIL-RES",
                List.of(new SaleItemRequest(10L, 2))
        );

        assertThatThrownBy(() -> salesService.createSale(request))
                .isInstanceOf(InsufficientStockException.class);

        verify(saleRepository, never()).save(any());
        verify(productServiceClient, never()).releaseStock(any());
    }

    @Test
    @DisplayName("createSale : Échec inattendu lors de la sauvegarde locale -> Compensation releaseStock déclenchée")
    void createSale_CompensatingReleaseTriggeredOnSaveFailure() {
        when(customerServiceClient.getCustomerById(1L)).thenReturn(mockCustomer);
        when(productServiceClient.getProductById(10L)).thenReturn(mockProduct1);
        when(productServiceClient.reserveStock(any())).thenReturn(null);
        when(saleRepository.save(any(Sale.class))).thenThrow(new RuntimeException("Erreur base locale"));

        CreateSaleRequest request = new CreateSaleRequest(
                1L,
                "REF-COMP-FAIL",
                List.of(new SaleItemRequest(10L, 2))
        );

        assertThatThrownBy(() -> salesService.createSale(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Erreur base locale");

        // Vérification que la compensation a bien été appelée pour annuler la réservation
        verify(productServiceClient).releaseStock("REF-COMP-FAIL");
    }

    @Test
    @DisplayName("updateSaleStatus : Transition vers CANCELLED -> Libération compensatoire de stock déclenchée")
    void updateSaleStatus_ToCancelled_ReleasesStock() {
        Sale sale = Sale.builder()
                .id(1L)
                .reference("SAL-20261022-CANCEL")
                .status(SaleStatus.PENDING)
                .customerId(1L)
                .totalAmount(new BigDecimal("100.00"))
                .build();
        when(saleRepository.findByIdWithItems(1L)).thenReturn(Optional.of(sale));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SaleResponse response = salesService.updateSaleStatus(1L, new UpdateSaleStatusRequest(SaleStatus.CANCELLED));

        assertThat(response.status()).isEqualTo(SaleStatus.CANCELLED);
        // Vérifie l'appel au Product Service pour libérer le stock réservé
        verify(productServiceClient).releaseStock("SAL-20261022-CANCEL");
        verify(saleRepository).save(sale);
    }

    @Test
    @DisplayName("createSale : Échec compensation réseau après échec persistance -> relance l'exception d'origine sans masquer l'erreur")
    void createSale_CompensatingReleaseFailureHandledAndLogged() {
        when(customerServiceClient.getCustomerById(1L)).thenReturn(mockCustomer);
        when(productServiceClient.getProductById(10L)).thenReturn(mockProduct1);
        when(productServiceClient.reserveStock(any())).thenReturn(null);
        when(saleRepository.save(any(Sale.class))).thenThrow(new RuntimeException("Crash base"));
        when(productServiceClient.releaseStock("REF-COMP-FAIL-2"))
                .thenThrow(new RemoteServiceUnavailableException("Réseau indisponible pour compensation", org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));

        CreateSaleRequest request = new CreateSaleRequest(
                1L,
                "REF-COMP-FAIL-2",
                List.of(new SaleItemRequest(10L, 2))
        );

        assertThatThrownBy(() -> salesService.createSale(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Crash base");

        verify(productServiceClient).releaseStock("REF-COMP-FAIL-2");
    }

    @Test
    @DisplayName("updateSaleStatus : Vente déjà CANCELLED -> idempotent, ne libère pas le stock une seconde fois")
    void updateSaleStatus_AlreadyCancelled_DoesNotReleaseAgain() {
        Sale sale = Sale.builder()
                .id(1L)
                .reference("SAL-20261022-ALREADY-CANCELLED")
                .status(SaleStatus.CANCELLED)
                .customerId(1L)
                .totalAmount(new BigDecimal("100.00"))
                .build();
        when(saleRepository.findByIdWithItems(1L)).thenReturn(Optional.of(sale));

        SaleResponse response = salesService.updateSaleStatus(1L, new UpdateSaleStatusRequest(SaleStatus.CANCELLED));

        assertThat(response.status()).isEqualTo(SaleStatus.CANCELLED);
        // Ne doit JAMAIS appeler releaseStock si la vente était déjà CANCELLED
        verify(productServiceClient, never()).releaseStock(any());
        verify(saleRepository, never()).save(any());
    }
}
