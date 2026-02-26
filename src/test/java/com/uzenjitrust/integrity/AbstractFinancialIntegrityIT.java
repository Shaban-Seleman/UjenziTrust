package com.uzenjitrust.integrity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uzenjitrust.build.orchestrator.MilestoneOrchestrator;
import com.uzenjitrust.build.orchestrator.MultiPartyMilestoneOrchestrator;
import com.uzenjitrust.build.orchestrator.RetentionOrchestrator;
import com.uzenjitrust.build.repo.MilestoneRepository;
import com.uzenjitrust.ledger.service.LedgerPostingService;
import com.uzenjitrust.ledger.service.LedgerTemplateService;
import com.uzenjitrust.market.service.OfferService;
import com.uzenjitrust.ops.api.SettlementWebhookRequest;
import com.uzenjitrust.ops.orchestrator.DisbursementOrchestrator;
import com.uzenjitrust.ops.repo.DisbursementOrderRepository;
import com.uzenjitrust.ops.service.DisbursementService;
import com.uzenjitrust.ops.service.EscrowService;
import com.uzenjitrust.ops.service.WebhookService;
import com.uzenjitrust.support.PostgresIntegrationTest;
import com.uzenjitrust.support.integrity.FinancialIntegrityTestDataFactory;
import com.uzenjitrust.support.integrity.IntegrityDbAssertions;
import org.springframework.beans.factory.annotation.Autowired;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

abstract class AbstractFinancialIntegrityIT extends PostgresIntegrationTest {

    @Autowired
    protected LedgerPostingService ledgerPostingService;
    @Autowired
    protected LedgerTemplateService ledgerTemplateService;
    @Autowired
    protected EscrowService escrowService;
    @Autowired
    protected DisbursementService disbursementService;
    @Autowired
    protected DisbursementOrchestrator disbursementOrchestrator;
    @Autowired
    protected WebhookService webhookService;
    @Autowired
    protected OfferService offerService;
    @Autowired
    protected MilestoneOrchestrator milestoneOrchestrator;
    @Autowired
    protected MultiPartyMilestoneOrchestrator multiPartyMilestoneOrchestrator;
    @Autowired
    protected RetentionOrchestrator retentionOrchestrator;
    @Autowired
    protected MilestoneRepository milestoneRepository;
    @Autowired
    protected DisbursementOrderRepository disbursementOrderRepository;
    @Autowired
    protected FinancialIntegrityTestDataFactory dataFactory;
    @Autowired
    protected IntegrityDbAssertions assertions;
    @Autowired
    protected ObjectMapper objectMapper;

    protected String settleByWebhook(UUID disbursementId, String eventId, String settlementRef) throws Exception {
        SettlementWebhookRequest request = new SettlementWebhookRequest(
                eventId,
                "DISBURSEMENT_SETTLED",
                java.time.Instant.now(),
                new SettlementWebhookRequest.Payload(disbursementId, settlementRef)
        );

        String rawBody = objectMapper.writeValueAsString(request);
        String signature = hmac(request.eventTs().toString() + "." + rawBody, "change-me-webhook-secret");
        return webhookService.processSettlementEvent(request, signature, rawBody);
    }

    protected String recomputeHash(IntegrityDbAssertions.HashChainMaterial material,
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

    private String hmac(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private String canonicalAmount(BigDecimal amount) {
        BigDecimal normalized = amount.stripTrailingZeros();
        if (normalized.scale() < 0) {
            normalized = normalized.setScale(0);
        }
        return normalized.toPlainString();
    }
}
