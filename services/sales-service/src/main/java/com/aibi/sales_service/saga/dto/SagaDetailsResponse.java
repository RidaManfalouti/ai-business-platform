package com.aibi.sales_service.saga.dto;

import com.aibi.sales_service.saga.entity.Saga;
import com.aibi.sales_service.saga.entity.SagaEvent;
import com.aibi.sales_service.saga.model.SagaStatus;
import com.aibi.sales_service.saga.model.SagaType;

import java.time.LocalDateTime;
import java.util.List;

public record SagaDetailsResponse(
        Long id,
        String sagaId,
        SagaType sagaType,
        Long saleId,
        String saleReference,
        String reservationId,
        SagaStatus status,
        int attemptCount,
        int maxAttempts,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastAttemptAt,
        LocalDateTime nextRetryAt,
        LocalDateTime completedAt,
        String lastError,
        List<SagaEvent> events
) {
    public static SagaDetailsResponse from(Saga saga, List<SagaEvent> events) {
        return new SagaDetailsResponse(
                saga.getId(),
                saga.getSagaId(),
                saga.getSagaType(),
                saga.getSaleId(),
                saga.getSaleReference(),
                saga.getReservationId(),
                saga.getStatus(),
                saga.getAttemptCount(),
                saga.getMaxAttempts(),
                saga.getCreatedAt(),
                saga.getUpdatedAt(),
                saga.getLastAttemptAt(),
                saga.getNextRetryAt(),
                saga.getCompletedAt(),
                saga.getLastError(),
                events != null ? events : List.of()
        );
    }
}
