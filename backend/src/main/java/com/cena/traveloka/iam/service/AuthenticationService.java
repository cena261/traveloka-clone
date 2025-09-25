package com.cena.traveloka.iam.service;

import com.cena.traveloka.iam.entity.AppUser;
import com.cena.traveloka.iam.entity.UserSession;
import com.cena.traveloka.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Service class for Authentication and OAuth2 integration
 *
 * Provides comprehensive authentication functionality including:
 * - OAuth2 JWT token processing and validation
 * - Keycloak user synchronization
 * - Session management integration
 * - Security context management
 * - Multi-device authentication tracking
 * - Token refresh and validation
 *
 * Key Features:
 * - Spring Security OAuth2 Resource Server integration
 * - Automatic user creation from Keycloak tokens
 * - Session lifecycle management
 * - Cache-optimized authentication checks
 * - Comprehensive audit logging
 * - Token introspection and validation
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthenticationService {

    private final UserRepository userRepository;
    private final UserSyncService userSyncService;
    private final SessionService sessionService;

    // === Authentication Processing ===

    /**
     * Process authentication from JWT token
     *
     * @param jwt JWT token from request
     * @param clientInfo Client information (IP, User-Agent, etc.)
     * @return Authenticated user with session
     */
    public AuthenticationResult processAuthentication(Jwt jwt, ClientInfo clientInfo) {
        log.info("Processing authentication for subject: {}", jwt.getSubject());

        try {
            // Extract user information from JWT
            String keycloakId = jwt.getSubject();
            String email = jwt.getClaimAsString("email");
            String firstName = jwt.getClaimAsString("given_name");
            String lastName = jwt.getClaimAsString("family_name");

            // Find or create user
            AppUser user = findOrCreateUser(keycloakId, email, firstName, lastName);

            // Create or update session
            UserSession session = sessionService.createSession(
                    user.getId().toString(),
                    extractDeviceId(clientInfo.getUserAgent()),
                    extractDeviceType(clientInfo.getUserAgent()),
                    generateSessionId(jwt),
                    clientInfo.getIpAddress(),
                    clientInfo.getUserAgent()
            );

            // Update last login
            updateUserLastLogin(user, clientInfo);

            log.info("Successfully authenticated user: {} with session: {}", user.getId(), session.getSessionId());

            return AuthenticationResult.builder()
                    .user(user)
                    .session(session)
                    .authenticated(true)
                    .authenticationTime(Instant.now())
                    .build();

        } catch (Exception e) {
            log.error("Authentication processing failed for subject: {}", jwt.getSubject(), e);
            throw new AuthenticationException("Authentication processing failed", e);
        }
    }

    /**
     * Find existing user or create new user from Keycloak data
     *
     * @param keycloakId Keycloak user ID
     * @param email User email
     * @param firstName User first name
     * @param lastName User last name
     * @return User entity
     */
    private AppUser findOrCreateUser(String keycloakId, String email, String firstName, String lastName) {
        // Try to find by Keycloak ID first
        Optional<AppUser> existingUser = userRepository.findByKeycloakId(keycloakId);
        if (existingUser.isPresent()) {
            log.debug("Found existing user by Keycloak ID: {}", keycloakId);
            return existingUser.get();
        }

        // Try to find by email and link Keycloak ID
        existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            log.info("Linking existing user {} with Keycloak ID: {}", email, keycloakId);
            AppUser user = existingUser.get();
            user.setKeycloakId(keycloakId);
            user.markSynced();
            return userRepository.save(user);
        }

        // Create new user from Keycloak data
        log.info("Creating new user from Keycloak data: {}", email);
        AppUser newUser = new AppUser();
        newUser.setKeycloakId(keycloakId);
        newUser.setEmail(email);
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setUsername(email); // Use email as username
        newUser.setCreatedBy("KEYCLOAK_SYNC");
        newUser.setUpdatedBy("KEYCLOAK_SYNC");
        newUser = userRepository.save(newUser);

        // Create sync event for user creation
        userSyncService.createUserSyncEvent("USER_CREATED", newUser, "KEYCLOAK_TO_LOCAL");

        return newUser;
    }

    /**
     * Update user's last login information
     *
     * @param user User to update
     * @param clientInfo Client information
     */
    private void updateUserLastLogin(AppUser user, ClientInfo clientInfo) {
        user.setLastLoginAt(Instant.now());
        user.setLastLoginIp(clientInfo.getIpAddress());
        user.updateProfileCompleteness();
        userRepository.save(user);
    }

    /**
     * Generate session ID from JWT
     *
     * @param jwt JWT token
     * @return Session ID
     */
    private String generateSessionId(Jwt jwt) {
        // Use JWT ID (jti) if available, otherwise generate from token hash
        String jti = jwt.getClaimAsString("jti");
        if (StringUtils.hasText(jti)) {
            return jti;
        }
        return "session_" + jwt.getTokenValue().hashCode();
    }

    // === Current User Operations ===

    /**
     * Get currently authenticated user
     *
     * @return Current user if authenticated
     */
    @Cacheable(value = "currentUser", key = "'current:' + authentication.name")
    @Transactional(readOnly = true)
    public Optional<AppUser> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("No authenticated user found in security context");
            return Optional.empty();
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String keycloakId = jwt.getSubject();

            log.debug("Getting current user by Keycloak ID: {}", keycloakId);
            return userRepository.findByKeycloakId(keycloakId);
        }

        log.warn("Unexpected authentication type: {}", authentication.getClass());
        return Optional.empty();
    }

    /**
     * Get current user ID
     *
     * @return Current user ID if authenticated
     */
    @Transactional(readOnly = true)
    public Optional<String> getCurrentUserId() {
        return getCurrentUser().map(user -> user.getId().toString());
    }

    /**
     * Get current JWT token
     *
     * @return Current JWT token if available
     */
    @Transactional(readOnly = true)
    public Optional<Jwt> getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return Optional.of(jwtAuth.getToken());
        }

        return Optional.empty();
    }

    /**
     * Check if current user has specific role
     *
     * @param roleName Role name to check
     * @return true if user has the role
     */
    @Transactional(readOnly = true)
    public boolean hasRole(String roleName) {
        return getCurrentJwt()
                .map(jwt -> {
                    // Check realm roles
                    Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
                    if (realmAccess != null && realmAccess.containsKey("roles")) {
                        @SuppressWarnings("unchecked")
                        java.util.List<String> roles = (java.util.List<String>) realmAccess.get("roles");
                        return roles.contains(roleName);
                    }
                    return false;
                })
                .orElse(false);
    }

    /**
     * Check if current user has any of the specified roles
     *
     * @param roleNames Role names to check
     * @return true if user has any of the roles
     */
    @Transactional(readOnly = true)
    public boolean hasAnyRole(String... roleNames) {
        for (String roleName : roleNames) {
            if (hasRole(roleName)) {
                return true;
            }
        }
        return false;
    }

    // === Session Management ===

    /**
     * Logout current user and invalidate session
     *
     * @param allDevices Whether to logout from all devices
     */
    @CacheEvict(value = "currentUser", allEntries = true)
    public void logout(boolean allDevices) {
        Optional<AppUser> currentUser = getCurrentUser();

        if (currentUser.isEmpty()) {
            log.debug("No current user to logout");
            return;
        }

        AppUser user = currentUser.get();
        log.info("Logging out user: {} (allDevices: {})", user.getId(), allDevices);

        if (allDevices) {
            // Invalidate all sessions for the user
            sessionService.terminateAllUserSessions(user.getId().toString(), "USER_LOGOUT");
        } else {
            // Invalidate current session only
            getCurrentJwt().ifPresent(jwt -> {
                String sessionId = generateSessionId(jwt);
                sessionService.terminateSession(sessionId, "USER_LOGOUT");
            });
        }

        // Clear security context
        SecurityContextHolder.clearContext();

        log.info("Successfully logged out user: {}", user.getId());
    }

    /**
     * Validate current session
     *
     * @return Session validation result
     */
    @Transactional(readOnly = true)
    public SessionValidationResult validateCurrentSession() {
        Optional<Jwt> jwt = getCurrentJwt();
        Optional<AppUser> user = getCurrentUser();

        if (jwt.isEmpty() || user.isEmpty()) {
            return SessionValidationResult.invalid("No valid authentication context");
        }

        String sessionId = generateSessionId(jwt.get());
        Optional<UserSession> session = sessionService.findActiveSession(sessionId);

        if (session.isEmpty()) {
            return SessionValidationResult.invalid("Session not found or expired");
        }

        UserSession userSession = session.get();

        // Check if session belongs to the current user
        if (!userSession.getUserId().equals(user.get().getId())) {
            return SessionValidationResult.invalid("Session user mismatch");
        }

        // Check session expiry
        if (userSession.getExpiresAt().isBefore(OffsetDateTime.now())) {
            return SessionValidationResult.invalid("Session expired");
        }

        // Check user status
        if (!"ACTIVE".equals(user.get().getStatus().name())) {
            return SessionValidationResult.invalid("User account not active");
        }

        return SessionValidationResult.valid(userSession);
    }

    // === Token Operations ===

    /**
     * Refresh authentication context
     * This method should be called when user data might have changed
     */
    @CacheEvict(value = "currentUser", allEntries = true)
    public void refreshAuthenticationContext() {
        log.debug("Refreshing authentication context");
        // Cache eviction will force fresh user data on next access
    }

    /**
     * Extract user information from JWT token
     *
     * @param jwt JWT token
     * @return User information
     */
    @Transactional(readOnly = true)
    public UserInfo extractUserInfo(Jwt jwt) {
        return UserInfo.builder()
                .keycloakId(jwt.getSubject())
                .email(jwt.getClaimAsString("email"))
                .firstName(jwt.getClaimAsString("given_name"))
                .lastName(jwt.getClaimAsString("family_name"))
                .username(jwt.getClaimAsString("preferred_username"))
                .emailVerified(jwt.getClaimAsBoolean("email_verified"))
                .issuedAt(jwt.getIssuedAt())
                .expiresAt(jwt.getExpiresAt())
                .build();
    }

    // === Helper Methods ===

    /**
     * Extract device ID from user agent string
     *
     * @param userAgent User agent string
     * @return Device ID or generated ID
     */
    private String extractDeviceId(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "unknown-" + System.currentTimeMillis();
        }

        // Simple device ID extraction based on user agent hash
        return "device-" + Math.abs(userAgent.hashCode());
    }

    /**
     * Extract device type from user agent string
     *
     * @param userAgent User agent string
     * @return Device type
     */
    private String extractDeviceType(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "UNKNOWN";
        }

        String ua = userAgent.toLowerCase();

        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return "MOBILE";
        } else if (ua.contains("tablet") || ua.contains("ipad")) {
            return "TABLET";
        } else {
            return "DESKTOP";
        }
    }

    // === Supporting Classes ===

    /**
     * Client information for authentication tracking
     */
    public static class ClientInfo {
        private final String ipAddress;
        private final String userAgent;
        private final String deviceInfo;

        public ClientInfo(String ipAddress, String userAgent, String deviceInfo) {
            this.ipAddress = ipAddress;
            this.userAgent = userAgent;
            this.deviceInfo = deviceInfo;
        }

        public String getIpAddress() { return ipAddress; }
        public String getUserAgent() { return userAgent; }
        public String getDeviceInfo() { return deviceInfo; }
    }

    /**
     * Authentication result
     */
    public static class AuthenticationResult {
        private final AppUser user;
        private final UserSession session;
        private final boolean authenticated;
        private final Instant authenticationTime;

        private AuthenticationResult(AppUser user, UserSession session, boolean authenticated, Instant authenticationTime) {
            this.user = user;
            this.session = session;
            this.authenticated = authenticated;
            this.authenticationTime = authenticationTime;
        }

        public static Builder builder() {
            return new Builder();
        }

        public AppUser getUser() { return user; }
        public UserSession getSession() { return session; }
        public boolean isAuthenticated() { return authenticated; }
        public Instant getAuthenticationTime() { return authenticationTime; }

        public static class Builder {
            private AppUser user;
            private UserSession session;
            private boolean authenticated;
            private Instant authenticationTime;

            public Builder user(AppUser user) { this.user = user; return this; }
            public Builder session(UserSession session) { this.session = session; return this; }
            public Builder authenticated(boolean authenticated) { this.authenticated = authenticated; return this; }
            public Builder authenticationTime(Instant authenticationTime) { this.authenticationTime = authenticationTime; return this; }

            public AuthenticationResult build() {
                return new AuthenticationResult(user, session, authenticated, authenticationTime);
            }
        }
    }

    /**
     * Session validation result
     */
    public static class SessionValidationResult {
        private final boolean valid;
        private final String reason;
        private final UserSession session;

        private SessionValidationResult(boolean valid, String reason, UserSession session) {
            this.valid = valid;
            this.reason = reason;
            this.session = session;
        }

        public static SessionValidationResult valid(UserSession session) {
            return new SessionValidationResult(true, null, session);
        }

        public static SessionValidationResult invalid(String reason) {
            return new SessionValidationResult(false, reason, null);
        }

        public boolean isValid() { return valid; }
        public String getReason() { return reason; }
        public UserSession getSession() { return session; }
    }

    /**
     * User information extracted from JWT
     */
    public static class UserInfo {
        private final String keycloakId;
        private final String email;
        private final String firstName;
        private final String lastName;
        private final String username;
        private final Boolean emailVerified;
        private final Instant issuedAt;
        private final Instant expiresAt;

        private UserInfo(String keycloakId, String email, String firstName, String lastName,
                        String username, Boolean emailVerified, Instant issuedAt, Instant expiresAt) {
            this.keycloakId = keycloakId;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.username = username;
            this.emailVerified = emailVerified;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getKeycloakId() { return keycloakId; }
        public String getEmail() { return email; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getUsername() { return username; }
        public Boolean getEmailVerified() { return emailVerified; }
        public Instant getIssuedAt() { return issuedAt; }
        public Instant getExpiresAt() { return expiresAt; }

        public static class Builder {
            private String keycloakId;
            private String email;
            private String firstName;
            private String lastName;
            private String username;
            private Boolean emailVerified;
            private Instant issuedAt;
            private Instant expiresAt;

            public Builder keycloakId(String keycloakId) { this.keycloakId = keycloakId; return this; }
            public Builder email(String email) { this.email = email; return this; }
            public Builder firstName(String firstName) { this.firstName = firstName; return this; }
            public Builder lastName(String lastName) { this.lastName = lastName; return this; }
            public Builder username(String username) { this.username = username; return this; }
            public Builder emailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; return this; }
            public Builder issuedAt(Instant issuedAt) { this.issuedAt = issuedAt; return this; }
            public Builder expiresAt(Instant expiresAt) { this.expiresAt = expiresAt; return this; }

            public UserInfo build() {
                return new UserInfo(keycloakId, email, firstName, lastName, username, emailVerified, issuedAt, expiresAt);
            }
        }
    }

    /**
     * Authentication exception
     */
    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }

        public AuthenticationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}