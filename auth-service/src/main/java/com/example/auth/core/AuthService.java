package com.example.auth.core;

import com.example.auth.code.VerificationCodeStore;
import com.example.auth.kafka.VerificationRequested;
import com.example.auth.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {

    private final VerificationCodeStore codes;
    private final KafkaTemplate<String, VerificationRequested> kafka;
    private final String topic;
    private final int ttlMin;

    public AuthService(VerificationCodeStore codes,
                       KafkaTemplate<String, VerificationRequested> kafka,
                       @Value("${kafka.topic.verification}") String topic,
                       @Value("${codes.ttlMinutes}") int ttlMin) {
        this.codes = codes;
        this.kafka = kafka;
        this.topic = topic;
        this.ttlMin = ttlMin;
    }

    @Transactional
    public void register(String email) {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        codes.putCode(email, code);

        var evt = new VerificationRequested(
                "VerificationRequested",
                UUID.randomUUID(),
                email,
                code,
                ttlMin * 60,
                Instant.now()
        );
        kafka.send(topic, email, evt);
    }

    public String verify(String email, String code, JwtService jwt) {
        if (!codes.verifyAndConsume(email, code)) {
            throw new IllegalArgumentException("Invalid or expired code");
        }
        // В этой версии без БД — просто генерим userId
        return jwt.issueToken(UUID.randomUUID().toString(), email.toLowerCase());
    }
}
