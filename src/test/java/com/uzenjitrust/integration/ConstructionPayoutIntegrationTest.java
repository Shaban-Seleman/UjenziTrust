package com.uzenjitrust.integration;

import com.uzenjitrust.build.domain.MilestoneEntity;
import com.uzenjitrust.build.domain.MilestoneStatus;
import com.uzenjitrust.build.domain.ProjectEntity;
import com.uzenjitrust.build.domain.ProjectStatus;
import com.uzenjitrust.build.orchestrator.MilestoneOrchestrator;
import com.uzenjitrust.build.repo.MilestoneRepository;
import com.uzenjitrust.build.repo.ProjectRepository;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.ledger.repo.JournalEntryRepository;
import com.uzenjitrust.ops.domain.DisbursementOrderEntity;
import com.uzenjitrust.ops.domain.EscrowEntity;
import com.uzenjitrust.ops.repo.DisbursementOrderRepository;
import com.uzenjitrust.ops.service.EscrowService;
import com.uzenjitrust.support.PostgresIntegrationTest;
import com.uzenjitrust.support.TestSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConstructionPayoutIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private MilestoneRepository milestoneRepository;
    @Autowired
    private EscrowService escrowService;
    @Autowired
    private MilestoneOrchestrator milestoneOrchestrator;
    @Autowired
    private DisbursementOrderRepository disbursementRepository;
    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Test
    void milestoneApprovalCreatesDisbursementAndLedgerEntry() {
        UUID ownerId = TestSecurity.randomUser();
        UUID contractorId = TestSecurity.randomUser();

        EscrowEntity escrow = escrowService.createEscrowIdempotent(
                "CONSTRUCTION_PROJECT:test-proj-1",
                "CONSTRUCTION_PROJECT",
                new BigDecimal("5000000"),
                "TZS",
                ownerId,
                contractorId
        );

        ProjectEntity project = new ProjectEntity();
        project.setOwnerUserId(ownerId);
        project.setContractorUserId(contractorId);
        project.setInspectorUserId(UUID.randomUUID());
        project.setEscrowId(escrow.getId());
        project.setTitle("House Build");
        project.setDescription("3-bedroom home");
        project.setStatus(ProjectStatus.ACTIVE);
        project.setRetentionRate(new BigDecimal("10.00"));
        project = projectRepository.save(project);

        MilestoneEntity milestone = new MilestoneEntity();
        milestone.setProject(project);
        milestone.setName("Foundation");
        milestone.setSequenceNo(1);
        milestone.setAmount(new BigDecimal("1000000"));
        milestone.setRetentionAmount(new BigDecimal("100000"));
        milestone.setStatus(MilestoneStatus.SUBMITTED);
        milestone = milestoneRepository.save(milestone);

        TestSecurity.as(ownerId, AppRole.OWNER);
        milestoneOrchestrator.approveMilestoneSingle(milestone.getId(), "single-1");

        DisbursementOrderEntity disbursement = disbursementRepository
                .findByBusinessKey("MILESTONE_SINGLE:" + milestone.getId())
                .orElseThrow();

        assertNotNull(disbursement.getId());
        assertEquals(0, new BigDecimal("900000").compareTo(disbursement.getAmount()));

        assertNotNull(journalEntryRepository.findByEntryTypeAndReferenceIdAndIdempotencyKey(
                "CONSTRUCTION_MILESTONE_AUTHORIZED",
                milestone.getId().toString(),
                "MILESTONE_AUTH_SINGLE:single-1"
        ).orElse(null));
    }
}
