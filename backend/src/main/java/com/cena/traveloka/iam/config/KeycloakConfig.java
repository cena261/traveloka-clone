package com.cena.traveloka.iam.config;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * T050: KeycloakConfig
 * Configuration for Keycloak Admin Client.
 *
 * Constitutional Compliance:
 * - FR-002: Keycloak integration for authentication
 * - FR-011: Bidirectional sync with identity provider
 * - NFR-004: Graceful handling of Keycloak unavailability
 * - Provides Keycloak admin client for user management
 * - Used by KeycloakSyncService
 */
@Slf4j
@Configuration
public class KeycloakConfig {

    @Value("${traveloka.iam.keycloak.server-url:http://localhost:8080}")
    private String serverUrl;

    @Value("${traveloka.iam.keycloak.realm:traveloka}")
    private String realm;

    @Value("${traveloka.iam.keycloak.admin-client-id:admin-cli}")
    private String clientId;

    @Value("${traveloka.iam.keycloak.admin-client-secret:}")
    private String clientSecret;

    @Value("${traveloka.iam.keycloak.admin-username:admin}")
    private String username;

    @Value("${traveloka.iam.keycloak.admin-password:admin}")
    private String password;

    @Value("${traveloka.iam.keycloak.connection-pool-size:10}")
    private int poolSize;

    @Value("${traveloka.iam.keycloak.connection-timeout:5000ms}")
    @DurationUnit(ChronoUnit.MILLIS)
    private Duration connectionTimeout;

    /**
     * Create Keycloak admin client bean.
     * Uses client credentials (client_id + client_secret) or password grant.
     *
     * @return Keycloak admin client instance
     */
    @Bean
    public Keycloak keycloakAdminClient() {
        log.info("Initializing Keycloak admin client: server={}, realm={}, clientId={}",
                serverUrl, realm, clientId);

        try {
            KeycloakBuilder builder = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(realm)
                    .clientId(clientId);

            // Use client credentials if client secret is provided
            if (clientSecret != null && !clientSecret.isEmpty()) {
                builder.grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                        .clientSecret(clientSecret);
                log.info("Using client credentials grant for Keycloak authentication");
            } else {
                // Fallback to password grant with admin username/password
                builder.grantType(OAuth2Constants.PASSWORD)
                        .username(username)
                        .password(password);
                log.info("Using password grant for Keycloak authentication");
            }

            Keycloak keycloak = builder.build();

            // Test connection
            keycloak.serverInfo().getInfo();
            log.info("Keycloak admin client initialized successfully");

            return keycloak;
        } catch (Exception ex) {
            log.error("Failed to initialize Keycloak admin client: {}", ex.getMessage());
            log.warn("Keycloak integration will not be available. Application will continue with limited functionality.");

            // Return null to allow application to start even if Keycloak is unavailable
            // Services using Keycloak should check for null and handle gracefully
            return null;
        }
    }

    /**
     * Get Keycloak server URL.
     *
     * @return Server URL
     */
    public String getServerUrl() {
        return serverUrl;
    }

    /**
     * Get Keycloak realm name.
     *
     * @return Realm name
     */
    public String getRealm() {
        return realm;
    }

    /**
     * Get Keycloak client ID.
     *
     * @return Client ID
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Get connection pool size.
     *
     * @return Pool size
     */
    public int getPoolSize() {
        return poolSize;
    }

    /**
     * Get connection timeout in milliseconds.
     *
     * @return Connection timeout in milliseconds
     */
    public long getConnectionTimeoutMillis() {
        return connectionTimeout.toMillis();
    }

    /**
     * Get connection timeout as Duration.
     *
     * @return Connection timeout Duration
     */
    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }
}
