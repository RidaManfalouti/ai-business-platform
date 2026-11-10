package com.aibi.product_service.repository;

import com.aibi.product_service.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<Product> findByTenantIdAndId(UUID tenantId, Long id);

    boolean existsByTenantIdAndNameIgnoreCase(UUID tenantId, String name);

    boolean existsByTenantIdAndNameIgnoreCaseAndIdNot(UUID tenantId, String name, Long id);

    Optional<Product> findByTenantIdAndNameIgnoreCase(UUID tenantId, String name);

    Page<Product> findByTenantIdAndNameContainingIgnoreCase(UUID tenantId, String name, Pageable pageable);

    Page<Product> findByTenantIdAndCategoryIgnoreCase(UUID tenantId, String category, Pageable pageable);

    @Modifying
    @Query(value = "UPDATE products SET stock_quantity = stock_quantity - :quantity, updated_at = NOW() WHERE id = :productId AND tenant_id = :tenantId AND stock_quantity >= :quantity", nativeQuery = true)
    int decrementStock(@Param("tenantId") UUID tenantId, @Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Modifying
    @Query(value = "UPDATE products SET stock_quantity = stock_quantity + :quantity, updated_at = NOW() WHERE id = :productId AND tenant_id = :tenantId", nativeQuery = true)
    int incrementStock(@Param("tenantId") UUID tenantId, @Param("productId") Long productId, @Param("quantity") Integer quantity);
}
