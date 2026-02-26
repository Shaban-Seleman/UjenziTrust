package com.uzenjitrust.build.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ApproveMilestoneMultiRequest(
        @NotBlank(message = "idempotencyKey is required")
        String idempotencyKey,
        @NotEmpty(message = "splits are required")
        List<Split> splits
) {
    public record Split(
            @NotBlank(message = "payeeType is required")
            String payeeType,
            @NotNull(message = "payeeId is required")
            UUID payeeId,
            @NotNull(message = "amount is required")
            BigDecimal amount,
            @NotBlank(message = "businessKey is required")
            String businessKey
    ) {
    }
}
