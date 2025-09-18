package com.example.auth;

import com.example.auth.code.VerificationCodeStore;
import com.example.auth.core.AuthService;
import com.example.auth.security.JwtService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuthServiceIT {

    static KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.3"));
    static GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7")).withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        if (!KAFKA.isRunning()) KAFKA.start();
        if (!REDIS.isRunning()) REDIS.start();
        r.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

        r.add("codes.resendWindowSeconds", () -> 0);
    }

    @Autowired AuthService authService;
    @Autowired VerificationCodeStore store;
    @Autowired StringRedisTemplate redis;
    @Autowired JwtService jwt;

    @BeforeEach
    void clean() {
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void registerPublishesKafkaAndStoresCode() {
        String email = "it@example.com";
        authService.register(email);


        var code = (String) redis.opsForHash().get("verify:" + email, "code");
        assertThat(code).isNotBlank();


        try (var consumer = new KafkaConsumer<String, String>(consumerProps())) {
            consumer.subscribe(List.of("auth.email.verification.request"));
            var records = consumer.poll(Duration.ofSeconds(3));
            assertThat(records.count()).isGreaterThan(0);
        }
    }

    @Test
    void verifyHappyPathIssuesJwt() {
        String email = "ok@example.com";
        authService.register(email);
        var code = (String) redis.opsForHash().get("verify:" + email, "code");

        var token = authService.verify(email, code, jwt);
        assertThat(token).isNotBlank();
    }

    @Test
    void wrongCodeDecrementsAttempts() {
        String email = "wrong@example.com";
        authService.register(email);

        assertThrows(IllegalArgumentException.class, () -> authService.verify(email, "000000", jwt));

        var left = (String) redis.opsForHash().get("verify:" + email, "attemptsLeft");
        assertThat(Integer.parseInt(left)).isLessThan(5);
    }

    private Properties consumerProps() {
        var p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        p.put(ConsumerConfig.GROUP_ID_CONFIG, "it-tests");
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return p;
    }
}
