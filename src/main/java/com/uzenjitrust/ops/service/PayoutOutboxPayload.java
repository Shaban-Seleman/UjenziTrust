package com.uzenjitrust.ops.service;

import java.util.UUID;

public record PayoutOutboxPayload(
        UUID disbursementId,
        UUID payeeId,
        String amount,
        String currency,
        String idempotencyKey
) {
}
