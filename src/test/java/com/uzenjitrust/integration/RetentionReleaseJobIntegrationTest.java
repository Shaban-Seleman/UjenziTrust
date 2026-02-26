package com.uzenjitrust.integration;

import com.uzenjitrust.build.domain.MilestoneEntity;
import com.uzenjitrust.build.domain.MilestoneStatus;
import com.uzenjitrust.build.domain.ProjectEntity;
import com.uzenjitrust.build.domain.ProjectStatus;
import com.uzenjitrust.build.orchestrator.RetentionOrchestrator;
import com.uzenjitrust.build.repo.MilestoneRepository;
import com.uzenjitrust.build.repo.ProjectRepository;
import com.uzenjitrust.ledger.repo.JournalEntryRepository;
import com.uzenjitrust.ops.domain.DisbursementOrderEntity;
import com.uzenjitrust.ops.domain.EscrowEntity;
import com.uzenjitrust.ops.repo.DisbursementOrderRepository;
import com.uzenjitrust.ops.service.EscrowService;
import com.uzenjitrust.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RetentionReleaseJobIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private MilestoneRepository milestoneRepository;
    @Autowired
    private EscrowService escrowService;
    @Autowired
    private RetentionOrchestrator retentionOrchestrator;
    @Autowired
    private DisbursementOrderRepository disbursementRepository;
    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Test
    void retentionReleaseJobCreatesDisbursementAndLedgerEntry() {
        UUID owner = UUID.randomUUID();
        UUID contractor = UUID.randomUUID();

        EscrowEntity escrow = escrowService.createEscrowIdempotent(
                "CONSTRUCTION_PROJECT:test-proj-ret",
                "CONSTRUCTION_PROJECT",
                new BigDecimal("4000000"),
                "TZS",
                owner,
                contractor
        );

        ProjectEntity project = new ProjectEntity();
        project.setOwnerUserId(owner);
        project.setContractorUserId(contractor);
        project.setInspectorUserId(UUID.randomUUID());
        project.setEscrowId(escrow.getId());
        project.setTitle("Retention Project");
        project.setStatus(ProjectStatus.ACTIVE);
        project.setRetentionRate(new BigDecimal("10.00"));
        project = projectRepository.save(project);

        MilestoneEntity milestone = new MilestoneEntity();
        milestone.setProject(project);
        milestone.setName("Finishing");
        milestone.setSequenceNo(2);
        milestone.setAmount(new BigDecimal("2000000"));
        milestone.setRetentionAmount(new BigDecimal("200000"));
        milestone.setStatus(MilestoneStatus.PAID);
        milestone.setPaidAt(Instant.now().minus(20, ChronoUnit.DAYS));
        milestone.setRetentionReleaseAt(Instant.now().minus(1, ChronoUnit.HOURS));
        milestone = milestoneRepository.save(milestone);

        int released = retentionOrchestrator.releaseDueRetentionsSystem();
        assertEquals(1, released);

        DisbursementOrderEntity disbursement = disbursementRepository
                .findByBusinessKey("RETENTION:" + milestone.getId())
                .orElseThrow();
        assertEquals(0, new BigDecimal("200000").compareTo(disbursement.getAmount()));

        assertNotNull(journalEntryRepository.findByEntryTypeAndReferenceIdAndIdempotencyKey(
                "RETENTION_RELEASE_AUTHORIZED",
                milestone.getId().toString(),
                "RETENTION_RELEASE_JOB:" + milestone.getId()
        ).orElse(null));

        MilestoneEntity updated = milestoneRepository.findById(milestone.getId()).orElseThrow();
        assertEquals(MilestoneStatus.RETENTION_RELEASED, updated.getStatus());
    }
}
