package com.uzenjitrust.integration;

import com.uzenjitrust.ops.domain.EscrowEntity;
import com.uzenjitrust.ops.service.EscrowService;
import com.uzenjitrust.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpsEscrowIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private EscrowService escrowService;

    @Test
    void escrowCreationIdempotentByBusinessKey() {
        String businessKey = "PROPERTY_PURCHASE:test-reservation-1";
        UUID payer = UUID.randomUUID();
        UUID beneficiary = UUID.randomUUID();

        EscrowEntity first = escrowService.createEscrowIdempotent(
                businessKey,
                "PROPERTY_PURCHASE",
                new BigDecimal("100000"),
                "TZS",
                payer,
                beneficiary
        );

        EscrowEntity second = escrowService.createEscrowIdempotent(
                businessKey,
                "PROPERTY_PURCHASE",
                new BigDecimal("100000"),
                "TZS",
                payer,
                beneficiary
        );

        assertEquals(first.getId(), second.getId());
        assertEquals(first.getBusinessKey(), second.getBusinessKey());
    }
}
