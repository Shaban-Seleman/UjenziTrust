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
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("verify")
class RetentionReleaseIdempotencyIT extends AbstractFinancialIntegrityIT {

    @Test
    void retentionReleaseJobSkipsIneligibleMilestones() {
        UUID owner = UUID.randomUUID();
        UUID contractor = UUID.randomUUID();
        UUID inspector = UUID.randomUUID();

        ProjectEntity project = dataFactory.projectWithEscrow(owner, contractor, inspector, "retention-ineligible", new BigDecimal("5000000"));

        MilestoneEntity notPaid = dataFactory.milestone(
                project,
                1,
                new BigDecimal("800000"),
                new BigDecimal("80000"),
                MilestoneStatus.APPROVED
        );
        notPaid.setRetentionReleaseAt(Instant.now().minus(1, ChronoUnit.HOURS));

        MilestoneEntity notDue = dataFactory.milestone(
                project,
                2,
                new BigDecimal("800000"),
                new BigDecimal("80000"),
                MilestoneStatus.PAID
        );
        notDue.setPaidAt(Instant.now().minus(20, ChronoUnit.DAYS));
        notDue.setRetentionReleaseAt(Instant.now().plus(1, ChronoUnit.HOURS));

        MilestoneEntity zeroRetention = dataFactory.milestone(
                project,
                3,
                new BigDecimal("800000"),
                BigDecimal.ZERO,
                MilestoneStatus.PAID
        );
        zeroRetention.setPaidAt(Instant.now().minus(20, ChronoUnit.DAYS));
        zeroRetention.setRetentionReleaseAt(Instant.now().minus(1, ChronoUnit.HOURS));

        milestoneRepository.save(notPaid);
        milestoneRepository.save(notDue);
        milestoneRepository.save(zeroRetention);

        assertEquals(0, retentionOrchestrator.releaseDueRetentionsSystem());

        assertEquals(0L, assertions.disbursementCountByBusinessKey("RETENTION:" + notPaid.getId()));
        assertEquals(0L, assertions.disbursementCountByBusinessKey("RETENTION:" + notDue.getId()));
        assertEquals(0L, assertions.disbursementCountByBusinessKey("RETENTION:" + zeroRetention.getId()));
    }

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
                "RETREL:" + milestone.getId()
        ));

        MilestoneEntity released = milestoneRepository.findById(milestone.getId()).orElseThrow();
        assertEquals(MilestoneStatus.RETENTION_RELEASED, released.getStatus());
        assertNotNull(released.getRetentionReleasedAt());
    }
}
