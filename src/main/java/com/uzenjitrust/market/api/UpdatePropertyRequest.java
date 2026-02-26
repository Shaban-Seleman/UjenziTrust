package com.uzenjitrust.market.api;

import java.math.BigDecimal;

public record UpdatePropertyRequest(
        String title,
        String description,
        String location,
        BigDecimal askingPrice,
        String currency
) {
}
