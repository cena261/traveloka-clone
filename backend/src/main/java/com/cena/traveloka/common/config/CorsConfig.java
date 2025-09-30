package com.cena.traveloka.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration with environment-based allowed origins.
 * Features:
 * - Environment-specific allowed origins configuration
 * - Flexible HTTP methods and headers configuration
 * - Credentials support for authenticated requests
 * - Path-specific CORS rules
 * - Development and production optimized settings
 * - Preflight request caching
 */
@Configuration
@ConditionalOnProperty(name = "app.cors.enabled", havingValue = "true", matchIfMissing = true)
public class CorsConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(CorsConfig.class);

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:8080,http://localhost:4200}")
    private List<String> allowedOrigins;

    @Value("${app.cors.allowed-origin-patterns:}")
    private List<String> allowedOriginPatterns;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS,HEAD}")
    private List<String> allowedMethods;

    @Value("${app.cors.allowed-headers:*}")
    private List<String> allowedHeaders;

    @Value("${app.cors.exposed-headers:Authorization,Content-Type,X-Total-Count,X-Page-Number,X-Page-Size,X-Correlation-ID}")
    private List<String> exposedHeaders;

    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${app.cors.max-age:3600}")
    private long maxAge;

    @Value("${app.environment:dev}")
    private String environment;

    /**
     * Configure CORS using WebMvcConfigurer
     * @param registry CORS registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        logger.info("Configuring CORS for environment: {}", environment);

        registry.addMapping("/**")
            .allowedOriginPatterns(getAllowedOriginPatterns())
            .allowedOrigins(getAllowedOrigins())
            .allowedMethods(allowedMethods.toArray(new String[0]))
            .allowedHeaders(allowedHeaders.toArray(new String[0]))
            .exposedHeaders(exposedHeaders.toArray(new String[0]))
            .allowCredentials(allowCredentials)
            .maxAge(maxAge);

        // API-specific CORS configuration
        registry.addMapping("/api/**")
            .allowedOriginPatterns(getAllowedOriginPatterns())
            .allowedOrigins(getAllowedOrigins())
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders(exposedHeaders.toArray(new String[0]))
            .allowCredentials(allowCredentials)
            .maxAge(maxAge);

        // Actuator endpoints with restricted CORS
        registry.addMapping("/actuator/**")
            .allowedOriginPatterns(getRestrictedOriginPatterns())
            .allowedOrigins(getRestrictedOrigins())
            .allowedMethods("GET", "OPTIONS")
            .allowedHeaders("Authorization", "Content-Type")
            .allowCredentials(false)
            .maxAge(maxAge);

        logger.info("CORS configured successfully with {} allowed origins", allowedOrigins.size());
    }

    /**
     * Configure CORS filter as bean
     * @return configured CorsFilter
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Default CORS configuration
        CorsConfiguration defaultConfig = createDefaultCorsConfiguration();
        source.registerCorsConfiguration("/**", defaultConfig);

        // API-specific CORS configuration
        CorsConfiguration apiConfig = createApiCorsConfiguration();
        source.registerCorsConfiguration("/api/**", apiConfig);

        // GraphQL-specific CORS configuration
        CorsConfiguration graphqlConfig = createGraphqlCorsConfiguration();
        source.registerCorsConfiguration("/graphql/**", graphqlConfig);

        // WebSocket-specific CORS configuration
        CorsConfiguration websocketConfig = createWebsocketCorsConfiguration();
        source.registerCorsConfiguration("/ws/**", websocketConfig);

        logger.info("CORS filter configured with multiple endpoint patterns");
        return new CorsFilter(source);
    }

    /**
     * Create default CORS configuration
     * @return configured CorsConfiguration
     */
    private CorsConfiguration createDefaultCorsConfiguration() {
        CorsConfiguration config = new CorsConfiguration();

        // Set allowed origins
        config.setAllowedOriginPatterns(List.of(getAllowedOriginPatterns()));
        if (!allowedOrigins.isEmpty()) {
            config.setAllowedOrigins(allowedOrigins);
        }

        // Set allowed methods
        config.setAllowedMethods(allowedMethods);

        // Set allowed headers
        if (allowedHeaders.contains("*")) {
            config.addAllowedHeader("*");
        } else {
            config.setAllowedHeaders(allowedHeaders);
        }

        // Set exposed headers
        config.setExposedHeaders(exposedHeaders);

        // Set credentials support
        config.setAllowCredentials(allowCredentials);

        // Set preflight max age
        config.setMaxAge(maxAge);

        return config;
    }

    /**
     * Create API-specific CORS configuration
     * @return configured CorsConfiguration
     */
    private CorsConfiguration createApiCorsConfiguration() {
        CorsConfiguration config = createDefaultCorsConfiguration();

        // API endpoints typically need all standard HTTP methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"));

        // Common API headers
        config.addAllowedHeader("Authorization");
        config.addAllowedHeader("Content-Type");
        config.addAllowedHeader("X-Requested-With");
        config.addAllowedHeader("Accept");
        config.addAllowedHeader("Origin");
        config.addAllowedHeader("X-Correlation-ID");

        return config;
    }

    /**
     * Create GraphQL-specific CORS configuration
     * @return configured CorsConfiguration
     */
    private CorsConfiguration createGraphqlCorsConfiguration() {
        CorsConfiguration config = createDefaultCorsConfiguration();

        // GraphQL typically uses POST and OPTIONS
        config.setAllowedMethods(Arrays.asList("POST", "OPTIONS", "GET"));

        // GraphQL-specific headers
        config.addExposedHeader("X-GraphQL-Error-Code");
        config.addExposedHeader("X-GraphQL-Query-Cost");

        return config;
    }

    /**
     * Create WebSocket-specific CORS configuration
     * @return configured CorsConfiguration
     */
    private CorsConfiguration createWebsocketCorsConfiguration() {
        CorsConfiguration config = createDefaultCorsConfiguration();

        // WebSocket typically uses GET and OPTIONS for handshake
        config.setAllowedMethods(Arrays.asList("GET", "OPTIONS"));

        // WebSocket-specific headers
        config.addAllowedHeader("Sec-WebSocket-Key");
        config.addAllowedHeader("Sec-WebSocket-Version");
        config.addAllowedHeader("Sec-WebSocket-Protocol");
        config.addAllowedHeader("Connection");
        config.addAllowedHeader("Upgrade");

        return config;
    }

    /**
     * Get allowed origins based on environment
     * @return array of allowed origins
     */
    private String[] getAllowedOrigins() {
        if (isProductionEnvironment()) {
            // In production, use specific domains only
            return allowedOrigins.stream()
                .filter(origin -> !origin.contains("localhost"))
                .toArray(String[]::new);
        }
        return allowedOrigins.toArray(new String[0]);
    }

    /**
     * Get allowed origin patterns based on environment
     * @return array of allowed origin patterns
     */
    private String[] getAllowedOriginPatterns() {
        if (allowedOriginPatterns.isEmpty()) {
            if (isProductionEnvironment()) {
                //deploy vercel hoac gi do
                return new String[]{"https://*.traveloka.com", "https://*.traveloka.co.id"};
            } else {
                return new String[]{"http://localhost:*", "https://localhost:*"};
            }
        }
        return allowedOriginPatterns.toArray(new String[0]);
    }

    /**
     * Get restricted origins for admin endpoints
     * @return array of restricted origins
     */
    private String[] getRestrictedOrigins() {
        if (isProductionEnvironment()) {
            return new String[]{"https://admin.traveloka.com"};
        }
        return new String[]{"http://localhost:3000", "http://localhost:8080"};
    }

    /**
     * Get restricted origin patterns for admin endpoints
     * @return array of restricted origin patterns
     */
    private String[] getRestrictedOriginPatterns() {
        if (isProductionEnvironment()) {
            return new String[]{"https://admin.traveloka.*"};
        }
        return new String[]{"http://localhost:*"};
    }

    /**
     * Check if current environment is production
     * @return true if production environment
     */
    private boolean isProductionEnvironment() {
        return "prod".equalsIgnoreCase(environment) || "production".equalsIgnoreCase(environment);
    }

}