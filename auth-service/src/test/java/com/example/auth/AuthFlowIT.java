package com.example.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort; // ВАЖНО: пакет для Spring Boot 3
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthFlowIT {

    @LocalServerPort
    int port;


    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);


    @Container
    static KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.3"));

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        // Redis
        registry.add("spring.data.redis.host", () -> redis.getHost());
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        registry.add("kafka.topic.verification", () -> "auth.email.verification.request.test");

        registry.add("spring.kafka.admin.auto-create", () -> "false");
    }

    @BeforeAll
    static void beforeAll() {
        if (!redis.isRunning()) redis.start();
        if (!kafka.isRunning()) kafka.start();
    }

    @AfterAll
    static void afterAll() {
        if (redis != null && redis.isRunning()) redis.stop();
        if (kafka != null && kafka.isRunning()) kafka.stop();
    }

    @Test
    void contextStarts() {

        assertThat(port).isPositive();
    }
}
