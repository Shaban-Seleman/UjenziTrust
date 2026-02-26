package com.uzenjitrust.users.api;

import com.uzenjitrust.common.security.AppRole;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record LoginResponse(
        String accessToken,
        Instant expiresAt,
        UUID userId,
        Set<AppRole> roles,
        String tokenId
) {
}
