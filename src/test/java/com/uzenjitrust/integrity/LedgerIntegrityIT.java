package com.uzenjitrust.integrity;

import com.uzenjitrust.build.domain.MilestoneEntity;
import com.uzenjitrust.build.domain.MilestoneStatus;
import com.uzenjitrust.build.domain.ProjectEntity;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.ledger.domain.LineType;
import com.uzenjitrust.ledger.service.LedgerPostingLine;
import com.uzenjitrust.ledger.service.LedgerPostingRequest;
import com.uzenjitrust.ledger.service.LedgerPostingResult;
import com.uzenjitrust.ops.domain.DisbursementOrderEntity;
import com.uzenjitrust.ops.domain.EscrowEntity;
import com.uzenjitrust.support.TestSecurity;
import com.uzenjitrust.support.integrity.IdempotencyTestHelper;
import com.uzenjitrust.support.integrity.IntegrityDbAssertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("verify")
class LedgerIntegrityIT extends AbstractFinancialIntegrityIT {

    @Test
    void ledgerEntriesAlwaysBalanced() throws Exception {
        Instant testStart = Instant.now();

        UUID owner = UUID.randomUUID();
        UUID contractor = UUID.randomUUID();
        UUID inspector = UUID.randomUUID();

        EscrowEntity escrow = escrowService.createEscrowIdempotent(
                "PROPERTY_PURCHASE:ledger-balance-escrow",
                "PROPERTY_PURCHASE",
                new BigDecimal("1500000"),
                "TZS",
                owner,
                contractor
        );

        ledgerPostingService.post(ledgerTemplateService.escrowFunded(
                escrow.getId().toString(),
                owner,
                new BigDecimal("1500000"),
                "TZS",
                "LEDGER_BALANCE:ESCROW_FUNDED"
        ));

        DisbursementOrderEntity disbursement = disbursementService.createDisbursementIdempotent(
                "LEDGER_BALANCE:DISB",
                escrow,
                null,
                "SELLER",
                contractor,
                new BigDecimal("500000"),
                "TZS"
        );
        settleByWebhook(disbursement.getId(), "LEDGER-BALANCE-EVT-1", "LEDGER-BALANCE-SETTLE-1");

        ProjectEntity project = dataFactory.projectWithEscrow(owner, contractor, inspector, "ledger-balance-proj", new BigDecimal("5000000"));
        MilestoneEntity milestone = dataFactory.milestone(
                project,
                1,
                new BigDecimal("1000000"),
                new BigDecimal("100000"),
                MilestoneStatus.SUBMITTED
        );

        TestSecurity.as(owner, AppRole.OWNER);
        milestoneOrchestrator.approveMilestoneSingle(milestone.getId(), "ledger-balance-ms-1");
        DisbursementOrderEntity milestoneDisbursement = disbursementOrderRepository
                .findByBusinessKey("MILESTONE_SINGLE:" + milestone.getId())
                .orElseThrow();
        settleByWebhook(milestoneDisbursement.getId(), "LEDGER-BALANCE-EVT-2", "LEDGER-BALANCE-SETTLE-2");

        List<IntegrityDbAssertions.UnbalancedJournal> unbalanced = assertions.findUnbalancedEntriesSince(testStart);
        assertTrue(unbalanced.isEmpty(), "Found unbalanced ledger entries: " + unbalanced);
    }

    @Test
    void hashChainLinksAndRecomputeMatches() {
        Instant testStart = Instant.now();

        for (int i = 1; i <= 5; i++) {
            ledgerPostingService.post(new LedgerPostingRequest(
                    "HASH_CHAIN_TEST",
                    "HASH_REF_" + i,
                    "HASH_IDEMP_" + i,
                    "Hash chain verification entry " + i,
                    UUID.randomUUID(),
                    List.of(
                            new LedgerPostingLine("1010", LineType.DEBIT, new BigDecimal(100 * i), "TZS"),
                            new LedgerPostingLine("2010", LineType.CREDIT, new BigDecimal(100 * i), "TZS")
                    )
            ));
        }

        List<IntegrityDbAssertions.HashChainMaterial> materials = assertions.hashChainMaterialsSince(testStart);
        assertTrue(materials.size() >= 5);

        for (int i = 0; i < materials.size(); i++) {
            IntegrityDbAssertions.HashChainMaterial material = materials.get(i);
            if (i > 0) {
                assertEquals(materials.get(i - 1).hash(), material.prevHash());
            }
            assertEquals(material.hash(), recomputeHash(material, material.lines()));
        }

        IntegrityDbAssertions.HashChainMaterial first = materials.get(0);
        List<IntegrityDbAssertions.HashLineMaterial> tamperedLines = new ArrayList<>(first.lines());
        IntegrityDbAssertions.HashLineMaterial firstLine = tamperedLines.get(0);
        tamperedLines.set(0, new IntegrityDbAssertions.HashLineMaterial(
                firstLine.accountCode(),
                firstLine.lineType(),
                firstLine.amount().add(BigDecimal.ONE),
                firstLine.currency()
        ));

        assertNotEquals(first.hash(), recomputeHash(first, tamperedLines));
    }

    @Test
    void idempotentCommandsDoNotDuplicateRows() throws Exception {
        UUID payer = UUID.randomUUID();
        UUID payee = UUID.randomUUID();

        IdempotencyTestHelper.Pair<EscrowEntity, EscrowEntity> escrows = IdempotencyTestHelper.runSameCommandTwice(() ->
                escrowService.createEscrowIdempotent(
                        "IDEMP_ESCROW_KEY",
                        "PROPERTY_PURCHASE",
                        new BigDecimal("700000"),
                        "TZS",
                        payer,
                        payee
                )
        );
        assertEquals(escrows.first().getId(), escrows.second().getId());
        assertEquals(1L, assertions.escrowCountByBusinessKey("IDEMP_ESCROW_KEY"));

        EscrowEntity escrow = escrows.first();
        IdempotencyTestHelper.Pair<DisbursementOrderEntity, DisbursementOrderEntity> disbursements = IdempotencyTestHelper.runSameCommandTwice(() ->
                disbursementOrchestrator.createDisbursementAndQueuePayout(
                        "IDEMP_DISB_KEY",
                        "IDEMP_OUTBOX_KEY",
                        escrow,
                        null,
                        "SELLER",
                        payee,
                        new BigDecimal("350000"),
                        "TZS"
                )
        );

        assertEquals(disbursements.first().getId(), disbursements.second().getId());
        assertEquals(1L, assertions.disbursementCountByBusinessKey("IDEMP_DISB_KEY"));
        assertEquals(1L, assertions.outboxCountByIdempotencyKey("IDEMP_OUTBOX_KEY"));

        IdempotencyTestHelper.Pair<LedgerPostingResult, LedgerPostingResult> ledgerPosts = IdempotencyTestHelper.runSameCommandTwice(() ->
                ledgerPostingService.post(ledgerTemplateService.escrowFunded(
                        escrow.getId().toString(),
                        payer,
                        new BigDecimal("700000"),
                        "TZS",
                        "IDEMP_LEDGER_KEY"
                ))
        );

        assertEquals(ledgerPosts.first().journalEntryId(), ledgerPosts.second().journalEntryId());
        assertEquals(1L, assertions.ledgerEntryCountByIdempotency(
                "ESCROW_FUNDED",
                escrow.getId().toString(),
                "IDEMP_LEDGER_KEY"
        ));
    }
}
