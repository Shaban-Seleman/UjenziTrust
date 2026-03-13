package com.uzenjitrust.ops.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.uzenjitrust.build.repo.MilestoneRepository;
import com.uzenjitrust.common.error.BadRequestException;
import com.uzenjitrust.common.error.ForbiddenException;
import com.uzenjitrust.common.error.NotFoundException;
import com.uzenjitrust.common.security.ActorPrincipal;
import com.uzenjitrust.common.security.ActorProvider;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.ops.domain.DisbursementOrderEntity;
import com.uzenjitrust.ops.domain.EscrowEntity;
import com.uzenjitrust.ops.domain.OutboxStatus;
import com.uzenjitrust.ops.domain.OutboxEventEntity;
import com.uzenjitrust.ops.domain.WebhookStatus;
import com.uzenjitrust.ops.domain.WebhookEventEntity;
import com.uzenjitrust.ops.repo.DisbursementOrderRepository;
import com.uzenjitrust.ops.repo.EscrowRepository;
import com.uzenjitrust.ops.repo.OutboxEventRepository;
import com.uzenjitrust.ops.repo.WebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class OpsReadService {

    private static final Logger log = LoggerFactory.getLogger(OpsReadService.class);

    private final EscrowRepository escrowRepository;
    private final DisbursementOrderRepository disbursementRepository;
    private final OutboxEventRepository outboxRepository;
    private final WebhookEventRepository webhookRepository;
    private final MilestoneRepository milestoneRepository;
    private final ActorProvider actorProvider;
    private final OperatorAuditService operatorAuditService;

    public OpsReadService(EscrowRepository escrowRepository,
                          DisbursementOrderRepository disbursementRepository,
                          OutboxEventRepository outboxRepository,
                          WebhookEventRepository webhookRepository,
                          MilestoneRepository milestoneRepository,
                          ActorProvider actorProvider,
                          OperatorAuditService operatorAuditService) {
        this.escrowRepository = escrowRepository;
        this.disbursementRepository = disbursementRepository;
        this.outboxRepository = outboxRepository;
        this.webhookRepository = webhookRepository;
        this.milestoneRepository = milestoneRepository;
        this.actorProvider = actorProvider;
        this.operatorAuditService = operatorAuditService;
    }

    @Transactional(readOnly = true)
    public Page<EscrowEntity> listEscrows(int page, int size, String sortBy, Sort.Direction direction) {
        ActorPrincipal actor = actorProvider.requireActor();
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        if (isAdmin(actor)) {
            return escrowRepository.findAll(pageable);
        }
        return escrowRepository.findByPayerUserIdOrBeneficiaryUserId(actor.userId(), actor.userId(), pageable);
    }

    @Transactional(readOnly = true)
    public EscrowEntity getEscrow(UUID escrowId) {
        EscrowEntity escrow = escrowRepository.findById(escrowId)
                .orElseThrow(() -> new NotFoundException("Escrow not found"));
        requireEscrowAccess(escrow);
        return escrow;
    }

    @Transactional(readOnly = true)
    public Page<DisbursementView> listDisbursementsByEscrow(UUID escrowId, int page, int size, String sortBy, Sort.Direction direction) {
        EscrowEntity escrow = escrowRepository.findById(escrowId)
                .orElseThrow(() -> new NotFoundException("Escrow not found"));
        requireEscrowAccess(escrow);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return disbursementRepository.findByEscrow_Id(escrowId, pageable).map(entity -> toDisbursementView(entity, null));
    }

    @Transactional(readOnly = true)
    public Page<DisbursementView> listDisbursementsAdmin(String status,
                                                         String payeeType,
                                                         UUID milestoneId,
                                                         UUID escrowId,
                                                         int page,
                                                         int size,
                                                         String sortBy,
                                                         Sort.Direction direction) {
        requireAdmin();
        Specification<DisbursementOrderEntity> spec = Specification.<DisbursementOrderEntity>where(matchesStringEnum("status", status))
                .and(matchesString("payeeType", payeeType))
                .and(matchesUuid("milestoneId", milestoneId))
                .and(matchesEscrowId(escrowId));
        Page<DisbursementOrderEntity> entities = disbursementRepository.findAll(
                spec,
                PageRequest.of(page, size, Sort.by(direction, sortBy))
        );
        Map<UUID, UUID> projectIds = projectIdsByMilestone(entities.map(DisbursementOrderEntity::getMilestoneId).getContent());
        return entities.map(entity -> toDisbursementView(entity, projectIds.get(entity.getMilestoneId())));
    }

    @Transactional(readOnly = true)
    public DisbursementView getDisbursement(UUID disbursementId) {
        requireAdmin();
        DisbursementOrderEntity entity = disbursementRepository.findById(disbursementId)
                .orElseThrow(() -> new NotFoundException("Disbursement not found"));
        UUID projectId = entity.getMilestoneId() == null ? null : projectIdsByMilestone(java.util.List.of(entity.getMilestoneId())).get(entity.getMilestoneId());
        return toDisbursementView(entity, projectId);
    }

    @Transactional(readOnly = true)
    public Page<OutboxEventView> listOutboxEvents(String status,
                                                  String eventType,
                                                  int page,
                                                  int size,
                                                  String sortBy,
                                                  Sort.Direction direction) {
        requireAdmin();
        Specification<OutboxEventEntity> spec = Specification.<OutboxEventEntity>where(matchesStringEnum("status", status))
                .and(matchesString("eventType", eventType));
        return outboxRepository.findAll(
                spec,
                PageRequest.of(page, size, Sort.by(direction, sortBy))
        ).map(this::toOutboxView);
    }

    @Transactional(readOnly = true)
    public OutboxEventDetailView getOutboxEvent(UUID eventId) {
        requireAdmin();
        OutboxEventEntity entity = outboxRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Outbox event not found"));
        return toOutboxDetailView(entity);
    }

    @Transactional
    public OutboxEventDetailView retryOutboxEvent(UUID eventId) {
        ActorPrincipal actor = actorProvider.requireActor();
        if (!isAdmin(actor)) {
            operatorAuditService.recordForbidden("OUTBOX_EVENT_RETRY", "OUTBOX_EVENT", eventId.toString(), "Outbox retry requires admin", null, "Insufficient role");
            throw new ForbiddenException("Insufficient role");
        }
        try {
            OutboxEventEntity entity = outboxRepository.findById(eventId)
                    .orElseThrow(() -> new NotFoundException("Outbox event not found"));
            if (entity.getStatus() != OutboxStatus.FAILED) {
                operatorAuditService.recordFailure("OUTBOX_EVENT_RETRY", "OUTBOX_EVENT", eventId.toString(), "Outbox retry rejected", java.util.Map.of(
                        "currentStatus", entity.getStatus().name()
                ), "Only failed outbox events can be retried");
                throw new BadRequestException("Only failed outbox events can be retried");
            }
            entity.setStatus(OutboxStatus.PENDING);
            entity.setNextAttemptAt(Instant.now());
            entity.setLastError(null);
            log.info("Operator retried outbox event eventId={} actorUserId={}", entity.getId(), actor.userId());
            operatorAuditService.recordSuccess("OUTBOX_EVENT_RETRY", "OUTBOX_EVENT", eventId.toString(), "Outbox event re-queued", java.util.Map.of(
                    "aggregateType", entity.getAggregateType(),
                    "aggregateId", entity.getAggregateId(),
                    "eventType", entity.getEventType(),
                    "retryCount", entity.getRetryCount()
            ));
            return toOutboxDetailView(entity);
        } catch (RuntimeException ex) {
            if (!(ex instanceof BadRequestException)) {
                operatorAuditService.recordFailure("OUTBOX_EVENT_RETRY", "OUTBOX_EVENT", eventId.toString(), "Outbox retry failed", null, ex.getMessage());
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Page<WebhookEventView> listWebhookEvents(String status,
                                                    String eventType,
                                                    String source,
                                                    int page,
                                                    int size,
                                                    String sortBy,
                                                    Sort.Direction direction) {
        requireAdmin();
        Specification<WebhookEventEntity> spec = Specification.<WebhookEventEntity>where(matchesStringEnum("status", status))
                .and(matchesString("eventType", eventType))
                .and(matchesString("source", source));
        return webhookRepository.findAll(
                spec,
                PageRequest.of(page, size, Sort.by(direction, sortBy))
        ).map(this::toWebhookView);
    }

    @Transactional(readOnly = true)
    public WebhookEventDetailView getWebhookEvent(UUID webhookEventId) {
        requireAdmin();
        WebhookEventEntity entity = webhookRepository.findById(webhookEventId)
                .orElseThrow(() -> new NotFoundException("Webhook event not found"));
        return toWebhookDetailView(entity);
    }

    private void requireEscrowAccess(EscrowEntity escrow) {
        ActorPrincipal actor = actorProvider.requireActor();
        if (isAdmin(actor)) {
            return;
        }
        boolean canAccess = actor.userId().equals(escrow.getPayerUserId())
                || actor.userId().equals(escrow.getBeneficiaryUserId());
        if (!canAccess) {
            throw new ForbiddenException("Escrow access denied");
        }
    }

    private ActorPrincipal requireAdmin() {
        ActorPrincipal actor = actorProvider.requireActor();
        if (!isAdmin(actor)) {
            throw new ForbiddenException("Insufficient role");
        }
        return actor;
    }

    private static boolean isAdmin(ActorPrincipal actor) {
        return actor.roles().contains(AppRole.ADMIN);
    }

    private DisbursementView toDisbursementView(DisbursementOrderEntity entity, UUID projectId) {
        return new DisbursementView(
                entity.getId(),
                entity.getBusinessKey(),
                entity.getEscrow() == null ? null : entity.getEscrow().getId(),
                entity.getMilestoneId(),
                projectId,
                entity.getPayeeType(),
                entity.getPayeeId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getStatus() == null ? null : entity.getStatus().name(),
                entity.getSettlementRef(),
                null,
                entity.getBankReference(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private OutboxEventView toOutboxView(OutboxEventEntity entity) {
        return new OutboxEventView(
                entity.getId(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getEventType(),
                summarizeOutboxPayload(entity.getPayload()),
                entity.getStatus() == null ? null : entity.getStatus().name(),
                entity.getIdempotencyKey(),
                entity.getRetryCount(),
                entity.getNextAttemptAt(),
                entity.getLastError(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private OutboxEventDetailView toOutboxDetailView(OutboxEventEntity entity) {
        OutboxEventView summary = toOutboxView(entity);
        return new OutboxEventDetailView(
                summary.id(),
                summary.aggregateType(),
                summary.aggregateId(),
                summary.eventType(),
                summary.payloadSummary(),
                entity.getPayload(),
                summary.status(),
                summary.idempotencyKey(),
                summary.attempts(),
                summary.nextAttemptAt(),
                summary.lastError(),
                summary.createdAt(),
                summary.updatedAt()
        );
    }

    private WebhookEventView toWebhookView(WebhookEventEntity entity) {
        JsonNode payload = entity.getPayload();
        UUID disbursementId = parseUuid(payload.path("payload").path("disbursementId").asText(null));
        String settlementRef = textOrNull(payload.path("payload").path("settlementRef"));
        UUID escrowId = null;
        UUID milestoneId = null;
        if (disbursementId != null) {
            var relatedDisbursement = disbursementRepository.findById(disbursementId).orElse(null);
            if (relatedDisbursement != null) {
                escrowId = relatedDisbursement.getEscrow() == null ? null : relatedDisbursement.getEscrow().getId();
                milestoneId = relatedDisbursement.getMilestoneId();
            }
        }
        return new WebhookEventView(
                entity.getId(),
                entity.getEventId(),
                entity.getSource(),
                entity.getEventType(),
                entity.getStatus() == null ? null : entity.getStatus().name(),
                disbursementId,
                escrowId,
                milestoneId,
                settlementRef,
                entity.getCreatedAt(),
                entity.getProcessedAt()
        );
    }

    private WebhookEventDetailView toWebhookDetailView(WebhookEventEntity entity) {
        WebhookEventView summary = toWebhookView(entity);
        return new WebhookEventDetailView(
                summary.id(),
                summary.eventId(),
                summary.provider(),
                summary.eventType(),
                summary.status(),
                summary.relatedDisbursementId(),
                summary.relatedEscrowId(),
                summary.relatedMilestoneId(),
                summary.relatedSettlementRef(),
                entity.getEventTs(),
                entity.getCreatedAt(),
                entity.getProcessedAt(),
                entity.getPayload()
        );
    }

    private Map<UUID, UUID> projectIdsByMilestone(Collection<UUID> milestoneIds) {
        Map<UUID, UUID> results = new HashMap<>();
        milestoneRepository.findAllById(milestoneIds.stream().filter(java.util.Objects::nonNull).toList())
                .forEach(milestone -> results.put(milestone.getId(), milestone.getProject() == null ? null : milestone.getProject().getId()));
        return results;
    }

    private String summarizeOutboxPayload(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            return null;
        }
        String disbursementId = textOrNull(payload.path("disbursementId"));
        String amount = textOrNull(payload.path("amount"));
        String currency = textOrNull(payload.path("currency"));
        if (disbursementId != null || amount != null) {
            return "disbursement=" + nullSafe(disbursementId) + ", amount=" + nullSafe(amount) + " " + nullSafe(currency);
        }
        return payload.toString();
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private static String nullSafe(String value) {
        return value == null ? "—" : value;
    }

    private static Specification<DisbursementOrderEntity> matchesEscrowId(UUID escrowId) {
        return (root, query, cb) -> escrowId == null ? null : cb.equal(root.get("escrow").get("id"), escrowId);
    }

    private static <T> Specification<T> matchesString(String field, String value) {
        return (root, query, cb) -> value == null || value.isBlank() ? null : cb.equal(root.get(field), value);
    }

    private static <T> Specification<T> matchesStringEnum(String field, String value) {
        return (root, query, cb) -> value == null || value.isBlank()
                ? null
                : cb.equal(root.get(field).as(String.class), value.toUpperCase());
    }

    private static <T> Specification<T> matchesUuid(String field, UUID value) {
        return (root, query, cb) -> value == null ? null : cb.equal(root.get(field), value);
    }

    public record DisbursementView(
            UUID id,
            String businessKey,
            UUID escrowId,
            UUID milestoneId,
            UUID projectId,
            String payeeType,
            UUID payeeId,
            BigDecimal amount,
            String currency,
            String status,
            String settlementRef,
            String instructionRef,
            String bankReference,
            Instant createdAt,
            Instant updatedAt
    ) { }

    public record OutboxEventView(
            UUID id,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payloadSummary,
            String status,
            String idempotencyKey,
            int attempts,
            Instant nextAttemptAt,
            String lastError,
            Instant createdAt,
            Instant updatedAt
    ) { }

    public record OutboxEventDetailView(
            UUID id,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payloadSummary,
            JsonNode payload,
            String status,
            String idempotencyKey,
            int attempts,
            Instant nextAttemptAt,
            String lastError,
            Instant createdAt,
            Instant updatedAt
    ) { }

    public record WebhookEventView(
            UUID id,
            String eventId,
            String provider,
            String eventType,
            String status,
            UUID relatedDisbursementId,
            UUID relatedEscrowId,
            UUID relatedMilestoneId,
            String relatedSettlementRef,
            Instant receivedAt,
            Instant processedAt
    ) { }

    public record WebhookEventDetailView(
            UUID id,
            String eventId,
            String provider,
            String eventType,
            String status,
            UUID relatedDisbursementId,
            UUID relatedEscrowId,
            UUID relatedMilestoneId,
            String relatedSettlementRef,
            Instant eventTs,
            Instant receivedAt,
            Instant processedAt,
            JsonNode payload
    ) { }
}
