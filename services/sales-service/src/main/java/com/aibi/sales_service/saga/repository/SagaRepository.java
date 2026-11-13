package com.aibi.sales_service.saga.repository;

import com.aibi.sales_service.saga.entity.Saga;
import com.aibi.sales_service.saga.model.SagaStatus;
import com.aibi.sales_service.saga.model.SagaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SagaRepository extends JpaRepository<Saga, Long> {

    Optional<Saga> findByTenantIdAndSagaId(UUID tenantId, String sagaId);

    Optional<Saga> findByTenantIdAndSaleReferenceAndSagaType(UUID tenantId, String saleReference, SagaType sagaType);

    List<Saga> findByTenantIdAndSaleReference(UUID tenantId, String saleReference);

    List<Saga> findByTenantIdAndStatus(UUID tenantId, SagaStatus status);

    List<Saga> findByTenantId(UUID tenantId);

    Optional<Saga> findBySagaId(String sagaId);

    Optional<Saga> findBySaleReferenceAndSagaType(String saleReference, SagaType sagaType);

    List<Saga> findBySaleReference(String saleReference);

    List<Saga> findByStatus(SagaStatus status);

    List<Saga> findByStatusIn(Collection<SagaStatus> statuses);

    long countByStatus(SagaStatus status);

    /**
     * Sélectionne et verrouille de façon atomique et sans blocage (FOR UPDATE SKIP LOCKED)
     * les sagas éligibles à une reprise de compensation, tous locataires confondus.
     */
    @Query(value = "SELECT * FROM sagas " +
            "WHERE status IN ('COMPENSATION_PENDING', 'COMPENSATING') " +
            "AND (next_retry_at IS NULL OR next_retry_at <= :now) " +
            "ORDER BY next_retry_at ASC NULLS FIRST " +
            "LIMIT :limit " +
            "FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<Saga> findEligibleForRecovery(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /**
     * Recherche les sagas bloquées dans un état intermédiaire depuis plus d'un seuil donné.
     */
    @Query("SELECT s FROM Saga s WHERE s.status IN :statuses AND s.createdAt < :threshold")
    List<Saga> findStaleSagas(@Param("statuses") Collection<SagaStatus> statuses, @Param("threshold") LocalDateTime threshold);

    /**
     * Retourne la date de création de la plus ancienne saga en cours ou en attente de compensation.
     */
    @Query("SELECT MIN(s.createdAt) FROM Saga s WHERE s.status IN ('COMPENSATION_PENDING', 'COMPENSATING')")
    Optional<LocalDateTime> findOldestStuckCompensationCreatedAt();

    /**
     * Retourne la date de création de la plus ancienne saga non terminée en phase d'exécution.
     */
    @Query("SELECT MIN(s.createdAt) FROM Saga s WHERE s.status IN ('STARTED', 'STOCK_RESERVED', 'SALE_PERSISTED')")
    Optional<LocalDateTime> findOldestPendingOperationCreatedAt();
}
