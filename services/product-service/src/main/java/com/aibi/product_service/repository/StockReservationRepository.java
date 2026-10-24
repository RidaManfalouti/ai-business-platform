package com.aibi.product_service.repository;

import com.aibi.product_service.entity.StockReservation;
import com.aibi.product_service.model.StockReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    @EntityGraph(attributePaths = {"items"})
    Optional<StockReservation> findByReservationId(String reservationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT r FROM StockReservation r WHERE r.reservationId = :reservationId")
    Optional<StockReservation> findByReservationIdForUpdate(@Param("reservationId") String reservationId);

    boolean existsByReservationId(String reservationId);

    @EntityGraph(attributePaths = {"items"})
    List<StockReservation> findByStatusAndCreatedAtBefore(StockReservationStatus status, LocalDateTime threshold);
}
