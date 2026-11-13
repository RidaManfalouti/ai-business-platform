package com.aibi.sales_service.repository;

import com.aibi.sales_service.entity.Sale;
import com.aibi.sales_service.model.SaleStatus;
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
import java.util.UUID;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    boolean existsByTenantIdAndReference(UUID tenantId, String reference);

    Optional<Sale> findByTenantIdAndReference(UUID tenantId, String reference);

    Optional<Sale> findByTenantIdAndId(UUID tenantId, Long id);

    Page<Sale> findByTenantId(UUID tenantId, Pageable pageable);

    @Query("SELECT s FROM Sale s LEFT JOIN FETCH s.items WHERE s.tenantId = :tenantId AND s.reference = :reference")
    Optional<Sale> findByTenantIdAndReferenceWithItems(@Param("tenantId") UUID tenantId, @Param("reference") String reference);

    @Query("SELECT s FROM Sale s LEFT JOIN FETCH s.items WHERE s.tenantId = :tenantId AND s.id = :id")
    Optional<Sale> findByTenantIdAndIdWithItems(@Param("tenantId") UUID tenantId, @Param("id") Long id);

    Page<Sale> findByTenantIdAndCustomerId(UUID tenantId, Long customerId, Pageable pageable);

    Page<Sale> findByTenantIdAndStatus(UUID tenantId, SaleStatus status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.tenantId = :tenantId AND s.status = com.aibi.sales_service.model.SaleStatus.COMPLETED AND (CAST(:startDate AS java.time.LocalDateTime) IS NULL OR s.createdAt >= :startDate) AND (CAST(:endDate AS java.time.LocalDateTime) IS NULL OR s.createdAt <= :endDate)")
    BigDecimal sumCompletedRevenue(@Param("tenantId") UUID tenantId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.tenantId = :tenantId AND s.status = com.aibi.sales_service.model.SaleStatus.COMPLETED AND (CAST(:startDate AS java.time.LocalDateTime) IS NULL OR s.createdAt >= :startDate) AND (CAST(:endDate AS java.time.LocalDateTime) IS NULL OR s.createdAt <= :endDate)")
    long countCompletedSales(@Param("tenantId") UUID tenantId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.tenantId = :tenantId AND (CAST(:startDate AS java.time.LocalDateTime) IS NULL OR s.createdAt >= :startDate) AND (CAST(:endDate AS java.time.LocalDateTime) IS NULL OR s.createdAt <= :endDate)")
    long countTotalSales(@Param("tenantId") UUID tenantId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT s.status, COUNT(s) FROM Sale s WHERE s.tenantId = :tenantId AND (CAST(:startDate AS java.time.LocalDateTime) IS NULL OR s.createdAt >= :startDate) AND (CAST(:endDate AS java.time.LocalDateTime) IS NULL OR s.createdAt <= :endDate) GROUP BY s.status")
    List<Object[]> countByStatus(@Param("tenantId") UUID tenantId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT CAST(created_at AS DATE) as sale_date, COUNT(*) as order_count, COALESCE(SUM(total_amount), 0) as total_rev FROM sales WHERE tenant_id = :tenantId AND status = 'COMPLETED' AND (CAST(:startDate AS TIMESTAMP) IS NULL OR created_at >= CAST(:startDate AS TIMESTAMP)) AND (CAST(:endDate AS TIMESTAMP) IS NULL OR created_at <= CAST(:endDate AS TIMESTAMP)) GROUP BY CAST(created_at AS DATE) ORDER BY sale_date ASC", nativeQuery = true)
    List<Object[]> getDailyTrendsNative(@Param("tenantId") UUID tenantId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT si.product_id, si.product_name_snapshot, COALESCE(SUM(si.quantity), 0) as total_qty, COALESCE(SUM(si.line_total), 0) as total_rev FROM sale_items si JOIN sales s ON si.sale_id = s.id WHERE s.tenant_id = :tenantId AND s.status = 'COMPLETED' AND (CAST(:startDate AS TIMESTAMP) IS NULL OR s.created_at >= CAST(:startDate AS TIMESTAMP)) AND (CAST(:endDate AS TIMESTAMP) IS NULL OR s.created_at <= CAST(:endDate AS TIMESTAMP)) GROUP BY si.product_id, si.product_name_snapshot ORDER BY total_qty DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> getTopProductsNative(@Param("tenantId") UUID tenantId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("limit") int limit);

    // Overloads / legacy fallbacks if needed for system jobs
    Page<Sale> findByStatus(SaleStatus status, Pageable pageable);
}
