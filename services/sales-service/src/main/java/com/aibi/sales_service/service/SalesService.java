package com.aibi.sales_service.service;

import com.aibi.sales_service.client.CustomerServiceClient;
import com.aibi.sales_service.client.ProductServiceClient;
import com.aibi.sales_service.client.dto.CustomerDto;
import com.aibi.sales_service.client.dto.ProductDto;
import com.aibi.sales_service.client.dto.StockReservationItemRequest;
import com.aibi.sales_service.client.dto.StockReservationRequest;
import com.aibi.sales_service.dto.*;
import com.aibi.sales_service.entity.Sale;
import com.aibi.sales_service.entity.SaleItem;
import com.aibi.sales_service.exception.DuplicateSaleReferenceException;
import com.aibi.sales_service.exception.InsufficientStockException;
import com.aibi.sales_service.exception.InvalidSaleStatusTransitionException;
import com.aibi.sales_service.exception.SaleNotFoundException;
import com.aibi.sales_service.model.SaleStatus;
import com.aibi.sales_service.repository.SaleRepository;
import com.aibi.sales_service.saga.model.SagaStatus;
import com.aibi.sales_service.saga.model.SagaType;
import com.aibi.sales_service.saga.service.SagaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SalesService {

    private static final Logger log = LoggerFactory.getLogger(SalesService.class);

    private final SaleRepository saleRepository;
    private final ProductServiceClient productServiceClient;
    private final CustomerServiceClient customerServiceClient;
    private final SagaService sagaService;

    public SalesService(SaleRepository saleRepository,
                        ProductServiceClient productServiceClient,
                        CustomerServiceClient customerServiceClient,
                        SagaService sagaService) {
        this.saleRepository = saleRepository;
        this.productServiceClient = productServiceClient;
        this.customerServiceClient = customerServiceClient;
        this.sagaService = sagaService;
    }

    /**
     * Crée une nouvelle vente avec réservation atomique de stock et orchestration de Saga durable.
     *
     * Garanties distribuées :
     * 1. La Saga est persistée dès le début (statut STARTED) pour garantir la traçabilité.
     * 2. Après réservation atomique de stock dans Product Service, la Saga passe à STOCK_RESERVED.
     * 3. Si la persistance locale de la vente échoue, la Saga passe immédiatement à COMPENSATION_PENDING.
     * 4. Une tentative de compensation synchrone (releaseStock) est immédiatement tentée.
     * 5. Si la compensation synchrone échoue (panne réseau, timeout, 5xx), la Saga reste en COMPENSATION_PENDING
     *    avec un prochain réessai planifié (bounded exponential backoff). Le SagaRecoveryWorker assurera
     *    la reprise automatique même en cas de redémarrage du service.
     */
    public SaleResponse createSale(CreateSaleRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("La vente doit comporter au moins une ligne d'article.");
        }

        // 1. Validation locale et agrégation des quantités par produit en cas de doublons dans la requête
        Map<Long, Integer> consolidatedItems = new LinkedHashMap<>();
        for (SaleItemRequest itemReq : request.items()) {
            if (itemReq.quantity() == null || itemReq.quantity() <= 0) {
                throw new IllegalArgumentException("La quantité pour le produit " + itemReq.productId() + " doit être strictement positive.");
            }
            consolidatedItems.merge(itemReq.productId(), itemReq.quantity(), Integer::sum);
        }

        // 2. Détermination ou génération de la référence unique
        String reference = request.reference();
        if (reference != null && !reference.isBlank()) {
            reference = reference.trim();
            if (saleRepository.existsByReference(reference)) {
                throw new DuplicateSaleReferenceException("Une vente avec la référence '" + reference + "' existe déjà.");
            }
        } else {
            reference = generateUniqueReference();
        }

        // 3. Vérification de l'existence du client distant (Customer Service) avant de toucher au stock
        CustomerDto customer = customerServiceClient.getCustomerById(request.customerId());
        if (customer != null) {
            log.debug("Client vérifié : {} {} (ID: {})", customer.firstName(), customer.lastName(), customer.id());
        }

        // 4. Initialisation de la Saga d'orchestration persistée
        String sagaId = "SAGA-CREATE-" + reference;
        sagaService.createSaga(sagaId, SagaType.CREATE_SALE, reference, null, SagaStatus.STARTED);

        // 5. Construction de l'entité Sale et calcul strict côté serveur des prix
        Sale sale = Sale.builder()
                .reference(reference)
                .customerId(request.customerId())
                .status(SaleStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal runningTotal = BigDecimal.ZERO;
        List<StockReservationItemRequest> reservationItems = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : consolidatedItems.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            ProductDto product = productServiceClient.getProductById(productId);

            // Vérification consultative préliminaire rapide (fail-fast)
            if (product.stockQuantity() != null && product.stockQuantity() < quantity) {
                throw new InsufficientStockException(
                        "Stock insuffisant pour le produit '" + product.name() + "' (ID: " + productId
                                + "). Demandé : " + quantity + ", disponible : " + product.stockQuantity()
                );
            }

            BigDecimal unitPrice = product.price().setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);

            SaleItem item = SaleItem.builder()
                    .productId(product.id())
                    .productNameSnapshot(product.name())
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotal)
                    .build();

            sale.addItem(item);
            runningTotal = runningTotal.add(lineTotal);
            reservationItems.add(new StockReservationItemRequest(productId, quantity));
        }

        sale.setTotalAmount(runningTotal.setScale(2, RoundingMode.HALF_UP));

        // 6. Réservation atomique du stock dans Product Service (décrément conditionnel en base)
        StockReservationRequest reservationRequest = new StockReservationRequest(reference, reservationItems);
        productServiceClient.reserveStock(reservationRequest);
        boolean stockReserved = true;
        sagaService.linkReservation(sagaId, reference);

        // 7. Sauvegarde locale avec compensation durable en cas d'échec
        try {
            Sale savedSale = saleRepository.save(sale);
            sagaService.linkSale(sagaId, savedSale.getId());
            sagaService.transitionSaga(sagaId, SagaStatus.SALE_PERSISTED, "Vente enregistrée en base");
            sagaService.transitionSaga(sagaId, SagaStatus.COMPLETED, "Transaction terminée avec succès");
            log.info("Vente créée avec succès. ID: {}, Réf: {}, Total: {}", savedSale.getId(), savedSale.getReference(), savedSale.getTotalAmount());
            return SaleResponse.fromEntity(savedSale);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Violation de contrainte d'intégrité lors de la sauvegarde de la vente (référence en doublon possible) : {}", ex.getMessage());
            handlePersistenceFailure(sagaId, reference, stockReserved, ex.getMessage());
            throw new DuplicateSaleReferenceException("Une vente avec la référence '" + reference + "' existe déjà.");
        } catch (Exception ex) {
            log.error("Échec persistance vente {} : {}. Exécution de la compensation releaseStock.", reference, ex.getMessage());
            handlePersistenceFailure(sagaId, reference, stockReserved, ex.getMessage());
            throw ex;
        }
    }

    private void handlePersistenceFailure(String sagaId, String reference, boolean stockReserved, String errorReason) {
        if (!stockReserved) {
            return;
        }

        sagaService.transitionSaga(sagaId, SagaStatus.COMPENSATION_PENDING, "Échec persistance vente : " + errorReason);

        try {
            productServiceClient.releaseStock(reference);
            sagaService.recordCompensationAttempt(sagaId, true, null, null);
            log.info("Compensation synchrone immédiate réussie pour la référence : {}", reference);
        } catch (Exception compEx) {
            log.error("Compensation synchrone échouée pour {} : {}. Saga durablement enregistrée en COMPENSATION_PENDING pour reprise planifiée.",
                    reference, compEx.getMessage());
            LocalDateTime nextRetry = LocalDateTime.now().plusSeconds(2);
            sagaService.recordCompensationAttempt(sagaId, false, compEx.getMessage(), nextRetry);
        }
    }

    @Transactional(readOnly = true)
    public SaleResponse getSaleById(Long id) {
        Sale sale = saleRepository.findByIdWithItems(id)
                .orElseThrow(() -> new SaleNotFoundException("Vente introuvable avec l'identifiant : " + id));
        return SaleResponse.fromEntity(sale);
    }

    @Transactional(readOnly = true)
    public Page<SaleResponse> getAllSales(Pageable pageable) {
        return saleRepository.findAll(pageable)
                .map(SaleResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<SaleResponse> getSalesByCustomerId(Long customerId, Pageable pageable) {
        return saleRepository.findByCustomerId(customerId, pageable)
                .map(SaleResponse::fromEntity);
    }

    /**
     * Met à jour le statut d'une vente en appliquant la machine à états métier.
     * En cas de transition vers CANCELLED, déclenche la libération compensatoire du stock
     * orchestrée par une Saga durable.
     */
    public SaleResponse updateSaleStatus(Long id, UpdateSaleStatusRequest request) {
        Sale sale = saleRepository.findByIdWithItems(id)
                .orElseThrow(() -> new SaleNotFoundException("Vente introuvable avec l'identifiant : " + id));

        SaleStatus currentStatus = sale.getStatus();
        SaleStatus targetStatus = request.status();

        if (currentStatus == targetStatus) {
            return SaleResponse.fromEntity(sale);
        }

        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new InvalidSaleStatusTransitionException(
                    "Transition de statut interdite : impossible de passer de " + currentStatus + " à " + targetStatus + "."
            );
        }

        // Si la vente passe à CANCELLED, libération compensatoire du stock réservé via Saga durable
        if (targetStatus == SaleStatus.CANCELLED) {
            log.info("Annulation de la vente {} : libération du stock réservé pour la référence {}", id, sale.getReference());
            String cancelSagaId = "SAGA-CANCEL-" + sale.getReference();
            sagaService.createSaga(cancelSagaId, SagaType.CANCEL_SALE, sale.getReference(), sale.getReference(), SagaStatus.COMPENSATION_PENDING);
            sagaService.linkSale(cancelSagaId, sale.getId());

            try {
                productServiceClient.releaseStock(sale.getReference());
                sagaService.recordCompensationAttempt(cancelSagaId, true, null, null);
            } catch (Exception ex) {
                log.warn("Échec de la libération synchrone lors de l'annulation de la vente {} : {}. Saga planifiée pour reprise.",
                        id, ex.getMessage());
                LocalDateTime nextRetry = LocalDateTime.now().plusSeconds(2);
                sagaService.recordCompensationAttempt(cancelSagaId, false, ex.getMessage(), nextRetry);
            }
        }

        sale.setStatus(targetStatus);
        Sale updated = saleRepository.save(sale);
        log.info("Statut de la vente {} mis à jour de {} vers {}", id, currentStatus, targetStatus);
        return SaleResponse.fromEntity(updated);
    }

    private String generateUniqueReference() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "SAL-" + datePrefix + "-" + uuidPart;
    }
}
