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

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    Optional<Product> findByNameIgnoreCase(String name);

    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Product> findByCategoryIgnoreCase(String category, Pageable pageable);

    @Modifying
    @Query(value = "UPDATE products SET stock_quantity = stock_quantity - :quantity, updated_at = NOW() WHERE id = :productId AND stock_quantity >= :quantity", nativeQuery = true)
    int decrementStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Modifying
    @Query(value = "UPDATE products SET stock_quantity = stock_quantity + :quantity, updated_at = NOW() WHERE id = :productId", nativeQuery = true)
    int incrementStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
