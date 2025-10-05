package com.cena.traveloka.iam.integration;

import com.cena.traveloka.common.enums.Status;
import com.cena.traveloka.iam.dto.request.LoginRequest;
import com.cena.traveloka.iam.dto.request.RegisterRequest;
import com.cena.traveloka.iam.dto.response.AuthResponse;
import com.cena.traveloka.iam.entity.Permission;
import com.cena.traveloka.iam.entity.Role;
import com.cena.traveloka.iam.entity.User;
import com.cena.traveloka.iam.repository.PermissionRepository;
import com.cena.traveloka.iam.repository.RoleRepository;
import com.cena.traveloka.iam.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T092: Role-based authorization test.
 *
 * Tests role-based access control including:
 * - User role assignment (CUSTOMER, ADMIN, SUPER_ADMIN)
 * - Permission-based access control
 * - Admin-only endpoint protection
 * - Super admin privilege verification
 * - Role hierarchy enforcement
 * - Unauthorized access prevention
 *
 * Uses TestContainers for PostgreSQL and Redis.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
public class AuthorizationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("traveloka_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    private Role customerRole;
    private Role adminRole;
    private Role superAdminRole;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        // Create roles
        customerRole = Role.builder()
                .name("CUSTOMER")
                .description("Regular customer role")
                .isSystem(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        customerRole = roleRepository.save(customerRole);

        adminRole = Role.builder()
                .name("ADMIN")
                .description("Administrator role")
                .isSystem(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        adminRole = roleRepository.save(adminRole);

        superAdminRole = Role.builder()
                .name("SUPER_ADMIN")
                .description("Super administrator role")
                .isSystem(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        superAdminRole = roleRepository.save(superAdminRole);

        // Create permissions
        Permission userReadPermission = Permission.builder()
                .name("user:read")
                .description("Read user data")
                .resource("USER")
                .action("READ")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        permissionRepository.save(userReadPermission);

        Permission userWritePermission = Permission.builder()
                .name("user:write")
                .description("Modify user data")
                .resource("USER")
                .action("WRITE")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        permissionRepository.save(userWritePermission);

        Permission userDeletePermission = Permission.builder()
                .name("user:delete")
                .description("Delete user data")
                .resource("USER")
                .action("DELETE")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        permissionRepository.save(userDeletePermission);

        // Assign permissions to roles
        adminRole.setPermissions(Set.of(userReadPermission, userWritePermission));
        roleRepository.save(adminRole);

        superAdminRole.setPermissions(Set.of(userReadPermission, userWritePermission, userDeletePermission));
        roleRepository.save(superAdminRole);
    }

    @Test
    void shouldAllowCustomer_ToAccessOwnProfile() throws Exception {
        String accessToken = createUserWithRole("customer@example.com", customerRole);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.email").value("customer@example.com"));
    }

    @Test
    void shouldDenyCustomer_FromAccessingAdminEndpoints() throws Exception {
        String accessToken = createUserWithRole("customer@example.com", customerRole);

        // Try to access admin-only endpoint (list all users)
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdmin_ToAccessAdminEndpoints() throws Exception {
        String accessToken = createUserWithRole("admin@example.com", adminRole);

        // Access admin endpoint (list all users)
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void shouldAllowAdmin_ToLockUserAccount() throws Exception {
        String accessToken = createUserWithRole("admin@example.com", adminRole);
        User targetUser = createUser("target@example.com", customerRole);

        mockMvc.perform(post("/api/v1/users/" + targetUser.getId() + "/lock")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("reason", "Suspicious activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("User account locked successfully"));
    }

    @Test
    void shouldAllowAdmin_ToUnlockUserAccount() throws Exception {
        String accessToken = createUserWithRole("admin@example.com", adminRole);
        User targetUser = createUser("target@example.com", customerRole);

        // Lock first
        targetUser.setAccountLocked(true);
        targetUser.setLockReason("Test lock");
        userRepository.save(targetUser);

        // Unlock
        mockMvc.perform(post("/api/v1/users/" + targetUser.getId() + "/unlock")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("User account unlocked successfully"));
    }

    @Test
    void shouldAllowSuperAdmin_ToAccessAllEndpoints() throws Exception {
        String accessToken = createUserWithRole("superadmin@example.com", superAdminRole);

        // List all users
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Lock user
        User targetUser = createUser("target@example.com", customerRole);
        mockMvc.perform(post("/api/v1/users/" + targetUser.getId() + "/lock")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Unlock user
        mockMvc.perform(post("/api/v1/users/" + targetUser.getId() + "/unlock")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenyCustomer_FromLockingUsers() throws Exception {
        String accessToken = createUserWithRole("customer@example.com", customerRole);
        User targetUser = createUser("target@example.com", customerRole);

        mockMvc.perform(post("/api/v1/users/" + targetUser.getId() + "/lock")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDenyUnauthorizedAccess_WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldDenyAccess_WithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer invalid-token-here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowMultipleRoles_PerUser() throws Exception {
        // Create user with both CUSTOMER and ADMIN roles
        User user = createUser("multirole@example.com", customerRole);
        user.getRoles().add(adminRole);
        userRepository.save(user);

        String accessToken = loginUser("multirole@example.com");

        // Should be able to access customer endpoints
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Should also be able to access admin endpoints
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldEnforceRoleHierarchy() throws Exception {
        // SUPER_ADMIN > ADMIN > CUSTOMER
        String superAdminToken = createUserWithRole("superadmin@example.com", superAdminRole);
        String adminToken = createUserWithRole("admin@example.com", adminRole);
        String customerToken = createUserWithRole("customer@example.com", customerRole);

        // All can access their own profile
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk());

        // Only ADMIN and SUPER_ADMIN can list users
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldVerifyPermissions_OnRoleAssignment() throws Exception {
        // Admin has user:read and user:write
        // Super Admin has user:read, user:write, and user:delete

        Role admin = roleRepository.findByName("ADMIN").orElseThrow();
        Role superAdmin = roleRepository.findByName("SUPER_ADMIN").orElseThrow();

        // Verify admin permissions
        Set<String> adminPermissionNames = admin.getPermissions().stream()
                .map(Permission::getName)
                .collect(java.util.stream.Collectors.toSet());
        Assertions.assertThat(adminPermissionNames).containsExactlyInAnyOrder("user:read", "user:write");

        // Verify super admin permissions
        Set<String> superAdminPermissionNames = superAdmin.getPermissions().stream()
                .map(Permission::getName)
                .collect(java.util.stream.Collectors.toSet());
        Assertions.assertThat(superAdminPermissionNames).containsExactlyInAnyOrder("user:read", "user:write", "user:delete");
    }

    // Helper methods

    private String createUserWithRole(String email, Role role) throws Exception {
        User user = createUser(email, role);
        return loginUser(email);
    }

    private User createUser(String email, Role role) {
        User user = User.builder()
                .username(email.split("@")[0])
                .email(email)
                .firstName("Test")
                .lastName("User")
                .status(Status.active)
                .emailVerified(true)
                .accountLocked(false)
                .twoFactorEnabled(false)
                .failedLoginAttempts(0)
                .termsAcceptedAt(OffsetDateTime.now())
                .privacyAcceptedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        user.setRoles(Set.of(role));
        return userRepository.save(user);
    }

    private String loginUser(String email) throws Exception {
        // First register the user
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username(email.split("@")[0])
                .email(email)
                .password("SecurePass123!")
                .firstName("Test")
                .lastName("User")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // Then login
        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password("SecurePass123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        String loginResponseBody = loginResult.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(
                objectMapper.readTree(loginResponseBody).get("data").toString(),
                AuthResponse.class
        );

        return authResponse.getAccessToken();
    }

    private static org.assertj.core.api.AbstractListAssert<?, java.util.List<?>, Object, org.assertj.core.api.ObjectAssert<Object>> assertThat(Set<Permission> permissions) {
        return org.assertj.core.api.Assertions.assertThat(permissions.stream().toList());
    }
}
