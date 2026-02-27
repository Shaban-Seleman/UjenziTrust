package com.uzenjitrust.ops.service;

import com.uzenjitrust.common.error.ForbiddenException;
import com.uzenjitrust.common.error.NotFoundException;
import com.uzenjitrust.common.security.ActorPrincipal;
import com.uzenjitrust.common.security.ActorProvider;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.ops.domain.DisbursementOrderEntity;
import com.uzenjitrust.ops.domain.EscrowEntity;
import com.uzenjitrust.ops.domain.OutboxEventEntity;
import com.uzenjitrust.ops.domain.WebhookEventEntity;
import com.uzenjitrust.ops.repo.DisbursementOrderRepository;
import com.uzenjitrust.ops.repo.EscrowRepository;
import com.uzenjitrust.ops.repo.OutboxEventRepository;
import com.uzenjitrust.ops.repo.WebhookEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OpsReadService {

    private final EscrowRepository escrowRepository;
    private final DisbursementOrderRepository disbursementRepository;
    private final OutboxEventRepository outboxRepository;
    private final WebhookEventRepository webhookRepository;
    private final ActorProvider actorProvider;

    public OpsReadService(EscrowRepository escrowRepository,
                          DisbursementOrderRepository disbursementRepository,
                          OutboxEventRepository outboxRepository,
                          WebhookEventRepository webhookRepository,
                          ActorProvider actorProvider) {
        this.escrowRepository = escrowRepository;
        this.disbursementRepository = disbursementRepository;
        this.outboxRepository = outboxRepository;
        this.webhookRepository = webhookRepository;
        this.actorProvider = actorProvider;
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
        return disbursementRepository.findByEscrow_Id(escrowId, pageable).map(DisbursementView::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<OutboxEventEntity> listOutboxEvents(int page, int size, String sortBy, Sort.Direction direction) {
        requireAdmin();
        return outboxRepository.findAll(PageRequest.of(page, size, Sort.by(direction, sortBy)));
    }

    @Transactional(readOnly = true)
    public Page<WebhookEventEntity> listWebhookEvents(int page, int size, String sortBy, Sort.Direction direction) {
        requireAdmin();
        return webhookRepository.findAll(PageRequest.of(page, size, Sort.by(direction, sortBy)));
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

    private void requireAdmin() {
        ActorPrincipal actor = actorProvider.requireActor();
        if (!isAdmin(actor)) {
            throw new ForbiddenException("Insufficient role");
        }
    }

    private static boolean isAdmin(ActorPrincipal actor) {
        return actor.roles().contains(AppRole.ADMIN);
    }

    public record DisbursementView(
            UUID id,
            String businessKey,
            UUID escrowId,
            UUID milestoneId,
            String payeeType,
            UUID payeeId,
            java.math.BigDecimal amount,
            String currency,
            String status,
            String settlementRef,
            String instructionRef,
            String bankReference,
            Instant createdAt
    ) {
        public static DisbursementView fromEntity(DisbursementOrderEntity entity) {
            return new DisbursementView(
                    entity.getId(),
                    entity.getBusinessKey(),
                    entity.getEscrow() == null ? null : entity.getEscrow().getId(),
                    entity.getMilestoneId(),
                    entity.getPayeeType(),
                    entity.getPayeeId(),
                    entity.getAmount(),
                    entity.getCurrency(),
                    entity.getStatus() == null ? null : entity.getStatus().name(),
                    entity.getSettlementRef(),
                    null,
                    entity.getBankReference(),
                    null
            );
        }
    }
}
