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
import com.aibi.product_service.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class StockReservationService {

    private static final Logger log = LoggerFactory.getLogger(StockReservationService.class);

    private final ProductRepository productRepository;
    private final StockReservationRepository reservationRepository;

    public StockReservationService(ProductRepository productRepository,
                                   StockReservationRepository reservationRepository) {
        this.productRepository = productRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Effectue une réservation atomique et idempotente de stock pour un ensemble d'articles au sein du locataire actif.
     *
     * Garanties :
     * 1. Isolation multi-tenant : toute opération est strictement bornée au tenantId de la session / requête.
     * 2. Idempotence : si reservationId existe déjà pour ce tenant avec le statut RESERVED, la réservation existante est retournée sans double décrément.
     * 3. Atomicité : le décrément s'effectue via une requête SQL conditionnelle (WHERE tenant_id = :tenantId AND stock_quantity >= :qty).
     * 4. Prévention des interblocages (deadlocks) : tri systématique des articles par productId croissant.
     * 5. Intégrité : le stock ne peut jamais devenir négatif (garanti par le WHERE SQL et la contrainte CHECK PostgreSQL).
     */
    public StockReservationResponse reserveStock(StockReservationRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        String reservationId = request.reservationId().trim();

        // 1. Vérification d'idempotence au niveau du tenant
        Optional<StockReservation> existingOpt = reservationRepository.findByTenantIdAndReservationId(tenantId, reservationId);
        if (existingOpt.isPresent()) {
            StockReservation existing = existingOpt.get();
            if (existing.getStatus() == StockReservationStatus.RESERVED) {
                log.info("Réservation idempotente reconnue pour tenantId : {} et reservationId : {}. Retour de la réservation existante.",
                        tenantId, reservationId);
                return StockReservationResponse.fromEntity(existing);
            } else {
                throw new IllegalStateException("La réservation '" + reservationId + "' est déjà dans l'état " + existing.getStatus() + ".");
            }
        }

        // 2. Consolidation des quantités par produit en cas d'articles multiples dans la même requête
        Map<Long, Integer> consolidated = new TreeMap<>(); // TreeMap trie naturellement par productId croissant (anti-deadlock)
        for (StockReservationItemRequest item : request.items()) {
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new IllegalArgumentException("La quantité pour le produit " + item.productId() + " doit être strictement positive.");
            }
            consolidated.merge(item.productId(), item.quantity(), Integer::sum);
        }

        // 3. Création de l'entité de réservation pour le tenant
        StockReservation reservation = StockReservation.builder()
                .tenantId(tenantId)
                .reservationId(reservationId)
                .status(StockReservationStatus.RESERVED)
                .build();

        // 4. Décrément atomique pour chaque produit (ordre croissant garanti)
        for (Map.Entry<Long, Integer> entry : consolidated.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            Product product = productRepository.findByTenantIdAndId(tenantId, productId)
                    .orElseThrow(() -> new ProductNotFoundException("Produit introuvable avec l'identifiant : " + productId));

            int updatedRows = productRepository.decrementStock(tenantId, productId, quantity);
            if (updatedRows == 0) {
                // Décrément impossible car stock_quantity < quantity
                log.warn("Échec réservation stock : stock insuffisant pour produit {} (demandé: {}, dispo actuelle: {})",
                        product.getName(), quantity, product.getStockQuantity());
                throw new InsufficientStockException(
                        "Stock insuffisant pour le produit '" + product.getName() + "' (ID: " + productId
                                + "). Demandé : " + quantity + ", disponible : " + product.getStockQuantity()
                );
            }

            reservation.addItem(StockReservationItem.builder()
                    .productId(productId)
                    .quantity(quantity)
                    .build());
        }

        StockReservation saved = reservationRepository.save(reservation);
        log.info("Réservation de stock créée avec succès. Tenant: {}, ID: {}, ReservationId: {}, Articles: {}",
                saved.getTenantId(), saved.getId(), saved.getReservationId(), consolidated.size());
        return StockReservationResponse.fromEntity(saved);
    }

    /**
     * Libère une réservation de stock de manière strictement atomique et idempotente (transaction compensatoire Saga)
     * au sein du locataire actif.
     *
     * Utilise un verrou pessimiste exclusif (SELECT FOR UPDATE) au niveau de la base de données PostgreSQL
     * pour garantir qu'aucune exécution concurrente ne puisse réincrémenter le stock plusieurs fois.
     */
    public StockReleaseResponse releaseStock(String reservationId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        String cleanReservationId = reservationId.trim();
        StockReservation reservation = reservationRepository.findByTenantIdAndReservationIdForUpdate(tenantId, cleanReservationId)
                .orElseThrow(() -> new ReservationNotFoundException("Réservation introuvable avec l'identifiant : " + cleanReservationId));

        if (reservation.getStatus() == StockReservationStatus.RELEASED) {
            log.info("Réservation {} déjà libérée pour tenant {}. Opération de libération idempotente sans action.", cleanReservationId, tenantId);
            return new StockReleaseResponse(cleanReservationId, StockReservationStatus.RELEASED.name(), "La réservation a déjà été libérée.");
        }

        for (StockReservationItem item : reservation.getItems()) {
            productRepository.incrementStock(tenantId, item.getProductId(), item.getQuantity());
            log.debug("Stock réincrémenté de {} pour le produit {} (tenant: {})", item.getQuantity(), item.getProductId(), tenantId);
        }

        reservation.setStatus(StockReservationStatus.RELEASED);
        reservation.setReleasedAt(java.time.LocalDateTime.now());
        reservationRepository.save(reservation);

        log.info("Réservation de stock {} libérée avec succès pour tenant {} (compensation exécutée).", cleanReservationId, tenantId);
        return new StockReleaseResponse(cleanReservationId, StockReservationStatus.RELEASED.name(), "Stock libéré avec succès.");
    }

    @Transactional(readOnly = true)
    public StockReservationResponse getReservation(String reservationId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        String cleanReservationId = reservationId.trim();
        StockReservation reservation = reservationRepository.findByTenantIdAndReservationId(tenantId, cleanReservationId)
                .orElseThrow(() -> new ReservationNotFoundException("Réservation introuvable avec l'identifiant : " + cleanReservationId));
        return StockReservationResponse.fromEntity(reservation);
    }

    @Transactional(readOnly = true)
    public List<StockReservationResponse> getStaleReservations(int olderThanMinutes) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        java.time.LocalDateTime threshold = java.time.LocalDateTime.now().minusMinutes(olderThanMinutes);
        return reservationRepository.findByTenantIdAndStatusAndCreatedAtBefore(tenantId, StockReservationStatus.RESERVED, threshold)
                .stream()
                .map(StockReservationResponse::fromEntity)
                .toList();
    }
}
