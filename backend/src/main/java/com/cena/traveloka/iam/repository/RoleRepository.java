package com.cena.traveloka.iam.repository;

import com.cena.traveloka.common.enums.Status;
import com.cena.traveloka.iam.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(String name);

    Optional<Role> findByKeycloakRoleId(UUID keycloakRoleId);

    boolean existsByName(String name);

    List<Role> findByIsSystemTrue();

    List<Role> findByStatus(Status status);

    List<Role> findByRoleType(String roleType);

    List<Role> findAllByOrderByPriorityDesc();

    List<Role> findByIsSystemTrueAndStatus(Status status);

    long countByStatus(Status status);
}
