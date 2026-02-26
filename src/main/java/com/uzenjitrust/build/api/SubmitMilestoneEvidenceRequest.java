package com.uzenjitrust.build.api;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record SubmitMilestoneEvidenceRequest(
        @NotNull(message = "evidence is required")
        Map<String, Object> evidence,
        String notes
) {
}
