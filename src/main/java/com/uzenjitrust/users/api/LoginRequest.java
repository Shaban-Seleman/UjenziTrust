package com.uzenjitrust.users.api;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "identifier is required")
        String identifier,
        String password
) {
}
