package com.uzenjitrust.users.repo;

import com.uzenjitrust.users.domain.UserEntity;
import com.uzenjitrust.users.domain.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    Optional<UserEntity> findByPhone(String phone);

    @Query("""
            select distinct u
            from UserEntity u, UserRoleEntity ur
            where ur.id.userId = u.id
              and ur.id.role = :role
              and u.status = :status
            order by u.email asc, u.phone asc
            """)
    List<UserEntity> findByRoleAndStatus(@Param("role") String role, @Param("status") UserStatus status);
}
