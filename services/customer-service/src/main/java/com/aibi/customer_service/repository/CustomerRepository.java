package com.aibi.customer_service.repository;

import com.aibi.customer_service.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByTenantIdAndId(UUID tenantId, Long id);

    Page<Customer> findByTenantId(UUID tenantId, Pageable pageable);

    boolean existsByTenantIdAndEmailIgnoreCase(UUID tenantId, String email);

    boolean existsByTenantIdAndEmailIgnoreCaseAndIdNot(UUID tenantId, String email, Long id);

    Optional<Customer> findByTenantIdAndEmailIgnoreCase(UUID tenantId, String email);

    @Query("SELECT c FROM Customer c WHERE c.tenantId = :tenantId AND (LOWER(c.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Customer> searchByTenantIdAndName(@Param("tenantId") UUID tenantId, @Param("query") String query, Pageable pageable);

    Page<Customer> findByTenantIdAndEmailContainingIgnoreCase(UUID tenantId, String email, Pageable pageable);

    Page<Customer> findByTenantIdAndCompanyContainingIgnoreCase(UUID tenantId, String company, Pageable pageable);
}
