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

@Repository
public interface SagaRepository extends JpaRepository<Saga, Long> {

    Optional<Saga> findBySagaId(String sagaId);

    Optional<Saga> findBySaleReferenceAndSagaType(String saleReference, SagaType sagaType);

    List<Saga> findBySaleReference(String saleReference);

    List<Saga> findByStatus(SagaStatus status);

    List<Saga> findByStatusIn(Collection<SagaStatus> statuses);

    long countByStatus(SagaStatus status);

    /**
     * Sélectionne et verrouille de façon atomique et sans blocage (FOR UPDATE SKIP LOCKED)
     * les sagas éligibles à une reprise de compensation.
     * Cette clause garantit qu'en environnement multi-instances, chaque saga n'est traitée
     * que par un unique worker de reprise à un instant T.
     */
    @Query(value = "SELECT * FROM sagas " +
            "WHERE status IN ('COMPENSATION_PENDING', 'COMPENSATING') " +
            "AND (next_retry_at IS NULL OR next_retry_at <= :now) " +
            "ORDER BY next_retry_at ASC NULLS FIRST " +
            "LIMIT :limit " +
            "FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<Saga> findEligibleForRecovery(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /**
     * Recherche les sagas bloquées dans un état intermédiaire depuis plus d'un seuil donné (détection d'anomalies de réconciliation).
     */
    @Query("SELECT s FROM Saga s WHERE s.status IN :statuses AND s.createdAt < :threshold")
    List<Saga> findStaleSagas(@Param("statuses") Collection<SagaStatus> statuses, @Param("threshold") LocalDateTime threshold);
}
