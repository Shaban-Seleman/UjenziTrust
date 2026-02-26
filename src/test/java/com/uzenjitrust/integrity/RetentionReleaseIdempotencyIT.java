package com.uzenjitrust.integrity;

import com.uzenjitrust.build.domain.MilestoneEntity;
import com.uzenjitrust.build.domain.MilestoneStatus;
import com.uzenjitrust.build.domain.ProjectEntity;
import com.uzenjitrust.support.integrity.ConcurrencyTestHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("verify")
class RetentionReleaseIdempotencyIT extends AbstractFinancialIntegrityIT {

    @Test
    void retentionReleaseJobDoesNotDoubleRelease() {
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
}
