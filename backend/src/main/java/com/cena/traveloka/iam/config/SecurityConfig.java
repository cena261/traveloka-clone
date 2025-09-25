package com.cena.traveloka.iam.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Security Configuration for IAM Module
 *
 * Configures OAuth2 Resource Server with JWT token validation
 * Integrates with Keycloak for authentication and authorization
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private List<String> allowedOrigins;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private List<String> allowedMethods;

    @Value("${app.cors.allowed-headers:*}")
    private List<String> allowedHeaders;

    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;

    // === Main Security Filter Chain ===

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security filter chain with OAuth2 Resource Server");

        http
            // Disable CSRF for stateless API
            .csrf(csrf -> csrf.disable())

            // Configure session management (stateless)
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Configure security headers
            .headers(headers -> headers
                    .frameOptions(f -> f.deny()) // X-Frame-Options: DENY
                    .contentTypeOptions(withDefaults()) // X-Content-Type-Options: nosniff
                    .httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)           // chú ý D hoa
                            .maxAgeInSeconds(31536000))
                    .referrerPolicy(rp -> rp.policy(
                            ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    // Permissions-Policy: truyền 1 chuỗi chính sách
                    .addHeaderWriter(
                            new org.springframework.security.web.header.writers.PermissionsPolicyHeaderWriter(
                                    "camera=(), microphone=(), geolocation=self"
                            )
                    )
            )

            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                    // Public endpoints
                    .requestMatchers(HttpMethod.GET, "/actuator/health/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/iam/auth/health").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/iam/auth/login").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/iam/auth/refresh").permitAll()

                    // API Documentation endpoints
                    .requestMatchers(HttpMethod.GET, "/v3/api-docs/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/swagger-ui/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/swagger-ui.html").permitAll()

                    // Authentication required for all other IAM endpoints
                    .requestMatchers("/api/iam/**").authenticated()

                    // Default - require authentication
                    .anyRequest().authenticated())

            // Configure OAuth2 Resource Server
            .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt
                            .decoder(jwtDecoder())
                            .jwtAuthenticationConverter(jwtAuthenticationConverter())))

            // Configure exception handling
            .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint((request, response, authException) -> {
                        log.warn("Authentication failed for request {}: {}",
                                request.getRequestURI(), authException.getMessage());
                        response.setStatus(401);
                        response.setContentType("application/json");
                        response.getWriter().write("""
                                {
                                    "error": "unauthorized",
                                    "errorDescription": "Authentication required",
                                    "errorCode": "IAM_UNAUTHORIZED",
                                    "status": 401,
                                    "timestamp": "%s"
                                }
                                """.formatted(java.time.Instant.now()));
                    })
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        log.warn("Access denied for request {}: {}",
                                request.getRequestURI(), accessDeniedException.getMessage());
                        response.setStatus(403);
                        response.setContentType("application/json");
                        response.getWriter().write("""
                                {
                                    "error": "forbidden",
                                    "errorDescription": "Access denied",
                                    "errorCode": "IAM_FORBIDDEN",
                                    "status": 403,
                                    "timestamp": "%s"
                                }
                                """.formatted(java.time.Instant.now()));
                    }));

        return http.build();
    }

    // === JWT Configuration ===

    @Bean
    public JwtDecoder jwtDecoder() {
        log.info("Configuring JWT decoder with issuer URI: {}", issuerUri);

        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri);

        // Configure JWT validation
        OAuth2TokenValidator<Jwt> withIssuer = new JwtIssuerValidator(issuerUri);
        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator(Duration.ofSeconds(60));
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withIssuer, withTimestamp);

        jwtDecoder.setJwtValidator(validator);

        return jwtDecoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        converter.setPrincipalClaimName("sub"); // Use subject as principal
        return converter;
    }

    @Bean
    public Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        return jwt -> {
            // Extract realm roles from Keycloak JWT
            Object realmAccess = jwt.getClaim("realm_access");
            if (realmAccess instanceof Map<?, ?> realmAccessMap) {
                Object roles = realmAccessMap.get("roles");
                if (roles instanceof List<?> rolesList) {
                    return rolesList.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                            .collect(Collectors.toList());
                }
            }

            // Extract resource access roles
            Object resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess instanceof Map<?, ?> resourceAccessMap) {
                // Process client-specific roles if needed
                // This can be extended based on your Keycloak client configuration
            }

            return Collections.emptyList();
        };
    }

    // === CORS Configuration ===

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("Configuring CORS with allowed origins: {}", allowedOrigins);

        CorsConfiguration configuration = new CorsConfiguration();

        // Configure allowed origins
        configuration.setAllowedOriginPatterns(allowedOrigins);

        // Configure allowed methods
        configuration.setAllowedMethods(allowedMethods);

        // Configure allowed headers
        if (allowedHeaders.contains("*")) {
            configuration.addAllowedHeader("*");
        } else {
            configuration.setAllowedHeaders(allowedHeaders);
        }

        // Configure credentials
        configuration.setAllowCredentials(allowCredentials);

        // Configure exposed headers
        configuration.addExposedHeader("X-Total-Count");
        configuration.addExposedHeader("X-Page-Number");
        configuration.addExposedHeader("X-Page-Size");
        configuration.addExposedHeader("Authorization");

        // Configure max age
        configuration.setMaxAge(3600L); // 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }

    // === Security Utilities ===

    /**
     * Custom JWT validator for additional security checks
     */
    public static class CustomJwtValidator implements OAuth2TokenValidator<Jwt> {

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            // Add custom validation logic here
            // For example, check for required claims, validate audience, etc.

            // Check if email is verified (optional)
            Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");
            if (emailVerified != null && !emailVerified) {
                log.warn("JWT token has unverified email for subject: {}", jwt.getSubject());
                // You can choose to reject unverified emails or just log a warning
            }

            // Check for required scopes (if using scope-based authorization)
            List<String> scopes = jwt.getClaimAsStringList("scope");
            if (scopes == null || scopes.isEmpty()) {
                log.debug("JWT token has no scopes for subject: {}", jwt.getSubject());
            }

            return OAuth2TokenValidatorResult.success();
        }
    }

    /**
     * Development-only security configuration
     * Can be activated with @Profile("dev")
     */
    // @Profile("dev")
    // @Bean
    // public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
    //     return http
    //             .csrf().disable()
    //             .authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
    //             .build();
    // }
}