package com.cena.traveloka.iam.service;

import com.cena.traveloka.common.enums.Status;
import com.cena.traveloka.iam.entity.Role;
import com.cena.traveloka.iam.entity.User;
import com.cena.traveloka.iam.repository.RoleRepository;
import com.cena.traveloka.iam.repository.UserRepository;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * T014: KeycloakSyncServiceTest
 * Service layer tests for Keycloak synchronization operations.
 *
 * TDD Phase: RED - These tests MUST fail before implementing KeycloakSyncService
 *
 * Constitutional Compliance:
 * - Principle VII: Test-First Development - Tests written before service implementation
 * - Tests FR-002: Keycloak integration for authentication
 * - Tests FR-011: Bidirectional sync with identity provider
 * - Tests NFR-004: Graceful handling of Keycloak unavailability
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KeycloakSyncService Tests")
class KeycloakSyncServiceTest {

    @Mock
    private Keycloak keycloak;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @Mock
    private RolesResource rolesResource;

    @InjectMocks
    private KeycloakSyncService keycloakSyncService;

    private User testUser;
    private Role testRole;
    private UserRepresentation keycloakUser;
    private RoleRepresentation keycloakRole;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .keycloakId(UUID.randomUUID())
                .username("johndoe")
                .email("john.doe@example.com")
                .firstName("John")
                .lastName("Doe")
                .status(Status.active)
                .emailVerified(true)
                .createdAt(OffsetDateTime.now())
                .build();

        testRole = Role.builder()
                .id(UUID.randomUUID())
                .keycloakRoleId(UUID.randomUUID())
                .name("CUSTOMER")
                .displayName("Customer")
                .status(Status.active)
                .build();

        keycloakUser = new UserRepresentation();
        keycloakUser.setId(testUser.getKeycloakId().toString());
        keycloakUser.setUsername(testUser.getUsername());
        keycloakUser.setEmail(testUser.getEmail());
        keycloakUser.setFirstName(testUser.getFirstName());
        keycloakUser.setLastName(testUser.getLastName());
        keycloakUser.setEnabled(true);
        keycloakUser.setEmailVerified(true);

        keycloakRole = new RoleRepresentation();
        keycloakRole.setId(testRole.getKeycloakRoleId().toString());
        keycloakRole.setName(testRole.getName());
        keycloakRole.setDescription(testRole.getDescription());
    }

    @Test
    @DisplayName("Should create user in Keycloak (FR-011)")
    void shouldCreateUserInKeycloak() {
        // Given
        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(201);
        when(response.getLocation()).thenReturn(java.net.URI.create("http://localhost:8080/users/" + testUser.getKeycloakId()));
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);

        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = keycloakSyncService.createUserInKeycloak(testUser, "Password123!");

        // Then
        assertThat(result).isNotNull();
        verify(usersResource).create(argThat(userRep ->
                userRep.getUsername().equals(testUser.getUsername()) &&
                userRep.getEmail().equals(testUser.getEmail()) &&
                userRep.isEnabled()
        ));
        verify(userRepository).save(argThat(user ->
                user.getKeycloakId() != null
        ));
    }

    @Test
    @DisplayName("Should handle Keycloak unavailability gracefully (NFR-004)")
    void shouldHandleKeycloakUnavailabilityGracefully() {
        // Given - Keycloak is null (unavailable)
        keycloakSyncService = new KeycloakSyncService(null, userRepository, roleRepository);

        // When
        boolean isAvailable = keycloakSyncService.isKeycloakAvailable();

        // Then
        assertThat(isAvailable).isFalse();
    }

    @Test
    @DisplayName("Should retry on Keycloak failure (NFR-004)")
    void shouldRetryOnKeycloakFailure() {
        // Given
        when(keycloak.realm(anyString()))
                .thenThrow(new RuntimeException("Connection timeout"))
                .thenThrow(new RuntimeException("Connection timeout"))
                .thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(201);
        when(response.getLocation()).thenReturn(java.net.URI.create("http://localhost:8080/users/" + testUser.getKeycloakId()));
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);

        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = keycloakSyncService.createUserInKeycloak(testUser, "Password123!");

        // Then
        assertThat(result).isNotNull();
        verify(keycloak, times(3)).realm(anyString()); // 2 failures + 1 success
    }

    @Test
    @DisplayName("Should fail after max retries (NFR-004)")
    void shouldFailAfterMaxRetries() {
        // Given
        when(keycloak.realm(anyString()))
                .thenThrow(new RuntimeException("Connection timeout"));

        // When/Then
        assertThatThrownBy(() -> keycloakSyncService.createUserInKeycloak(testUser, "Password123!"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create user in Keycloak after 3 retries");

        verify(keycloak, times(3)).realm(anyString()); // Max retries = 3
    }

    @Test
    @DisplayName("Should sync user from Keycloak to database (FR-011)")
    void shouldSyncUserFromKeycloakToDatabase() {
        // Given
        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(testUser.getKeycloakId().toString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(keycloakUser);

        when(userRepository.findByKeycloakId(testUser.getKeycloakId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = keycloakSyncService.syncUserFromKeycloak(testUser.getKeycloakId());

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(argThat(user ->
                user.getEmail().equals(keycloakUser.getEmail()) &&
                user.getEmailVerified().equals(keycloakUser.isEmailVerified())
        ));
    }

    @Test
    @DisplayName("Should update user in Keycloak (FR-011)")
    void shouldUpdateUserInKeycloak() {
        // Given
        testUser.setFirstName("Updated");
        testUser.setLastName("Name");

        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(testUser.getKeycloakId().toString())).thenReturn(userResource);
        doNothing().when(userResource).update(any(UserRepresentation.class));

        // When
        keycloakSyncService.updateUserInKeycloak(testUser);

        // Then
        verify(userResource).update(argThat(userRep ->
                userRep.getFirstName().equals("Updated") &&
                userRep.getLastName().equals("Name")
        ));
    }

    @Test
    @DisplayName("Should delete user from Keycloak")
    void shouldDeleteUserFromKeycloak() {
        // Given
        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(testUser.getKeycloakId().toString())).thenReturn(userResource);
        doNothing().when(userResource).remove();

        // When
        keycloakSyncService.deleteUserFromKeycloak(testUser.getKeycloakId());

        // Then
        verify(userResource).remove();
    }

    @Test
    @DisplayName("Should assign role to user in Keycloak")
    void shouldAssignRoleToUserInKeycloak() {
        // Given
        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(testUser.getKeycloakId().toString())).thenReturn(userResource);
        when(realmResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get(testRole.getName())).thenReturn(mock(org.keycloak.admin.client.resource.RoleResource.class));
        when(rolesResource.get(testRole.getName()).toRepresentation()).thenReturn(keycloakRole);

        // When
        keycloakSyncService.assignRoleToUser(testUser.getKeycloakId(), testRole.getName());

        // Then
        verify(userResource.roles().realmLevel()).add(anyList());
    }

    @Test
    @DisplayName("Should remove role from user in Keycloak")
    void shouldRemoveRoleFromUserInKeycloak() {
        // Given
        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(testUser.getKeycloakId().toString())).thenReturn(userResource);
        when(realmResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get(testRole.getName())).thenReturn(mock(org.keycloak.admin.client.resource.RoleResource.class));
        when(rolesResource.get(testRole.getName()).toRepresentation()).thenReturn(keycloakRole);

        // When
        keycloakSyncService.removeRoleFromUser(testUser.getKeycloakId(), testRole.getName());

        // Then
        verify(userResource.roles().realmLevel()).remove(anyList());
    }

    @Test
    @DisplayName("Should sync all users from Keycloak (bulk sync)")
    void shouldSyncAllUsersFromKeycloak() {
        // Given
        List<UserRepresentation> keycloakUsers = Arrays.asList(keycloakUser);

        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.list()).thenReturn(keycloakUsers);
        when(userRepository.findByKeycloakId(any(UUID.class))).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        int synced = keycloakSyncService.syncAllUsersFromKeycloak();

        // Then
        assertThat(synced).isEqualTo(1);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should enable user in Keycloak")
    void shouldEnableUserInKeycloak() {
        // Given
        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(testUser.getKeycloakId().toString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(keycloakUser);
        doNothing().when(userResource).update(any(UserRepresentation.class));

        // When
        keycloakSyncService.enableUserInKeycloak(testUser.getKeycloakId());

        // Then
        verify(userResource).update(argThat(userRep ->
                userRep.isEnabled()
        ));
    }

    @Test
    @DisplayName("Should disable user in Keycloak")
    void shouldDisableUserInKeycloak() {
        // Given
        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(testUser.getKeycloakId().toString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(keycloakUser);
        doNothing().when(userResource).update(any(UserRepresentation.class));

        // When
        keycloakSyncService.disableUserInKeycloak(testUser.getKeycloakId());

        // Then
        verify(userResource).update(argThat(userRep ->
                !userRep.isEnabled()
        ));
    }

    @Test
    @DisplayName("Should reset password in Keycloak")
    void shouldResetPasswordInKeycloak() {
        // Given
        String newPassword = "NewPassword123!";

        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(testUser.getKeycloakId().toString())).thenReturn(userResource);

        // When
        keycloakSyncService.resetPasswordInKeycloak(testUser.getKeycloakId(), newPassword);

        // Then
        verify(userResource).resetPassword(argThat(credRep ->
                credRep.getValue().equals(newPassword) &&
                credRep.isTemporary() == false
        ));
    }

    @Test
    @DisplayName("Should handle user not found in Keycloak")
    void shouldHandleUserNotFoundInKeycloak() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(nonExistentId.toString())).thenThrow(new NotFoundException());

        // When/Then
        assertThatThrownBy(() -> keycloakSyncService.syncUserFromKeycloak(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found in Keycloak");
    }
}
