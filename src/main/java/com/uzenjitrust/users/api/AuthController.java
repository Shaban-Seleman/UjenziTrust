package com.uzenjitrust.users.api;

import com.uzenjitrust.common.security.ActorProvider;
import com.uzenjitrust.common.security.ActorPrincipal;
import com.uzenjitrust.users.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;
    private final ActorProvider actorProvider;

    public AuthController(AuthService authService, ActorProvider actorProvider) {
        this.authService = authService;
        this.actorProvider = actorProvider;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated actor")
    public ResponseEntity<MeResponse> me() {
        ActorPrincipal actor = actorProvider.requireActor();
        return ResponseEntity.ok(new MeResponse(actor.userId(), actor.roles()));
    }
}
