package com.uzenjitrust.users.repo;

import com.uzenjitrust.users.domain.UserRoleEntity;
import com.uzenjitrust.users.domain.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleId> {

    List<UserRoleEntity> findByIdUserId(UUID userId);
}
