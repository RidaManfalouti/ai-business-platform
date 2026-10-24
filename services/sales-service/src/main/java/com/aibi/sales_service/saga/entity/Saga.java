package com.aibi.sales_service.saga.entity;

import com.aibi.sales_service.saga.exception.InvalidSagaStateTransitionException;
import com.aibi.sales_service.saga.model.SagaStatus;
import com.aibi.sales_service.saga.model.SagaType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sagas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Saga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "saga_id", nullable = false, unique = true, length = 64)
    private String sagaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "saga_type", nullable = false, length = 30)
    private SagaType sagaType;

    @Column(name = "sale_id")
    private Long saleId;

    @Column(name = "sale_reference", nullable = false, length = 64)
    private String saleReference;

    @Column(name = "reservation_id", length = 100)
    private String reservationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SagaStatus status;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private int maxAttempts = 5;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = SagaStatus.STARTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void transitionTo(SagaStatus nextStatus, String diagnostic) {
        if (this.status != nextStatus && !this.status.canTransitionTo(nextStatus)) {
            throw new InvalidSagaStateTransitionException(
                    "Transition d'état Saga invalide : impossible de passer de " + this.status + " à " + nextStatus
                            + " pour la Saga " + this.sagaId + " (" + diagnostic + ")"
            );
        }
        this.status = nextStatus;
        this.updatedAt = LocalDateTime.now();
        if (nextStatus == SagaStatus.COMPLETED || nextStatus == SagaStatus.COMPENSATED) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public void recordAttempt(boolean success, String error, LocalDateTime nextRetry) {
        this.attemptCount++;
        this.lastAttemptAt = LocalDateTime.now();
        this.lastError = sanitizeError(error);

        if (success) {
            this.nextRetryAt = null;
            transitionTo(SagaStatus.COMPENSATED, "Compensation confirmée avec succès");
        } else {
            if (this.attemptCount >= this.maxAttempts) {
                this.nextRetryAt = null;
                transitionTo(SagaStatus.FAILED_REQUIRES_ATTENTION, "Nombre maximal de tentatives atteint (" + this.maxAttempts + ")");
            } else {
                this.nextRetryAt = nextRetry;
                transitionTo(SagaStatus.COMPENSATION_PENDING, "Tentative " + this.attemptCount + " échouée, prochain essai à " + nextRetry);
            }
        }
    }

    private String sanitizeError(String error) {
        if (error == null || error.isBlank()) {
            return null;
        }
        // Exclure tout token / secret potentiel et limiter à 990 caractères
        String sanitized = error.replaceAll("(?i)(bearer\\s+)[a-zA-Z0-9._-]+", "$1***")
                .replaceAll("(?i)(password\\s*=\\s*)[^\\s,;&]+", "$1***")
                .replaceAll("(?i)(secret\\s*=\\s*)[^\\s,;&]+", "$1***");
        if (sanitized.length() > 990) {
            return sanitized.substring(0, 990) + "...";
        }
        return sanitized;
    }
}
