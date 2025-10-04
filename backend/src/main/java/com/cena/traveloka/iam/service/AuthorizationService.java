package com.cena.traveloka.iam.service;

import com.cena.traveloka.iam.entity.Permission;
import com.cena.traveloka.iam.entity.Role;
import com.cena.traveloka.iam.entity.User;
import com.cena.traveloka.iam.repository.PermissionRepository;
import com.cena.traveloka.iam.repository.RoleRepository;
import com.cena.traveloka.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * T055: AuthorizationService
 * Service for role-based access control (RBAC).
 *
 * Constitutional Compliance:
 * - FR-005: Role-based access control with distinct permission levels
 * - FR-006: Service method-level authorization enforcement
 * - Principle III: Layered Architecture - Business logic in service layer
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorizationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    /**
     * Check if user has specific role.
     *
     * @param userId User ID
     * @param roleName Role name
     * @return true if user has the role
     */
    @Cacheable(value = "userRoles", key = "#userId + '_' + #roleName")
    public boolean hasRole(UUID userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        return user.getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase(roleName));
    }

    /**
     * Check if user has any of the specified roles.
     *
     * @param userId User ID
     * @param roleNames Role names
     * @return true if user has any of the roles
     */
    public boolean hasAnyRole(UUID userId, String... roleNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        Set<String> userRoleNames = user.getRoles().stream()
                .map(Role::getName)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        for (String roleName : roleNames) {
            if (userRoleNames.contains(roleName.toUpperCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if user has all specified roles.
     *
     * @param userId User ID
     * @param roleNames Role names
     * @return true if user has all the roles
     */
    public boolean hasAllRoles(UUID userId, String... roleNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        Set<String> userRoleNames = user.getRoles().stream()
                .map(Role::getName)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        for (String roleName : roleNames) {
            if (!userRoleNames.contains(roleName.toUpperCase())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if user has specific permission.
     *
     * @param userId User ID
     * @param permissionName Permission name
     * @return true if user has the permission
     */
    @Cacheable(value = "userPermissions", key = "#userId + '_' + #permissionName")
    public boolean hasPermission(UUID userId, String permissionName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Check permissions from roles
        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(permission -> permission.getName().equalsIgnoreCase(permissionName));
    }

    /**
     * Get all permissions for user.
     *
     * @param userId User ID
     * @return Set of permission names
     */
    @Cacheable(value = "userAllPermissions", key = "#userId")
    public Set<String> getUserPermissions(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Get all roles for user.
     *
     * @param userId User ID
     * @return Set of role names
     */
    @Cacheable(value = "userAllRoles", key = "#userId")
    public Set<String> getUserRoles(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        return user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Check if user is admin (ADMIN or SUPER_ADMIN role).
     *
     * @param userId User ID
     * @return true if user is admin
     */
    public boolean isAdmin(UUID userId) {
        return hasAnyRole(userId, "ADMIN", "SUPER_ADMIN");
    }

    /**
     * Check if user is partner admin.
     *
     * @param userId User ID
     * @return true if user is partner admin
     */
    public boolean isPartnerAdmin(UUID userId) {
        return hasAnyRole(userId, "PARTNER_ADMIN", "ADMIN", "SUPER_ADMIN");
    }

    /**
     * Check if authenticated user has role.
     *
     * @param authentication Spring Security authentication
     * @param roleName Role name
     * @return true if authenticated user has the role
     */
    public boolean authenticationHasRole(Authentication authentication, String roleName) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equalsIgnoreCase("ROLE_" + roleName) ||
                                      authority.equalsIgnoreCase(roleName));
    }

    /**
     * Check if authenticated user has any of the specified roles.
     *
     * @param authentication Spring Security authentication
     * @param roleNames Role names
     * @return true if authenticated user has any of the roles
     */
    public boolean authenticationHasAnyRole(Authentication authentication, String... roleNames) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        for (String roleName : roleNames) {
            boolean hasRole = authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(authority -> authority.equalsIgnoreCase("ROLE_" + roleName) ||
                                          authority.equalsIgnoreCase(roleName));
            if (hasRole) {
                return true;
            }
        }

        return false;
    }

    /**
     * Assign role to user.
     *
     * @param userId User ID
     * @param roleName Role name
     */
    @Transactional
    public void assignRoleToUser(UUID userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        user.getRoles().add(role);
        userRepository.save(user);

        log.info("Role {} assigned to user: {}", roleName, userId);
    }

    /**
     * Remove role from user.
     *
     * @param userId User ID
     * @param roleName Role name
     */
    @Transactional
    public void removeRoleFromUser(UUID userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        user.getRoles().removeIf(role -> role.getName().equalsIgnoreCase(roleName));
        userRepository.save(user);

        log.info("Role {} removed from user: {}", roleName, userId);
    }

    /**
     * Check if role exists.
     *
     * @param roleName Role name
     * @return true if role exists
     */
    public boolean roleExists(String roleName) {
        return roleRepository.findByName(roleName).isPresent();
    }

    /**
     * Check if permission exists.
     *
     * @param permissionName Permission name
     * @return true if permission exists
     */
    public boolean permissionExists(String permissionName) {
        return permissionRepository.findByName(permissionName).isPresent();
    }
}
