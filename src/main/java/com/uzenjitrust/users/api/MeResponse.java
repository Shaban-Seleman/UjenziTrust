package com.uzenjitrust.users.api;

import com.uzenjitrust.common.security.AppRole;

import java.util.Set;
import java.util.UUID;

public record MeResponse(UUID userId, Set<AppRole> roles) {
}
