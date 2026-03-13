package com.uzenjitrust.build.orchestrator;

import com.uzenjitrust.build.domain.MilestoneEntity;
import com.uzenjitrust.build.domain.MilestoneStatus;
import com.uzenjitrust.build.domain.ProjectEntity;
import com.uzenjitrust.build.repo.MilestoneRepository;
import com.uzenjitrust.common.error.BadRequestException;
import com.uzenjitrust.common.error.ForbiddenException;
import com.uzenjitrust.common.error.NotFoundException;
import com.uzenjitrust.common.security.AuthorizationService;
import com.uzenjitrust.ledger.service.LedgerPostingService;
import com.uzenjitrust.ledger.service.LedgerTemplateService;
import com.uzenjitrust.ops.domain.EscrowEntity;
import com.uzenjitrust.ops.service.OperatorAuditService;
import com.uzenjitrust.ops.orchestrator.DisbursementOrchestrator;
import com.uzenjitrust.ops.repo.EscrowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RetentionOrchestrator {

    private final MilestoneRepository milestoneRepository;
    private final EscrowRepository escrowRepository;
    private final AuthorizationService authorizationService;
    private final LedgerTemplateService ledgerTemplateService;
    private final LedgerPostingService ledgerPostingService;
    private final DisbursementOrchestrator disbursementOrchestrator;
    private final OperatorAuditService operatorAuditService;

    public RetentionOrchestrator(MilestoneRepository milestoneRepository,
                                 EscrowRepository escrowRepository,
                                 AuthorizationService authorizationService,
                                 LedgerTemplateService ledgerTemplateService,
                                 LedgerPostingService ledgerPostingService,
                                 DisbursementOrchestrator disbursementOrchestrator,
                                 OperatorAuditService operatorAuditService) {
        this.milestoneRepository = milestoneRepository;
        this.escrowRepository = escrowRepository;
        this.authorizationService = authorizationService;
        this.ledgerTemplateService = ledgerTemplateService;
        this.ledgerPostingService = ledgerPostingService;
        this.disbursementOrchestrator = disbursementOrchestrator;
        this.operatorAuditService = operatorAuditService;
    }

    @Transactional
    public MilestoneEntity releaseRetention(UUID milestoneId, String idempotencyKey) {
        try {
            MilestoneEntity milestone = milestoneRepository.findByIdForUpdate(milestoneId)
                    .orElseThrow(() -> new NotFoundException("Milestone not found"));
            ProjectEntity project = milestone.getProject();
            var actor = authorizationService.requireOwner(project.getOwnerUserId());

            if (!isEligibleForRetentionRelease(milestone, Instant.now())) {
                throw new BadRequestException("Milestone retention is not eligible for release");
            }

            MilestoneEntity released = releaseLockedMilestoneRetention(milestone, actor.userId(), Instant.now());
            operatorAuditService.recordSuccess(
                    "MILESTONE_RETENTION_RELEASE",
                    "MILESTONE",
                    milestoneId.toString(),
                    "Retention release triggered",
                    java.util.Map.of(
                            "projectId", project.getId(),
                            "retentionAmount", milestone.getRetentionAmount(),
                            "retentionReleasedAt", released.getRetentionReleasedAt(),
                            "outboxKey", retentionOutboxKey(milestoneId)
                    )
            );
            return released;
        } catch (ForbiddenException ex) {
            operatorAuditService.recordForbidden("MILESTONE_RETENTION_RELEASE", "MILESTONE", milestoneId.toString(), "Retention release forbidden", null, ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            operatorAuditService.recordFailure("MILESTONE_RETENTION_RELEASE", "MILESTONE", milestoneId.toString(), "Retention release failed", null, ex.getMessage());
            throw ex;
        }
    }

    @Transactional
    public int releaseDueRetentionsSystem() {
        List<UUID> dueMilestoneIds = milestoneRepository.findIdsEligibleForRetentionRelease(
                MilestoneStatus.PAID,
                Instant.now()
        );

        int released = 0;
        for (UUID milestoneId : dueMilestoneIds) {
            MilestoneEntity milestone = milestoneRepository.findByIdForUpdate(milestoneId).orElse(null);
            if (milestone == null || !isEligibleForRetentionRelease(milestone, Instant.now())) {
                continue;
            }

            releaseLockedMilestoneRetention(milestone, milestone.getProject().getOwnerUserId(), Instant.now());
            released++;
        }
        return released;
    }

    private MilestoneEntity releaseLockedMilestoneRetention(MilestoneEntity milestone,
                                                            UUID actorUserId,
                                                            Instant now) {
        ProjectEntity project = milestone.getProject();
        EscrowEntity escrow = escrowRepository.findById(project.getEscrowId())
                .orElseThrow(() -> new NotFoundException("Project escrow not found"));

        ledgerPostingService.post(ledgerTemplateService.retentionReleaseAuthorized(
                milestone.getId(),
                actorUserId,
                milestone.getRetentionAmount(),
                "TZS",
                retentionLedgerIdempotencyKey(milestone.getId())
        ));

        disbursementOrchestrator.createDisbursementAndQueuePayout(
                retentionBusinessKey(milestone.getId()),
                retentionOutboxKey(milestone.getId()),
                escrow,
                milestone.getId(),
                "RETENTION",
                project.getContractorUserId(),
                milestone.getRetentionAmount(),
                "TZS"
        );

        milestone.setStatus(MilestoneStatus.RETENTION_RELEASED);
        milestone.setRetentionReleasedAt(now);
        return milestone;
    }

    private boolean isEligibleForRetentionRelease(MilestoneEntity milestone, Instant now) {
        return milestone.getStatus() == MilestoneStatus.PAID
                && milestone.getRetentionAmount() != null
                && milestone.getRetentionAmount().compareTo(java.math.BigDecimal.ZERO) > 0
                && milestone.getRetentionReleaseAt() != null
                && !milestone.getRetentionReleaseAt().isAfter(now)
                && milestone.getRetentionReleasedAt() == null;
    }

    private String retentionBusinessKey(UUID milestoneId) {
        return "RETENTION:" + milestoneId;
    }

    private String retentionOutboxKey(UUID milestoneId) {
        return "OUTBOX:RETENTION:" + milestoneId;
    }

    private String retentionLedgerIdempotencyKey(UUID milestoneId) {
        return "RETREL:" + milestoneId;
    }
}
