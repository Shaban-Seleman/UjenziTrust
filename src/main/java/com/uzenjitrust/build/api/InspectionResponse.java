package com.uzenjitrust.build.api;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record InspectionResponse(
        UUID inspectionId,
        UUID milestoneId,
        Instant scheduledAt,
        UUID inspectorId,
        String status,
        String result,
        JsonNode reportJson,
        Instant completedAt,
        Instant createdAt
) {
}
