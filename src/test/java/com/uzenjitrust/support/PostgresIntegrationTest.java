package com.uzenjitrust.support;

import org.junit.jupiter.api.AfterEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public abstract class PostgresIntegrationTest {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("uzenji_test")
            .withUsername("uzenji")
            .withPassword("uzenji");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.task.scheduling.enabled", () -> "false");
        registry.add("spring.task.execution.pool.core-size", () -> "1");
        registry.add("app.security.dev-login-enabled", () -> "true");
        registry.add("app.ops.webhook-secret", () -> "change-me-webhook-secret");
    }

    @AfterEach
    void clearActor() {
        TestSecurity.clear();
    }
}
