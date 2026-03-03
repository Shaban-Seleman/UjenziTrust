package com.uzenjitrust.ops.service;

import com.uzenjitrust.build.domain.MilestoneEntity;
import com.uzenjitrust.build.domain.MilestoneStatus;
import com.uzenjitrust.build.repo.MilestoneRepository;
import com.uzenjitrust.common.error.NotFoundException;
import com.uzenjitrust.ledger.service.LedgerAccountCodes;
import com.uzenjitrust.ledger.service.LedgerPostingService;
import com.uzenjitrust.ledger.service.LedgerTemplateService;
import com.uzenjitrust.ops.domain.DisbursementOrderEntity;
import com.uzenjitrust.ops.domain.DisbursementStatus;
import com.uzenjitrust.ops.domain.EscrowEntity;
import com.uzenjitrust.ops.repo.DisbursementOrderRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class DisbursementService {

    private final DisbursementOrderRepository disbursementRepository;
    private final MilestoneRepository milestoneRepository;
    private final OpsProperties opsProperties;
    private final LedgerTemplateService ledgerTemplateService;
    private final LedgerPostingService ledgerPostingService;

    public DisbursementService(DisbursementOrderRepository disbursementRepository,
                               MilestoneRepository milestoneRepository,
                               OpsProperties opsProperties,
                               LedgerTemplateService ledgerTemplateService,
                               LedgerPostingService ledgerPostingService) {
        this.disbursementRepository = disbursementRepository;
        this.milestoneRepository = milestoneRepository;
        this.opsProperties = opsProperties;
        this.ledgerTemplateService = ledgerTemplateService;
        this.ledgerPostingService = ledgerPostingService;
    }

    @Transactional
    public DisbursementOrderEntity createDisbursementIdempotent(String businessKey,
                                                                 EscrowEntity escrow,
                                                                 UUID milestoneId,
                                                                 String payeeType,
                                                                 UUID payeeId,
                                                                 BigDecimal amount,
                                                                 String currency) {
        return disbursementRepository.findByBusinessKey(businessKey).orElseGet(() -> {
            DisbursementOrderEntity disbursement = new DisbursementOrderEntity();
            disbursement.setBusinessKey(businessKey);
            disbursement.setEscrow(escrow);
            disbursement.setMilestoneId(milestoneId);
            disbursement.setPayeeType(payeeType);
            disbursement.setPayeeId(payeeId);
            disbursement.setAmount(amount);
            disbursement.setCurrency(currency);
            disbursement.setStatus(DisbursementStatus.PENDING);
            try {
                return disbursementRepository.save(disbursement);
            } catch (DataIntegrityViolationException ex) {
                return disbursementRepository.findByBusinessKey(businessKey).orElseThrow(() -> ex);
            }
        });
    }

    @Transactional
    public DisbursementOrderEntity markSubmitted(UUID disbursementId, String bankReference) {
        DisbursementOrderEntity disbursement = disbursementRepository.findByIdForUpdate(disbursementId)
                .orElseThrow(() -> new NotFoundException("Disbursement not found"));
        if (disbursement.getStatus() == DisbursementStatus.PENDING || disbursement.getStatus() == DisbursementStatus.FAILED) {
            disbursement.setStatus(DisbursementStatus.SUBMITTED);
            disbursement.setBankReference(bankReference);
        }
        return disbursement;
    }

    @Transactional
    public void settleDisbursement(UUID disbursementId,
                                   String settlementRef,
                                   UUID actorUserId,
                                   String ledgerPayableAccountCode,
                                   String settlementEventId) {
        DisbursementOrderEntity disbursement = disbursementRepository.findByIdForUpdate(disbursementId)
                .orElseThrow(() -> new NotFoundException("Disbursement not found"));

        if (disbursement.getStatus() == DisbursementStatus.SETTLED) {
            return;
        }

        disbursement.setStatus(DisbursementStatus.SETTLED);
        disbursement.setSettlementRef(settlementRef);

        ledgerPostingService.post(ledgerTemplateService.bankPayoutSettled(
                disbursement.getId().toString(),
                actorUserId,
                ledgerPayableAccountCode,
                disbursement.getAmount(),
                disbursement.getCurrency(),
                "BANK_SETTLEMENT:" + settlementEventId
        ));

        if (disbursement.getMilestoneId() != null) {
            MilestoneEntity milestone = milestoneRepository.findByIdForUpdate(disbursement.getMilestoneId())
                    .orElseThrow(() -> new NotFoundException("Milestone not found"));

            long unsettledCount = disbursementRepository.countByMilestoneIdAndStatusNot(
                    disbursement.getMilestoneId(),
                    DisbursementStatus.SETTLED
            );
            if (unsettledCount == 0 && milestone.getStatus() != MilestoneStatus.PAID && milestone.getStatus() != MilestoneStatus.RETENTION_RELEASED) {
                Instant now = Instant.now();
                Instant paidAt = milestone.getPaidAt() == null ? now : milestone.getPaidAt();
                milestone.setStatus(MilestoneStatus.PAID);
                milestone.setPaidAt(paidAt);
                if (milestone.getRetentionReleaseAt() == null) {
                    milestone.setRetentionReleaseAt(paidAt.plus(opsProperties.getRetentionDays(), ChronoUnit.DAYS));
                }
            }
        }
    }

    public static String payableAccountFor(String payeeType) {
        try {
            return LedgerAccountCodes.payableForPayeeType(payeeType);
        } catch (IllegalArgumentException ex) {
            throw new com.uzenjitrust.common.error.BadRequestException(ex.getMessage());
        }
    }
}
