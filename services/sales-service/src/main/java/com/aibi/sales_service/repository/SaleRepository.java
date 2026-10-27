package com.aibi.sales_service.repository;

import com.aibi.sales_service.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    boolean existsByReference(String reference);

    Optional<Sale> findByReference(String reference);

    @Query("SELECT s FROM Sale s LEFT JOIN FETCH s.items WHERE s.id = :id")
    Optional<Sale> findByIdWithItems(@Param("id") Long id);

    Page<Sale> findByCustomerId(Long customerId, Pageable pageable);

    Page<Sale> findByStatus(com.aibi.sales_service.model.SaleStatus status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.status = com.aibi.sales_service.model.SaleStatus.COMPLETED AND (CAST(:startDate AS java.time.LocalDateTime) IS NULL OR s.createdAt >= :startDate) AND (CAST(:endDate AS java.time.LocalDateTime) IS NULL OR s.createdAt <= :endDate)")
    BigDecimal sumCompletedRevenue(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.status = com.aibi.sales_service.model.SaleStatus.COMPLETED AND (CAST(:startDate AS java.time.LocalDateTime) IS NULL OR s.createdAt >= :startDate) AND (CAST(:endDate AS java.time.LocalDateTime) IS NULL OR s.createdAt <= :endDate)")
    long countCompletedSales(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(s) FROM Sale s WHERE (CAST(:startDate AS java.time.LocalDateTime) IS NULL OR s.createdAt >= :startDate) AND (CAST(:endDate AS java.time.LocalDateTime) IS NULL OR s.createdAt <= :endDate)")
    long countTotalSales(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT s.status, COUNT(s) FROM Sale s WHERE (CAST(:startDate AS java.time.LocalDateTime) IS NULL OR s.createdAt >= :startDate) AND (CAST(:endDate AS java.time.LocalDateTime) IS NULL OR s.createdAt <= :endDate) GROUP BY s.status")
    List<Object[]> countByStatus(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT CAST(created_at AS DATE) as sale_date, COUNT(*) as order_count, COALESCE(SUM(total_amount), 0) as total_rev FROM sales WHERE status = 'COMPLETED' AND (CAST(:startDate AS TIMESTAMP) IS NULL OR created_at >= CAST(:startDate AS TIMESTAMP)) AND (CAST(:endDate AS TIMESTAMP) IS NULL OR created_at <= CAST(:endDate AS TIMESTAMP)) GROUP BY CAST(created_at AS DATE) ORDER BY sale_date ASC", nativeQuery = true)
    List<Object[]> getDailyTrendsNative(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT si.product_id, si.product_name_snapshot, COALESCE(SUM(si.quantity), 0) as total_qty, COALESCE(SUM(si.line_total), 0) as total_rev FROM sale_items si JOIN sales s ON si.sale_id = s.id WHERE s.status = 'COMPLETED' AND (CAST(:startDate AS TIMESTAMP) IS NULL OR s.created_at >= CAST(:startDate AS TIMESTAMP)) AND (CAST(:endDate AS TIMESTAMP) IS NULL OR s.created_at <= CAST(:endDate AS TIMESTAMP)) GROUP BY si.product_id, si.product_name_snapshot ORDER BY total_qty DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> getTopProductsNative(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("limit") int limit);
}
