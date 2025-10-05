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

        registry.addMapping("/api/**")
            .allowedOriginPatterns(getAllowedOriginPatterns())
            .allowedOrigins(getAllowedOrigins())
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders(exposedHeaders.toArray(new String[0]))
            .allowCredentials(allowCredentials)
            .maxAge(maxAge);

        registry.addMapping("/actuator/**")
            .allowedOriginPatterns(getRestrictedOriginPatterns())
            .allowedOrigins(getRestrictedOrigins())
            .allowedMethods("GET", "OPTIONS")
            .allowedHeaders("Authorization", "Content-Type")
            .allowCredentials(false)
            .maxAge(maxAge);

        logger.info("CORS configured successfully with {} allowed origins", allowedOrigins.size());
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        CorsConfiguration defaultConfig = createDefaultCorsConfiguration();
        source.registerCorsConfiguration("/**", defaultConfig);

        CorsConfiguration apiConfig = createApiCorsConfiguration();
        source.registerCorsConfiguration("/api/**", apiConfig);

        CorsConfiguration graphqlConfig = createGraphqlCorsConfiguration();
        source.registerCorsConfiguration("/graphql/**", graphqlConfig);

        CorsConfiguration websocketConfig = createWebsocketCorsConfiguration();
        source.registerCorsConfiguration("/ws/**", websocketConfig);

        logger.info("CORS filter configured with multiple endpoint patterns");
        return new CorsFilter(source);
    }

    private CorsConfiguration createDefaultCorsConfiguration() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of(getAllowedOriginPatterns()));
        if (!allowedOrigins.isEmpty()) {
            config.setAllowedOrigins(allowedOrigins);
        }

        config.setAllowedMethods(allowedMethods);

        if (allowedHeaders.contains("*")) {
            config.addAllowedHeader("*");
        } else {
            config.setAllowedHeaders(allowedHeaders);
        }

        config.setExposedHeaders(exposedHeaders);

        config.setAllowCredentials(allowCredentials);

        config.setMaxAge(maxAge);

        return config;
    }

    private CorsConfiguration createApiCorsConfiguration() {
        CorsConfiguration config = createDefaultCorsConfiguration();

        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"));

        config.addAllowedHeader("Authorization");
        config.addAllowedHeader("Content-Type");
        config.addAllowedHeader("X-Requested-With");
        config.addAllowedHeader("Accept");
        config.addAllowedHeader("Origin");
        config.addAllowedHeader("X-Correlation-ID");

        return config;
    }

    private CorsConfiguration createGraphqlCorsConfiguration() {
        CorsConfiguration config = createDefaultCorsConfiguration();

        config.setAllowedMethods(Arrays.asList("POST", "OPTIONS", "GET"));

        config.addExposedHeader("X-GraphQL-Error-Code");
        config.addExposedHeader("X-GraphQL-Query-Cost");

        return config;
    }

    private CorsConfiguration createWebsocketCorsConfiguration() {
        CorsConfiguration config = createDefaultCorsConfiguration();

        config.setAllowedMethods(Arrays.asList("GET", "OPTIONS"));

        config.addAllowedHeader("Sec-WebSocket-Key");
        config.addAllowedHeader("Sec-WebSocket-Version");
        config.addAllowedHeader("Sec-WebSocket-Protocol");
        config.addAllowedHeader("Connection");
        config.addAllowedHeader("Upgrade");

        return config;
    }

    private String[] getAllowedOrigins() {
        if (isProductionEnvironment()) {
            return allowedOrigins.stream()
                .filter(origin -> !origin.contains("localhost"))
                .toArray(String[]::new);
        }
        return allowedOrigins.toArray(new String[0]);
    }

    private String[] getAllowedOriginPatterns() {
        if (allowedOriginPatterns.isEmpty()) {
            if (isProductionEnvironment()) {
                return new String[]{"https://*.traveloka.com", "https://*.traveloka.co.id"};
            } else {
                return new String[]{"http://localhost:*", "https://localhost:*"};
            }
        }
        return allowedOriginPatterns.toArray(new String[0]);
    }

    private String[] getRestrictedOrigins() {
        if (isProductionEnvironment()) {
            return new String[]{"https://admin.traveloka.com"};
        }
        return new String[]{"http://localhost:3000", "http://localhost:8080"};
    }

    private String[] getRestrictedOriginPatterns() {
        if (isProductionEnvironment()) {
            return new String[]{"https://admin.traveloka.*"};
        }
        return new String[]{"http://localhost:*"};
    }

    private boolean isProductionEnvironment() {
        return "prod".equalsIgnoreCase(environment) || "production".equalsIgnoreCase(environment);
    }

}