package com.uzenjitrust.build.api;

import jakarta.validation.constraints.NotBlank;

public record ReleaseRetentionRequest(
        @NotBlank(message = "idempotencyKey is required")
        String idempotencyKey
) {
}
