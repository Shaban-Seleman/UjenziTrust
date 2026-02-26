package com.uzenjitrust.ops.api;

import java.time.Instant;
import java.util.UUID;

public record SettlementWebhookRequest(
        String eventId,
        String eventType,
        Instant eventTs,
        Payload payload
) {
    public record Payload(
            UUID disbursementId,
            String settlementRef
    ) {
    }
}
