package com.cena.traveloka.iam.mapper;

import com.cena.traveloka.iam.dto.request.LoginRequest;
import com.cena.traveloka.iam.dto.response.LoginResponse;
import com.cena.traveloka.iam.dto.response.LogoutResponse;
import com.cena.traveloka.iam.dto.response.TokenResponse;
import com.cena.traveloka.iam.dto.response.UserRes;
import com.cena.traveloka.iam.dto.response.SessionRes;
import com.cena.traveloka.iam.entity.AppUser;
import com.cena.traveloka.iam.entity.UserSession;
import com.cena.traveloka.iam.service.AuthenticationService;
import org.mapstruct.*;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

/**
 * MapStruct mapper for Authentication DTOs and responses
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {UserMapper.class, SessionMapper.class}
)
public interface AuthenticationMapper {

    // === Authentication Result to Response DTOs ===

    /**
     * Map AuthenticationResult to LoginResponse
     */
    @Mapping(target = "accessToken", ignore = true) // Set by service from JWT
    @Mapping(target = "refreshToken", ignore = true) // Set by service from JWT
    @Mapping(target = "tokenType", constant = "Bearer")
    @Mapping(target = "expiresIn", ignore = true) // Set by service from JWT
    @Mapping(target = "user", source = "user")
    @Mapping(target = "session", source = "session")
    @Mapping(target = "roles", ignore = true) // Set by service from JWT
    @Mapping(target = "permissions", ignore = true) // Set by service from JWT
    @Mapping(target = "loginTime", source = "authenticationTime")
    @Mapping(target = "firstLogin", source = ".", qualifiedByName = "isFirstLogin")
    @Mapping(target = "passwordChangeRequired", constant = "false") // Handled by Keycloak
    @Mapping(target = "mfaRequired", constant = "false") // Handled by Keycloak
    LoginResponse toLoginResponse(AuthenticationService.AuthenticationResult authResult);

    /**
     * Create LogoutResponse
     */
    default LogoutResponse createLogoutResponse(boolean success, String message, int sessionsTerminated) {
        LogoutResponse response = new LogoutResponse();
        response.setSuccess(success);
        response.setMessage(message);
        response.setLogoutTime(Instant.now());
        response.setSessionsTerminated(sessionsTerminated);
        return response;
    }

    /**
     * Map JWT to TokenResponse
     */
    @Mapping(target = "accessToken", source = "tokenValue")
    @Mapping(target = "refreshToken", ignore = true) // Not available from access token
    @Mapping(target = "tokenType", constant = "Bearer")
    @Mapping(target = "expiresIn", source = ".", qualifiedByName = "calculateExpiresIn")
    @Mapping(target = "issuedAt", source = "issuedAt")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "scope", source = ".", qualifiedByName = "extractScope")
    TokenResponse toTokenResponse(Jwt jwt);

    // === UserInfo Mapping ===

    /**
     * Map UserInfo to simple user data
     */
    default UserRes mapUserInfoToUserRes(AuthenticationService.UserInfo userInfo, AppUser user) {
        if (userInfo == null) {
            return null;
        }

        UserRes userRes = new UserRes();
        userRes.setEmail(userInfo.getEmail());
        userRes.setFirstName(userInfo.getFirstName());
        userRes.setLastName(userInfo.getLastName());
        userRes.setEmailVerified(userInfo.getEmailVerified());

        // If we have a full user entity, use its data
        if (user != null) {
            // Note: UserMapper dependency will be injected by MapStruct
            // This requires UserMapper to be available in the uses clause
            UserMapper userMapper = org.mapstruct.factory.Mappers.getMapper(UserMapper.class);
            return userMapper.toUserRes(user);
        }

        return userRes;
    }

    // === Custom Mapping Methods ===

    /**
     * Check if this is the user's first login
     */
    @Named("isFirstLogin")
    default Boolean isFirstLogin(AuthenticationService.AuthenticationResult authResult) {
        if (authResult == null || authResult.getUser() == null) {
            return false;
        }

        AppUser user = authResult.getUser();
        return user.getLastLoginAt() == null ||
               user.getLastLoginAt().equals(authResult.getAuthenticationTime());
    }

    /**
     * Calculate expires in seconds from JWT
     */
    @Named("calculateExpiresIn")
    default Long calculateExpiresIn(Jwt jwt) {
        if (jwt == null || jwt.getExpiresAt() == null) {
            return null;
        }

        long expiresIn = jwt.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, expiresIn);
    }

    /**
     * Extract scope from JWT
     */
    @Named("extractScope")
    default String extractScope(Jwt jwt) {
        if (jwt == null) {
            return null;
        }

        Object scope = jwt.getClaim("scope");
        if (scope instanceof String) {
            return (String) scope;
        }

        if (scope instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> scopes = (List<String>) scope;
            return String.join(" ", scopes);
        }

        return null;
    }

    // === Enhanced LoginResponse Builder ===

    /**
     * Build complete LoginResponse with JWT data
     */
    default LoginResponse buildLoginResponse(
            AuthenticationService.AuthenticationResult authResult,
            Jwt jwt,
            List<String> roles,
            List<String> permissions) {

        LoginResponse response = toLoginResponse(authResult);

        if (jwt != null) {
            response.setAccessToken(jwt.getTokenValue());
            response.setExpiresIn(calculateExpiresIn(jwt));
        }

        response.setRoles(roles);
        response.setPermissions(permissions);

        return response;
    }

    /**
     * Build TokenResponse with refresh token
     */
    default TokenResponse buildTokenResponse(Jwt accessToken, String refreshToken) {
        TokenResponse response = toTokenResponse(accessToken);
        response.setRefreshToken(refreshToken);
        return response;
    }

    // === Validation Methods ===

    /**
     * Validate and map login request
     */
    default boolean isValidLoginRequest(LoginRequest request) {
        return request != null &&
               request.getAccessToken() != null &&
               !request.getAccessToken().trim().isEmpty();
    }

    /**
     * Create client info from login request
     */
    default AuthenticationService.ClientInfo createClientInfo(LoginRequest request) {
        if (request == null) {
            return new AuthenticationService.ClientInfo(null, null, null);
        }

        return new AuthenticationService.ClientInfo(
            request.getIpAddress(),
            request.getUserAgent(),
            request.getDeviceInfo()
        );
    }

    // === Error Response Helpers ===

    /**
     * Create failed login response
     */
    default LoginResponse createFailedLoginResponse(String errorMessage) {
        LoginResponse response = new LoginResponse();
        response.setLoginTime(Instant.now());
        // Note: In a real implementation, you might want to use a separate error structure
        return response;
    }

    /**
     * Create failed logout response
     */
    default LogoutResponse createFailedLogoutResponse(String errorMessage) {
        return createLogoutResponse(false, errorMessage, 0);
    }
}