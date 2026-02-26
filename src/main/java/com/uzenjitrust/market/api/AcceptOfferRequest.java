package com.uzenjitrust.market.api;

import jakarta.validation.constraints.NotBlank;

public record AcceptOfferRequest(
        @NotBlank(message = "idempotencyKey is required")
        String idempotencyKey,
        String notes
) {
}
