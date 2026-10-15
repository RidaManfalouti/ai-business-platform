package com.aibi.customer_service.repository;

import com.aibi.customer_service.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    Optional<Customer> findByEmailIgnoreCase(String email);

    @Query("SELECT c FROM Customer c WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Customer> searchByName(@Param("query") String query, Pageable pageable);

    Page<Customer> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    Page<Customer> findByCompanyContainingIgnoreCase(String company, Pageable pageable);
}
