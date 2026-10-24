package com.aibi.sales_service.controller;

import com.aibi.sales_service.reconciliation.dto.ReconciliationReport;
import com.aibi.sales_service.reconciliation.service.StockReconciliationService;
import com.aibi.sales_service.saga.dto.SagaDetailsResponse;
import com.aibi.sales_service.saga.entity.Saga;
import com.aibi.sales_service.saga.entity.SagaEvent;
import com.aibi.sales_service.saga.model.SagaStatus;
import com.aibi.sales_service.saga.service.SagaRecoveryWorker;
import com.aibi.sales_service.saga.service.SagaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Saga & Reconciliation Admin", description = "Endpoints opérationnels d'administration des Sagas et de réconciliation de stock (ROLE_ADMIN)")
public class SagaAdminController {

    private final SagaService sagaService;
    private final SagaRecoveryWorker recoveryWorker;
    private final StockReconciliationService reconciliationService;

    public SagaAdminController(SagaService sagaService,
                               SagaRecoveryWorker recoveryWorker,
                               StockReconciliationService reconciliationService) {
        this.sagaService = sagaService;
        this.recoveryWorker = recoveryWorker;
        this.reconciliationService = reconciliationService;
    }

    @GetMapping("/sagas")
    @Operation(summary = "Lister les orchestrations Saga", description = "Retourne la liste des Sagas, avec filtre optionnel par statut (ex: FAILED_REQUIRES_ATTENTION)")
    public ResponseEntity<List<Saga>> getSagas(
            @Parameter(description = "Filtrer par statut de Saga", example = "FAILED_REQUIRES_ATTENTION")
            @RequestParam(required = false) SagaStatus status) {
        return ResponseEntity.ok(sagaService.getAllSagas(status));
    }

    @GetMapping("/sagas/{sagaId}")
    @Operation(summary = "Consulter le détail d'une Saga et son journal d'événements")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saga trouvée"),
            @ApiResponse(responseCode = "404", description = "Saga introuvable")
    })
    public ResponseEntity<SagaDetailsResponse> getSagaDetails(@PathVariable String sagaId) {
        Saga saga = sagaService.getSagaById(sagaId)
                .orElseThrow(() -> new IllegalArgumentException("Saga introuvable : " + sagaId));
        List<SagaEvent> events = sagaService.getSagaEvents(sagaId);
        return ResponseEntity.ok(SagaDetailsResponse.from(saga, events));
    }

    @PostMapping("/sagas/{sagaId}/retry")
    @Operation(summary = "Déclencher un réessai manuel de compensation d'une Saga",
            description = "Réinitialise le statut à COMPENSATION_PENDING et déclenche immédiatement le worker de reprise")
    public ResponseEntity<Saga> retrySaga(@PathVariable String sagaId) {
        Saga updated = sagaService.manualRetry(sagaId);
        recoveryWorker.recoverPendingSagas();
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/sagas/{sagaId}/resolve")
    @Operation(summary = "Résoudre manuellement une Saga en anomalie",
            description = "Force le statut de la Saga à COMPENSATED après vérification opérationnelle manuelle")
    public ResponseEntity<Saga> resolveSaga(
            @PathVariable String sagaId,
            @RequestBody(required = false) Map<String, String> body) {
        String note = (body != null && body.containsKey("note")) ? body.get("note") : "Résolution administrative";
        Saga updated = sagaService.manualResolve(sagaId, note);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/reconciliation/run")
    @Operation(summary = "Déclencher un audit de réconciliation de stock à la demande",
            description = "Compare les ventes locales, les Sagas et les réservations Product Service sans jamais altérer le stock en base")
    public ResponseEntity<ReconciliationReport> runReconciliation(
            @Parameter(description = "Seuil d'ancienneté en minutes", example = "15")
            @RequestParam(defaultValue = "15") int staleMinutes) {
        ReconciliationReport report = reconciliationService.runReconciliation(Duration.ofMinutes(staleMinutes));
        return ResponseEntity.ok(report);
    }

    @GetMapping("/reconciliation/latest")
    @Operation(summary = "Consulter le dernier rapport d'audit de réconciliation")
    public ResponseEntity<ReconciliationReport> getLatestReconciliationReport() {
        ReconciliationReport report = reconciliationService.getLatestReport();
        if (report == null) {
            report = reconciliationService.runReconciliation(Duration.ofMinutes(15));
        }
        return ResponseEntity.ok(report);
    }
}
