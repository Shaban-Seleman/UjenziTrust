package com.uzenjitrust.common.error;

import java.time.Instant;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String Detail,
        String correlationId,
        String path
) {
}
