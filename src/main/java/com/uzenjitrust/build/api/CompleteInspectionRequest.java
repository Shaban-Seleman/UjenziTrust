package com.uzenjitrust.build.api;

import jakarta.validation.constraints.NotBlank;

public record CompleteInspectionRequest(
        @NotBlank(message = "reportJson is required")
        String reportJson
) {
}
