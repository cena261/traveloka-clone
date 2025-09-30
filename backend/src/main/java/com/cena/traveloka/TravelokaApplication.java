package com.cena.traveloka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main application class for Traveloka Backend
 *
 * This application provides comprehensive travel booking platform services including:
 * - Identity and Access Management (IAM) with Keycloak integration
 * - Hotel property and partner management
 * - Property search and filtering capabilities
 * - User profile and preference management
 * - Multi-device session management
 * - Real-time synchronization and caching
 *
 * Key Features:
 * - Spring Boot 3.x with Java 17
 * - PostgreSQL with PostGIS for spatial data
 * - Redis for distributed caching and session storage
 * - OAuth2 resource server with JWT authentication
 * - Comprehensive monitoring and observability
 * - High-performance architecture with 10,000+ concurrent users support
 *
 * @author Traveloka Engineering Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration.class
})
//@SpringBootApplication(scanBasePackages = {
//    "com.cena.traveloka.common",
//    "com.cena.traveloka.iam",
//    "com.cena.traveloka.catalog",
//    "com.cena.traveloka.search",
//    "com.cena.traveloka.availability",
//    "com.cena.traveloka.pricing",
//    "com.cena.traveloka.booking",
//    "com.cena.traveloka.payment",
//    "com.cena.traveloka.review",
//    "com.cena.traveloka.notification"
//})
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
public class TravelokaApplication {
    public static void main(String[] args) {
        SpringApplication.run(TravelokaApplication.class, args);
    }
}
