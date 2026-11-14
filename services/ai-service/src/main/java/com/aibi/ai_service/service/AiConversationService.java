package com.aibi.ai_service.service;

import com.aibi.ai_service.dto.ConversationDetailResponse;
import com.aibi.ai_service.dto.ConversationResponse;
import com.aibi.ai_service.dto.MessageResponse;
import com.aibi.ai_service.entity.AiConversation;
import com.aibi.ai_service.entity.AiMessage;
import com.aibi.ai_service.exception.ResourceNotFoundException;
import com.aibi.ai_service.repository.AiConversationRepository;
import com.aibi.ai_service.repository.AiMessageRepository;
import com.aibi.ai_service.security.SecurityUtils;
import com.aibi.ai_service.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AiConversationService {

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;

    public AiConversationService(AiConversationRepository conversationRepository, AiMessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional(readOnly = true)
    public Page<ConversationResponse> getConversations(Pageable pageable) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Long userId = SecurityUtils.getRequiredUserId();

        return conversationRepository.findByTenantIdAndUserIdOrderByUpdatedAtDesc(tenantId, userId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ConversationDetailResponse getConversation(UUID conversationId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Long userId = SecurityUtils.getRequiredUserId();

        AiConversation conversation = conversationRepository.findByIdAndTenantIdAndUserId(conversationId, tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation introuvable ou non autorisée: " + conversationId));

        List<MessageResponse> messages = messageRepository.findByConversationIdAndTenantIdOrderByCreatedAtAsc(conversationId, tenantId)
                .stream()
                .filter(m -> m.getRole() == com.aibi.ai_service.entity.MessageRole.USER || m.getRole() == com.aibi.ai_service.entity.MessageRole.ASSISTANT)
                .map(this::toMessageResponse)
                .toList();

        return new ConversationDetailResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messages
        );
    }

    @Transactional
    public void deleteConversation(UUID conversationId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Long userId = SecurityUtils.getRequiredUserId();

        AiConversation conversation = conversationRepository.findByIdAndTenantIdAndUserId(conversationId, tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation introuvable ou non autorisée: " + conversationId));

        conversationRepository.delete(conversation);
    }

    @Transactional
    public AiConversation findOrCreateConversation(UUID conversationId, String initialTitle) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Long userId = SecurityUtils.getRequiredUserId();

        if (conversationId != null) {
            return conversationRepository.findByIdAndTenantIdAndUserId(conversationId, tenantId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation introuvable ou non autorisée: " + conversationId));
        }

        String title = initialTitle != null && !initialTitle.isBlank()
                ? (initialTitle.length() > 60 ? initialTitle.substring(0, 57) + "..." : initialTitle)
                : "Nouvelle conversation";

        AiConversation conversation = AiConversation.builder()
                .tenantId(tenantId)
                .userId(userId)
                .title(title)
                .build();

        return conversationRepository.save(conversation);
    }

    private ConversationResponse toResponse(AiConversation conv) {
        return new ConversationResponse(
                conv.getId(),
                conv.getTitle(),
                conv.getCreatedAt(),
                conv.getUpdatedAt()
        );
    }

    private MessageResponse toMessageResponse(AiMessage msg) {
        return new MessageResponse(
                msg.getId(),
                msg.getRole().name().toLowerCase(),
                msg.getContent(),
                msg.getCreatedAt()
        );
    }
}
