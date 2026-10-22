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
     * Effectue une réservation atomique et idempotente de stock pour un ensemble d'articles.
     *
     * Garanties :
     * 1. Idempotence : si reservationId existe déjà avec le statut RESERVED, la réservation existante est retournée sans double décrément.
     * 2. Atomicité : le décrément s'effectue via une requête SQL conditionnelle (WHERE stock_quantity >= :qty).
     * 3. Prévention des interblocages (deadlocks) : tri systématique des articles par productId croissant.
     * 4. Intégrité : le stock ne peut jamais devenir négatif (garanti par le WHERE SQL et la contrainte CHECK PostgreSQL).
     */
    public StockReservationResponse reserveStock(StockReservationRequest request) {
        String reservationId = request.reservationId().trim();

        // 1. Vérification d'idempotence
        Optional<StockReservation> existingOpt = reservationRepository.findByReservationId(reservationId);
        if (existingOpt.isPresent()) {
            StockReservation existing = existingOpt.get();
            if (existing.getStatus() == StockReservationStatus.RESERVED) {
                log.info("Réservation idempotente reconnue pour reservationId : {}. Retour de la réservation existante.", reservationId);
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

        // 3. Création de l'entité de réservation
        StockReservation reservation = StockReservation.builder()
                .reservationId(reservationId)
                .status(StockReservationStatus.RESERVED)
                .build();

        // 4. Décrément atomique pour chaque produit (ordre croissant garanti)
        for (Map.Entry<Long, Integer> entry : consolidated.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ProductNotFoundException("Produit introuvable avec l'identifiant : " + productId));

            int updatedRows = productRepository.decrementStock(productId, quantity);
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
        log.info("Réservation de stock créée avec succès. ID: {}, ReservationId: {}, Articles: {}",
                saved.getId(), saved.getReservationId(), consolidated.size());
        return StockReservationResponse.fromEntity(saved);
    }

    /**
     * Libère une réservation de stock de manière idempotente (transaction compensatoire Saga).
     */
    public StockReleaseResponse releaseStock(String reservationId) {
        String cleanReservationId = reservationId.trim();
        StockReservation reservation = reservationRepository.findByReservationId(cleanReservationId)
                .orElseThrow(() -> new ReservationNotFoundException("Réservation introuvable avec l'identifiant : " + cleanReservationId));

        if (reservation.getStatus() == StockReservationStatus.RELEASED) {
            log.info("Réservation {} déjà libérée. Opération de libération idempotente sans action.", cleanReservationId);
            return new StockReleaseResponse(cleanReservationId, StockReservationStatus.RELEASED.name(), "La réservation a déjà été libérée.");
        }

        for (StockReservationItem item : reservation.getItems()) {
            productRepository.incrementStock(item.getProductId(), item.getQuantity());
            log.debug("Stock réincrémenté de {} pour le produit {}", item.getQuantity(), item.getProductId());
        }

        reservation.setStatus(StockReservationStatus.RELEASED);
        reservationRepository.save(reservation);

        log.info("Réservation de stock {} libérée avec succès (compensation exécutée).", cleanReservationId);
        return new StockReleaseResponse(cleanReservationId, StockReservationStatus.RELEASED.name(), "Stock libéré avec succès.");
    }

    @Transactional(readOnly = true)
    public StockReservationResponse getReservation(String reservationId) {
        String cleanReservationId = reservationId.trim();
        StockReservation reservation = reservationRepository.findByReservationId(cleanReservationId)
                .orElseThrow(() -> new ReservationNotFoundException("Réservation introuvable avec l'identifiant : " + cleanReservationId));
        return StockReservationResponse.fromEntity(reservation);
    }
}
