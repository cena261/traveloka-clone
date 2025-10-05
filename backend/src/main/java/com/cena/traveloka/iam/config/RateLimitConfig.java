package com.cena.traveloka.iam.config;

import com.cena.traveloka.iam.security.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Rate limiting configuration for IAM endpoints.
 * <p>
 * Applies rate limiting to authentication endpoints to prevent abuse and brute force attacks.
 * </p>
 *
 * <p>Rate Limits:</p>
 * <ul>
 *   <li>Login: 5 attempts per minute per IP</li>
 *   <li>Register: 3 attempts per minute per IP</li>
 *   <li>Password Reset: 3 attempts per minute per IP</li>
 *   <li>Verify Email: 5 attempts per minute per IP</li>
 * </ul>
 *
 * @author Traveloka IAM Team
 * @since 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class RateLimitConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns(
                        "/api/v1/auth/login",
                        "/api/v1/auth/register",
                        "/api/v1/auth/forgot-password",
                        "/api/v1/auth/reset-password",
                        "/api/v1/auth/verify-email"
                );
    }
}
