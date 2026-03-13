package com.uzenjitrust.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uzenjitrust.build.domain.MilestoneEntity;
import com.uzenjitrust.build.domain.MilestoneStatus;
import com.uzenjitrust.build.domain.ProjectEntity;
import com.uzenjitrust.build.domain.ProjectStatus;
import com.uzenjitrust.build.repo.MilestoneRepository;
import com.uzenjitrust.build.repo.ProjectRepository;
import com.uzenjitrust.common.error.BadRequestException;
import com.uzenjitrust.common.error.ForbiddenException;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.ledger.domain.LineType;
import com.uzenjitrust.ledger.service.LedgerPostingLine;
import com.uzenjitrust.ledger.service.LedgerPostingRequest;
import com.uzenjitrust.ledger.service.LedgerPostingService;
import com.uzenjitrust.ledger.service.LedgerReadService;
import com.uzenjitrust.ops.domain.DisbursementOrderEntity;
import com.uzenjitrust.ops.domain.DisbursementStatus;
import com.uzenjitrust.ops.domain.EscrowEntity;
import com.uzenjitrust.ops.domain.EscrowStatus;
import com.uzenjitrust.ops.domain.OutboxEventEntity;
import com.uzenjitrust.ops.domain.OutboxStatus;
import com.uzenjitrust.ops.domain.WebhookEventEntity;
import com.uzenjitrust.ops.domain.WebhookStatus;
import com.uzenjitrust.ops.repo.DisbursementOrderRepository;
import com.uzenjitrust.ops.repo.EscrowRepository;
import com.uzenjitrust.ops.repo.OutboxEventRepository;
import com.uzenjitrust.ops.repo.WebhookEventRepository;
import com.uzenjitrust.ops.service.OpsReadService;
import com.uzenjitrust.ops.service.OutboxService;
import com.uzenjitrust.support.PostgresIntegrationTest;
import com.uzenjitrust.support.TestSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpsObservabilityIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private EscrowRepository escrowRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private MilestoneRepository milestoneRepository;
    @Autowired
    private DisbursementOrderRepository disbursementOrderRepository;
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    @Autowired
    private WebhookEventRepository webhookEventRepository;
    @Autowired
    private OutboxService outboxService;
    @Autowired
    private OpsReadService opsReadService;
    @Autowired
    private LedgerPostingService ledgerPostingService;
    @Autowired
    private LedgerReadService ledgerReadService;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminCanListDisbursementsAndReadDetail() {
        UUID ownerId = TestSecurity.randomUser();
        UUID adminId = TestSecurity.randomUser();

        EscrowEntity escrow = createEscrow(ownerId);
        ProjectEntity project = createProject(ownerId, escrow.getId());
        MilestoneEntity milestone = createMilestone(project);
        DisbursementOrderEntity disbursement = createDisbursement(escrow, milestone.getId(), "CONTRACTOR", DisbursementStatus.SUBMITTED);

        TestSecurity.as(adminId, AppRole.ADMIN);
        var page = opsReadService.listDisbursementsAdmin("SUBMITTED", "CONTRACTOR", null, null, 0, 10, "createdAt", org.springframework.data.domain.Sort.Direction.DESC);
        var detail = opsReadService.getDisbursement(disbursement.getId());

        assertEquals(1, page.getTotalElements());
        assertEquals(disbursement.getId(), page.getContent().getFirst().id());
        assertEquals(project.getId(), detail.projectId());
        assertEquals(escrow.getId(), detail.escrowId());
        assertNotNull(detail.updatedAt());

        TestSecurity.as(ownerId, AppRole.OWNER);
        assertThrows(ForbiddenException.class, () -> opsReadService.listDisbursementsAdmin(null, null, null, null, 0, 10, "createdAt", org.springframework.data.domain.Sort.Direction.DESC));
    }

    @Test
    void retryOutboxEventRequiresAdminAndOnlyAllowsFailedEvents() {
        UUID adminId = TestSecurity.randomUser();
        UUID ownerId = TestSecurity.randomUser();

        OutboxEventEntity event = outboxService.createEventIdempotent(
                "DISBURSEMENT",
                UUID.randomUUID().toString(),
                "PAYOUT_REQUESTED",
                java.util.Map.of("disbursementId", UUID.randomUUID(), "amount", "120000", "currency", "TZS"),
                "OUTBOX-RETRY-TEST"
        );
        event.setStatus(OutboxStatus.FAILED);
        event.setRetryCount(5);
        event.setLastError("Bank unavailable");
        outboxEventRepository.save(event);

        TestSecurity.as(ownerId, AppRole.OWNER);
        assertThrows(ForbiddenException.class, () -> opsReadService.retryOutboxEvent(event.getId()));

        TestSecurity.as(adminId, AppRole.ADMIN);
        var retried = opsReadService.retryOutboxEvent(event.getId());
        assertEquals("PENDING", retried.status());
        assertNull(retried.lastError());

        OutboxEventEntity pending = outboxEventRepository.findById(event.getId()).orElseThrow();
        pending.setStatus(OutboxStatus.PENDING);
        outboxEventRepository.save(pending);
        assertThrows(BadRequestException.class, () -> opsReadService.retryOutboxEvent(event.getId()));
    }

    @Test
    void adminCanInspectWebhookAndLedgerDetails() throws Exception {
        UUID ownerId = TestSecurity.randomUser();
        UUID adminId = TestSecurity.randomUser();

        EscrowEntity escrow = createEscrow(ownerId);
        ProjectEntity project = createProject(ownerId, escrow.getId());
        MilestoneEntity milestone = createMilestone(project);
        DisbursementOrderEntity disbursement = createDisbursement(escrow, milestone.getId(), "INSPECTOR", DisbursementStatus.SETTLED);

        WebhookEventEntity webhook = new WebhookEventEntity();
        webhook.setSource("MOCK_BANK");
        webhook.setEventId("evt-observe-1");
        webhook.setEventType("DISBURSEMENT_SETTLED");
        webhook.setPayload(objectMapper.readTree("""
                {
                  "eventId":"evt-observe-1",
                  "eventType":"DISBURSEMENT_SETTLED",
                  "payload":{
                    "disbursementId":"%s",
                    "settlementRef":"settlement-123"
                  }
                }
                """.formatted(disbursement.getId())));
        webhook.setSignature("redacted");
        webhook.setEventTs(Instant.now());
        webhook.setStatus(WebhookStatus.PROCESSED);
        webhook.setProcessedAt(Instant.now());
        webhook = webhookEventRepository.save(webhook);

        UUID actorId = TestSecurity.randomUser();
        var post = ledgerPostingService.post(new LedgerPostingRequest(
                "OPS_TEST",
                "ref-1",
                "ledger-ops-observe",
                "ops visibility test",
                actorId,
                List.of(
                        new LedgerPostingLine("1010", LineType.DEBIT, new BigDecimal("1000"), "TZS"),
                        new LedgerPostingLine("2010", LineType.CREDIT, new BigDecimal("1000"), "TZS")
                )
        ));

        TestSecurity.as(adminId, AppRole.ADMIN);
        var webhookDetail = opsReadService.getWebhookEvent(webhook.getId());
        var ledgerDetail = ledgerReadService.getJournalEntry(post.journalEntryId());

        assertEquals(disbursement.getId(), webhookDetail.relatedDisbursementId());
        assertEquals(escrow.getId(), webhookDetail.relatedEscrowId());
        assertEquals(milestone.getId(), webhookDetail.relatedMilestoneId());
        assertEquals("settlement-123", webhookDetail.relatedSettlementRef());
        assertEquals(2, ledgerDetail.lines().size());
        assertNotNull(ledgerDetail.hash());

        TestSecurity.as(ownerId, AppRole.OWNER);
        assertThrows(ForbiddenException.class, () -> ledgerReadService.getJournalEntry(post.journalEntryId()));
    }

    private EscrowEntity createEscrow(UUID ownerId) {
        EscrowEntity escrow = new EscrowEntity();
        escrow.setBusinessKey("OPS-ESCROW-" + UUID.randomUUID());
        escrow.setEscrowType("PROJECT");
        escrow.setStatus(EscrowStatus.ACTIVE);
        escrow.setCurrency("TZS");
        escrow.setTotalAmount(new BigDecimal("500000"));
        escrow.setPayerUserId(ownerId);
        escrow.setBeneficiaryUserId(UUID.randomUUID());
        return escrowRepository.save(escrow);
    }

    private ProjectEntity createProject(UUID ownerId, UUID escrowId) {
        ProjectEntity project = new ProjectEntity();
        project.setOwnerUserId(ownerId);
        project.setContractorUserId(UUID.randomUUID());
        project.setInspectorUserId(UUID.randomUUID());
        project.setEscrowId(escrowId);
        project.setTitle("Ops Project");
        project.setStatus(ProjectStatus.ACTIVE);
        project.setRetentionRate(new BigDecimal("10.00"));
        return projectRepository.save(project);
    }

    private MilestoneEntity createMilestone(ProjectEntity project) {
        MilestoneEntity milestone = new MilestoneEntity();
        milestone.setProject(project);
        milestone.setName("Ops milestone");
        milestone.setSequenceNo(1);
        milestone.setAmount(new BigDecimal("100000"));
        milestone.setRetentionAmount(new BigDecimal("10000"));
        milestone.setStatus(MilestoneStatus.APPROVED);
        return milestoneRepository.save(milestone);
    }

    private DisbursementOrderEntity createDisbursement(EscrowEntity escrow,
                                                       UUID milestoneId,
                                                       String payeeType,
                                                       DisbursementStatus status) {
        UUID suffix = UUID.randomUUID();
        DisbursementOrderEntity entity = new DisbursementOrderEntity();
        entity.setBusinessKey("OPS-DISB-" + suffix);
        entity.setEscrow(escrow);
        entity.setMilestoneId(milestoneId);
        entity.setPayeeType(payeeType);
        entity.setPayeeId(UUID.randomUUID());
        entity.setAmount(new BigDecimal("90000"));
        entity.setCurrency("TZS");
        entity.setStatus(status);
        entity.setBankReference("bank-ref-" + suffix);
        entity.setSettlementRef("settlement-ref-" + suffix);
        return disbursementOrderRepository.save(entity);
    }
}
