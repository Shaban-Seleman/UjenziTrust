package com.uzenjitrust.ledger.service;

import com.uzenjitrust.ledger.domain.LineType;

import java.math.BigDecimal;

public record LedgerPostingLine(
        String accountCode,
        LineType lineType,
        BigDecimal amount,
        String currency
) {
}
