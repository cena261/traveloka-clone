package com.cena.traveloka.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration with Keycloak JWT resource server.
 * Features:
 * - JWT-based authentication with Keycloak integration
 * - Role-based access control with method-level security
 * - CORS configuration for cross-origin requests
 * - Stateless session management
 * - Public endpoint configuration
 * - Authority mapping from JWT claims
 * - Environment-specific security settings
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String jwkSetUri;

    @Value("${app.security.jwt.authorities-claim-name:realm_access.roles}")
    private String authoritiesClaimName;

    @Value("${app.security.jwt.authority-prefix:ROLE_}")
    private String authorityPrefix;

    @Value("${app.security.public-endpoints:/api/public/**,/actuator/health,/actuator/info,/swagger-ui/**,/v3/api-docs/**}")
    private List<String> publicEndpoints;

    @Value("${app.security.admin-endpoints:/api/admin/**,/actuator/**}")
    private List<String> adminEndpoints;

    @Value("${app.security.cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private List<String> allowedOrigins;

    @Value("${app.security.cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}")
    private List<String> allowedMethods;

    @Value("${app.security.cors.allowed-headers:*}")
    private List<String> allowedHeaders;

    @Value("${app.security.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${app.security.cors.max-age:3600}")
    private long corsMaxAge;

    /**
     * Configure security filter chain
     * @param http HttpSecurity configuration
     * @return configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless API
            .csrf(AbstractHttpConfigurer::disable)

            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Configure session management (stateless)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers(publicEndpoints.toArray(new String[0])).permitAll()

                // Admin endpoints require ADMIN role
                .requestMatchers(adminEndpoints.toArray(new String[0])).hasRole("ADMIN")

                // API endpoints require authentication
                .requestMatchers("/api/**").authenticated()

                // All other requests require authentication
                .anyRequest().authenticated()
            )

            // Configure OAuth2 Resource Server with JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    /**
     * Configure JWT decoder for Keycloak
     * @return configured JwtDecoder
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        if (jwkSetUri != null && !jwkSetUri.trim().isEmpty()) {
            return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        } else {
            return NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
        }
    }

    /**
     * Configure JWT authentication converter for authority mapping
     * @return configured JwtAuthenticationConverter
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

        // Configure authority claim name (e.g., "realm_access.roles" for Keycloak)
        authoritiesConverter.setAuthoritiesClaimName(authoritiesClaimName);

        // Configure authority prefix (e.g., "ROLE_")
        authoritiesConverter.setAuthorityPrefix(authorityPrefix);

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return jwtConverter;
    }

    /**
     * Configure CORS settings
     * @return configured CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Set allowed origins
        configuration.setAllowedOriginPatterns(allowedOrigins);

        // Set allowed methods
        configuration.setAllowedMethods(allowedMethods);

        // Set allowed headers
        if (allowedHeaders.contains("*")) {
            configuration.addAllowedHeader("*");
        } else {
            configuration.setAllowedHeaders(allowedHeaders);
        }

        // Set credentials support
        configuration.setAllowCredentials(allowCredentials);

        // Set preflight max age
        configuration.setMaxAge(corsMaxAge);

        // Expose common headers
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-Requested-With", "Accept",
            "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers",
            "X-Total-Count", "X-Page-Number", "X-Page-Size"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}