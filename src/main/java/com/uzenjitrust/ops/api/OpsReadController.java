package com.uzenjitrust.ops.api;

import com.uzenjitrust.ops.domain.EscrowEntity;
import com.uzenjitrust.ops.service.OpsReadService;
import com.uzenjitrust.ops.service.OperatorAuditService;
import com.uzenjitrust.ops.service.SystemStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/ops")
@Tag(name = "Ops Read")
public class OpsReadController {

    private final OpsReadService opsReadService;
    private final SystemStatusService systemStatusService;
    private final OperatorAuditService operatorAuditService;

    public OpsReadController(OpsReadService opsReadService,
                             SystemStatusService systemStatusService,
                             OperatorAuditService operatorAuditService) {
        this.opsReadService = opsReadService;
        this.systemStatusService = systemStatusService;
        this.operatorAuditService = operatorAuditService;
    }

    @GetMapping("/escrows")
    @Operation(summary = "List escrows visible to current actor")
    public ResponseEntity<Page<EscrowEntity>> listEscrows(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        return ResponseEntity.ok(opsReadService.listEscrows(page, size, sortBy, direction));
    }

    @GetMapping("/escrows/{escrowId}")
    @Operation(summary = "Get escrow visible to current actor")
    public ResponseEntity<EscrowEntity> getEscrow(@PathVariable UUID escrowId) {
        return ResponseEntity.ok(opsReadService.getEscrow(escrowId));
    }

    @GetMapping("/escrows/{escrowId}/disbursements")
    @Operation(summary = "List disbursements by escrow")
    public ResponseEntity<Page<OpsReadService.DisbursementView>> listDisbursements(
            @PathVariable UUID escrowId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        return ResponseEntity.ok(opsReadService.listDisbursementsByEscrow(escrowId, page, size, sortBy, direction));
    }

    @GetMapping("/disbursements")
    @Operation(summary = "List disbursements (admin)")
    public ResponseEntity<Page<OpsReadService.DisbursementView>> listAdminDisbursements(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String payeeType,
            @RequestParam(required = false) UUID milestoneId,
            @RequestParam(required = false) UUID escrowId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        return ResponseEntity.ok(opsReadService.listDisbursementsAdmin(status, payeeType, milestoneId, escrowId, page, size, sortBy, direction));
    }

    @GetMapping("/disbursements/{disbursementId}")
    @Operation(summary = "Get disbursement detail (admin)")
    public ResponseEntity<OpsReadService.DisbursementView> getDisbursement(@PathVariable UUID disbursementId) {
        return ResponseEntity.ok(opsReadService.getDisbursement(disbursementId));
    }

    @GetMapping("/outbox")
    @Operation(summary = "List outbox events (admin)")
    public ResponseEntity<Page<OpsReadService.OutboxEventView>> listOutbox(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        return ResponseEntity.ok(opsReadService.listOutboxEvents(status, eventType, page, size, sortBy, direction));
    }

    @GetMapping("/outbox/{eventId}")
    @Operation(summary = "Get outbox event detail (admin)")
    public ResponseEntity<OpsReadService.OutboxEventDetailView> getOutboxEvent(@PathVariable UUID eventId) {
        return ResponseEntity.ok(opsReadService.getOutboxEvent(eventId));
    }

    @PostMapping("/outbox/{eventId}/retry")
    @Operation(summary = "Retry failed outbox event (admin)")
    public ResponseEntity<OpsReadService.OutboxEventDetailView> retryOutboxEvent(@PathVariable UUID eventId) {
        return ResponseEntity.ok(opsReadService.retryOutboxEvent(eventId));
    }

    @GetMapping("/webhooks/events")
    @Operation(summary = "List webhook events (admin)")
    public ResponseEntity<Page<OpsReadService.WebhookEventView>> listWebhookEvents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        return ResponseEntity.ok(opsReadService.listWebhookEvents(status, eventType, source, page, size, sortBy, direction));
    }

    @GetMapping("/webhooks/events/{eventId}")
    @Operation(summary = "Get webhook event detail (admin)")
    public ResponseEntity<OpsReadService.WebhookEventDetailView> getWebhookEvent(@PathVariable UUID eventId) {
        return ResponseEntity.ok(opsReadService.getWebhookEvent(eventId));
    }

    @GetMapping("/system-health")
    @Operation(summary = "Get system health and scheduler status (admin)")
    public ResponseEntity<SystemStatusService.SystemHealthView> getSystemHealth() {
        return ResponseEntity.ok(systemStatusService.currentStatus());
    }

    @GetMapping("/operator-audit-events")
    @Operation(summary = "List operator audit events (admin)")
    public ResponseEntity<Page<OperatorAuditService.OperatorAuditView>> listOperatorAuditEvents(
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        return ResponseEntity.ok(operatorAuditService.list(actionType, outcome, actorUserId, resourceType, page, size, sortBy, direction));
    }

    @GetMapping("/operator-audit-events/{auditEventId}")
    @Operation(summary = "Get operator audit event detail (admin)")
    public ResponseEntity<OperatorAuditService.OperatorAuditDetailView> getOperatorAuditEvent(@PathVariable UUID auditEventId) {
        return ResponseEntity.ok(operatorAuditService.get(auditEventId));
    }
}
