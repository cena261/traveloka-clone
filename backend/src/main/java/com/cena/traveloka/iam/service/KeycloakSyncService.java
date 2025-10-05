package com.cena.traveloka.iam.service;

import com.cena.traveloka.iam.entity.User;
import com.cena.traveloka.iam.repository.RoleRepository;
import com.cena.traveloka.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.NotFoundException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class KeycloakSyncService {

    private final Keycloak keycloak;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Value("${traveloka.iam.keycloak.realm:master}")
    private String realmName;

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    public User createUserInKeycloak(User user, String password) {
        if (!isKeycloakAvailable()) {
            log.warn("Keycloak is not available. Skipping user creation in Keycloak.");
            return user;
        }

        return executeWithRetry(() -> {
            RealmResource realmResource = keycloak.realm(realmName);
            UsersResource usersResource = realmResource.users();

            UserRepresentation userRep = new UserRepresentation();
            userRep.setUsername(user.getUsername());
            userRep.setEmail(user.getEmail());
            userRep.setFirstName(user.getFirstName());
            userRep.setLastName(user.getLastName());
            userRep.setEnabled(true);
            userRep.setEmailVerified(user.getEmailVerified());

            Response response = usersResource.create(userRep);

            if (response.getStatus() == 201) {
                URI location = response.getLocation();
                String keycloakId = location.getPath().replaceAll(".*/([^/]+)$", "$1");

                UserResource userResource = usersResource.get(keycloakId);
                CredentialRepresentation credential = new CredentialRepresentation();
                credential.setType(CredentialRepresentation.PASSWORD);
                credential.setValue(password);
                credential.setTemporary(false);
                userResource.resetPassword(credential);

                user.setKeycloakId(UUID.fromString(keycloakId));
                userRepository.save(user);

                log.info("User created in Keycloak: {} with ID: {}", user.getEmail(), keycloakId);
                return user;
            } else {
                throw new RuntimeException("Failed to create user in Keycloak. Status: " + response.getStatus());
            }
        }, "create user in Keycloak");
    }

    public User syncUserFromKeycloak(UUID keycloakId) {
        if (!isKeycloakAvailable()) {
            throw new RuntimeException("Keycloak is not available");
        }

        return executeWithRetry(() -> {
            RealmResource realmResource = keycloak.realm(realmName);
            UserResource userResource = realmResource.users().get(keycloakId.toString());
            UserRepresentation userRep = userResource.toRepresentation();

            User user = userRepository.findByKeycloakId(keycloakId)
                    .orElseThrow(() -> new RuntimeException("User not found with Keycloak ID: " + keycloakId));

            user.setUsername(userRep.getUsername());
            user.setEmail(userRep.getEmail());
            user.setFirstName(userRep.getFirstName());
            user.setLastName(userRep.getLastName());
            user.setEmailVerified(userRep.isEmailVerified());
            user.setUpdatedAt(OffsetDateTime.now());

            userRepository.save(user);

            log.info("User synced from Keycloak: {}", user.getEmail());
            return user;
        }, "sync user from Keycloak");
    }

    public void updateUserInKeycloak(User user) {
        if (!isKeycloakAvailable() || user.getKeycloakId() == null) {
            log.warn("Keycloak unavailable or user has no Keycloak ID. Skipping update.");
            return;
        }

        executeWithRetry(() -> {
            RealmResource realmResource = keycloak.realm(realmName);
            UserResource userResource = realmResource.users().get(user.getKeycloakId().toString());

            UserRepresentation userRep = userResource.toRepresentation();
            userRep.setFirstName(user.getFirstName());
            userRep.setLastName(user.getLastName());
            userRep.setEmail(user.getEmail());
            userRep.setEmailVerified(user.getEmailVerified());
            userRep.setEnabled(user.getStatus() == com.cena.traveloka.common.enums.Status.active);

            userResource.update(userRep);

            log.info("User updated in Keycloak: {}", user.getEmail());
            return null;
        }, "update user in Keycloak");
    }

    public void deleteUserFromKeycloak(UUID keycloakId) {
        if (!isKeycloakAvailable()) {
            log.warn("Keycloak is not available. Skipping user deletion in Keycloak.");
            return;
        }

        executeWithRetry(() -> {
            RealmResource realmResource = keycloak.realm(realmName);
            UserResource userResource = realmResource.users().get(keycloakId.toString());
            userResource.remove();

            log.info("User deleted from Keycloak: {}", keycloakId);
            return null;
        }, "delete user from Keycloak");
    }

    public void assignRoleToUser(UUID keycloakId, String roleName) {
        if (!isKeycloakAvailable()) {
            log.warn("Keycloak is not available. Skipping role assignment.");
            return;
        }

        executeWithRetry(() -> {
            RealmResource realmResource = keycloak.realm(realmName);
            UserResource userResource = realmResource.users().get(keycloakId.toString());

            RoleResource roleResource = realmResource.roles().get(roleName);
            RoleRepresentation roleRep = roleResource.toRepresentation();

            userResource.roles().realmLevel().add(Collections.singletonList(roleRep));

            log.info("Role {} assigned to user {} in Keycloak", roleName, keycloakId);
            return null;
        }, "assign role to user in Keycloak");
    }

    public void removeRoleFromUser(UUID keycloakId, String roleName) {
        if (!isKeycloakAvailable()) {
            log.warn("Keycloak is not available. Skipping role removal.");
            return;
        }

        executeWithRetry(() -> {
            RealmResource realmResource = keycloak.realm(realmName);
            UserResource userResource = realmResource.users().get(keycloakId.toString());

            RoleResource roleResource = realmResource.roles().get(roleName);
            RoleRepresentation roleRep = roleResource.toRepresentation();

            userResource.roles().realmLevel().remove(Collections.singletonList(roleRep));

            log.info("Role {} removed from user {} in Keycloak", roleName, keycloakId);
            return null;
        }, "remove role from user in Keycloak");
    }

    public int syncAllUsersFromKeycloak() {
        if (!isKeycloakAvailable()) {
            log.warn("Keycloak is not available. Skipping bulk user sync.");
            return 0;
        }

        return executeWithRetry(() -> {
            RealmResource realmResource = keycloak.realm(realmName);
            List<UserRepresentation> keycloakUsers = realmResource.users().list();

            int synced = 0;
            for (UserRepresentation userRep : keycloakUsers) {
                try {
                    UUID keycloakId = UUID.fromString(userRep.getId());
                    Optional<User> optionalUser = userRepository.findByKeycloakId(keycloakId);

                    if (optionalUser.isPresent()) {
                        User user = optionalUser.get();
                        user.setUsername(userRep.getUsername());
                        user.setEmail(userRep.getEmail());
                        user.setFirstName(userRep.getFirstName());
                        user.setLastName(userRep.getLastName());
                        user.setEmailVerified(userRep.isEmailVerified());
                        user.setUpdatedAt(OffsetDateTime.now());
                        userRepository.save(user);
                        synced++;
                    }
                } catch (Exception e) {
                    log.error("Failed to sync user: {}", userRep.getUsername(), e);
                }
            }

            log.info("Synced {} users from Keycloak", synced);
            return synced;
        }, "sync all users from Keycloak");
    }

    public void enableUserInKeycloak(UUID keycloakId) {
        updateUserEnabledStatus(keycloakId, true);
    }

    public void disableUserInKeycloak(UUID keycloakId) {
        updateUserEnabledStatus(keycloakId, false);
    }

    public void resetPasswordInKeycloak(UUID keycloakId, String newPassword) {
        if (!isKeycloakAvailable()) {
            log.warn("Keycloak is not available. Skipping password reset.");
            return;
        }

        executeWithRetry(() -> {
            RealmResource realmResource = keycloak.realm(realmName);
            UserResource userResource = realmResource.users().get(keycloakId.toString());

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(newPassword);
            credential.setTemporary(false);

            userResource.resetPassword(credential);

            log.info("Password reset in Keycloak for user: {}", keycloakId);
            return null;
        }, "reset password in Keycloak");
    }

    public boolean isKeycloakAvailable() {
        return keycloak != null;
    }


    private void updateUserEnabledStatus(UUID keycloakId, boolean enabled) {
        if (!isKeycloakAvailable()) {
            log.warn("Keycloak is not available. Skipping user status update.");
            return;
        }

        executeWithRetry(() -> {
            RealmResource realmResource = keycloak.realm(realmName);
            UserResource userResource = realmResource.users().get(keycloakId.toString());

            UserRepresentation userRep = userResource.toRepresentation();
            userRep.setEnabled(enabled);
            userResource.update(userRep);

            log.info("User {} in Keycloak: {}", enabled ? "enabled" : "disabled", keycloakId);
            return null;
        }, "update user enabled status in Keycloak");
    }

    private <T> T executeWithRetry(KeycloakOperation<T> operation, String operationName) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < MAX_RETRIES) {
            try {
                return operation.execute();
            } catch (NotFoundException e) {
                throw new RuntimeException("User not found in Keycloak", e);
            } catch (Exception e) {
                attempts++;
                lastException = e;
                log.warn("Failed to {} (attempt {}/{}): {}", operationName, attempts, MAX_RETRIES, e.getMessage());

                if (attempts < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempts); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            }
        }

        throw new RuntimeException("Failed to " + operationName + " after " + MAX_RETRIES + " retries", lastException);
    }

    @FunctionalInterface
    private interface KeycloakOperation<T> {
        T execute() throws Exception;
    }
}
