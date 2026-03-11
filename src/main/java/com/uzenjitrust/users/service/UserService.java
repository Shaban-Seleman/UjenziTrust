package com.uzenjitrust.users.service;

import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.common.security.AuthorizationService;
import com.uzenjitrust.common.error.NotFoundException;
import com.uzenjitrust.users.domain.UserEntity;
import com.uzenjitrust.users.domain.UserStatus;
import com.uzenjitrust.users.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;

    public UserService(UserRepository userRepository,
                       AuthorizationService authorizationService) {
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
    }

    public UserEntity findByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new NotFoundException("User not found");
        }
        return identifier.contains("@")
                ? userRepository.findByEmailIgnoreCase(identifier.trim()).orElseThrow(() -> new NotFoundException("User not found"))
                : userRepository.findByPhone(identifier.trim()).orElseThrow(() -> new NotFoundException("User not found"));
    }

    public List<UserDirectoryEntry> listActiveUsersByRole(AppRole role) {
        authorizationService.requireRole(AppRole.OWNER, AppRole.ADMIN);
        return userRepository.findByRoleAndStatus(role.name(), UserStatus.ACTIVE).stream()
                .map(user -> new UserDirectoryEntry(user.getId(), user.getEmail(), user.getPhone(), role))
                .toList();
    }

    public record UserDirectoryEntry(java.util.UUID id, String email, String phone, AppRole role) {
    }
}
