package com.cena.traveloka.iam.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Keycloak Configuration for IAM Module
 *
 * Configures Keycloak admin client and integration settings
 */
@Configuration
@ConfigurationProperties(prefix = "app.keycloak")
@Data
@Slf4j
public class KeycloakConfig {

    private String serverUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
    private String adminUsername;
    private String adminPassword;
    private boolean enabled = true;

    // Timeout configurations
    private int connectionTimeout = 30000; // 30 seconds
    private int readTimeout = 30000; // 30 seconds

    // Admin API configurations
    private String adminRealm = "master";
    private boolean verifyEmailEnabled = true;
    private boolean autoSyncEnabled = true;
    private int syncIntervalMinutes = 60;

    @Bean
    public RestTemplate keycloakRestTemplate() {
        log.info("Configuring Keycloak REST template with server URL: {}", serverUrl);

        RestTemplate restTemplate = new RestTemplate();

        // Configure timeouts and other settings
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("User-Agent", "Traveloka-IAM/1.0");
            return execution.execute(request, body);
        });

        return restTemplate;
    }

    /**
     * Keycloak admin client configuration
     */
    @Bean
    public KeycloakAdminConfig keycloakAdminConfig() {
        return KeycloakAdminConfig.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .adminUsername(adminUsername)
                .adminPassword(adminPassword)
                .adminRealm(adminRealm)
                .connectionTimeout(connectionTimeout)
                .readTimeout(readTimeout)
                .enabled(enabled)
                .build();
    }

    /**
     * Keycloak sync configuration
     */
    @Bean
    public KeycloakSyncConfig keycloakSyncConfig() {
        return KeycloakSyncConfig.builder()
                .enabled(autoSyncEnabled)
                .intervalMinutes(syncIntervalMinutes)
                .verifyEmailEnabled(verifyEmailEnabled)
                .build();
    }

    /**
     * Keycloak admin client configuration
     */
    @Data
    @lombok.Builder
    public static class KeycloakAdminConfig {
        private String serverUrl;
        private String realm;
        private String clientId;
        private String clientSecret;
        private String adminUsername;
        private String adminPassword;
        private String adminRealm;
        private int connectionTimeout;
        private int readTimeout;
        private boolean enabled;
    }

    /**
     * Keycloak synchronization configuration
     */
    @Data
    @lombok.Builder
    public static class KeycloakSyncConfig {
        private boolean enabled;
        private int intervalMinutes;
        private boolean verifyEmailEnabled;
    }
}
