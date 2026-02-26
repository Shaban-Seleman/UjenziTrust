package com.uzenjitrust.ledger.service;

import java.util.List;
import java.util.UUID;

public record LedgerPostingRequest(
        String entryType,
        String referenceId,
        String idempotencyKey,
        String description,
        UUID actorUserId,
        List<LedgerPostingLine> lines
) {
}
