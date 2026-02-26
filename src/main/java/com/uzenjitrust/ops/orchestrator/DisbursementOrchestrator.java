package com.uzenjitrust.ops.orchestrator;

import com.uzenjitrust.ops.domain.DisbursementOrderEntity;
import com.uzenjitrust.ops.domain.EscrowEntity;
import com.uzenjitrust.ops.service.DisbursementService;
import com.uzenjitrust.ops.service.OutboxService;
import com.uzenjitrust.ops.service.PayoutOutboxPayload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class DisbursementOrchestrator {

    private final DisbursementService disbursementService;
    private final OutboxService outboxService;

    public DisbursementOrchestrator(DisbursementService disbursementService, OutboxService outboxService) {
        this.disbursementService = disbursementService;
        this.outboxService = outboxService;
    }

    @Transactional
    public DisbursementOrderEntity createDisbursementAndQueuePayout(String disbursementBusinessKey,
                                                                     String outboxIdempotencyKey,
                                                                     EscrowEntity escrow,
                                                                     UUID milestoneId,
                                                                     String payeeType,
                                                                     UUID payeeId,
                                                                     BigDecimal amount,
                                                                     String currency) {
        DisbursementOrderEntity disbursement = disbursementService.createDisbursementIdempotent(
                disbursementBusinessKey,
                escrow,
                milestoneId,
                payeeType,
                payeeId,
                amount,
                currency
        );

        outboxService.createEventIdempotent(
                "DISBURSEMENT",
                disbursement.getId().toString(),
                "BANK_PAYOUT_REQUESTED",
                new PayoutOutboxPayload(
                        disbursement.getId(),
                        disbursement.getPayeeId(),
                        disbursement.getAmount().toPlainString(),
                        disbursement.getCurrency(),
                        outboxIdempotencyKey
                ),
                outboxIdempotencyKey
        );

        return disbursement;
    }
}
