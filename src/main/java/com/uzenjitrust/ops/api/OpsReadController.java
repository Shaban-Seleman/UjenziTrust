package com.uzenjitrust.ops.api;

import com.uzenjitrust.ops.domain.EscrowEntity;
import com.uzenjitrust.ops.domain.OutboxEventEntity;
import com.uzenjitrust.ops.domain.WebhookEventEntity;
import com.uzenjitrust.ops.service.OpsReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/ops")
@Tag(name = "Ops Read")
public class OpsReadController {

    private final OpsReadService opsReadService;

    public OpsReadController(OpsReadService opsReadService) {
        this.opsReadService = opsReadService;
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

    @GetMapping("/outbox")
    @Operation(summary = "List outbox events (admin)")
    public ResponseEntity<Page<OutboxEventEntity>> listOutbox(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        return ResponseEntity.ok(opsReadService.listOutboxEvents(page, size, sortBy, direction));
    }

    @GetMapping("/webhooks/events")
    @Operation(summary = "List webhook events (admin)")
    public ResponseEntity<Page<WebhookEventEntity>> listWebhookEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        return ResponseEntity.ok(opsReadService.listWebhookEvents(page, size, sortBy, direction));
    }
}
