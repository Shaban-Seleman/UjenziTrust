package com.uzenjitrust.integrity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uzenjitrust.build.api.ApproveMilestoneMultiRequest;
import com.uzenjitrust.build.domain.MilestoneEntity;
import com.uzenjitrust.build.domain.MilestoneStatus;
import com.uzenjitrust.build.domain.ProjectEntity;
import com.uzenjitrust.build.orchestrator.MilestoneOrchestrator;
import com.uzenjitrust.build.orchestrator.MultiPartyMilestoneOrchestrator;
import com.uzenjitrust.build.orchestrator.RetentionOrchestrator;
import com.uzenjitrust.build.repo.MilestoneRepository;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.ledger.domain.LineType;
import com.uzenjitrust.ledger.service.LedgerPostingLine;
import com.uzenjitrust.ledger.service.LedgerPostingRequest;
import com.uzenjitrust.ledger.service.LedgerPostingResult;
import com.uzenjitrust.ledger.service.LedgerPostingService;
import com.uzenjitrust.ledger.service.LedgerTemplateService;
import com.uzenjitrust.market.api.AcceptOfferRequest;
import com.uzenjitrust.market.domain.OfferEntity;
import com.uzenjitrust.market.domain.PropertyEntity;
import com.uzenjitrust.market.repo.OfferRepository;
import com.uzenjitrust.market.service.OfferService;
import com.uzenjitrust.ops.api.SettlementWebhookRequest;
import com.uzenjitrust.ops.domain.DisbursementOrderEntity;
import com.uzenjitrust.ops.domain.DisbursementStatus;
import com.uzenjitrust.ops.domain.EscrowEntity;
import com.uzenjitrust.ops.orchestrator.DisbursementOrchestrator;
import com.uzenjitrust.ops.repo.DisbursementOrderRepository;
import com.uzenjitrust.ops.service.DisbursementService;
import com.uzenjitrust.ops.service.EscrowService;
import com.uzenjitrust.ops.service.WebhookService;
import com.uzenjitrust.support.PostgresIntegrationTest;
import com.uzenjitrust.support.TestSecurity;
import com.uzenjitrust.support.integrity.ConcurrencyTestHelper;
import com.uzenjitrust.support.integrity.FinancialIntegrityTestDataFactory;
import com.uzenjitrust.support.integrity.IdempotencyTestHelper;
import com.uzenjitrust.support.integrity.IntegrityDbAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialIntegrityVerificationSuiteTest extends PostgresIntegrationTest {

    @Autowired
    private LedgerPostingService ledgerPostingService;
    @Autowired
    private LedgerTemplateService ledgerTemplateService;
    @Autowired
    private EscrowService escrowService;
    @Autowired
    private DisbursementService disbursementService;
    @Autowired
    private DisbursementOrchestrator disbursementOrchestrator;
    @Autowired
    private WebhookService webhookService;
    @Autowired
    private OfferService offerService;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private MilestoneOrchestrator milestoneOrchestrator;
    @Autowired
    private MultiPartyMilestoneOrchestrator multiPartyMilestoneOrchestrator;
    @Autowired
    private RetentionOrchestrator retentionOrchestrator;
    @Autowired
    private MilestoneRepository milestoneRepository;
    @Autowired
    private DisbursementOrderRepository disbursementOrderRepository;
    @Autowired
    private FinancialIntegrityTestDataFactory dataFactory;
    @Autowired
    private IntegrityDbAssertions assertions;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testLedgerBalancedAcrossOperations() throws Exception {
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
    void testLedgerHashChainIntegrityAndTamperDetection() {
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
                String expectedPrev = materials.get(i - 1).hash();
                assertEquals(expectedPrev, material.prevHash(), "Hash chain link broken at index " + material.chainIndex());
            }
            String recomputed = recomputeHash(material, material.lines());
            assertEquals(material.hash(), recomputed, "Stored hash mismatch at index " + material.chainIndex());
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

        String tamperedHash = recomputeHash(first, tamperedLines);
        assertNotEquals(first.hash(), tamperedHash, "Tampered material must fail hash verification");
    }

    @Test
    void testIdempotencyPostingAndDisbursementCreation() throws Exception {
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

        DisbursementOrderEntity webhookTarget = disbursementService.createDisbursementIdempotent(
                "IDEMP_WEBHOOK_DISB",
                escrow,
                null,
                "SELLER",
                payee,
                new BigDecimal("100000"),
                "TZS"
        );

        String first = settleByWebhook(webhookTarget.getId(), "IDEMP-WEBHOOK-EVT", "IDEMP-WEBHOOK-SETTLE");
        String second = settleByWebhook(webhookTarget.getId(), "IDEMP-WEBHOOK-EVT", "IDEMP-WEBHOOK-SETTLE");

        assertEquals("PROCESSED", first);
        assertEquals("DUPLICATE", second);
        assertEquals(1L, assertions.webhookCountByEventId("IDEMP-WEBHOOK-EVT"));
        assertEquals(1L, assertions.ledgerEntryCount("BANK_PAYOUT_SETTLED", webhookTarget.getId().toString()));
    }

    @Test
    void testDoubleReservationImpossibleUnderConcurrency() {
        UUID seller = UUID.randomUUID();
        UUID buyerA = UUID.randomUUID();
        UUID buyerB = UUID.randomUUID();

        PropertyEntity property = dataFactory.property(seller, "Concurrency Property", new BigDecimal("120000000"));
        OfferEntity offerA = dataFactory.offer(property, buyerA, new BigDecimal("110000000"));
        OfferEntity offerB = dataFactory.offer(property, buyerB, new BigDecimal("115000000"));

        AtomicInteger turn = new AtomicInteger();
        List<ConcurrencyTestHelper.Result<OfferService.AcceptOfferResult>> results = ConcurrencyTestHelper.runConcurrently(2, () -> {
            TestSecurity.as(seller, AppRole.SELLER);
            if (turn.getAndIncrement() == 0) {
                return offerService.accept(offerA.getId(), new AcceptOfferRequest("CONCUR-ACC-A", "accept A"));
            }
            return offerService.accept(offerB.getId(), new AcceptOfferRequest("CONCUR-ACC-B", "accept B"));
        });

        long successes = results.stream().filter(ConcurrencyTestHelper.Result::isSuccess).count();
        long failures = results.stream().filter(r -> !r.isSuccess()).count();
        assertEquals(1L, successes);
        assertEquals(1L, failures);

        assertEquals(1L, assertions.activeReservationCount(property.getId()));
        assertEquals(1L, assertions.acceptedOfferCount(property.getId()));
    }

    @Test
    void testDuplicateWebhookIgnored() throws Exception {
        UUID payer = UUID.randomUUID();
        UUID payee = UUID.randomUUID();

        EscrowEntity escrow = escrowService.createEscrowIdempotent(
                "WEBHOOK_DUP_ESCROW",
                "PROPERTY_PURCHASE",
                new BigDecimal("250000"),
                "TZS",
                payer,
                payee
        );

        DisbursementOrderEntity disbursement = disbursementService.createDisbursementIdempotent(
                "WEBHOOK_DUP_DISB",
                escrow,
                null,
                "SELLER",
                payee,
                new BigDecimal("150000"),
                "TZS"
        );

        String first = settleByWebhook(disbursement.getId(), "DUP-EVT-1", "DUP-SETTLE-1");
        String second = settleByWebhook(disbursement.getId(), "DUP-EVT-1", "DUP-SETTLE-1");

        assertEquals("PROCESSED", first);
        assertEquals("DUPLICATE", second);

        DisbursementOrderEntity reloaded = disbursementOrderRepository.findByBusinessKey("WEBHOOK_DUP_DISB").orElseThrow();
        assertEquals(DisbursementStatus.SETTLED, reloaded.getStatus());
        assertEquals(1L, assertions.webhookCountByEventId("DUP-EVT-1"));
        assertEquals(1L, assertions.ledgerEntryCount("BANK_PAYOUT_SETTLED", disbursement.getId().toString()));
    }

    @Test
    void testMilestonePaidOnlyAfterAllSplitsSettled() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID contractor = UUID.randomUUID();
        UUID inspector = UUID.randomUUID();

        ProjectEntity project = dataFactory.projectWithEscrow(owner, contractor, inspector, "multi-split", new BigDecimal("7000000"));
        MilestoneEntity milestone = dataFactory.milestone(
                project,
                1,
                new BigDecimal("1000000"),
                new BigDecimal("100000"),
                MilestoneStatus.SUBMITTED
        );

        TestSecurity.as(owner, AppRole.OWNER);
        multiPartyMilestoneOrchestrator.approveMilestoneMulti(milestone.getId(), new ApproveMilestoneMultiRequest(
                "MULTI-IDEMP-1",
                List.of(
                        new ApproveMilestoneMultiRequest.Split("CONTRACTOR", contractor, new BigDecimal("300000"), "SPLIT-A"),
                        new ApproveMilestoneMultiRequest.Split("CONTRACTOR", contractor, new BigDecimal("300000"), "SPLIT-B"),
                        new ApproveMilestoneMultiRequest.Split("INSPECTOR", inspector, new BigDecimal("300000"), "SPLIT-C")
                )
        ));

        List<DisbursementOrderEntity> splits = disbursementOrderRepository.findByMilestoneId(milestone.getId());
        assertEquals(3, splits.size());

        settleByWebhook(splits.get(0).getId(), "MSPLIT-EVT-1", "MSPLIT-SETTLE-1");
        MilestoneEntity afterOne = milestoneRepository.findById(milestone.getId()).orElseThrow();
        assertEquals(MilestoneStatus.APPROVED, afterOne.getStatus());
        assertEquals(2L, assertions.unsettledDisbursementCount(milestone.getId()));

        settleByWebhook(splits.get(1).getId(), "MSPLIT-EVT-2", "MSPLIT-SETTLE-2");
        settleByWebhook(splits.get(2).getId(), "MSPLIT-EVT-3", "MSPLIT-SETTLE-3");

        MilestoneEntity paid = milestoneRepository.findById(milestone.getId()).orElseThrow();
        assertEquals(MilestoneStatus.PAID, paid.getStatus());
        assertNotNull(paid.getPaidAt());
        assertNotNull(paid.getRetentionReleaseAt());
        assertEquals(0L, assertions.unsettledDisbursementCount(milestone.getId()));
    }

    @Test
    void testRetentionJobIdempotentNoDoubleRelease() {
        UUID owner = UUID.randomUUID();
        UUID contractor = UUID.randomUUID();
        UUID inspector = UUID.randomUUID();

        ProjectEntity project = dataFactory.projectWithEscrow(owner, contractor, inspector, "retention-idem", new BigDecimal("5000000"));
        MilestoneEntity milestone = dataFactory.milestone(
                project,
                1,
                new BigDecimal("800000"),
                new BigDecimal("80000"),
                MilestoneStatus.PAID
        );
        milestone.setPaidAt(Instant.now().minus(20, ChronoUnit.DAYS));
        milestone.setRetentionReleaseAt(Instant.now().minus(1, ChronoUnit.HOURS));
        milestoneRepository.save(milestone);

        retentionOrchestrator.releaseDueRetentionsSystem();
        retentionOrchestrator.releaseDueRetentionsSystem();

        ConcurrencyTestHelper.runConcurrently(2, () -> retentionOrchestrator.releaseDueRetentionsSystem());

        String retentionBusinessKey = "RETENTION:" + milestone.getId();
        String retentionOutboxKey = "OUTBOX:RETENTION:" + milestone.getId();

        assertEquals(1L, assertions.disbursementCountByBusinessKey(retentionBusinessKey));
        assertEquals(1L, assertions.outboxCountByIdempotencyKey(retentionOutboxKey));
        assertEquals(1L, assertions.ledgerEntryCountByIdempotency(
                "RETENTION_RELEASE_AUTHORIZED",
                milestone.getId().toString(),
                "RETENTION_RELEASE_JOB:" + milestone.getId()
        ));
    }

    private String settleByWebhook(UUID disbursementId, String eventId, String settlementRef) throws Exception {
        SettlementWebhookRequest request = new SettlementWebhookRequest(
                eventId,
                "DISBURSEMENT_SETTLED",
                Instant.now(),
                new SettlementWebhookRequest.Payload(disbursementId, settlementRef)
        );

        String rawBody = objectMapper.writeValueAsString(request);
        String signature = hmac(request.eventTs().toString() + "." + rawBody, "change-me-webhook-secret");
        return webhookService.processSettlementEvent(request, signature, rawBody);
    }

    private String hmac(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private String recomputeHash(IntegrityDbAssertions.HashChainMaterial material,
                                 List<IntegrityDbAssertions.HashLineMaterial> lines) {
        String canonicalLines = lines.stream()
                .sorted(Comparator
                        .comparing(IntegrityDbAssertions.HashLineMaterial::accountCode)
                        .thenComparing(IntegrityDbAssertions.HashLineMaterial::lineType)
                        .thenComparing(IntegrityDbAssertions.HashLineMaterial::currency)
                        .thenComparing(IntegrityDbAssertions.HashLineMaterial::amount))
                .map(line -> String.join(":",
                        line.accountCode(),
                        line.lineType(),
                        canonicalAmount(line.amount()),
                        line.currency()))
                .reduce((a, b) -> a + "|" + b)
                .orElse("");

        String payload = String.join("#",
                String.valueOf(material.chainIndex()),
                material.entryType(),
                material.referenceId(),
                material.idempotencyKey(),
                material.actorUserId(),
                material.prevHash() == null ? "GENESIS" : material.prevHash(),
                canonicalLines
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String canonicalAmount(BigDecimal amount) {
        BigDecimal normalized = amount.stripTrailingZeros();
        if (normalized.scale() < 0) {
            normalized = normalized.setScale(0);
        }
        return normalized.toPlainString();
    }
}
