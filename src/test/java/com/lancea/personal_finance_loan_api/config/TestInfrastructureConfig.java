package com.lancea.personal_finance_loan_api.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@TestConfiguration(proxyBeanMethods = false)
public class TestInfrastructureConfig {

    private static final int REDIS_PORT = 6379;

    @Bean
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:18-alpine").withDatabaseName("finance_test");
    }

    @Bean
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>("redis:7-alpine").withExposedPorts(REDIS_PORT);
    }

    @Bean
    DynamicPropertyRegistrar integrationTestProperties(PostgreSQLContainer postgres,
            @Qualifier("redisContainer") GenericContainer<?> redis) throws NoSuchAlgorithmException {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var keyPair = generator.generateKeyPair();
        var base64 = Base64.getEncoder();

        return registry -> {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
            registry.add("spring.data.redis.host", redis::getHost);
            registry.add("spring.data.redis.port", () -> redis.getMappedPort(REDIS_PORT));
            registry.add("app.jwt.key.private", () -> base64.encodeToString(keyPair.getPrivate().getEncoded()));
            registry.add("app.jwt.key.public", () -> base64.encodeToString(keyPair.getPublic().getEncoded()));
        };
    }
}
