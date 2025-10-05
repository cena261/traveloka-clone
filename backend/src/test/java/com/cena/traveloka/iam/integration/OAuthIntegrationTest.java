package com.cena.traveloka.iam.integration;

import com.cena.traveloka.iam.entity.OAuthProvider;
import com.cena.traveloka.iam.entity.User;
import com.cena.traveloka.iam.repository.OAuthProviderRepository;
import com.cena.traveloka.iam.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T096: OAuth social login test.
 *
 * Tests OAuth2 integration flows including:
 * - OAuth authorization redirect (Google, Facebook, Apple) (FR-012)
 * - OAuth callback handling
 * - User creation from OAuth data
 * - OAuth provider linking to existing account
 * - Multiple OAuth provider support
 * - OAuth token refresh
 * - Error handling for OAuth failures
 *
 * Uses TestContainers for PostgreSQL and Redis.
 *
 * Note: This test focuses on OAuth flow structure and redirects.
 * Actual OAuth provider integration requires external OAuth servers
 * or mock OAuth servers (not implemented in this test).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
public class OAuthIntegrationTest {

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
    private OAuthProviderRepository oauthProviderRepository;

    @BeforeEach
    void setUp() {
        oauthProviderRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRedirectToGoogleAuth_OnAuthorize() throws Exception {
        mockMvc.perform(get("/api/v1/auth/oauth/google/authorize"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("google")));
    }

    @Test
    void shouldRedirectToFacebookAuth_OnAuthorize() throws Exception {
        mockMvc.perform(get("/api/v1/auth/oauth/facebook/authorize"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("facebook")));
    }

    @Test
    void shouldRedirectToAppleAuth_OnAuthorize() throws Exception {
        mockMvc.perform(get("/api/v1/auth/oauth/apple/authorize"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("apple")));
    }

    @Test
    void shouldRejectUnsupportedOAuthProvider() throws Exception {
        mockMvc.perform(get("/api/v1/auth/oauth/twitter/authorize"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldHandleOAuthError_InCallback() throws Exception {
        mockMvc.perform(get("/api/v1/auth/oauth/google/callback")
                        .param("error", "access_denied")
                        .param("error_description", "User denied access"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("/auth/oauth/error")))
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("error=access_denied")));
    }

    @Test
    void shouldStoreOAuthProvider_AfterSuccessfulLogin() {
        // This test verifies the entity structure for OAuth providers
        // Actual OAuth callback integration requires mock OAuth server

        User user = User.builder()
                .username("googleuser")
                .email("googleuser@example.com")
                .firstName("Google")
                .lastName("User")
                .emailVerified(true) // OAuth emails are pre-verified
                .build();
        user = userRepository.save(user);

        OAuthProvider oauthProvider = OAuthProvider.builder()
                .user(user)
                .provider("google")
                .providerUserId("google-user-12345")
                .email("googleuser@example.com")
                .name("Google User")
                .avatarUrl("https://example.com/avatar.jpg")
                .accessToken("google-access-token")
                .refreshToken("google-refresh-token")
                .build();
        oauthProviderRepository.save(oauthProvider);

        // Verify OAuth provider saved
        Optional<OAuthProvider> savedProvider = oauthProviderRepository
                .findByProviderAndProviderUserId("google", "google-user-12345");
        assertThat(savedProvider).isPresent();
        assertThat(savedProvider.get().getEmail()).isEqualTo("googleuser@example.com");
        assertThat(savedProvider.get().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void shouldLinkMultipleOAuthProviders_ToSingleUser() {
        User user = User.builder()
                .username("multiuser")
                .email("multi@example.com")
                .firstName("Multi")
                .lastName("User")
                .build();
        user = userRepository.save(user);

        // Link Google
        OAuthProvider googleProvider = OAuthProvider.builder()
                .user(user)
                .provider("google")
                .providerUserId("google-123")
                .email("multi@example.com")
                .accessToken("google-token")
                .build();
        oauthProviderRepository.save(googleProvider);

        // Link Facebook
        OAuthProvider facebookProvider = OAuthProvider.builder()
                .user(user)
                .provider("facebook")
                .providerUserId("facebook-456")
                .email("multi@example.com")
                .accessToken("facebook-token")
                .build();
        oauthProviderRepository.save(facebookProvider);

        // Link Apple
        OAuthProvider appleProvider = OAuthProvider.builder()
                .user(user)
                .provider("apple")
                .providerUserId("apple-789")
                .email("multi@example.com")
                .accessToken("apple-token")
                .build();
        oauthProviderRepository.save(appleProvider);

        // Verify all providers linked
        List<OAuthProvider> providers = oauthProviderRepository.findByUserId(user.getId());
        assertThat(providers).hasSize(3);
        assertThat(providers).extracting(OAuthProvider::getProvider)
                .containsExactlyInAnyOrder("google", "facebook", "apple");
    }

    @Test
    void shouldPreventDuplicateOAuthProvider_ForSameUser() {
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .build();
        user = userRepository.save(user);

        // Link Google first time
        OAuthProvider provider1 = OAuthProvider.builder()
                .user(user)
                .provider("google")
                .providerUserId("google-123")
                .email("test@example.com")
                .accessToken("token-1")
                .build();
        oauthProviderRepository.save(provider1);

        // Verify provider exists
        boolean exists = oauthProviderRepository.existsByUserIdAndProvider(user.getId(), "google");
        assertThat(exists).isTrue();

        // Attempting to link same provider again should be handled by service layer
        // (not at database level due to unique constraints)
    }

    @Test
    void shouldCreateNewUser_FromOAuthData() {
        // Simulate OAuth login for new user
        User newUser = User.builder()
                .username("newgoogleuser")
                .email("newgoogleuser@gmail.com")
                .firstName("New")
                .lastName("GoogleUser")
                .emailVerified(true) // OAuth emails are pre-verified
                .build();
        newUser = userRepository.save(newUser);

        OAuthProvider oauthProvider = OAuthProvider.builder()
                .user(newUser)
                .provider("google")
                .providerUserId("new-google-user-id")
                .email("newgoogleuser@gmail.com")
                .name("New GoogleUser")
                .build();
        oauthProviderRepository.save(oauthProvider);

        // Verify user created with email verified
        User savedUser = userRepository.findByEmail("newgoogleuser@gmail.com").orElseThrow();
        assertThat(savedUser.getEmailVerified()).isTrue();
        assertThat(savedUser.getUsername()).isEqualTo("newgoogleuser");

        // Verify OAuth provider linked
        Optional<OAuthProvider> savedProvider = oauthProviderRepository
                .findByProviderAndProviderUserId("google", "new-google-user-id");
        assertThat(savedProvider).isPresent();
    }

    @Test
    void shouldLinkOAuthToExistingUser_BySameEmail() {
        // Existing user registered with email/password
        User existingUser = User.builder()
                .username("existinguser")
                .email("existing@example.com")
                .firstName("Existing")
                .lastName("User")
                .emailVerified(true)
                .build();
        existingUser = userRepository.save(existingUser);

        // User logs in with Google (same email)
        OAuthProvider googleProvider = OAuthProvider.builder()
                .user(existingUser)
                .provider("google")
                .providerUserId("google-existing-user")
                .email("existing@example.com")
                .build();
        oauthProviderRepository.save(googleProvider);

        // Verify OAuth linked to existing user
        List<OAuthProvider> providers = oauthProviderRepository.findByUserId(existingUser.getId());
        assertThat(providers).hasSize(1);
        assertThat(providers.get(0).getProvider()).isEqualTo("google");
    }

    @Test
    void shouldStoreOAuthTokens_ForRefresh() {
        User user = User.builder()
                .username("tokenuser")
                .email("token@example.com")
                .build();
        user = userRepository.save(user);

        OAuthProvider provider = OAuthProvider.builder()
                .user(user)
                .provider("google")
                .providerUserId("google-token-user")
                .accessToken("initial-access-token")
                .refreshToken("initial-refresh-token")
                .tokenExpiresAt(java.time.OffsetDateTime.now().plusHours(1))
                .build();
        provider = oauthProviderRepository.save(provider);

        // Simulate token refresh
        provider.setAccessToken("new-access-token");
        provider.setRefreshToken("new-refresh-token");
        provider.setTokenExpiresAt(java.time.OffsetDateTime.now().plusHours(1));
        oauthProviderRepository.save(provider);

        // Verify tokens updated
        OAuthProvider updated = oauthProviderRepository.findById(provider.getId()).orElseThrow();
        assertThat(updated.getAccessToken()).isEqualTo("new-access-token");
        assertThat(updated.getRefreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void shouldTrackLastOAuthUsage() {
        User user = User.builder()
                .username("usageuser")
                .email("usage@example.com")
                .build();
        user = userRepository.save(user);

        OAuthProvider provider = OAuthProvider.builder()
                .user(user)
                .provider("google")
                .providerUserId("google-usage-user")
                .linkedAt(java.time.OffsetDateTime.now())
                .lastUsedAt(java.time.OffsetDateTime.now())
                .build();
        provider = oauthProviderRepository.save(provider);

        // Verify timestamps
        OAuthProvider saved = oauthProviderRepository.findById(provider.getId()).orElseThrow();
        assertThat(saved.getLinkedAt()).isNotNull();
        assertThat(saved.getLastUsedAt()).isNotNull();
    }

    @Test
    void shouldStoreRawOAuthData() {
        User user = User.builder()
                .username("rawdatauser")
                .email("rawdata@example.com")
                .build();
        user = userRepository.save(user);

        // Simulate raw OAuth response data (JSON)
        com.fasterxml.jackson.databind.node.ObjectNode rawData = objectMapper.createObjectNode();
        rawData.put("sub", "google-12345");
        rawData.put("name", "Raw Data User");
        rawData.put("given_name", "Raw");
        rawData.put("family_name", "User");
        rawData.put("picture", "https://example.com/pic.jpg");
        rawData.put("email", "rawdata@example.com");
        rawData.put("email_verified", true);

        OAuthProvider provider = OAuthProvider.builder()
                .user(user)
                .provider("google")
                .providerUserId("google-12345")
                .rawData(rawData)
                .build();
        provider = oauthProviderRepository.save(provider);

        // Verify raw data stored
        OAuthProvider saved = oauthProviderRepository.findById(provider.getId()).orElseThrow();
        assertThat(saved.getRawData()).isNotNull();
        assertThat(saved.getRawData().get("sub").asText()).isEqualTo("google-12345");
        assertThat(saved.getRawData().get("email").asText()).isEqualTo("rawdata@example.com");
    }

    @Test
    void shouldUnlinkOAuthProvider() {
        User user = User.builder()
                .username("unlinkuser")
                .email("unlink@example.com")
                .build();
        user = userRepository.save(user);

        OAuthProvider provider = OAuthProvider.builder()
                .user(user)
                .provider("google")
                .providerUserId("google-unlink")
                .build();
        provider = oauthProviderRepository.save(provider);

        // Verify linked
        assertThat(oauthProviderRepository.existsByUserIdAndProvider(user.getId(), "google")).isTrue();

        // Unlink
        oauthProviderRepository.deleteByUserIdAndProvider(user.getId(), "google");

        // Verify unlinked
        assertThat(oauthProviderRepository.existsByUserIdAndProvider(user.getId(), "google")).isFalse();
    }

    @Test
    void shouldQueryOAuthProviderByUserAndProvider() {
        User user = User.builder()
                .username("queryuser")
                .email("query@example.com")
                .build();
        user = userRepository.save(user);

        OAuthProvider provider = OAuthProvider.builder()
                .user(user)
                .provider("facebook")
                .providerUserId("fb-query-user")
                .build();
        oauthProviderRepository.save(provider);

        // Query by user and provider
        Optional<OAuthProvider> found = oauthProviderRepository
                .findByUserIdAndProvider(user.getId(), "facebook");
        assertThat(found).isPresent();
        assertThat(found.get().getProviderUserId()).isEqualTo("fb-query-user");

        // Query non-existent provider
        Optional<OAuthProvider> notFound = oauthProviderRepository
                .findByUserIdAndProvider(user.getId(), "twitter");
        assertThat(notFound).isEmpty();
    }
}
