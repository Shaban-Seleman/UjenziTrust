package com.uzenjitrust.build.api;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ScheduleInspectionRequest(
        @NotNull(message = "projectId is required")
        UUID projectId,
        @NotNull(message = "milestoneId is required")
        UUID milestoneId,
        @NotNull(message = "scheduledAt is required")
        Instant scheduledAt,
        BigDecimal feeAmount
) {
}
