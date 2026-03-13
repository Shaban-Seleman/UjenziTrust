package com.uzenjitrust.integration;

import com.uzenjitrust.common.error.BadRequestException;
import com.uzenjitrust.common.error.ForbiddenException;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.ops.domain.OutboxEventEntity;
import com.uzenjitrust.ops.domain.OutboxStatus;
import com.uzenjitrust.ops.repo.OutboxEventRepository;
import com.uzenjitrust.ops.service.OperatorAuditService;
import com.uzenjitrust.ops.service.OpsReadService;
import com.uzenjitrust.ops.service.OutboxService;
import com.uzenjitrust.support.PostgresIntegrationTest;
import com.uzenjitrust.support.TestSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OperatorAuditIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private OutboxService outboxService;
    @Autowired
    private OpsReadService opsReadService;
    @Autowired
    private OperatorAuditService operatorAuditService;
    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void auditRecordCreatedOnSuccessfulOutboxRetry() {
        UUID adminId = TestSecurity.randomUser();
        OutboxEventEntity event = failedOutboxEvent("audit-success");

        TestSecurity.as(adminId, AppRole.ADMIN);
        opsReadService.retryOutboxEvent(event.getId());

        var page = operatorAuditService.list("OUTBOX_EVENT_RETRY", "SUCCESS", adminId, "OUTBOX_EVENT", 0, 10, "createdAt", Sort.Direction.DESC);

        assertEquals(1, page.getTotalElements());
        var audit = page.getContent().getFirst();
        assertEquals(event.getId().toString(), audit.resourceId());
        assertEquals("SUCCESS", audit.outcome());
        assertEquals("OUTBOX_EVENT_RETRY", audit.actionType());
    }

    @Test
    void auditRecordCreatedOnFailedOperatorAction() {
        UUID adminId = TestSecurity.randomUser();
        OutboxEventEntity event = failedOutboxEvent("audit-failure");
        event.setStatus(OutboxStatus.PENDING);
        outboxEventRepository.save(event);

        TestSecurity.as(adminId, AppRole.ADMIN);
        assertThrows(BadRequestException.class, () -> opsReadService.retryOutboxEvent(event.getId()));

        var page = operatorAuditService.list(OperatorAuditService.ACTION_OPERATOR_ACTION_FAILED, "FAILED", adminId, "OUTBOX_EVENT", 0, 10, "createdAt", Sort.Direction.DESC);

        assertEquals(1, page.getTotalElements());
        var audit = operatorAuditService.get(page.getContent().getFirst().id());
        assertEquals("FAILED", audit.outcome());
        assertEquals(OperatorAuditService.ACTION_OPERATOR_ACTION_FAILED, audit.actionType());
        assertNotNull(audit.metadata());
    }

    @Test
    void auditRecordCreatedOnForbiddenAccessAttemptAndReadEndpointsAreAdminOnly() {
        UUID ownerId = TestSecurity.randomUser();
        OutboxEventEntity event = failedOutboxEvent("audit-forbidden");

        TestSecurity.as(ownerId, AppRole.OWNER);
        assertThrows(ForbiddenException.class, () -> opsReadService.retryOutboxEvent(event.getId()));
        assertThrows(ForbiddenException.class, () -> operatorAuditService.list(null, null, null, null, 0, 10, "createdAt", Sort.Direction.DESC));

        UUID adminId = TestSecurity.randomUser();
        TestSecurity.as(adminId, AppRole.ADMIN);
        var page = operatorAuditService.list(
                OperatorAuditService.ACTION_ADMIN_FORBIDDEN_ACCESS_ATTEMPT,
                "FORBIDDEN",
                ownerId,
                "OUTBOX_EVENT",
                0,
                10,
                "createdAt",
                Sort.Direction.DESC
        );

        assertEquals(1, page.getTotalElements());
        var audit = operatorAuditService.get(page.getContent().getFirst().id());
        assertEquals(event.getId().toString(), audit.resourceId());
        assertEquals("FORBIDDEN", audit.outcome());
        assertEquals(OperatorAuditService.ACTION_ADMIN_FORBIDDEN_ACCESS_ATTEMPT, audit.actionType());
    }

    private OutboxEventEntity failedOutboxEvent(String suffix) {
        OutboxEventEntity event = outboxService.createEventIdempotent(
                "DISBURSEMENT",
                UUID.randomUUID().toString(),
                "PAYOUT_REQUESTED",
                Map.of("disbursementId", UUID.randomUUID(), "amount", "120000", "currency", "TZS"),
                "OUTBOX-AUDIT-" + suffix
        );
        event.setStatus(OutboxStatus.FAILED);
        event.setRetryCount(3);
        event.setLastError("Simulated failure");
        return outboxEventRepository.save(event);
    }
}
