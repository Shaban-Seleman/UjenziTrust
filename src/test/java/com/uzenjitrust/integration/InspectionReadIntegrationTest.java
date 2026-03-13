package com.uzenjitrust.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uzenjitrust.build.api.InspectionResponse;
import com.uzenjitrust.build.domain.InspectionEntity;
import com.uzenjitrust.build.domain.InspectionStatus;
import com.uzenjitrust.build.domain.MilestoneEntity;
import com.uzenjitrust.build.domain.MilestoneStatus;
import com.uzenjitrust.build.domain.ProjectEntity;
import com.uzenjitrust.build.domain.ProjectStatus;
import com.uzenjitrust.build.repo.InspectionRepository;
import com.uzenjitrust.build.repo.MilestoneRepository;
import com.uzenjitrust.build.repo.ProjectRepository;
import com.uzenjitrust.build.service.InspectionService;
import com.uzenjitrust.build.service.MilestoneService;
import com.uzenjitrust.common.error.ForbiddenException;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.support.PostgresIntegrationTest;
import com.uzenjitrust.support.TestSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InspectionReadIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private MilestoneRepository milestoneRepository;
    @Autowired
    private InspectionRepository inspectionRepository;
    @Autowired
    private InspectionService inspectionService;
    @Autowired
    private MilestoneService milestoneService;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listInspectionsByMilestoneReturnsVisibleInspections() throws Exception {
        UUID ownerId = TestSecurity.randomUser();
        UUID contractorId = TestSecurity.randomUser();
        UUID inspectorId = TestSecurity.randomUser();

        MilestoneEntity milestone = createMilestone(ownerId, contractorId, inspectorId);
        InspectionEntity older = createInspection(milestone, inspectorId, "PASS", Instant.parse("2026-03-01T10:00:00Z"));
        InspectionEntity newer = createInspection(milestone, inspectorId, "FAIL", Instant.parse("2026-03-02T10:00:00Z"));

        TestSecurity.as(ownerId, AppRole.OWNER);
        List<InspectionResponse> inspections = inspectionService.listByMilestone(milestone.getId());

        assertEquals(2, inspections.size());
        assertEquals(newer.getId(), inspections.get(0).inspectionId());
        assertEquals("FAIL", inspections.get(0).result());
        assertEquals(older.getId(), inspections.get(1).inspectionId());
        assertEquals("PASS", inspections.get(1).result());
    }

    @Test
    void getInspectionByIdReturnsVisibleInspection() throws Exception {
        UUID ownerId = TestSecurity.randomUser();
        UUID contractorId = TestSecurity.randomUser();
        UUID inspectorId = TestSecurity.randomUser();

        MilestoneEntity milestone = createMilestone(ownerId, contractorId, inspectorId);
        InspectionEntity inspection = createInspection(milestone, inspectorId, "PASS", Instant.parse("2026-03-03T11:00:00Z"));

        TestSecurity.as(inspectorId, AppRole.INSPECTOR);
        InspectionResponse response = inspectionService.getVisibleById(inspection.getId());

        assertEquals(inspection.getId(), response.inspectionId());
        assertEquals(milestone.getId(), response.milestoneId());
        assertEquals("COMPLETED", response.status());
        assertEquals("PASS", response.result());
    }

    @Test
    void unauthorizedUserCannotReadInspection() throws Exception {
        UUID ownerId = TestSecurity.randomUser();
        UUID contractorId = TestSecurity.randomUser();
        UUID inspectorId = TestSecurity.randomUser();
        UUID outsiderId = TestSecurity.randomUser();

        MilestoneEntity milestone = createMilestone(ownerId, contractorId, inspectorId);
        InspectionEntity inspection = createInspection(milestone, inspectorId, "PASS", Instant.parse("2026-03-04T12:00:00Z"));

        TestSecurity.as(outsiderId, AppRole.OWNER);
        assertThrows(ForbiddenException.class, () -> inspectionService.getVisibleById(inspection.getId()));
        assertThrows(ForbiddenException.class, () -> inspectionService.listByMilestone(milestone.getId()));
    }

    @Test
    void milestoneReadIncludesLatestInspectionSummary() throws Exception {
        UUID ownerId = TestSecurity.randomUser();
        UUID contractorId = TestSecurity.randomUser();
        UUID inspectorId = TestSecurity.randomUser();

        MilestoneEntity milestone = createMilestone(ownerId, contractorId, inspectorId);
        createInspection(milestone, inspectorId, "PASS", Instant.parse("2026-03-01T09:00:00Z"));
        InspectionEntity latest = createInspection(milestone, inspectorId, "FAIL", Instant.parse("2026-03-05T09:00:00Z"));

        TestSecurity.as(contractorId, AppRole.CONTRACTOR);
        MilestoneEntity visible = milestoneService.getVisibleById(milestone.getId());

        assertEquals(latest.getId(), visible.getInspectionId());
        assertEquals("COMPLETED", visible.getInspectionStatus());
        assertEquals("FAIL", visible.getInspectionResult());
        assertEquals(latest.getCompletedAt(), visible.getInspectionCompletedAt());
    }

    @Test
    void milestoneWithoutInspectionHasEmptySummary() {
        UUID ownerId = TestSecurity.randomUser();
        UUID contractorId = TestSecurity.randomUser();
        UUID inspectorId = TestSecurity.randomUser();

        MilestoneEntity milestone = createMilestone(ownerId, contractorId, inspectorId);

        TestSecurity.as(ownerId, AppRole.OWNER);
        MilestoneEntity visible = milestoneService.getVisibleById(milestone.getId());

        assertNull(visible.getInspectionId());
        assertNull(visible.getInspectionStatus());
        assertNull(visible.getInspectionResult());
        assertNull(visible.getInspectionCompletedAt());
    }

    private MilestoneEntity createMilestone(UUID ownerId, UUID contractorId, UUID inspectorId) {
        ProjectEntity project = new ProjectEntity();
        project.setOwnerUserId(ownerId);
        project.setContractorUserId(contractorId);
        project.setInspectorUserId(inspectorId);
        project.setTitle("Inspection Project");
        project.setStatus(ProjectStatus.ACTIVE);
        project.setRetentionRate(new BigDecimal("10.00"));
        project = projectRepository.save(project);

        MilestoneEntity milestone = new MilestoneEntity();
        milestone.setProject(project);
        milestone.setName("Inspection milestone");
        milestone.setSequenceNo(1);
        milestone.setAmount(new BigDecimal("100000"));
        milestone.setRetentionAmount(new BigDecimal("10000"));
        milestone.setStatus(MilestoneStatus.SUBMITTED);
        return milestoneRepository.save(milestone);
    }

    private InspectionEntity createInspection(MilestoneEntity milestone,
                                              UUID inspectorId,
                                              String result,
                                              Instant createdAt) throws Exception {
        InspectionEntity inspection = new InspectionEntity();
        inspection.setProject(milestone.getProject());
        inspection.setMilestone(milestone);
        inspection.setInspectorUserId(inspectorId);
        inspection.setStatus(InspectionStatus.COMPLETED);
        inspection.setScheduledAt(createdAt.minusSeconds(3600));
        inspection.setCompletedAt(createdAt);
        inspection.setReport(objectMapper.readTree("{\"result\":\"" + result + "\"}"));
        inspection = inspectionRepository.save(inspection);

        var createdAtField = InspectionEntity.class.getDeclaredField("createdAt");
        createdAtField.setAccessible(true);
        createdAtField.set(inspection, createdAt);

        var updatedAtField = InspectionEntity.class.getDeclaredField("updatedAt");
        updatedAtField.setAccessible(true);
        updatedAtField.set(inspection, createdAt);

        return inspectionRepository.save(inspection);
    }
}
