package com.uzenjitrust.market.api;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CounterOfferRequest(
        @NotNull(message = "amount is required")
        BigDecimal amount,
        String notes
) {
}
