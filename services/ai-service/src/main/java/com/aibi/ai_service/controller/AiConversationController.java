package com.aibi.ai_service.controller;

import com.aibi.ai_service.dto.ConversationDetailResponse;
import com.aibi.ai_service.dto.ConversationResponse;
import com.aibi.ai_service.service.AiConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai/conversations")
@PreAuthorize("hasAnyRole('USER', 'ADMIN', 'OWNER', 'MANAGER', 'SELLER', 'VIEWER')")
@Tag(name = "AI Conversations", description = "Gestion de l'historique et du cycle de vie des conversations IA")
public class AiConversationController {

    private final AiConversationService conversationService;

    public AiConversationController(AiConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    @Operation(summary = "Lister les conversations de l'utilisateur", description = "Retourne la liste paginée des conversations IA de l'utilisateur au sein du locataire actif")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des conversations récupérée avec succès"),
            @ApiResponse(responseCode = "403", description = "Contexte locataire manquant ou invalide")
    })
    public ResponseEntity<Page<ConversationResponse>> getConversations(
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(conversationService.getConversations(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter le détail d'une conversation", description = "Retourne l'ensemble des échanges d'une conversation appartenant au locataire et à l'utilisateur")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversation trouvée"),
            @ApiResponse(responseCode = "404", description = "Conversation introuvable ou n'appartenant pas à ce locataire")
    })
    public ResponseEntity<ConversationDetailResponse> getConversation(
            @Parameter(description = "Identifiant UUID de la conversation") @PathVariable UUID id) {
        return ResponseEntity.ok(conversationService.getConversation(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une conversation", description = "Supprime définitivement une conversation et l'historique des messages associés")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Conversation supprimée avec succès"),
            @ApiResponse(responseCode = "404", description = "Conversation introuvable ou non autorisée")
    })
    public ResponseEntity<Void> deleteConversation(
            @Parameter(description = "Identifiant UUID de la conversation") @PathVariable UUID id) {
        conversationService.deleteConversation(id);
        return ResponseEntity.noContent().build();
    }
}
