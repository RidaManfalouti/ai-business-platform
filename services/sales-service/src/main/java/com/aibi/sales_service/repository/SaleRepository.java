package com.aibi.sales_service.repository;

import com.aibi.sales_service.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    boolean existsByReference(String reference);

    Optional<Sale> findByReference(String reference);

    @Query("SELECT s FROM Sale s LEFT JOIN FETCH s.items WHERE s.id = :id")
    Optional<Sale> findByIdWithItems(@Param("id") Long id);

    Page<Sale> findByCustomerId(Long customerId, Pageable pageable);

    Page<Sale> findByStatus(com.aibi.sales_service.model.SaleStatus status, Pageable pageable);
}
