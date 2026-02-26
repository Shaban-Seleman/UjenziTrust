package com.uzenjitrust.ledger.service;

import java.time.Instant;
import java.util.UUID;

public record LedgerPostingResult(
        UUID journalEntryId,
        long chainIndex,
        String hash,
        String prevHash,
        Instant createdAt,
        boolean reused
) {
}
