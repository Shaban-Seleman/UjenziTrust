package com.uzenjitrust.users.service;

import com.uzenjitrust.common.error.NotFoundException;
import com.uzenjitrust.users.domain.UserEntity;
import com.uzenjitrust.users.repo.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserEntity findByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new NotFoundException("User not found");
        }
        return identifier.contains("@")
                ? userRepository.findByEmailIgnoreCase(identifier.trim()).orElseThrow(() -> new NotFoundException("User not found"))
                : userRepository.findByPhone(identifier.trim()).orElseThrow(() -> new NotFoundException("User not found"));
    }
}
