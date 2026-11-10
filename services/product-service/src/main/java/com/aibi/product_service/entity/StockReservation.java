package com.aibi.product_service.entity;

import com.aibi.product_service.model.StockReservationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "stock_reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "reservation_id", nullable = false, length = 100)
    private String reservationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockReservationStatus status;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StockReservationItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addItem(StockReservationItem item) {
        items.add(item);
        item.setReservation(this);
    }
}
