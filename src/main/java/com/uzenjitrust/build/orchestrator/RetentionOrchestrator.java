package com.uzenjitrust.build.orchestrator;

import com.uzenjitrust.build.domain.MilestoneEntity;
import com.uzenjitrust.build.domain.MilestoneStatus;
import com.uzenjitrust.build.domain.ProjectEntity;
import com.uzenjitrust.build.repo.MilestoneRepository;
import com.uzenjitrust.common.security.AuthorizationService;
import com.uzenjitrust.ledger.service.LedgerPostingService;
import com.uzenjitrust.ledger.service.LedgerTemplateService;
import com.uzenjitrust.ops.domain.EscrowEntity;
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

    public RetentionOrchestrator(MilestoneRepository milestoneRepository,
                                 EscrowRepository escrowRepository,
                                 AuthorizationService authorizationService,
                                 LedgerTemplateService ledgerTemplateService,
                                 LedgerPostingService ledgerPostingService,
                                 DisbursementOrchestrator disbursementOrchestrator) {
        this.milestoneRepository = milestoneRepository;
        this.escrowRepository = escrowRepository;
        this.authorizationService = authorizationService;
        this.ledgerTemplateService = ledgerTemplateService;
        this.ledgerPostingService = ledgerPostingService;
        this.disbursementOrchestrator = disbursementOrchestrator;
    }

    @Transactional
    public MilestoneEntity releaseRetention(UUID milestoneId, String idempotencyKey) {
        MilestoneEntity milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new com.uzenjitrust.common.error.NotFoundException("Milestone not found"));
        ProjectEntity project = milestone.getProject();
        var actor = authorizationService.requireOwner(project.getOwnerUserId());

        if (milestone.getRetentionAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return milestone;
        }

        EscrowEntity escrow = escrowRepository.findById(project.getEscrowId())
                .orElseThrow(() -> new com.uzenjitrust.common.error.NotFoundException("Project escrow not found"));

        ledgerPostingService.post(ledgerTemplateService.retentionReleaseAuthorized(
                milestone.getId(),
                actor.userId(),
                milestone.getRetentionAmount(),
                "TZS",
                "RETENTION_RELEASE_AUTH:" + idempotencyKey
        ));

        disbursementOrchestrator.createDisbursementAndQueuePayout(
                "RETENTION:" + milestone.getId(),
                "OUTBOX:RETENTION:" + milestone.getId(),
                escrow,
                milestone.getId(),
                "RETENTION",
                project.getContractorUserId(),
                milestone.getRetentionAmount(),
                "TZS"
        );

        milestone.setStatus(MilestoneStatus.RETENTION_RELEASED);
        milestone.setRetentionReleaseAt(null);
        return milestone;
    }

    @Transactional
    public int releaseDueRetentionsSystem() {
        List<MilestoneEntity> due = milestoneRepository.findByStatusAndRetentionReleaseAtLessThanEqual(
                MilestoneStatus.PAID,
                Instant.now()
        );
        for (MilestoneEntity milestone : due) {
            ProjectEntity project = milestone.getProject();
            EscrowEntity escrow = escrowRepository.findById(project.getEscrowId()).orElse(null);
            if (escrow == null || milestone.getRetentionAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                continue;
            }

            ledgerPostingService.post(ledgerTemplateService.retentionReleaseAuthorized(
                    milestone.getId(),
                    project.getOwnerUserId(),
                    milestone.getRetentionAmount(),
                    "TZS",
                    "RETENTION_RELEASE_JOB:" + milestone.getId()
            ));

            disbursementOrchestrator.createDisbursementAndQueuePayout(
                    "RETENTION:" + milestone.getId(),
                    "OUTBOX:RETENTION:" + milestone.getId(),
                    escrow,
                    milestone.getId(),
                    "RETENTION",
                    project.getContractorUserId(),
                    milestone.getRetentionAmount(),
                    "TZS"
            );

            milestone.setStatus(MilestoneStatus.RETENTION_RELEASED);
            milestone.setRetentionReleaseAt(null);
        }
        return due.size();
    }
}
