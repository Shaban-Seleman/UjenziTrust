package com.uzenjitrust.market.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreatePropertyRequest(
        @NotBlank(message = "title is required")
        String title,
        String description,
        String location,
        @NotNull(message = "askingPrice is required")
        BigDecimal askingPrice,
        @NotBlank(message = "currency is required")
        String currency
) {
}
