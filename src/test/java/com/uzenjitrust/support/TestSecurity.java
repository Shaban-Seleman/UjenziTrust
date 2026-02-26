package com.uzenjitrust.support;

import com.uzenjitrust.common.security.ActorPrincipal;
import com.uzenjitrust.common.security.AppRole;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

public final class TestSecurity {

    private TestSecurity() {
    }

    public static UUID randomUser() {
        return UUID.randomUUID();
    }

    public static void as(UUID userId, AppRole... roles) {
        ActorPrincipal principal = new ActorPrincipal(userId, Set.copyOf(Arrays.asList(roles)));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
