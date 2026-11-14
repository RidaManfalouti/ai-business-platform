package com.aibi.ai_service.repository;

import com.aibi.ai_service.entity.AiMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiMessageRepository extends JpaRepository<AiMessage, UUID> {

    List<AiMessage> findByConversationIdAndTenantIdOrderByCreatedAtAsc(UUID conversationId, UUID tenantId);

    Page<AiMessage> findByConversationIdAndTenantIdOrderByCreatedAtDesc(UUID conversationId, UUID tenantId, Pageable pageable);

    void deleteByConversationIdAndTenantId(UUID conversationId, UUID tenantId);
}
