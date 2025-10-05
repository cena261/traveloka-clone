package com.cena.traveloka.iam.repository;

import com.cena.traveloka.iam.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByName(String name);

    List<Permission> findByResource(String resource);

    List<Permission> findByAction(String action);

    Optional<Permission> findByResourceAndAction(String resource, String action);

    boolean existsByName(String name);
}
