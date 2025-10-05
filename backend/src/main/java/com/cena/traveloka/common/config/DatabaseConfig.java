package com.cena.traveloka.common.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(basePackages = "com.cena.traveloka")
@EnableJpaAuditing
@EnableTransactionManagement
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${app.datasource.pool.minimum-idle:5}")
    private int minimumIdle;

    @Value("${app.datasource.pool.maximum-pool-size:20}")
    private int maximumPoolSize;

    @Value("${app.datasource.pool.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${app.datasource.pool.idle-timeout:600000}")
    private long idleTimeout;

    @Value("${app.datasource.pool.max-lifetime:1800000}")
    private long maxLifetime;

    @Value("${app.datasource.pool.validation-timeout:5000}")
    private long validationTimeout;

    @Value("${app.datasource.pool.leak-detection-threshold:60000}")
    private long leakDetectionThreshold;

    @Value("${app.datasource.pool.auto-commit:true}")
    private boolean autoCommit;

    @Value("${app.datasource.pool.read-only:false}")
    private boolean readOnly;

    @Value("${app.datasource.pool.connection-test-query:SELECT 1}")
    private String connectionTestQuery;

    @Value("${app.datasource.pool.pool-name:TravelokaHikariPool}")
    private String poolName;

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.datasource.enabled", havingValue = "true", matchIfMissing = true)
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);

        config.setMinimumIdle(minimumIdle);
        config.setMaximumPoolSize(maximumPoolSize);

        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setValidationTimeout(validationTimeout);

        config.setAutoCommit(autoCommit);
        config.setReadOnly(readOnly);
        config.setConnectionTestQuery(connectionTestQuery);
        config.setPoolName(poolName);

        config.setLeakDetectionThreshold(leakDetectionThreshold);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        return new HikariDataSource(config);
    }
}