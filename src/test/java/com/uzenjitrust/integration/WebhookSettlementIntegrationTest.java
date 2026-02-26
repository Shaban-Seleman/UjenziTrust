package com.uzenjitrust.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uzenjitrust.build.domain.MilestoneEntity;
import com.uzenjitrust.build.domain.MilestoneStatus;
import com.uzenjitrust.build.domain.ProjectEntity;
import com.uzenjitrust.build.domain.ProjectStatus;
import com.uzenjitrust.build.repo.MilestoneRepository;
import com.uzenjitrust.build.repo.ProjectRepository;
import com.uzenjitrust.ledger.repo.JournalEntryRepository;
import com.uzenjitrust.ops.api.SettlementWebhookRequest;
import com.uzenjitrust.ops.domain.DisbursementOrderEntity;
import com.uzenjitrust.ops.domain.DisbursementStatus;
import com.uzenjitrust.ops.domain.EscrowEntity;
import com.uzenjitrust.ops.repo.DisbursementOrderRepository;
import com.uzenjitrust.ops.service.DisbursementService;
import com.uzenjitrust.ops.service.EscrowService;
import com.uzenjitrust.ops.service.WebhookService;
import com.uzenjitrust.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WebhookSettlementIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private MilestoneRepository milestoneRepository;
    @Autowired
    private EscrowService escrowService;
    @Autowired
    private DisbursementService disbursementService;
    @Autowired
    private DisbursementOrderRepository disbursementRepository;
    @Autowired
    private WebhookService webhookService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Test
    void webhookSettlementPostsLedgerAndMarksMilestonePaidWhenAllSettled() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID contractor = UUID.randomUUID();

        EscrowEntity escrow = escrowService.createEscrowIdempotent(
                "CONSTRUCTION_PROJECT:test-proj-wh",
                "CONSTRUCTION_PROJECT",
                new BigDecimal("3000000"),
                "TZS",
                owner,
                contractor
        );

        ProjectEntity project = new ProjectEntity();
        project.setOwnerUserId(owner);
        project.setContractorUserId(contractor);
        project.setInspectorUserId(UUID.randomUUID());
        project.setEscrowId(escrow.getId());
        project.setTitle("Webhook Build");
        project.setStatus(ProjectStatus.ACTIVE);
        project.setRetentionRate(new BigDecimal("10.00"));
        project = projectRepository.save(project);

        MilestoneEntity milestone = new MilestoneEntity();
        milestone.setProject(project);
        milestone.setName("Roof");
        milestone.setSequenceNo(1);
        milestone.setAmount(new BigDecimal("1000000"));
        milestone.setRetentionAmount(new BigDecimal("100000"));
        milestone.setStatus(MilestoneStatus.APPROVED);
        milestone = milestoneRepository.save(milestone);

        DisbursementOrderEntity d1 = disbursementService.createDisbursementIdempotent(
                "wh-d1",
                escrow,
                milestone.getId(),
                "CONTRACTOR",
                contractor,
                new BigDecimal("450000"),
                "TZS"
        );

        DisbursementOrderEntity d2 = disbursementService.createDisbursementIdempotent(
                "wh-d2",
                escrow,
                milestone.getId(),
                "CONTRACTOR",
                contractor,
                new BigDecimal("450000"),
                "TZS"
        );

        settle(d1.getId(), "evt-1", "settle-1");
        assertEquals(DisbursementStatus.SETTLED, disbursementRepository.findByBusinessKey("wh-d1").orElseThrow().getStatus());
        assertNotNull(journalEntryRepository.findByEntryTypeAndReferenceIdAndIdempotencyKey(
                "BANK_PAYOUT_SETTLED", d1.getId().toString(), "BANK_SETTLEMENT:evt-1").orElse(null));

        milestone = milestoneRepository.findById(milestone.getId()).orElseThrow();
        assertEquals(MilestoneStatus.APPROVED, milestone.getStatus());

        settle(d2.getId(), "evt-2", "settle-2");
        assertEquals(DisbursementStatus.SETTLED, disbursementRepository.findByBusinessKey("wh-d2").orElseThrow().getStatus());

        milestone = milestoneRepository.findById(milestone.getId()).orElseThrow();
        assertEquals(MilestoneStatus.PAID, milestone.getStatus());
        assertNotNull(milestone.getPaidAt());
        assertNotNull(milestone.getRetentionReleaseAt());
    }

    private void settle(UUID disbursementId, String eventId, String settlementRef) throws Exception {
        SettlementWebhookRequest request = new SettlementWebhookRequest(
                eventId,
                "DISBURSEMENT_SETTLED",
                Instant.now(),
                new SettlementWebhookRequest.Payload(disbursementId, settlementRef)
        );
        String rawBody = objectMapper.writeValueAsString(request);
        String signature = hmac(request.eventTs().toString() + "." + rawBody, "change-me-webhook-secret");
        webhookService.processSettlementEvent(request, signature, rawBody);
    }

    private String hmac(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
