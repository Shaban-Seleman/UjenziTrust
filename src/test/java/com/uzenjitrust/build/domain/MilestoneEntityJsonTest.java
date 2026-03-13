package com.uzenjitrust.build.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MilestoneEntityJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void serializationExposesProjectIdWithoutSerializingLazyProjectAssociation() throws Exception {
        ProjectEntity project = new ProjectEntity();
        UUID projectId = UUID.randomUUID();
        project.setOwnerUserId(UUID.randomUUID());
        project.setTitle("Project");
        project.setStatus(ProjectStatus.ACTIVE);
        project.setRetentionRate(new BigDecimal("10.00"));
        setProjectId(project, projectId);

        MilestoneEntity milestone = new MilestoneEntity();
        milestone.setProject(project);
        milestone.setName("Milestone");
        milestone.setSequenceNo(1);
        milestone.setAmount(new BigDecimal("1000"));
        milestone.setRetentionAmount(new BigDecimal("100"));
        milestone.setStatus(MilestoneStatus.PLANNED);
        milestone.setInspectionStatus("COMPLETED");
        milestone.setInspectionResult("PASS");
        milestone.setInspectionCompletedAt(Instant.parse("2026-03-11T10:15:30Z"));
        UUID inspectionId = UUID.randomUUID();
        milestone.setInspectionId(inspectionId);

        var json = objectMapper.readTree(objectMapper.writeValueAsBytes(milestone));

        assertEquals(projectId.toString(), json.get("projectId").asText());
        assertEquals("COMPLETED", json.get("inspectionStatus").asText());
        assertEquals("PASS", json.get("inspectionResult").asText());
        assertEquals(inspectionId.toString(), json.get("inspectionId").asText());
        assertFalse(json.has("project"));
    }

    private static void setProjectId(ProjectEntity project, UUID projectId) throws Exception {
        var field = ProjectEntity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(project, projectId);
    }
}
