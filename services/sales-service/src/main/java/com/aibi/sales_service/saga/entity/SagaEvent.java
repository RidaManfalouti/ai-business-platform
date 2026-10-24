package com.aibi.sales_service.saga.entity;

import com.aibi.sales_service.saga.model.SagaStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "saga_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "saga_id", nullable = false, length = 64)
    private String sagaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private SagaStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private SagaStatus toStatus;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(length = 1000)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
