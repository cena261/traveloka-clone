package com.cena.traveloka.common.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T041: Integration test for Database Configuration with HikariCP connection pool.
 *
 * This test verifies:
 * - PostgreSQL container starts successfully
 * - HikariCP connection pool is configured correctly
 * - Database connections can be obtained and used
 * - Connection pool settings match specifications (min=5, max=20)
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class DatabaseConfigIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private DataSource dataSource;

    @Test
    void dataSourceIsConfigured() {
        assertThat(dataSource).isNotNull();
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
    }

    @Test
    void hikariPoolSettingsAreCorrect() {
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;

        // Verify HikariCP settings from research.md specifications
        assertThat(hikariDataSource.getMinimumIdle()).isEqualTo(5);
        assertThat(hikariDataSource.getMaximumPoolSize()).isEqualTo(20);
        assertThat(hikariDataSource.getIdleTimeout()).isEqualTo(300000); // 5 minutes
        assertThat(hikariDataSource.getMaxLifetime()).isEqualTo(1200000); // 20 minutes
    }

    @Test
    void canObtainConnectionFromPool() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection).isNotNull();
            assertThat(connection.isValid(5)).isTrue();
        }
    }

    @Test
    void canExecuteQueryAgainstDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1 AS test_value")) {

            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt("test_value")).isEqualTo(1);
        }
    }

    @Test
    void postgresVersionIsCorrect() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT version()")) {

            assertThat(resultSet.next()).isTrue();
            String version = resultSet.getString(1);
            assertThat(version).contains("PostgreSQL 16");
        }
    }

    @Test
    void connectionPoolCanHandleMultipleConnections() throws Exception {
        // Test that we can get multiple connections (up to minimum idle)
        Connection conn1 = null;
        Connection conn2 = null;
        Connection conn3 = null;

        try {
            conn1 = dataSource.getConnection();
            conn2 = dataSource.getConnection();
            conn3 = dataSource.getConnection();

            assertThat(conn1).isNotNull();
            assertThat(conn2).isNotNull();
            assertThat(conn3).isNotNull();

            assertThat(conn1.isValid(5)).isTrue();
            assertThat(conn2.isValid(5)).isTrue();
            assertThat(conn3.isValid(5)).isTrue();
        } finally {
            if (conn1 != null) conn1.close();
            if (conn2 != null) conn2.close();
            if (conn3 != null) conn3.close();
        }
    }
}
