package com.cena.traveloka.iam.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * T049: IamSecurityConfig
 * IAM-specific security configuration.
 *
 * Constitutional Compliance:
 * - NFR-001: BCrypt password hashing
 * - FR-002: Keycloak integration (handled by common SecurityConfig)
 * - FR-006: Service method-level authorization (handled by common SecurityConfig)
 *
 * NOTE: This config provides IAM-specific beans only.
 * Main security configuration is in common module's SecurityConfig.
 * The common SecurityConfig already handles:
 * - JWT authentication with Keycloak
 * - CORS configuration
 * - Session management (stateless)
 * - Public/protected endpoints
 * - Role-based access control
 */
@Configuration
@RequiredArgsConstructor
public class IamSecurityConfig {

    /**
     * Password encoder bean.
     * Uses BCrypt for secure password hashing (NFR-001).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Strength: 12 rounds
    }

    /**
     * Authentication manager bean.
     * Used by authentication services.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
