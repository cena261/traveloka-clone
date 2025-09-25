package com.cena.traveloka.iam.controller;

import com.cena.traveloka.iam.dto.request.LoginRequest;
import com.cena.traveloka.iam.dto.request.LogoutRequest;
import com.cena.traveloka.iam.dto.request.RefreshTokenRequest;
import com.cena.traveloka.iam.dto.response.*;
import com.cena.traveloka.iam.mapper.AuthenticationMapper;
import com.cena.traveloka.iam.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Authentication and OAuth2 Operations
 *
 * Provides endpoints for OAuth2 authentication, session management, and token operations
 * Integrates with Keycloak for identity management
 */
@RestController
@RequestMapping("/api/iam/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "OAuth2 authentication and token management")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final AuthenticationMapper authenticationMapper;

    // === Authentication Operations ===

    /**
     * Process OAuth2 login
     */
    @PostMapping("/login")
    @Operation(summary = "OAuth2 Login", description = "Processes OAuth2 authentication and creates user session")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        log.info("Processing login request for IP: {}", request.getIpAddress());

        try {
            // Validate login request
            if (!authenticationMapper.isValidLoginRequest(request)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.badRequest("Invalid login request"));
            }

            // Extract client information
            AuthenticationService.ClientInfo clientInfo = authenticationMapper.createClientInfo(request);

            // TODO: Process JWT token from request.getAccessToken()
            // For now, return a mock response indicating OAuth2 flow should be handled by Keycloak
            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setTokenType("Bearer");

            return ResponseEntity.ok(ApiResponse.success("Login processed successfully", loginResponse));

        } catch (Exception e) {
            log.error("Login processing failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.unauthorized("Authentication failed: " + e.getMessage()));
        }
    }

    /**
     * Process logout
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Terminates user session and invalidates tokens")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(
            @Valid @RequestBody LogoutRequest request,
            Authentication authentication) {

        log.info("Processing logout request (allDevices: {})", request.getAllDevices());

        try {
            authenticationService.logout(request.getAllDevices());

            LogoutResponse logoutResponse = authenticationMapper.createLogoutResponse(
                    true, "Logout successful", request.getAllDevices() ? 1 : 1);

            return ResponseEntity.ok(ApiResponse.success("Logout successful", logoutResponse));

        } catch (Exception e) {
            log.error("Logout processing failed", e);
            LogoutResponse logoutResponse = authenticationMapper.createFailedLogoutResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Logout failed", ErrorResponse.internalServerError(e.getMessage())));
        }
    }

    /**
     * Refresh access token
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh Token", description = "Refreshes access token using refresh token")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid refresh token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        log.info("Processing token refresh request");

        try {
            // TODO: Implement token refresh with Keycloak
            // For now, return a placeholder response
            TokenResponse tokenResponse = new TokenResponse();
            tokenResponse.setTokenType("Bearer");

            return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", tokenResponse));

        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.unauthorized("Token refresh failed: " + e.getMessage()));
        }
    }

    // === Current User Operations ===

    /**
     * Get current user information
     */
    @GetMapping("/me")
    @Operation(summary = "Get Current User", description = "Retrieves current authenticated user information")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserRes>> getCurrentUser(Authentication authentication) {

        log.debug("Getting current user information");

        try {
            Optional<com.cena.traveloka.iam.entity.AppUser> currentUser = authenticationService.getCurrentUser();

            if (currentUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Current user not found"));
            }

            // Map user to response DTO
            UserRes userRes = new UserRes();
            com.cena.traveloka.iam.entity.AppUser user = currentUser.get();
            userRes.setId(user.getId().toString());
            userRes.setEmail(user.getEmail());
            userRes.setFirstName(user.getFirstName());
            userRes.setLastName(user.getLastName());
            userRes.setStatus(user.getStatus());

            return ResponseEntity.ok(ApiResponse.success(userRes));

        } catch (Exception e) {
            log.error("Failed to get current user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.internalServerError("Failed to retrieve user information"));
        }
    }

    /**
     * Get current user roles
     */
    @GetMapping("/me/roles")
    @Operation(summary = "Get Current User Roles", description = "Retrieves current user's roles and permissions")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<String>>> getCurrentUserRoles(Authentication authentication) {

        log.debug("Getting current user roles");

        try {
            List<String> roles = List.of(); // TODO: Extract roles from JWT or database

            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                // Extract roles from JWT claims
                Object realmAccess = jwt.getClaim("realm_access");
                if (realmAccess instanceof java.util.Map<?, ?> realmAccessMap) {
                    Object rolesObj = realmAccessMap.get("roles");
                    if (rolesObj instanceof List<?> rolesList) {
                        roles = rolesList.stream()
                                .filter(String.class::isInstance)
                                .map(String.class::cast)
                                .toList();
                    }
                }
            }

            return ResponseEntity.ok(ApiResponse.success(roles));

        } catch (Exception e) {
            log.error("Failed to get current user roles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.internalServerError("Failed to retrieve user roles"));
        }
    }

    /**
     * Check if user has specific role
     */
    @GetMapping("/me/has-role/{roleName}")
    @Operation(summary = "Check User Role", description = "Checks if current user has specific role")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Boolean>> hasRole(
            @PathVariable String roleName,
            Authentication authentication) {

        log.debug("Checking if current user has role: {}", roleName);

        try {
            boolean hasRole = authenticationService.hasRole(roleName);
            return ResponseEntity.ok(ApiResponse.success(hasRole));

        } catch (Exception e) {
            log.error("Failed to check user role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.internalServerError("Failed to check user role"));
        }
    }

    // === Session Validation ===

    /**
     * Validate current session
     */
    @GetMapping("/validate-session")
    @Operation(summary = "Validate Session", description = "Validates current user session")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Object>> validateSession(Authentication authentication) {

        log.debug("Validating current session");

        try {
            AuthenticationService.SessionValidationResult result = authenticationService.validateCurrentSession();

            if (result.isValid()) {
                return ResponseEntity.ok(ApiResponse.success("Session is valid", Map.of(
                        "valid", true,
                        "sessionId", result.getSession().getSessionId(),
                        "expiresAt", result.getSession().getExpiresAt()
                )));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.unauthorized("Session is invalid: " + result.getReason()));
            }

        } catch (Exception e) {
            log.error("Session validation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.internalServerError("Session validation failed"));
        }
    }

    /**
     * Refresh authentication context
     */
    @PostMapping("/refresh-context")
    @Operation(summary = "Refresh Authentication Context", description = "Refreshes cached user data")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> refreshContext(Authentication authentication) {

        log.info("Refreshing authentication context");

        try {
            authenticationService.refreshAuthenticationContext();
            return ResponseEntity.ok(ApiResponse.success("Authentication context refreshed"));

        } catch (Exception e) {
            log.error("Failed to refresh authentication context", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.internalServerError("Failed to refresh context"));
        }
    }

    // === Token Operations ===

    /**
     * Get current JWT token information
     */
    @GetMapping("/token-info")
    @Operation(summary = "Get Token Info", description = "Retrieves information about current JWT token")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Object>> getTokenInfo(Authentication authentication) {

        log.debug("Getting token information");

        try {
            Optional<Jwt> jwt = authenticationService.getCurrentJwt();

            if (jwt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("No JWT token found"));
            }

            AuthenticationService.UserInfo userInfo = authenticationService.extractUserInfo(jwt.get());

            Map<String, Object> tokenInfo = Map.of(
                    "subject", userInfo.getKeycloakId(),
                    "email", userInfo.getEmail(),
                    "firstName", userInfo.getFirstName(),
                    "lastName", userInfo.getLastName(),
                    "emailVerified", userInfo.getEmailVerified(),
                    "issuedAt", userInfo.getIssuedAt(),
                    "expiresAt", userInfo.getExpiresAt()
            );

            return ResponseEntity.ok(ApiResponse.success(tokenInfo));

        } catch (Exception e) {
            log.error("Failed to get token information", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.internalServerError("Failed to retrieve token information"));
        }
    }

    // === Health Check ===

    /**
     * Authentication health check
     */
    @GetMapping("/health")
    @Operation(summary = "Authentication Health Check", description = "Checks authentication service health")
    public ResponseEntity<ApiResponse<Object>> healthCheck() {

        log.debug("Authentication health check");

        try {
            Map<String, Object> health = Map.of(
                    "status", "UP",
                    "timestamp", java.time.Instant.now(),
                    "service", "IAM Authentication"
            );

            return ResponseEntity.ok(ApiResponse.success(health));

        } catch (Exception e) {
            log.error("Authentication health check failed", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.serviceUnavailable("Authentication service unavailable"));
        }
    }

    // === Development/Debug Endpoints ===

    /**
     * Get authentication context (for debugging)
     */
    @GetMapping("/debug/context")
    @Operation(summary = "Debug Authentication Context", description = "Returns authentication context for debugging")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> debugAuthContext(Authentication authentication) {

        log.debug("Getting authentication context for debugging");

        try {
            Map<String, Object> context = Map.of(
                    "authenticationType", authentication.getClass().getSimpleName(),
                    "principal", authentication.getPrincipal().toString(),
                    "authorities", authentication.getAuthorities().toString(),
                    "authenticated", authentication.isAuthenticated(),
                    "name", authentication.getName()
            );

            return ResponseEntity.ok(ApiResponse.success(context));

        } catch (Exception e) {
            log.error("Failed to get authentication context", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.internalServerError("Failed to retrieve authentication context"));
        }
    }
}
