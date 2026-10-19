package com.hrgenius.repository;

import com.hrgenius.entity.Role;
import com.hrgenius.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);

    /** Finds a user by username, or by email when the identifier looks like an email address. */
    default Optional<User> findByUsernameOrEmail(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        String id = identifier.trim();
        return id.contains("@") ? findByEmailIgnoreCase(id) : findByUsername(id);
    }

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = :role AND u.enabled = true")
    List<User> findActiveByRole(@Param("role") Role.RoleName role);
}
