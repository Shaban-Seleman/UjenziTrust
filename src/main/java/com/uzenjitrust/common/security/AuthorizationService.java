package com.uzenjitrust.common.security;

import com.uzenjitrust.common.error.ForbiddenException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;

@Service
public class AuthorizationService {

    private final ActorProvider actorProvider;

    public AuthorizationService(ActorProvider actorProvider) {
        this.actorProvider = actorProvider;
    }

    public ActorPrincipal requireRole(AppRole... allowedRoles) {
        ActorPrincipal actor = actorProvider.requireActor();
        boolean authorized = Arrays.stream(allowedRoles).anyMatch(actor.roles()::contains);
        if (!authorized) {
            throw new ForbiddenException("Insufficient role");
        }
        return actor;
    }

    public ActorPrincipal requireAdmin() {
        return requireRole(AppRole.ADMIN);
    }

    public ActorPrincipal requireOwner(UUID ownerUserId) {
        ActorPrincipal actor = requireRole(AppRole.OWNER);
        if (!actor.userId().equals(ownerUserId)) {
            throw new ForbiddenException("Actor is not the resource owner");
        }
        return actor;
    }

    public ActorPrincipal requireSeller(UUID sellerUserId) {
        ActorPrincipal actor = requireRole(AppRole.SELLER);
        if (!actor.userId().equals(sellerUserId)) {
            throw new ForbiddenException("Actor is not the property seller");
        }
        return actor;
    }

    public ActorPrincipal requireBuyer(UUID buyerUserId) {
        ActorPrincipal actor = requireRole(AppRole.BUYER);
        if (!actor.userId().equals(buyerUserId)) {
            throw new ForbiddenException("Actor is not the offer buyer");
        }
        return actor;
    }

    public ActorPrincipal requireContractor(UUID contractorUserId) {
        ActorPrincipal actor = requireRole(AppRole.CONTRACTOR);
        if (!actor.userId().equals(contractorUserId)) {
            throw new ForbiddenException("Actor is not the project contractor");
        }
        return actor;
    }

    public ActorPrincipal requireInspector(UUID inspectorUserId) {
        ActorPrincipal actor = requireRole(AppRole.INSPECTOR);
        if (!actor.userId().equals(inspectorUserId)) {
            throw new ForbiddenException("Actor is not the assigned inspector");
        }
        return actor;
    }
}
