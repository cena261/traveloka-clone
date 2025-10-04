package com.cena.traveloka.iam.controller;

import com.cena.traveloka.common.dto.PageResponse;
import com.cena.traveloka.common.enums.Status;
import com.cena.traveloka.iam.dto.request.UpdateProfileRequest;
import com.cena.traveloka.iam.dto.response.UserDetailDto;
import com.cena.traveloka.iam.dto.response.UserDto;
import com.cena.traveloka.iam.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T016: Test UserController endpoints
 * Controller test for UserController (TDD - Phase 3.2).
 *
 * Tests all user management endpoints:
 * - GET /api/v1/users/me
 * - PUT /api/v1/users/me
 * - GET /api/v1/users (admin)
 * - GET /api/v1/users/{id} (admin)
 * - POST /api/v1/users/{id}/lock (admin)
 * - POST /api/v1/users/{id}/unlock (admin)
 *
 * Constitutional Compliance:
 * - Principle VII: Test Coverage - TDD mandatory, tests before implementation
 * - Principle IV: Entity Immutability - Uses DTOs for API contracts
 * - FR-019: View/update profile
 * - FR-008: Account lockout
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security for unit tests
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private UserDto userDto;
    private UserDetailDto userDetailDto;
    private UpdateProfileRequest validUpdateRequest;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();

        userDto = UserDto.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .displayName("Test User")
                .status(Status.ACTIVE)
                .emailVerified(true)
                .phoneVerified(false)
                .twoFactorEnabled(false)
                .build();

        userDetailDto = UserDetailDto.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .displayName("Test User")
                .status(Status.ACTIVE)
                .emailVerified(true)
                .phoneVerified(false)
                .twoFactorEnabled(false)
                .build();

        validUpdateRequest = UpdateProfileRequest.builder()
                .firstName("Updated")
                .lastName("User")
                .displayName("Updated User")
                .phone("+84901234567")
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/users/me - Get Current User Profile (FR-019)")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should get current user profile successfully")
        void shouldGetCurrentUserProfile_Success() throws Exception {
            // Given
            when(userService.getCurrentUser(anyString()))
                    .thenReturn(userDetailDto);

            // When & Then
            mockMvc.perform(get("/api/v1/users/me")
                            .header("Authorization", "Bearer jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.id").value(userDetailDto.getId().toString()))
                    .andExpect(jsonPath("$.data.username").value("testuser"))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.firstName").value("Test"))
                    .andExpect(jsonPath("$.data.lastName").value("User"));

            verify(userService).getCurrentUser(anyString());
        }

        @Test
        @DisplayName("Should fail when no authorization header provided")
        void shouldFailGetCurrentUser_WhenNoAuthHeader() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/me - Update Current User Profile (FR-019)")
    class UpdateCurrentUserTests {

        @Test
        @DisplayName("Should update current user profile successfully")
        void shouldUpdateCurrentUserProfile_Success() throws Exception {
            // Given
            when(userService.updateCurrentUser(any(UpdateProfileRequest.class), anyString()))
                    .thenReturn(userDetailDto);

            // When & Then
            mockMvc.perform(put("/api/v1/users/me")
                            .header("Authorization", "Bearer jwt-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.id").exists())
                    .andExpect(jsonPath("$.data.username").value("testuser"));

            verify(userService).updateCurrentUser(any(UpdateProfileRequest.class), anyString());
        }

        @Test
        @DisplayName("Should fail update when phone format is invalid (FR-018)")
        void shouldFailUpdate_WhenPhoneInvalid() throws Exception {
            // Given
            validUpdateRequest.setPhone("invalid-phone");

            // When & Then
            mockMvc.perform(put("/api/v1/users/me")
                            .header("Authorization", "Bearer jwt-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail update when first name exceeds max length")
        void shouldFailUpdate_WhenFirstNameTooLong() throws Exception {
            // Given
            validUpdateRequest.setFirstName("A".repeat(101)); // Exceeds 100 chars

            // When & Then
            mockMvc.perform(put("/api/v1/users/me")
                            .header("Authorization", "Bearer jwt-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should update profile with valid Vietnamese phone (FR-018)")
        void shouldUpdateProfile_WithValidVietnamesePhone() throws Exception {
            // Given
            validUpdateRequest.setPhone("+84901234567");
            when(userService.updateCurrentUser(any(UpdateProfileRequest.class), anyString()))
                    .thenReturn(userDetailDto);

            // When & Then
            mockMvc.perform(put("/api/v1/users/me")
                            .header("Authorization", "Bearer jwt-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"));

            verify(userService).updateCurrentUser(any(UpdateProfileRequest.class), anyString());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users - List All Users (Admin Only)")
    class ListUsersTests {

        @Test
        @DisplayName("Should list users with pagination successfully")
        void shouldListUsers_Success() throws Exception {
            // Given
            PageResponse<UserDto> pageResponse = PageResponse.<UserDto>builder()
                    .content(List.of(userDto))
                    .totalElements(1L)
                    .totalPages(1)
                    .size(20)
                    .number(0)
                    .first(true)
                    .last(true)
                    .build();

            when(userService.getAllUsers(any(PageRequest.class)))
                    .thenReturn(pageResponse);

            // When & Then
            mockMvc.perform(get("/api/v1/users")
                            .header("Authorization", "Bearer admin-jwt-token")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].username").value("testuser"))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.size").value(20))
                    .andExpect(jsonPath("$.data.number").value(0));

            verify(userService).getAllUsers(any(PageRequest.class));
        }

        @Test
        @DisplayName("Should use default pagination when parameters not provided")
        void shouldListUsers_WithDefaultPagination() throws Exception {
            // Given
            PageResponse<UserDto> pageResponse = PageResponse.<UserDto>builder()
                    .content(List.of(userDto))
                    .totalElements(1L)
                    .totalPages(1)
                    .size(20) // Default size
                    .number(0) // Default page
                    .first(true)
                    .last(true)
                    .build();

            when(userService.getAllUsers(any(PageRequest.class)))
                    .thenReturn(pageResponse);

            // When & Then
            mockMvc.perform(get("/api/v1/users")
                            .header("Authorization", "Bearer admin-jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.size").value(20))
                    .andExpect(jsonPath("$.data.number").value(0));

            verify(userService).getAllUsers(any(PageRequest.class));
        }

        @Test
        @DisplayName("Should respect max page size limit (100)")
        void shouldListUsers_WithMaxPageSize() throws Exception {
            // Given
            PageResponse<UserDto> pageResponse = PageResponse.<UserDto>builder()
                    .content(List.of(userDto))
                    .totalElements(1L)
                    .totalPages(1)
                    .size(100) // Max size
                    .number(0)
                    .first(true)
                    .last(true)
                    .build();

            when(userService.getAllUsers(any(PageRequest.class)))
                    .thenReturn(pageResponse);

            // When & Then
            mockMvc.perform(get("/api/v1/users")
                            .header("Authorization", "Bearer admin-jwt-token")
                            .param("page", "0")
                            .param("size", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.size").value(100));

            verify(userService).getAllUsers(any(PageRequest.class));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{id} - Get User By ID (Admin Only)")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should get user by ID successfully")
        void shouldGetUserById_Success() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            when(userService.getUserById(eq(userId)))
                    .thenReturn(userDetailDto);

            // When & Then
            mockMvc.perform(get("/api/v1/users/{id}", userId)
                            .header("Authorization", "Bearer admin-jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.username").value("testuser"))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"));

            verify(userService).getUserById(eq(userId));
        }

        @Test
        @DisplayName("Should fail when user ID format is invalid")
        void shouldFailGetUser_WhenIdInvalid() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/users/{id}", "invalid-uuid")
                            .header("Authorization", "Bearer admin-jwt-token"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/{id}/lock - Lock User Account (Admin Only, FR-008)")
    class LockUserTests {

        @Test
        @DisplayName("Should lock user account successfully")
        void shouldLockUser_Success() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            doNothing().when(userService).lockUser(eq(userId), anyString());

            // When & Then
            mockMvc.perform(post("/api/v1/users/{id}/lock", userId)
                            .header("Authorization", "Bearer admin-jwt-token")
                            .param("reason", "Suspicious activity detected"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").exists());

            verify(userService).lockUser(eq(userId), anyString());
        }

        @Test
        @DisplayName("Should lock user without reason")
        void shouldLockUser_WithoutReason() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            doNothing().when(userService).lockUser(eq(userId), eq(null));

            // When & Then
            mockMvc.perform(post("/api/v1/users/{id}/lock", userId)
                            .header("Authorization", "Bearer admin-jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"));

            verify(userService).lockUser(eq(userId), eq(null));
        }

        @Test
        @DisplayName("Should fail lock when user ID is invalid")
        void shouldFailLock_WhenIdInvalid() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/users/{id}/lock", "invalid-uuid")
                            .header("Authorization", "Bearer admin-jwt-token"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/{id}/unlock - Unlock User Account (Admin Only, FR-008)")
    class UnlockUserTests {

        @Test
        @DisplayName("Should unlock user account successfully")
        void shouldUnlockUser_Success() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            doNothing().when(userService).unlockUser(eq(userId));

            // When & Then
            mockMvc.perform(post("/api/v1/users/{id}/unlock", userId)
                            .header("Authorization", "Bearer admin-jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").exists());

            verify(userService).unlockUser(eq(userId));
        }

        @Test
        @DisplayName("Should fail unlock when user ID is invalid")
        void shouldFailUnlock_WhenIdInvalid() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/users/{id}/unlock", "invalid-uuid")
                            .header("Authorization", "Bearer admin-jwt-token"))
                    .andExpect(status().isBadRequest());
        }
    }
}
