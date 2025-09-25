package com.cena.traveloka.iam.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger Configuration for IAM Module
 *
 * Configures API documentation with security schemes and detailed information
 */
@Configuration
@Slf4j
public class OpenApiConfig {

    @Value("${app.api.version:1.0.0}")
    private String apiVersion;

    @Value("${app.api.title:Traveloka IAM API}")
    private String apiTitle;

    @Value("${app.api.description:Identity and Access Management API for Traveloka Platform}")
    private String apiDescription;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${app.api.contact.name:Traveloka Engineering}")
    private String contactName;

    @Value("${app.api.contact.email:engineering@traveloka.com}")
    private String contactEmail;

    @Value("${app.api.contact.url:https://traveloka.com}")
    private String contactUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        log.info("Configuring OpenAPI documentation for IAM module");

        return new OpenAPI()
                .info(createApiInfo())
                .servers(createServers())
                .components(createComponents())
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }

    private Info createApiInfo() {
        return new Info()
                .title(apiTitle)
                .version(apiVersion)
                .description(createDetailedDescription())
                .contact(createContact())
                .license(createLicense());
    }

    private String createDetailedDescription() {
        return """
                # Traveloka Identity and Access Management API

                The IAM API provides comprehensive identity and access management functionality for the Traveloka platform.

                ## Features

                ### User Management
                - Complete user lifecycle management (create, read, update, delete)
                - User search and filtering with advanced criteria
                - Bulk operations for administrative tasks
                - User status management (active, suspended, deleted)

                ### User Profiles
                - Extended user profile management
                - Profile verification workflows
                - Demographic analytics and reporting
                - Travel document management

                ### User Preferences
                - Localization preferences (language, timezone, currency)
                - Notification and privacy settings
                - Booking and search preferences
                - Custom preference management

                ### Session Management
                - Multi-device session tracking
                - Session lifecycle management
                - Concurrent session limits
                - Session security monitoring

                ### Authentication & Authorization
                - OAuth2/JWT token management
                - Keycloak integration
                - Role-based access control (RBAC)
                - Session validation and refresh

                ## Security

                ### Authentication
                All API endpoints (except public endpoints) require authentication via JWT Bearer tokens.

                ### Authorization
                Role-based access control is implemented with the following roles:
                - **ADMIN**: Full system access
                - **USER_MANAGER**: User and profile management
                - **VERIFIER**: Profile verification capabilities
                - **USER**: Basic user operations

                ### Rate Limiting
                - 100 requests per minute per user
                - 1000 requests per hour per user
                - Admin users bypass rate limits

                ## Error Handling

                The API uses consistent error response format:
                ```json
                {
                    "success": false,
                    "message": "Error description",
                    "error": {
                        "error": "error_code",
                        "errorDescription": "Detailed error message",
                        "errorCode": "IAM_ERROR_CODE",
                        "status": 400,
                        "timestamp": "2023-01-01T00:00:00Z",
                        "traceId": "trace123"
                    }
                }
                ```

                ## Pagination

                List endpoints support pagination with the following parameters:
                - `page`: Page number (0-based, default: 0)
                - `size`: Page size (max: 100, default: 20)
                - `sortBy`: Sort field (default: createdAt)
                - `sortDirection`: Sort direction (ASC/DESC, default: DESC)

                ## Contact

                For API support, please contact the Traveloka Engineering team.
                """;
    }

    private Contact createContact() {
        return new Contact()
                .name(contactName)
                .email(contactEmail)
                .url(contactUrl);
    }

    private License createLicense() {
        return new License()
                .name("Proprietary")
                .url("https://traveloka.com/terms");
    }

    private List<Server> createServers() {
        return List.of(
                new Server()
                        .url("/")
                        .description("Current server"),
                new Server()
                        .url("https://api.traveloka.com")
                        .description("Production server"),
                new Server()
                        .url("https://api-staging.traveloka.com")
                        .description("Staging server"),
                new Server()
                        .url("http://localhost:8080")
                        .description("Local development server")
        );
    }

    private Components createComponents() {
        return new Components()
                .addSecuritySchemes("bearer-jwt", createBearerJwtSecurityScheme())
                .addSchemas("UserStatus", createUserStatusSchema())
                .addSchemas("SessionStatus", createSessionStatusSchema())
                .addExamples("UserCreateExample", createUserCreateExample())
                .addExamples("ErrorResponseExample", createErrorResponseExample());
    }

    private SecurityScheme createBearerJwtSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        JWT Bearer token obtained from Keycloak authentication.

                        **How to obtain:**
                        1. Authenticate with Keycloak using OAuth2 flow
                        2. Extract the access_token from the response
                        3. Use the token in the Authorization header: `Bearer <token>`

                        **Token format:** `Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...`

                        **Token expiration:** Tokens typically expire after 15 minutes and must be refreshed.
                        """);
    }

//    private io.swagger.v3.oas.models.media.Schema<?> createUserStatusSchema() {
//        return new io.swagger.v3.oas.models.media.Schema<>()
//                .type("string")
//                .description("User account status")
//                .addEnumItemObject("ACTIVE")
//                .addEnumItemObject("INACTIVE")
//                .addEnumItemObject("SUSPENDED")
//                .addEnumItemObject("DELETED")
//                .example("ACTIVE");
//    }
    private Schema<String> createUserStatusSchema() {
        Schema<String> s = new Schema<String>()
                .type("string")
                .description("User account status")
                .example("ACTIVE");

        s.addEnumItemObject("ACTIVE");
        s.addEnumItemObject("INACTIVE");
        s.addEnumItemObject("SUSPENDED");
        s.addEnumItemObject("DELETED");
        return s;
    }

    private Schema<String> createSessionStatusSchema() {
        Schema<String> s = new Schema<String>()
                .type("string")
                .description("User session status")
                .example("ACTIVE");

        s.addEnumItemObject("ACTIVE");
        s.addEnumItemObject("EXPIRED");
        s.addEnumItemObject("TERMINATED");
        return s;
    }


    private io.swagger.v3.oas.models.examples.Example createUserCreateExample() {
        return new io.swagger.v3.oas.models.examples.Example()
                .summary("Create user example")
                .description("Example request body for creating a new user")
                .value("""
                        {
                            "email": "john.doe@example.com",
                            "firstName": "John",
                            "lastName": "Doe",
                            "phoneNumber": "+1234567890",
                            "dateOfBirth": "1990-01-15",
                            "gender": "MALE",
                            "country": "US",
                            "preferredLanguage": "en",
                            "timeZone": "America/New_York",
                            "emailVerified": false,
                            "phoneVerified": false
                        }
                        """);
    }

    private io.swagger.v3.oas.models.examples.Example createErrorResponseExample() {
        return new io.swagger.v3.oas.models.examples.Example()
                .summary("Error response example")
                .description("Example error response format")
                .value("""
                        {
                            "success": false,
                            "message": "Validation failed",
                            "error": {
                                "error": "validation_failed",
                                "errorDescription": "Request validation failed",
                                "errorCode": "IAM_VALIDATION_FAILED",
                                "status": 400,
                                "path": "/api/iam/users",
                                "method": "POST",
                                "timestamp": "2023-01-01T00:00:00Z",
                                "traceId": "trace123",
                                "validationErrors": [
                                    {
                                        "field": "email",
                                        "rejectedValue": "invalid-email",
                                        "message": "Email must be valid",
                                        "code": "Email"
                                    }
                                ]
                            }
                        }
                        """);
    }
}