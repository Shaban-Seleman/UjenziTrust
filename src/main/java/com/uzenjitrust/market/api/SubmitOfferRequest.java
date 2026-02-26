package com.uzenjitrust.market.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SubmitOfferRequest(
        @NotNull(message = "amount is required")
        BigDecimal amount,
        @NotBlank(message = "currency is required")
        String currency,
        String notes
) {
}
