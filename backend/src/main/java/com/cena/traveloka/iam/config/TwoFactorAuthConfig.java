package com.cena.traveloka.iam.config;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TwoFactorAuthConfig {

    @Bean
    public GoogleAuthenticator googleAuthenticator() {
        return new GoogleAuthenticator();
    }
}
