package com.uzenjitrust.integrity;

import com.uzenjitrust.ops.domain.DisbursementOrderEntity;
import com.uzenjitrust.ops.domain.DisbursementStatus;
import com.uzenjitrust.ops.domain.EscrowEntity;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("verify")
class WebhookIdempotencyIT extends AbstractFinancialIntegrityIT {

    @Test
    void webhookDuplicateEventIdIsNoOp() throws Exception {
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
}
