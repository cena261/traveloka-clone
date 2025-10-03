package com.cena.traveloka.iam.repository;

import com.cena.traveloka.common.enums.Status;
import com.cena.traveloka.iam.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T005: RoleRepository query test
 * Tests role repository queries including findByName and status filtering.
 *
 * TDD Phase: RED - These tests MUST fail before implementing RoleRepository
 *
 * Constitutional Compliance:
 * - Principle IV: Entity Immutability - Role entity is READ-ONLY, not modified here
 * - Principle VII: Test-First Development - Tests written before repository implementation
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("RoleRepository Tests")
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    private Role adminRole;
    private Role customerRole;
    private Role partnerRole;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        roleRepository.deleteAll();

        // Create test roles following FR-005 requirements
        adminRole = Role.builder()
                .keycloakRoleId(UUID.randomUUID())
                .name("ADMIN")
                .displayName("Administrator")
                .description("System administrator with full access")
                .roleType("system")
                .isSystem(true)
                .priority(100)
                .status(Status.active)
                .createdAt(OffsetDateTime.now())
                .build();

        customerRole = Role.builder()
                .keycloakRoleId(UUID.randomUUID())
                .name("CUSTOMER")
                .displayName("Customer")
                .description("Regular customer role")
                .roleType("user")
                .isSystem(true)
                .priority(10)
                .status(Status.active)
                .createdAt(OffsetDateTime.now())
                .build();

        partnerRole = Role.builder()
                .keycloakRoleId(UUID.randomUUID())
                .name("PARTNER_ADMIN")
                .displayName("Partner Administrator")
                .description("Partner admin role")
                .roleType("partner")
                .isSystem(false)
                .priority(50)
                .status(Status.active)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should save role successfully")
    void shouldSaveRole() {
        // When
        Role savedRole = roleRepository.save(adminRole);

        // Then
        assertThat(savedRole).isNotNull();
        assertThat(savedRole.getId()).isNotNull();
        assertThat(savedRole.getName()).isEqualTo("ADMIN");
        assertThat(savedRole.getIsSystem()).isTrue();
    }

    @Test
    @DisplayName("Should find role by name")
    void shouldFindRoleByName() {
        // Given
        roleRepository.save(adminRole);
        roleRepository.save(customerRole);

        // When
        Optional<Role> foundAdmin = roleRepository.findByName("ADMIN");
        Optional<Role> foundCustomer = roleRepository.findByName("CUSTOMER");
        Optional<Role> notFound = roleRepository.findByName("NONEXISTENT");

        // Then
        assertThat(foundAdmin).isPresent();
        assertThat(foundAdmin.get().getDisplayName()).isEqualTo("Administrator");
        assertThat(foundCustomer).isPresent();
        assertThat(foundCustomer.get().getDisplayName()).isEqualTo("Customer");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("Should find role by Keycloak role ID")
    void shouldFindRoleByKeycloakRoleId() {
        // Given
        Role savedRole = roleRepository.save(adminRole);

        // When
        Optional<Role> foundRole = roleRepository.findByKeycloakRoleId(savedRole.getKeycloakRoleId());

        // Then
        assertThat(foundRole).isPresent();
        assertThat(foundRole.get().getName()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Should check if role name exists")
    void shouldCheckRoleNameExists() {
        // Given
        roleRepository.save(adminRole);

        // When
        boolean exists = roleRepository.existsByName("ADMIN");
        boolean notExists = roleRepository.existsByName("NONEXISTENT");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should find all system roles")
    void shouldFindSystemRoles() {
        // Given
        roleRepository.save(adminRole);
        roleRepository.save(customerRole);
        roleRepository.save(partnerRole);

        // When
        List<Role> systemRoles = roleRepository.findByIsSystemTrue();

        // Then
        assertThat(systemRoles).hasSize(2);
        assertThat(systemRoles)
                .extracting(Role::getName)
                .containsExactlyInAnyOrder("ADMIN", "CUSTOMER");
    }

    @Test
    @DisplayName("Should find roles by status")
    void shouldFindRolesByStatus() {
        // Given
        roleRepository.save(adminRole);
        roleRepository.save(customerRole);

        // Create inactive role
        Role inactiveRole = Role.builder()
                .keycloakRoleId(UUID.randomUUID())
                .name("INACTIVE_ROLE")
                .displayName("Inactive Role")
                .status(Status.inactive)
                .createdAt(OffsetDateTime.now())
                .build();
        roleRepository.save(inactiveRole);

        // When
        List<Role> activeRoles = roleRepository.findByStatus(Status.active);
        List<Role> inactiveRoles = roleRepository.findByStatus(Status.inactive);

        // Then
        assertThat(activeRoles).hasSize(2);
        assertThat(inactiveRoles).hasSize(1);
        assertThat(inactiveRoles.get(0).getName()).isEqualTo("INACTIVE_ROLE");
    }

    @Test
    @DisplayName("Should find roles by type")
    void shouldFindRolesByType() {
        // Given
        roleRepository.save(adminRole);
        roleRepository.save(customerRole);
        roleRepository.save(partnerRole);

        // When
        List<Role> systemTypeRoles = roleRepository.findByRoleType("system");
        List<Role> partnerTypeRoles = roleRepository.findByRoleType("partner");

        // Then
        assertThat(systemTypeRoles).hasSize(1);
        assertThat(systemTypeRoles.get(0).getName()).isEqualTo("ADMIN");
        assertThat(partnerTypeRoles).hasSize(1);
        assertThat(partnerTypeRoles.get(0).getName()).isEqualTo("PARTNER_ADMIN");
    }

    @Test
    @DisplayName("Should find roles ordered by priority descending")
    void shouldFindRolesOrderedByPriority() {
        // Given
        roleRepository.save(customerRole);  // priority 10
        roleRepository.save(partnerRole);   // priority 50
        roleRepository.save(adminRole);     // priority 100

        // When
        List<Role> rolesOrderedByPriority = roleRepository.findAllByOrderByPriorityDesc();

        // Then
        assertThat(rolesOrderedByPriority).hasSize(3);
        assertThat(rolesOrderedByPriority.get(0).getName()).isEqualTo("ADMIN");
        assertThat(rolesOrderedByPriority.get(1).getName()).isEqualTo("PARTNER_ADMIN");
        assertThat(rolesOrderedByPriority.get(2).getName()).isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("Should find active system roles")
    void shouldFindActiveSystemRoles() {
        // Given
        roleRepository.save(adminRole);
        roleRepository.save(customerRole);

        // Create inactive system role
        Role inactiveSystemRole = Role.builder()
                .keycloakRoleId(UUID.randomUUID())
                .name("SUPER_ADMIN")
                .displayName("Super Administrator")
                .isSystem(true)
                .status(Status.inactive)
                .createdAt(OffsetDateTime.now())
                .build();
        roleRepository.save(inactiveSystemRole);

        // When
        List<Role> activeSystemRoles = roleRepository.findByIsSystemTrueAndStatus(Status.active);

        // Then
        assertThat(activeSystemRoles).hasSize(2);
        assertThat(activeSystemRoles)
                .extracting(Role::getName)
                .containsExactlyInAnyOrder("ADMIN", "CUSTOMER");
    }

    @Test
    @DisplayName("Should update role details")
    void shouldUpdateRole() {
        // Given
        Role savedRole = roleRepository.save(adminRole);

        // When
        savedRole.setDescription("Updated admin description");
        savedRole.setUpdatedAt(OffsetDateTime.now());
        Role updatedRole = roleRepository.save(savedRole);

        // Then
        assertThat(updatedRole.getDescription()).isEqualTo("Updated admin description");
        assertThat(updatedRole.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should delete role")
    void shouldDeleteRole() {
        // Given
        Role savedRole = roleRepository.save(adminRole);
        UUID roleId = savedRole.getId();

        // When
        roleRepository.deleteById(roleId);

        // Then
        Optional<Role> deletedRole = roleRepository.findById(roleId);
        assertThat(deletedRole).isEmpty();
    }

    @Test
    @DisplayName("Should find all roles")
    void shouldFindAllRoles() {
        // Given
        roleRepository.save(adminRole);
        roleRepository.save(customerRole);
        roleRepository.save(partnerRole);

        // When
        List<Role> allRoles = roleRepository.findAll();

        // Then
        assertThat(allRoles).hasSize(3);
    }

    @Test
    @DisplayName("Should count roles by status")
    void shouldCountRolesByStatus() {
        // Given
        roleRepository.save(adminRole);
        roleRepository.save(customerRole);

        Role inactiveRole = Role.builder()
                .keycloakRoleId(UUID.randomUUID())
                .name("INACTIVE")
                .status(Status.inactive)
                .createdAt(OffsetDateTime.now())
                .build();
        roleRepository.save(inactiveRole);

        // When
        long activeCount = roleRepository.countByStatus(Status.active);
        long inactiveCount = roleRepository.countByStatus(Status.inactive);

        // Then
        assertThat(activeCount).isEqualTo(2);
        assertThat(inactiveCount).isEqualTo(1);
    }
}
