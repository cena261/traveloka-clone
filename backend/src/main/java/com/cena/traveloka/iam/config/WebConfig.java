package com.cena.traveloka.iam.config;

import com.cena.traveloka.iam.interceptor.AuditLoggingInterceptor;
import com.cena.traveloka.iam.interceptor.RateLimitingInterceptor;
import com.cena.traveloka.iam.interceptor.RequestTracingInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web Configuration for IAM Module
 *
 * Configures interceptors and other web-related settings
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    private final RequestTracingInterceptor requestTracingInterceptor;
    private final AuditLoggingInterceptor auditLoggingInterceptor;
    private final RateLimitingInterceptor rateLimitingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("Configuring IAM interceptors");

        // Request tracing interceptor (the highest priority)
        registry.addInterceptor(requestTracingInterceptor)
                .addPathPatterns("/api/iam/**")
                .order(1);

        // Rate limiting interceptor (second priority)
        registry.addInterceptor(rateLimitingInterceptor)
                .addPathPatterns("/api/iam/**")
                .excludePathPatterns(
                        "/api/iam/auth/health",
                        "/api/iam/*/health"
                )
                .order(2);

        // Audit logging interceptor (third priority)
        registry.addInterceptor(auditLoggingInterceptor)
                .addPathPatterns("/api/iam/**")
                .excludePathPatterns(
                        "/api/iam/auth/health",
                        "/api/iam/*/health"
                )
                .order(3);
    }
}