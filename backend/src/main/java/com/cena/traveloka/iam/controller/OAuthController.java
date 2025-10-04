package com.cena.traveloka.iam.controller;

import com.cena.traveloka.iam.dto.response.AuthResponse;
import com.cena.traveloka.iam.service.OAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * T081-T082: OAuthController
 * REST API controller for OAuth authentication operations.
 *
 * Endpoints:
 * - GET /oauth/{provider}/authorize (T081)
 * - GET /oauth/{provider}/callback (T082)
 *
 * Supported providers: google, facebook, apple
 *
 * Constitutional Compliance:
 * - Principle III: Layered Architecture - Controller delegates to service layer
 * - Principle IV: Entity Immutability - Uses DTOs for API contracts
 * - FR-012: OAuth2 integration (Google, Facebook, Apple)
 * - FR-011: Keycloak acts as OAuth broker
 */
@Slf4j
@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oauthService;

    @Value("${app.oauth.redirect-uri:http://localhost:8080/oauth/{provider}/callback}")
    private String redirectUriTemplate;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * T081: Initiate OAuth authorization flow (FR-012).
     * Redirects user to OAuth provider's authorization page.
     *
     * @param provider OAuth provider (google, facebook, apple)
     * @param response HTTP response for redirect
     * @throws IOException if redirect fails
     */
    @GetMapping("/{provider}/authorize")
    public void initiateOAuthFlow(
            @PathVariable String provider,
            HttpServletResponse response
    ) throws IOException {
        log.info("Initiating OAuth flow for provider: {}", provider);

        // Validate provider
        if (!isValidProvider(provider)) {
            throw new IllegalArgumentException("Unsupported OAuth provider: " + provider);
        }

        // Get OAuth authorization URL from service
        String authorizationUrl = oauthService.getAuthorizationUrl(provider);

        // Redirect to OAuth provider
        response.sendRedirect(authorizationUrl);
    }

    /**
     * T082: Handle OAuth callback (FR-012).
     * Processes authorization code and completes authentication.
     *
     * @param provider OAuth provider (google, facebook, apple)
     * @param code Authorization code from OAuth provider
     * @param state State parameter for CSRF protection
     * @param error Error code if authorization failed
     * @param errorDescription Error description if authorization failed
     * @param request HTTP request for IP and user agent extraction
     * @param response HTTP response for redirect
     * @throws IOException if redirect fails
     */
    @GetMapping("/{provider}/callback")
    public void handleOAuthCallback(
            @PathVariable String provider,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        log.info("OAuth callback received for provider: {}", provider);

        // Check for errors from OAuth provider
        if (error != null) {
            log.error("OAuth error from provider {}: {} - {}", provider, error, errorDescription);
            String errorRedirectUrl = String.format("%s/auth/oauth/error?error=%s&description=%s",
                    frontendUrl, error, errorDescription != null ? errorDescription : "Unknown error");
            response.sendRedirect(errorRedirectUrl);
            return;
        }

        // Validate authorization code
        if (code == null || code.isBlank()) {
            log.error("Missing authorization code in OAuth callback");
            String errorRedirectUrl = String.format("%s/auth/oauth/error?error=missing_code", frontendUrl);
            response.sendRedirect(errorRedirectUrl);
            return;
        }

        try {
            // Extract client information
            String ipAddress = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");

            // Process OAuth callback and authenticate user
            AuthResponse authResponse = oauthService.handleOAuthCallback(
                    provider,
                    code,
                    state,
                    ipAddress,
                    userAgent
            );

            // Redirect to frontend with tokens
            String successRedirectUrl = String.format(
                    "%s/auth/oauth/success?access_token=%s&refresh_token=%s&token_type=%s&expires_in=%d",
                    frontendUrl,
                    authResponse.getAccessToken(),
                    authResponse.getRefreshToken(),
                    authResponse.getTokenType(),
                    authResponse.getExpiresIn()
            );

            response.sendRedirect(successRedirectUrl);

            log.info("OAuth authentication successful for provider: {}", provider);

        } catch (Exception e) {
            log.error("Error processing OAuth callback for provider: {}", provider, e);
            String errorRedirectUrl = String.format(
                    "%s/auth/oauth/error?error=authentication_failed&description=%s",
                    frontendUrl,
                    e.getMessage()
            );
            response.sendRedirect(errorRedirectUrl);
        }
    }

    /**
     * Validate OAuth provider name.
     *
     * @param provider Provider name
     * @return true if provider is supported
     */
    private boolean isValidProvider(String provider) {
        return provider != null && (
                provider.equalsIgnoreCase("google") ||
                provider.equalsIgnoreCase("facebook") ||
                provider.equalsIgnoreCase("apple")
        );
    }

    /**
     * Get client IP address from HTTP request.
     * Handles X-Forwarded-For header for proxied requests.
     *
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
