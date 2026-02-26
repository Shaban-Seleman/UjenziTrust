package com.uzenjitrust.ops.bank;

import java.math.BigDecimal;
import java.util.UUID;

public record BankPayoutRequest(
        UUID disbursementId,
        UUID payeeId,
        BigDecimal amount,
        String currency,
        String idempotencyKey
) {
}
