package com.uzenjitrust.users.api;

import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@Tag(name = "Users")
public class UserDirectoryController {

    private final UserService userService;

    public UserDirectoryController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/directory")
    @Operation(summary = "List active users by role for assignment flows")
    public ResponseEntity<List<UserDirectoryResponse>> listByRole(@RequestParam AppRole role) {
        return ResponseEntity.ok(userService.listActiveUsersByRole(role).stream()
                .map(user -> new UserDirectoryResponse(user.id(), user.email(), user.phone(), user.role()))
                .toList());
    }

    public record UserDirectoryResponse(UUID id, String email, String phone, AppRole role) {
    }
}
