package com.aibi.sales_service.saga.service;

import com.aibi.sales_service.saga.entity.Saga;
import com.aibi.sales_service.saga.entity.SagaEvent;
import com.aibi.sales_service.saga.exception.InvalidSagaStateTransitionException;
import com.aibi.sales_service.saga.model.SagaStatus;
import com.aibi.sales_service.saga.model.SagaType;
import com.aibi.sales_service.saga.repository.SagaEventRepository;
import com.aibi.sales_service.saga.repository.SagaRepository;
import com.aibi.sales_service.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SagaService {

    private static final Logger log = LoggerFactory.getLogger(SagaService.class);

    private final SagaRepository sagaRepository;
    private final SagaEventRepository sagaEventRepository;

    public SagaService(SagaRepository sagaRepository, SagaEventRepository sagaEventRepository) {
        this.sagaRepository = sagaRepository;
        this.sagaEventRepository = sagaEventRepository;
    }

    @Transactional
    public Saga createSaga(String sagaId, SagaType sagaType, String saleReference, String reservationId, SagaStatus initialStatus) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return createSaga(tenantId, sagaId, sagaType, saleReference, reservationId, initialStatus);
    }

    @Transactional
    public Saga createSaga(UUID tenantId, String sagaId, SagaType sagaType, String saleReference, String reservationId, SagaStatus initialStatus) {
        Optional<Saga> existing = sagaRepository.findByTenantIdAndSagaId(tenantId, sagaId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Saga saga = Saga.builder()
                .tenantId(tenantId)
                .sagaId(sagaId)
                .sagaType(sagaType)
                .saleReference(saleReference)
                .reservationId(reservationId)
                .status(initialStatus != null ? initialStatus : SagaStatus.STARTED)
                .attemptCount(0)
                .maxAttempts(5)
                .build();

        Saga saved = sagaRepository.saveAndFlush(saga);
        recordEvent(tenantId, sagaId, null, saved.getStatus(), "SAGA_CREATED", "Saga initialisée avec statut " + saved.getStatus());
        log.info("Saga persistée pour tenant {}. ID: {}, Type: {}, Réf: {}, Statut: {}", tenantId, sagaId, sagaType, saleReference, saved.getStatus());
        return saved;
    }

    private Optional<Saga> findSaga(String sagaId) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return sagaRepository.findByTenantIdAndSagaId(tenantId, sagaId);
        }
        return sagaRepository.findBySagaId(sagaId);
    }

    @Transactional
    public Saga transitionSaga(String sagaId, SagaStatus newStatus, String message) {
        Saga saga = findSaga(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("Saga introuvable : " + sagaId));

        SagaStatus oldStatus = saga.getStatus();
        if (oldStatus == newStatus) {
            return saga;
        }

        saga.transitionTo(newStatus, message);
        Saga saved = sagaRepository.saveAndFlush(saga);
        recordEvent(saga.getTenantId(), sagaId, oldStatus, newStatus, "STATE_TRANSITION", message);
        log.info("Saga {} transitionnée de {} vers {} : {}", sagaId, oldStatus, newStatus, message);
        return saved;
    }

    @Transactional
    public void linkSale(String sagaId, Long saleId) {
        findSaga(sagaId).ifPresent(saga -> {
            saga.setSaleId(saleId);
            sagaRepository.saveAndFlush(saga);
        });
    }

    @Transactional
    public void linkReservation(String sagaId, String reservationId) {
        findSaga(sagaId).ifPresent(saga -> {
            saga.setReservationId(reservationId);
            SagaStatus oldStatus = saga.getStatus();
            saga.transitionTo(SagaStatus.STOCK_RESERVED, "Réservation de stock confirmée (" + reservationId + ")");
            sagaRepository.saveAndFlush(saga);
            recordEvent(saga.getTenantId(), sagaId, oldStatus, SagaStatus.STOCK_RESERVED, "STOCK_RESERVED", "Réservation confirmée : " + reservationId);
        });
    }

    @Transactional
    public void recordCompensationAttempt(String sagaId, boolean success, String error, LocalDateTime nextRetry) {
        Saga saga = findSaga(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("Saga introuvable : " + sagaId));

        SagaStatus oldStatus = saga.getStatus();
        saga.recordAttempt(success, error, nextRetry);
        Saga saved = sagaRepository.saveAndFlush(saga);

        String eventType = success ? "COMPENSATION_SUCCEEDED" : "COMPENSATION_FAILED";
        String msg = success ? "Libération du stock confirmée avec succès." : "Tentative échouée: " + error;
        recordEvent(saga.getTenantId(), sagaId, oldStatus, saved.getStatus(), eventType, msg);
    }

    @Transactional
    public boolean claimSagaForCompensation(String sagaId) {
        Optional<Saga> opt = findSaga(sagaId);
        if (opt.isEmpty()) return false;
        Saga saga = opt.get();

        if (saga.getStatus() != SagaStatus.COMPENSATION_PENDING && saga.getStatus() != SagaStatus.COMPENSATING) {
            return false;
        }

        SagaStatus oldStatus = saga.getStatus();
        saga.setStatus(SagaStatus.COMPENSATING);
        saga.setLastAttemptAt(LocalDateTime.now());
        sagaRepository.saveAndFlush(saga);
        recordEvent(saga.getTenantId(), sagaId, oldStatus, SagaStatus.COMPENSATING, "COMPENSATION_STARTED", "Tentative de compensation en cours");
        return true;
    }

    @Transactional
    public Saga manualRetry(String sagaId) {
        Saga saga = findSaga(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("Saga introuvable : " + sagaId));

        SagaStatus oldStatus = saga.getStatus();
        if (oldStatus != SagaStatus.FAILED_REQUIRES_ATTENTION && oldStatus != SagaStatus.COMPENSATION_PENDING) {
            throw new InvalidSagaStateTransitionException("Seules les Sagas en état FAILED_REQUIRES_ATTENTION ou COMPENSATION_PENDING peuvent faire l'objet d'un réessai manuel.");
        }

        saga.setStatus(SagaStatus.COMPENSATION_PENDING);
        saga.setNextRetryAt(LocalDateTime.now());
        saga.setLastError("Réessai manuel déclenché par un administrateur.");
        Saga saved = sagaRepository.saveAndFlush(saga);
        recordEvent(saga.getTenantId(), sagaId, oldStatus, SagaStatus.COMPENSATING, "MANUAL_RETRY", "Réessai manuel ordonné par un administrateur.");
        log.warn("Saga {} réarmée pour réessai manuel.", sagaId);
        return saved;
    }

    @Transactional
    public Saga manualResolve(String sagaId, String resolutionNote) {
        Saga saga = findSaga(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("Saga introuvable : " + sagaId));

        SagaStatus oldStatus = saga.getStatus();
        saga.setStatus(SagaStatus.COMPENSATED);
        saga.setCompletedAt(LocalDateTime.now());
        saga.setLastError("Résolution administrative manuelle : " + resolutionNote);
        Saga saved = sagaRepository.saveAndFlush(saga);
        recordEvent(saga.getTenantId(), sagaId, oldStatus, SagaStatus.COMPENSATED, "MANUAL_RESOLUTION", "Résolution manuelle : " + resolutionNote);
        log.warn("Saga {} résolue administrativement (statut forcé COMPENSATED). Note: {}", sagaId, resolutionNote);
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Saga> getSagaById(String sagaId) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return sagaRepository.findByTenantIdAndSagaId(tenantId, sagaId);
        }
        return sagaRepository.findBySagaId(sagaId);
    }

    @Transactional(readOnly = true)
    public List<Saga> getSagasBySaleReference(String saleReference) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return sagaRepository.findByTenantIdAndSaleReference(tenantId, saleReference);
        }
        return sagaRepository.findBySaleReference(saleReference);
    }

    @Transactional(readOnly = true)
    public List<Saga> getAllSagas(SagaStatus statusFilter) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            if (statusFilter != null) {
                return sagaRepository.findByTenantIdAndStatus(tenantId, statusFilter);
            }
            return sagaRepository.findByTenantId(tenantId);
        }
        if (statusFilter != null) {
            return sagaRepository.findByStatus(statusFilter);
        }
        return sagaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<SagaEvent> getSagaEvents(String sagaId) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return sagaEventRepository.findByTenantIdAndSagaIdOrderByCreatedAtAsc(tenantId, sagaId);
        }
        return sagaEventRepository.findBySagaIdOrderByCreatedAtAsc(sagaId);
    }

    private void recordEvent(UUID tenantId, String sagaId, SagaStatus from, SagaStatus to, String eventType, String message) {
        try {
            SagaEvent event = SagaEvent.builder()
                    .tenantId(tenantId != null ? tenantId : UUID.fromString("00000000-0000-0000-0000-000000000001"))
                    .sagaId(sagaId)
                    .fromStatus(from)
                    .toStatus(to)
                    .eventType(eventType)
                    .message(message)
                    .build();
            sagaEventRepository.save(event);
        } catch (Exception ex) {
            log.warn("Échec de l'enregistrement de l'événement Saga pour {} : {}", sagaId, ex.getMessage());
        }
    }
}
