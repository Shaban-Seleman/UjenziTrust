package com.uzenjitrust.build.orchestrator;

import com.uzenjitrust.build.domain.MilestoneEntity;
import com.uzenjitrust.build.domain.MilestoneStatus;
import com.uzenjitrust.build.domain.ProjectEntity;
import com.uzenjitrust.build.repo.MilestoneRepository;
import com.uzenjitrust.common.error.BadRequestException;
import com.uzenjitrust.common.error.NotFoundException;
import com.uzenjitrust.common.security.AuthorizationService;
import com.uzenjitrust.ledger.service.LedgerPostingService;
import com.uzenjitrust.ledger.service.LedgerTemplateService;
import com.uzenjitrust.ops.domain.EscrowEntity;
import com.uzenjitrust.ops.orchestrator.DisbursementOrchestrator;
import com.uzenjitrust.ops.repo.EscrowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MilestoneOrchestrator {

    private final MilestoneRepository milestoneRepository;
    private final EscrowRepository escrowRepository;
    private final AuthorizationService authorizationService;
    private final LedgerTemplateService ledgerTemplateService;
    private final LedgerPostingService ledgerPostingService;
    private final DisbursementOrchestrator disbursementOrchestrator;

    public MilestoneOrchestrator(MilestoneRepository milestoneRepository,
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
    public MilestoneEntity approveMilestoneSingle(java.util.UUID milestoneId, String idempotencyKey) {
        MilestoneEntity milestone = milestoneRepository.findByIdForUpdate(milestoneId)
                .orElseThrow(() -> new NotFoundException("Milestone not found"));
        ProjectEntity project = milestone.getProject();
        var actor = authorizationService.requireOwner(project.getOwnerUserId());

        if (milestone.getStatus() != MilestoneStatus.SUBMITTED && milestone.getStatus() != MilestoneStatus.INSPECTED) {
            throw new BadRequestException("Milestone must be submitted or inspected before approval");
        }

        EscrowEntity escrow = escrowRepository.findById(project.getEscrowId())
                .orElseThrow(() -> new NotFoundException("Project escrow not found"));

        ledgerPostingService.post(ledgerTemplateService.milestoneAuthorizedSingle(
                milestone.getId(),
                actor.userId(),
                milestone.getAmount(),
                milestone.getRetentionAmount(),
                "TZS",
                "MILESTONE_AUTH_SINGLE:" + idempotencyKey
        ));

        java.math.BigDecimal payoutAmount = milestone.getAmount().subtract(milestone.getRetentionAmount());
        disbursementOrchestrator.createDisbursementAndQueuePayout(
                "MILESTONE_SINGLE:" + milestone.getId(),
                "OUTBOX:MILESTONE_SINGLE:" + milestone.getId(),
                escrow,
                milestone.getId(),
                "CONTRACTOR",
                project.getContractorUserId(),
                payoutAmount,
                "TZS"
        );

        milestone.setStatus(MilestoneStatus.APPROVED);
        return milestone;
    }
}
