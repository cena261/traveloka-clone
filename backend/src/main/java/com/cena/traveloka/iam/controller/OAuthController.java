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

    @GetMapping("/{provider}/authorize")
    public void initiateOAuthFlow(
            @PathVariable String provider,
            HttpServletResponse response
    ) throws IOException {
        log.info("Initiating OAuth flow for provider: {}", provider);

        if (!isValidProvider(provider)) {
            throw new IllegalArgumentException("Unsupported OAuth provider: " + provider);
        }

        String authorizationUrl = oauthService.getAuthorizationUrl(provider);

        response.sendRedirect(authorizationUrl);
    }

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

        if (error != null) {
            log.error("OAuth error from provider {}: {} - {}", provider, error, errorDescription);
            String errorRedirectUrl = String.format("%s/auth/oauth/error?error=%s&description=%s",
                    frontendUrl, error, errorDescription != null ? errorDescription : "Unknown error");
            response.sendRedirect(errorRedirectUrl);
            return;
        }

        if (code == null || code.isBlank()) {
            log.error("Missing authorization code in OAuth callback");
            String errorRedirectUrl = String.format("%s/auth/oauth/error?error=missing_code", frontendUrl);
            response.sendRedirect(errorRedirectUrl);
            return;
        }

        try {
            String ipAddress = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");

            AuthResponse authResponse = oauthService.handleOAuthCallback(
                    provider,
                    code,
                    state,
                    ipAddress,
                    userAgent
            );

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

    private boolean isValidProvider(String provider) {
        return provider != null && (
                provider.equalsIgnoreCase("google") ||
                provider.equalsIgnoreCase("facebook") ||
                provider.equalsIgnoreCase("apple")
        );
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
