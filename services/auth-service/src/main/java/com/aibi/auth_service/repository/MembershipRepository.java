package com.aibi.auth_service.repository;

import com.aibi.auth_service.entity.Membership;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    @EntityGraph(attributePaths = {"tenant"})
    List<Membership> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"tenant"})
    List<Membership> findByUserIdAndStatus(Long userId, Membership.MembershipStatus status);

    @EntityGraph(attributePaths = {"tenant"})
    Optional<Membership> findByTenantIdAndUserId(UUID tenantId, Long userId);

    @EntityGraph(attributePaths = {"tenant"})
    Optional<Membership> findByTenantIdAndUserIdAndStatus(UUID tenantId, Long userId, Membership.MembershipStatus status);

    @EntityGraph(attributePaths = {"user"})
    List<Membership> findByTenantId(UUID tenantId);

    boolean existsByTenantIdAndUserId(UUID tenantId, Long userId);
}
