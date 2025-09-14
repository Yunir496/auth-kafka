package com.example.auth.kafka;

import java.time.Instant;
import java.util.UUID;

public record VerificationRequested(
        String type,
        UUID correlationId,
        String email,
        String code,
        int ttlSeconds,
        Instant issuedAt
) {}
