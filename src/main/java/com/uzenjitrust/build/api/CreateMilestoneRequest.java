package com.uzenjitrust.build.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateMilestoneRequest(
        @NotBlank(message = "name is required")
        String name,
        String description,
        @NotNull(message = "sequenceNo is required")
        Integer sequenceNo,
        @NotNull(message = "amount is required")
        BigDecimal amount,
        BigDecimal retentionAmount,
        LocalDate dueDate
) {
}
