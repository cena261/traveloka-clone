package com.cena.traveloka.iam.config;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Two-Factor Authentication.
 */
@Configuration
public class TwoFactorAuthConfig {

    /**
     * Create GoogleAuthenticator bean for TOTP generation and verification.
     *
     * @return GoogleAuthenticator instance
     */
    @Bean
    public GoogleAuthenticator googleAuthenticator() {
        return new GoogleAuthenticator();
    }
}
