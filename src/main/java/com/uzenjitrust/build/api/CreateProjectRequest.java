package com.uzenjitrust.build.api;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateProjectRequest(
        @NotBlank(message = "title is required")
        String title,
        String description,
        BigDecimal totalBudget,
        String currency,
        BigDecimal retentionRate
) {
}
