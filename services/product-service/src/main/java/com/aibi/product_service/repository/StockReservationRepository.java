package com.aibi.product_service.repository;

import com.aibi.product_service.entity.StockReservation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    @EntityGraph(attributePaths = {"items"})
    Optional<StockReservation> findByReservationId(String reservationId);

    boolean existsByReservationId(String reservationId);
}
