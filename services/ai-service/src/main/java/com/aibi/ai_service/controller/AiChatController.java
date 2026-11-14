package com.aibi.ai_service.controller;

import com.aibi.ai_service.dto.ChatRequest;
import com.aibi.ai_service.dto.ChatResponse;
import com.aibi.ai_service.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@PreAuthorize("hasAnyRole('USER', 'ADMIN', 'OWNER', 'MANAGER', 'SELLER', 'VIEWER')")
@Tag(name = "AI Chat", description = "Endpoints de dialogue avec l'assistant commercial IA (Copilot)")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Envoyer un message à l'assistant IA", description = "Transmet une question en langage naturel à l'IA, interroge les outils métiers si nécessaire et persiste l'échange dans la conversation active")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réponse générée avec succès"),
            @ApiResponse(responseCode = "400", description = "Message vide ou invalide"),
            @ApiResponse(responseCode = "403", description = "Accès refusé ou absence de contexte locataire vérifié"),
            @ApiResponse(responseCode = "404", description = "Conversation introuvable pour ce locataire"),
            @ApiResponse(responseCode = "429", description = "Limite de requêtes atteinte"),
            @ApiResponse(responseCode = "502", description = "Erreur de communication avec le fournisseur LLM")
    })
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = aiChatService.processChat(request);
        return ResponseEntity.ok(response);
    }
}
