package com.uzenjitrust.build.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MilestoneEntityJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

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

        var json = objectMapper.readTree(objectMapper.writeValueAsBytes(milestone));

        assertEquals(projectId.toString(), json.get("projectId").asText());
        assertFalse(json.has("project"));
    }

    private static void setProjectId(ProjectEntity project, UUID projectId) throws Exception {
        var field = ProjectEntity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(project, projectId);
    }
}
