package com.uzenjitrust.common.security;

import java.util.Set;
import java.util.UUID;

public record ActorPrincipal(UUID userId, Set<AppRole> roles) {
}
