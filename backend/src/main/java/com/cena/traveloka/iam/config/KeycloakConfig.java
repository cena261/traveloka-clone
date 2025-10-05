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

    @Bean
    public Keycloak keycloakAdminClient() {
        log.info("Initializing Keycloak admin client: server={}, realm={}, clientId={}",
                serverUrl, realm, clientId);

        try {
            KeycloakBuilder builder = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(realm)
                    .clientId(clientId);

            if (clientSecret != null && !clientSecret.isEmpty()) {
                builder.grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                        .clientSecret(clientSecret);
                log.info("Using client credentials grant for Keycloak authentication");
            } else {
                builder.grantType(OAuth2Constants.PASSWORD)
                        .username(username)
                        .password(password);
                log.info("Using password grant for Keycloak authentication");
            }

            Keycloak keycloak = builder.build();

            keycloak.serverInfo().getInfo();
            log.info("Keycloak admin client initialized successfully");

            return keycloak;
        } catch (Exception ex) {
            log.error("Failed to initialize Keycloak admin client: {}", ex.getMessage());
            log.warn("Keycloak integration will not be available. Application will continue with limited functionality.");

            return null;
        }
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getRealm() {
        return realm;
    }

    public String getClientId() {
        return clientId;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public long getConnectionTimeoutMillis() {
        return connectionTimeout.toMillis();
    }

    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }
}
