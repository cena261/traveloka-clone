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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)

            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(authz -> authz
                .requestMatchers(publicEndpoints.toArray(new String[0])).permitAll()

                .requestMatchers(
                    "/api/v1/auth/register",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/forgot-password",
                    "/api/v1/auth/reset-password",
                    "/api/v1/auth/verify-email",
                    "/oauth/*/authorize",
                    "/oauth/*/callback"
                ).permitAll()

                .requestMatchers(adminEndpoints.toArray(new String[0])).hasRole("ADMIN")

                .requestMatchers("/api/**").authenticated()

                .anyRequest().authenticated()
            )

            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        if (jwkSetUri != null && !jwkSetUri.trim().isEmpty()) {
            return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        } else {
            return NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
        }
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName(authoritiesClaimName);

        authoritiesConverter.setAuthorityPrefix(authorityPrefix);

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return jwtConverter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(allowedOrigins);

        configuration.setAllowedMethods(allowedMethods);

        if (allowedHeaders.contains("*")) {
            configuration.addAllowedHeader("*");
        } else {
            configuration.setAllowedHeaders(allowedHeaders);
        }

        configuration.setAllowCredentials(allowCredentials);

        configuration.setMaxAge(corsMaxAge);

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