package com.uzenjitrust.ledger.service;

import com.uzenjitrust.ledger.domain.LineType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LedgerTemplateService {

    public LedgerPostingRequest milestoneAuthorizedSingle(UUID milestoneId,
                                                          UUID actorUserId,
                                                          BigDecimal grossAmount,
                                                          BigDecimal retentionAmount,
                                                          String currency,
                                                          String idempotencyKey) {
        BigDecimal payout = grossAmount.subtract(retentionAmount);
        List<LedgerPostingLine> lines = new ArrayList<>();
        lines.add(new LedgerPostingLine(LedgerAccountCodes.ESCROW_LIABILITY, LineType.DEBIT, grossAmount, currency));
        lines.add(new LedgerPostingLine(LedgerAccountCodes.PAYABLE_CONTRACTOR, LineType.CREDIT, payout, currency));
        if (retentionAmount.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(new LedgerPostingLine(LedgerAccountCodes.PAYABLE_RETENTION, LineType.CREDIT, retentionAmount, currency));
        }

        return new LedgerPostingRequest(
                "CONSTRUCTION_MILESTONE_AUTHORIZED",
                milestoneId.toString(),
                idempotencyKey,
                "Milestone payout authorized",
                actorUserId,
                lines
        );
    }

    public LedgerPostingRequest milestoneAuthorizedMulti(UUID milestoneId,
                                                         UUID actorUserId,
                                                         BigDecimal totalAmount,
                                                         String currency,
                                                         String idempotencyKey,
                                                         List<BigDecimal> splitAmounts,
                                                         BigDecimal retentionAmount) {
        BigDecimal splitTotal = splitAmounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (splitTotal.add(retentionAmount).compareTo(totalAmount) != 0) {
            throw new IllegalArgumentException("Split amounts + retention must equal total milestone amount");
        }

        List<LedgerPostingLine> lines = new ArrayList<>();
        lines.add(new LedgerPostingLine(LedgerAccountCodes.ESCROW_LIABILITY, LineType.DEBIT, totalAmount, currency));

        for (BigDecimal split : splitAmounts) {
            lines.add(new LedgerPostingLine(LedgerAccountCodes.PAYABLE_CONTRACTOR, LineType.CREDIT, split, currency));
        }

        if (retentionAmount.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(new LedgerPostingLine(LedgerAccountCodes.PAYABLE_RETENTION, LineType.CREDIT, retentionAmount, currency));
        }

        return new LedgerPostingRequest(
                "CONSTRUCTION_MILESTONE_AUTHORIZED",
                milestoneId.toString(),
                idempotencyKey,
                "Milestone multi-party payout authorized",
                actorUserId,
                lines
        );
    }

    public LedgerPostingRequest inspectionFeeAuthorized(UUID inspectionId,
                                                        UUID actorUserId,
                                                        BigDecimal feeAmount,
                                                        String currency,
                                                        String idempotencyKey) {
        return new LedgerPostingRequest(
                "INSPECTION_FEE_AUTHORIZED",
                inspectionId.toString(),
                idempotencyKey,
                "Inspection fee authorized",
                actorUserId,
                List.of(
                        new LedgerPostingLine(LedgerAccountCodes.ESCROW_LIABILITY, LineType.DEBIT, feeAmount, currency),
                        new LedgerPostingLine(LedgerAccountCodes.PAYABLE_INSPECTOR, LineType.CREDIT, feeAmount, currency)
                )
        );
    }

    public LedgerPostingRequest retentionReleaseAuthorized(UUID milestoneId,
                                                           UUID actorUserId,
                                                           BigDecimal amount,
                                                           String currency,
                                                           String idempotencyKey) {
        return new LedgerPostingRequest(
                "RETENTION_RELEASE_AUTHORIZED",
                milestoneId.toString(),
                idempotencyKey,
                "Retention release authorized",
                actorUserId,
                List.of(
                        new LedgerPostingLine(LedgerAccountCodes.PAYABLE_RETENTION, LineType.DEBIT, amount, currency),
                        new LedgerPostingLine(LedgerAccountCodes.PAYABLE_CONTRACTOR, LineType.CREDIT, amount, currency)
                )
        );
    }

    public LedgerPostingRequest bankPayoutSettled(String disbursementId,
                                                  UUID actorUserId,
                                                  String payableAccountCode,
                                                  BigDecimal amount,
                                                  String currency,
                                                  String idempotencyKey) {
        return new LedgerPostingRequest(
                "BANK_PAYOUT_SETTLED",
                disbursementId,
                idempotencyKey,
                "Bank payout settled",
                actorUserId,
                List.of(
                        new LedgerPostingLine(payableAccountCode, LineType.DEBIT, amount, currency),
                        new LedgerPostingLine(LedgerAccountCodes.BANK_CASH, LineType.CREDIT, amount, currency)
                )
        );
    }

    public LedgerPostingRequest escrowFunded(String escrowId,
                                             UUID actorUserId,
                                             BigDecimal amount,
                                             String currency,
                                             String idempotencyKey) {
        return new LedgerPostingRequest(
                "ESCROW_FUNDED",
                escrowId,
                idempotencyKey,
                "Escrow funded",
                actorUserId,
                List.of(
                        new LedgerPostingLine(LedgerAccountCodes.BANK_CASH, LineType.DEBIT, amount, currency),
                        new LedgerPostingLine(LedgerAccountCodes.ESCROW_LIABILITY, LineType.CREDIT, amount, currency)
                )
        );
    }

    public LedgerPostingRequest escrowReleasedToSeller(String escrowId,
                                                       UUID actorUserId,
                                                       BigDecimal amount,
                                                       String currency,
                                                       String idempotencyKey) {
        return new LedgerPostingRequest(
                "ESCROW_RELEASED_TO_SELLER",
                escrowId,
                idempotencyKey,
                "Escrow release to seller authorized",
                actorUserId,
                List.of(
                        new LedgerPostingLine(LedgerAccountCodes.ESCROW_LIABILITY, LineType.DEBIT, amount, currency),
                        new LedgerPostingLine(LedgerAccountCodes.PAYABLE_SELLER, LineType.CREDIT, amount, currency)
                )
        );
    }
}
