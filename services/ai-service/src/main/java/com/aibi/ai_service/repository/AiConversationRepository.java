package com.aibi.ai_service.repository;

import com.aibi.ai_service.entity.AiConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiConversationRepository extends JpaRepository<AiConversation, UUID> {

    Optional<AiConversation> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<AiConversation> findByIdAndTenantIdAndUserId(UUID id, UUID tenantId, Long userId);

    Page<AiConversation> findByTenantIdAndUserIdOrderByUpdatedAtDesc(UUID tenantId, Long userId, Pageable pageable);

    Page<AiConversation> findByTenantIdOrderByUpdatedAtDesc(UUID tenantId, Pageable pageable);

    void deleteByIdAndTenantId(UUID id, UUID tenantId);

    void deleteByIdAndTenantIdAndUserId(UUID id, UUID tenantId, Long userId);
}
