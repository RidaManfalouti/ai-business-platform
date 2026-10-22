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
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class SalesService {

    private static final Logger log = LoggerFactory.getLogger(SalesService.class);

    private final SaleRepository saleRepository;
    private final ProductServiceClient productServiceClient;
    private final CustomerServiceClient customerServiceClient;

    public SalesService(SaleRepository saleRepository,
                        ProductServiceClient productServiceClient,
                        CustomerServiceClient customerServiceClient) {
        this.saleRepository = saleRepository;
        this.productServiceClient = productServiceClient;
        this.customerServiceClient = customerServiceClient;
    }

    /**
     * Crée une nouvelle vente avec réservation atomique de stock.
     *
     * IMPORTANT (Gestion de la cohérence distribuée et Saga compensatoire) :
     * 1. La vérification consultative read-only est remplacée par une réservation de stock atomique
     *    côté Product Service (via un décrément conditionnel en base).
     * 2. Si la création de la vente échoue après la réservation (ex: erreur DB, timeout), une action
     *    compensatoire 'releaseStock' est immédiatement déclenchée pour restituer le stock réservé.
     * 3. Fenêtres de défaillance résiduelles : si la compensation échoue en raison d'une panne réseau majeure,
     *    une réconciliation idempotente asynchrone est requise. L'atomicité n'est donc pas une transaction 2PC,
     *    mais un pattern Saga à cohérence à terme (eventual consistency).
     *
     * Idempotence & anti-doublon :
     * Si le client fournit une référence de commande (clé d'idempotence), l'unicité est vérifiée
     * en amont et garantie au niveau de la base PostgreSQL par la contrainte UNIQUE idx_sales_reference.
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

        // 4. Construction de l'entité Sale et calcul strict côté serveur des prix
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

        // 5. Réservation atomique du stock dans Product Service (décrément conditionnel en base)
        StockReservationRequest reservationRequest = new StockReservationRequest(reference, reservationItems);
        productServiceClient.reserveStock(reservationRequest);
        boolean stockReserved = true;

        // 6. Sauvegarde locale avec compensation en cas d'échec
        try {
            Sale savedSale = saleRepository.save(sale);
            log.info("Vente créée avec succès. ID: {}, Réf: {}, Total: {}", savedSale.getId(), savedSale.getReference(), savedSale.getTotalAmount());
            return SaleResponse.fromEntity(savedSale);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Violation de contrainte d'intégrité lors de la sauvegarde de la vente (référence en doublon possible) : {}", ex.getMessage());
            throw new DuplicateSaleReferenceException("Une vente avec la référence '" + reference + "' existe déjà.");
        } catch (Exception ex) {
            log.error("Échec persistance vente {} : {}. Exécution de la compensation releaseStock.", reference, ex.getMessage());
            if (stockReserved) {
                try {
                    productServiceClient.releaseStock(reference);
                } catch (Exception compEx) {
                    log.error("CRITIQUE : Échec compensation pour réservation {} : {}", reference, compEx.getMessage());
                }
            }
            throw ex;
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

        // Si la vente passe à CANCELLED, libération compensatoire du stock réservé
        if (targetStatus == SaleStatus.CANCELLED) {
            log.info("Annulation de la vente {} : libération du stock réservé pour la référence {}", id, sale.getReference());
            productServiceClient.releaseStock(sale.getReference());
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
