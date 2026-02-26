package com.uzenjitrust.users.service;

import com.uzenjitrust.common.error.BadRequestException;
import com.uzenjitrust.common.error.ForbiddenException;
import com.uzenjitrust.common.error.UnauthorizedException;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.common.security.JwtService;
import com.uzenjitrust.common.security.SecurityProperties;
import com.uzenjitrust.users.api.LoginRequest;
import com.uzenjitrust.users.api.LoginResponse;
import com.uzenjitrust.users.domain.UserEntity;
import com.uzenjitrust.users.domain.UserRoleEntity;
import com.uzenjitrust.users.domain.UserSessionEntity;
import com.uzenjitrust.users.domain.UserStatus;
import com.uzenjitrust.users.repo.UserRoleRepository;
import com.uzenjitrust.users.repo.UserSessionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserService userService;
    private final UserRoleRepository userRoleRepository;
    private final UserSessionRepository sessionRepository;
    private final JwtService jwtService;
    private final SecurityProperties securityProperties;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserService userService,
                       UserRoleRepository userRoleRepository,
                       UserSessionRepository sessionRepository,
                       JwtService jwtService,
                       SecurityProperties securityProperties,
                       PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.userRoleRepository = userRoleRepository;
        this.sessionRepository = sessionRepository;
        this.jwtService = jwtService;
        this.securityProperties = securityProperties;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        if (request.identifier() == null || request.identifier().isBlank()) {
            throw new BadRequestException("identifier is required");
        }

        UserEntity user = userService.findByIdentifier(request.identifier());
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException("User is not active");
        }

        if (request.password() != null && user.getPasswordHash() != null) {
            if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                throw new UnauthorizedException("Invalid credentials");
            }
        } else if (!securityProperties.isDevLoginEnabled()) {
            throw new UnauthorizedException("Password authentication is required");
        }

        Set<AppRole> roles = userRoleRepository.findByIdUserId(user.getId()).stream()
                .map(UserRoleEntity::getId)
                .map(id -> AppRole.valueOf(id.getRole()))
                .collect(Collectors.toSet());

        if (roles.isEmpty()) {
            throw new ForbiddenException("User has no roles assigned");
        }

        JwtService.JwtToken token = jwtService.issueToken(user.getId(), roles);

        UserSessionEntity session = new UserSessionEntity();
        session.setUserId(user.getId());
        session.setTokenId(token.tokenId());
        session.setExpiresAt(token.expiresAt());
        sessionRepository.save(session);

        return new LoginResponse(token.token(), token.expiresAt(), user.getId(), roles, token.tokenId());
    }
}
