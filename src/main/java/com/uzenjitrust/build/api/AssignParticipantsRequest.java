package com.uzenjitrust.build.api;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignParticipantsRequest(
        @NotNull(message = "contractorUserId is required")
        UUID contractorUserId,
        @NotNull(message = "inspectorUserId is required")
        UUID inspectorUserId
) {
}
