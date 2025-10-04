package com.cena.traveloka.iam.service;

import com.cena.traveloka.common.enums.Status;
import com.cena.traveloka.iam.dto.response.AuthResponse;
import com.cena.traveloka.iam.dto.response.UserDto;
import com.cena.traveloka.iam.entity.OAuthProvider;
import com.cena.traveloka.iam.entity.User;
import com.cena.traveloka.iam.mapper.UserMapper;
import com.cena.traveloka.iam.repository.OAuthProviderRepository;
import com.cena.traveloka.iam.repository.UserRepository;
import com.cena.traveloka.iam.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * T060: OAuthService
 * Service for OAuth provider operations.
 *
 * Constitutional Compliance:
 * - FR-012: OAuth2 integration (Google, Facebook, Apple)
 * - Principle III: Layered Architecture - Business logic in service layer
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OAuthService {

    private final OAuthProviderRepository oauthProviderRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final SessionService sessionService;
    private final UserMapper userMapper;

    /**
     * Handle OAuth login/registration (FR-012).
     *
     * @param provider Provider name (google, facebook, apple)
     * @param providerUserId Provider-specific user ID
     * @param email User email from provider
     * @param name User name from provider
     * @param avatarUrl Avatar URL from provider
     * @param accessToken OAuth access token
     * @param refreshToken OAuth refresh token
     * @param tokenExpiresAt Token expiration time
     * @param rawData Raw OAuth response data
     * @param ipAddress Client IP address
     * @param userAgent Client user agent
     * @return AuthResponse with JWT tokens
     */
    public AuthResponse handleOAuthLogin(
            String provider,
            String providerUserId,
            String email,
            String name,
            String avatarUrl,
            String accessToken,
            String refreshToken,
            OffsetDateTime tokenExpiresAt,
            JsonNode rawData,
            String ipAddress,
            String userAgent
    ) {
        // Check if OAuth provider is already linked
        Optional<OAuthProvider> existingOAuth = oauthProviderRepository
                .findByProviderAndProviderUserId(provider, providerUserId);

        User user;

        if (existingOAuth.isPresent()) {
            // Existing OAuth link - login
            user = existingOAuth.get().getUser();

            // Update OAuth tokens
            OAuthProvider oauthProvider = existingOAuth.get();
            oauthProvider.setAccessToken(accessToken);
            oauthProvider.setRefreshToken(refreshToken);
            oauthProvider.setTokenExpiresAt(tokenExpiresAt);
            oauthProvider.setRawData(rawData);
            oauthProvider.setLastUsedAt(OffsetDateTime.now());
            oauthProviderRepository.save(oauthProvider);

            log.info("OAuth login successful for provider: {} and user: {}", provider, user.getId());
        } else {
            // Check if user exists by email
            Optional<User> existingUser = userRepository.findByEmail(email);

            if (existingUser.isPresent()) {
                // Link OAuth to existing account
                user = existingUser.get();
                createOAuthLink(user, provider, providerUserId, email, name, avatarUrl,
                               accessToken, refreshToken, tokenExpiresAt, rawData);

                log.info("OAuth provider {} linked to existing user: {}", provider, user.getId());
            } else {
                // Create new user with OAuth
                user = createUserFromOAuth(email, name, provider);
                createOAuthLink(user, provider, providerUserId, email, name, avatarUrl,
                               accessToken, refreshToken, tokenExpiresAt, rawData);

                log.info("New user created via OAuth provider: {}", provider);
            }
        }

        // Generate JWT tokens
        Authentication authentication = createAuthentication(user);
        String jwtAccessToken = jwtTokenProvider.generateAccessToken(
                authentication,
                user.getId().toString(),
                user.getEmail()
        );
        String jwtRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId().toString());

        // Create session
        sessionService.createSession(user, jwtAccessToken, jwtRefreshToken, ipAddress, userAgent);

        UserDto userDto = userMapper.toDto(user);

        return AuthResponse.builder()
                .accessToken(jwtAccessToken)
                .refreshToken(jwtRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .user(userDto)
                .build();
    }

    /**
     * Link OAuth provider to existing user.
     *
     * @param userId User ID
     * @param provider Provider name
     * @param providerUserId Provider user ID
     * @param email Email from provider
     * @param name Name from provider
     * @param avatarUrl Avatar URL
     * @param accessToken OAuth access token
     * @param refreshToken OAuth refresh token
     * @param tokenExpiresAt Token expiration
     * @param rawData Raw OAuth data
     */
    public void linkOAuthProvider(
            UUID userId,
            String provider,
            String providerUserId,
            String email,
            String name,
            String avatarUrl,
            String accessToken,
            String refreshToken,
            OffsetDateTime tokenExpiresAt,
            JsonNode rawData
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Check if provider already linked
        if (oauthProviderRepository.existsByUserIdAndProvider(userId, provider)) {
            throw new RuntimeException("OAuth provider already linked: " + provider);
        }

        // Check if provider account is used by another user
        Optional<OAuthProvider> existingOAuth = oauthProviderRepository
                .findByProviderAndProviderUserId(provider, providerUserId);

        if (existingOAuth.isPresent()) {
            throw new RuntimeException("OAuth account already linked to another user");
        }

        createOAuthLink(user, provider, providerUserId, email, name, avatarUrl,
                       accessToken, refreshToken, tokenExpiresAt, rawData);

        log.info("OAuth provider {} linked to user: {}", provider, userId);
    }

    /**
     * Unlink OAuth provider from user.
     *
     * @param userId User ID
     * @param provider Provider name
     */
    public void unlinkOAuthProvider(UUID userId, String provider) {
        if (!oauthProviderRepository.existsByUserIdAndProvider(userId, provider)) {
            throw new RuntimeException("OAuth provider not linked: " + provider);
        }

        oauthProviderRepository.deleteByUserIdAndProvider(userId, provider);

        log.info("OAuth provider {} unlinked from user: {}", provider, userId);
    }

    /**
     * Get user's OAuth providers.
     *
     * @param userId User ID
     * @return List of OAuthProvider
     */
    @Transactional(readOnly = true)
    public List<OAuthProvider> getUserOAuthProviders(UUID userId) {
        return oauthProviderRepository.findByUserId(userId);
    }

    /**
     * Check if OAuth provider is linked to user.
     *
     * @param userId User ID
     * @param provider Provider name
     * @return true if linked
     */
    @Transactional(readOnly = true)
    public boolean isProviderLinked(UUID userId, String provider) {
        return oauthProviderRepository.existsByUserIdAndProvider(userId, provider);
    }

    /**
     * Update OAuth tokens.
     *
     * @param userId User ID
     * @param provider Provider name
     * @param accessToken New access token
     * @param refreshToken New refresh token
     * @param tokenExpiresAt New expiration time
     */
    public void updateOAuthTokens(
            UUID userId,
            String provider,
            String accessToken,
            String refreshToken,
            OffsetDateTime tokenExpiresAt
    ) {
        OAuthProvider oauthProvider = oauthProviderRepository.findByUserIdAndProvider(userId, provider)
                .orElseThrow(() -> new RuntimeException("OAuth provider not linked"));

        oauthProvider.setAccessToken(accessToken);
        oauthProvider.setRefreshToken(refreshToken);
        oauthProvider.setTokenExpiresAt(tokenExpiresAt);
        oauthProviderRepository.save(oauthProvider);

        log.info("OAuth tokens updated for user: {} and provider: {}", userId, provider);
    }

    /**
     * Get OAuth provider by user and provider name.
     *
     * @param userId User ID
     * @param provider Provider name
     * @return OAuthProvider
     */
    @Transactional(readOnly = true)
    public OAuthProvider getOAuthProvider(UUID userId, String provider) {
        return oauthProviderRepository.findByUserIdAndProvider(userId, provider)
                .orElseThrow(() -> new RuntimeException("OAuth provider not found"));
    }

    // Private helper methods

    /**
     * Create user from OAuth data.
     *
     * @param email Email
     * @param name Full name
     * @param provider OAuth provider
     * @return Created User
     */
    private User createUserFromOAuth(String email, String name, String provider) {
        // Parse first name and last name
        String[] nameParts = name != null ? name.split(" ", 2) : new String[]{"", ""};
        String firstName = nameParts.length > 0 ? nameParts[0] : "";
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        // Generate username from email
        String username = email.split("@")[0] + "_" + provider;

        // Check if username exists, add suffix if needed
        int suffix = 1;
        String finalUsername = username;
        while (userRepository.existsByUsername(finalUsername)) {
            finalUsername = username + "_" + suffix++;
        }

        User user = User.builder()
                .username(finalUsername)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .status(Status.active)
                .emailVerified(true) // OAuth emails are pre-verified
                // Password managed by Keycloak, not stored locally
                .termsAcceptedAt(OffsetDateTime.now())
                .privacyAcceptedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();

        return userRepository.save(user);
    }

    /**
     * Create OAuth provider link.
     *
     * @param user User entity
     * @param provider Provider name
     * @param providerUserId Provider user ID
     * @param email Email
     * @param name Name
     * @param avatarUrl Avatar URL
     * @param accessToken Access token
     * @param refreshToken Refresh token
     * @param tokenExpiresAt Token expiration
     * @param rawData Raw OAuth data
     * @return Created OAuthProvider
     */
    private OAuthProvider createOAuthLink(
            User user,
            String provider,
            String providerUserId,
            String email,
            String name,
            String avatarUrl,
            String accessToken,
            String refreshToken,
            OffsetDateTime tokenExpiresAt,
            JsonNode rawData
    ) {
        OAuthProvider oauthProvider = OAuthProvider.builder()
                .user(user)
                .provider(provider)
                .providerUserId(providerUserId)
                .email(email)
                .name(name)
                .avatarUrl(avatarUrl)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenExpiresAt(tokenExpiresAt)
                .rawData(rawData)
                .linkedAt(OffsetDateTime.now())
                .lastUsedAt(OffsetDateTime.now())
                .build();

        return oauthProviderRepository.save(oauthProvider);
    }

    /**
     * Get OAuth authorization URL for provider.
     *
     * @param provider OAuth provider (google, facebook, apple)
     * @return Authorization URL
     */
    public String getAuthorizationUrl(String provider) {
        // TODO: Generate OAuth authorization URL based on provider
        // For now, return placeholder
        return "https://" + provider + ".com/oauth/authorize";
    }

    /**
     * Handle OAuth callback with authorization code.
     *
     * @param provider OAuth provider
     * @param code Authorization code
     * @param state CSRF state
     * @param ipAddress Client IP
     * @param userAgent User agent
     * @return AuthResponse with JWT tokens
     */
    public AuthResponse handleOAuthCallback(
            String provider,
            String code,
            String state,
            String ipAddress,
            String userAgent
    ) {
        // TODO: Exchange authorization code for tokens
        // TODO: Get user info from provider
        // TODO: Call handleOAuthLogin with user data
        throw new UnsupportedOperationException("handleOAuthCallback not yet implemented - requires OAuth client configuration");
    }

    /**
     * Create Spring Security Authentication object.
     *
     * @param user User entity
     * @return Authentication
     */
    private Authentication createAuthentication(User user) {
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                authorities
        );
    }
}
