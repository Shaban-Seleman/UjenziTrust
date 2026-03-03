package com.uzenjitrust.integration;

import com.uzenjitrust.build.domain.MilestoneEntity;
import com.uzenjitrust.build.domain.MilestoneStatus;
import com.uzenjitrust.build.domain.ProjectEntity;
import com.uzenjitrust.build.domain.ProjectStatus;
import com.uzenjitrust.build.repo.MilestoneRepository;
import com.uzenjitrust.build.repo.ProjectRepository;
import com.uzenjitrust.build.service.MilestoneService;
import com.uzenjitrust.common.error.ForbiddenException;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.support.PostgresIntegrationTest;
import com.uzenjitrust.support.TestSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BuildMilestoneReadIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private MilestoneRepository milestoneRepository;
    @Autowired
    private MilestoneService milestoneService;

    @Test
    void listByProjectOnlyAllowsAssignedActors() {
        UUID ownerId = TestSecurity.randomUser();
        UUID contractorId = TestSecurity.randomUser();
        UUID inspectorId = TestSecurity.randomUser();
        UUID outsiderId = TestSecurity.randomUser();

        ProjectEntity project = new ProjectEntity();
        project.setOwnerUserId(ownerId);
        project.setContractorUserId(contractorId);
        project.setInspectorUserId(inspectorId);
        project.setTitle("Read access project");
        project.setStatus(ProjectStatus.ACTIVE);
        project.setRetentionRate(new BigDecimal("10.00"));
        project = projectRepository.save(project);
        UUID projectId = project.getId();

        MilestoneEntity milestone = new MilestoneEntity();
        milestone.setProject(project);
        milestone.setName("Access-controlled milestone");
        milestone.setSequenceNo(1);
        milestone.setAmount(new BigDecimal("100000"));
        milestone.setRetentionAmount(new BigDecimal("10000"));
        milestone.setStatus(MilestoneStatus.PLANNED);
        milestoneRepository.save(milestone);

        TestSecurity.as(ownerId, AppRole.OWNER);
        List<MilestoneEntity> ownerView = milestoneService.listByProject(projectId);
        assertEquals(1, ownerView.size());

        TestSecurity.as(contractorId, AppRole.CONTRACTOR);
        assertEquals(1, milestoneService.listByProject(projectId).size());

        TestSecurity.as(inspectorId, AppRole.INSPECTOR);
        assertEquals(1, milestoneService.listByProject(projectId).size());

        TestSecurity.as(outsiderId, AppRole.OWNER);
        assertThrows(ForbiddenException.class, () -> milestoneService.listByProject(projectId));
    }
}
