package com.cena.traveloka.common;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T040: Integration test to verify Spring Boot application context loads successfully
 * with all configurations from the Common module.
 *
 * This test ensures that:
 * - All configuration classes are properly loaded
 * - No circular dependencies exist
 * - Bean definitions are valid
 * - Application context initializes without errors
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplicationContextTest {

    @Test
    void contextLoads() {
        // If the application context loads successfully, this test passes
        // Spring Boot will fail the test if there are any context initialization issues
        assertThat(true).isTrue();
    }

    @Test
    void applicationContextIsNotNull(org.springframework.context.ApplicationContext context) {
        assertThat(context).isNotNull();
        assertThat(context.getBeanDefinitionCount()).isGreaterThan(0);
    }
}
