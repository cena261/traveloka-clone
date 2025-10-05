package com.cena.traveloka.iam.repository;

import com.cena.traveloka.common.enums.Status;
import com.cena.traveloka.iam.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByKeycloakId(UUID keycloakId);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findByStatus(Status status);

    List<User> findByAccountLockedTrue();

    List<User> findByFailedLoginAttemptsGreaterThanEqual(int threshold);

    List<User> findByStatusAndLockedUntilBefore(Status status, java.time.OffsetDateTime lockedUntil);

    long countByStatus(Status status);
}
