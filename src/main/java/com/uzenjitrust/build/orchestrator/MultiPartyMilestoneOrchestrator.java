package com.uzenjitrust.build.orchestrator;

import com.uzenjitrust.build.api.ApproveMilestoneMultiRequest;
import com.uzenjitrust.build.domain.MilestoneEntity;
import com.uzenjitrust.build.domain.MilestonePayoutSplitEntity;
import com.uzenjitrust.build.domain.MilestoneStatus;
import com.uzenjitrust.build.domain.ProjectEntity;
import com.uzenjitrust.build.repo.MilestonePayoutSplitRepository;
import com.uzenjitrust.build.repo.MilestoneRepository;
import com.uzenjitrust.common.error.BadRequestException;
import com.uzenjitrust.common.error.ForbiddenException;
import com.uzenjitrust.common.error.NotFoundException;
import com.uzenjitrust.common.security.AuthorizationService;
import com.uzenjitrust.ledger.service.LedgerAccountCodes;
import com.uzenjitrust.ledger.service.LedgerPostingService;
import com.uzenjitrust.ledger.service.LedgerTemplateService;
import com.uzenjitrust.ops.domain.EscrowEntity;
import com.uzenjitrust.ops.service.OperatorAuditService;
import com.uzenjitrust.ops.orchestrator.DisbursementOrchestrator;
import com.uzenjitrust.ops.repo.EscrowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class MultiPartyMilestoneOrchestrator {

    private final MilestoneRepository milestoneRepository;
    private final MilestonePayoutSplitRepository splitRepository;
    private final EscrowRepository escrowRepository;
    private final AuthorizationService authorizationService;
    private final LedgerTemplateService ledgerTemplateService;
    private final LedgerPostingService ledgerPostingService;
    private final DisbursementOrchestrator disbursementOrchestrator;
    private final OperatorAuditService operatorAuditService;

    public MultiPartyMilestoneOrchestrator(MilestoneRepository milestoneRepository,
                                           MilestonePayoutSplitRepository splitRepository,
                                           EscrowRepository escrowRepository,
                                           AuthorizationService authorizationService,
                                           LedgerTemplateService ledgerTemplateService,
                                           LedgerPostingService ledgerPostingService,
                                           DisbursementOrchestrator disbursementOrchestrator,
                                           OperatorAuditService operatorAuditService) {
        this.milestoneRepository = milestoneRepository;
        this.splitRepository = splitRepository;
        this.escrowRepository = escrowRepository;
        this.authorizationService = authorizationService;
        this.ledgerTemplateService = ledgerTemplateService;
        this.ledgerPostingService = ledgerPostingService;
        this.disbursementOrchestrator = disbursementOrchestrator;
        this.operatorAuditService = operatorAuditService;
    }

    @Transactional
    public MilestoneEntity approveMilestoneMulti(java.util.UUID milestoneId, ApproveMilestoneMultiRequest request) {
        try {
            MilestoneEntity milestone = milestoneRepository.findByIdForUpdate(milestoneId)
                    .orElseThrow(() -> new NotFoundException("Milestone not found"));
            ProjectEntity project = milestone.getProject();
            var actor = authorizationService.requireOwner(project.getOwnerUserId());

            if (milestone.getStatus() != MilestoneStatus.SUBMITTED && milestone.getStatus() != MilestoneStatus.INSPECTED) {
                throw new BadRequestException("Milestone must be submitted or inspected before approval");
            }

            EscrowEntity escrow = escrowRepository.findById(project.getEscrowId())
                    .orElseThrow(() -> new NotFoundException("Project escrow not found"));

            List<LedgerTemplateService.PayableAllocation> allocations = new ArrayList<>();
            for (ApproveMilestoneMultiRequest.Split split : request.splits()) {
                String payableAccountCode = payableAccountFor(split.payeeType());
                MilestonePayoutSplitEntity splitEntity = splitRepository.findByBusinessKey(split.businessKey()).orElseGet(() -> {
                    MilestonePayoutSplitEntity entity = new MilestonePayoutSplitEntity();
                    entity.setMilestone(milestone);
                    entity.setPayeeType(split.payeeType());
                    entity.setPayeeId(split.payeeId());
                    entity.setAmount(split.amount());
                    entity.setBusinessKey(split.businessKey());
                    return splitRepository.save(entity);
                });

                allocations.add(new LedgerTemplateService.PayableAllocation(payableAccountCode, splitEntity.getAmount()));
                disbursementOrchestrator.createDisbursementAndQueuePayout(
                        "MILESTONE_SPLIT:" + splitEntity.getBusinessKey(),
                        "OUTBOX:MILESTONE_SPLIT:" + splitEntity.getBusinessKey(),
                        escrow,
                        milestone.getId(),
                        splitEntity.getPayeeType(),
                        splitEntity.getPayeeId(),
                        splitEntity.getAmount(),
                        "TZS"
                );
            }

            ledgerPostingService.post(ledgerTemplateService.milestoneAuthorizedMulti(
                    milestone.getId(),
                    actor.userId(),
                    milestone.getAmount(),
                    "TZS",
                    "MILESTONE_AUTH_MULTI:" + request.idempotencyKey(),
                    allocations,
                    milestone.getRetentionAmount()
            ));

            milestone.setStatus(MilestoneStatus.APPROVED);
            operatorAuditService.recordSuccess(
                    "MILESTONE_APPROVE_MULTI",
                    "MILESTONE",
                    milestoneId.toString(),
                    "Approved milestone with split payouts",
                    java.util.Map.of(
                            "projectId", project.getId(),
                            "splitCount", request.splits().size(),
                            "splits", request.splits().stream().map(split -> java.util.Map.of(
                                    "payeeType", split.payeeType(),
                                    "payeeId", split.payeeId(),
                                    "amount", split.amount(),
                                    "businessKey", split.businessKey()
                            )).toList()
                    )
            );
            return milestone;
        } catch (ForbiddenException ex) {
            operatorAuditService.recordForbidden("MILESTONE_APPROVE_MULTI", "MILESTONE", milestoneId.toString(), "Multi-party milestone approval forbidden", java.util.Map.of(
                    "splitCount", request.splits().size()
            ), ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            operatorAuditService.recordFailure("MILESTONE_APPROVE_MULTI", "MILESTONE", milestoneId.toString(), "Multi-party milestone approval failed", java.util.Map.of(
                    "splitCount", request.splits().size()
            ), ex.getMessage());
            throw ex;
        }
    }

    private String payableAccountFor(String payeeType) {
        try {
            return LedgerAccountCodes.payableForPayeeType(payeeType);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }
}
