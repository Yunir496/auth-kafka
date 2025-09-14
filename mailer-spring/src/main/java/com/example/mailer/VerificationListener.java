package com.example.mailer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class VerificationListener {
    private final ObjectMapper om = new ObjectMapper();

    record VerificationRequested(
            String type, String correlationId, String email, String code, int ttlSeconds, String issuedAt) {}

    @KafkaListener(topics = "${kafka.topic.verification:auth.email.verification.request}", groupId = "mailer-spring")
    public void onMessage(String payload) throws Exception {
        var evt = om.readValue(payload, VerificationRequested.class);
        System.out.printf("[MAILER] Send code %s to %s (valid %ss)%n", evt.code(), evt.email(), evt.ttlSeconds());
    }
}
