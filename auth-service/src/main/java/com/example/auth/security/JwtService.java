package com.example.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessMinutes;

    public JwtService(
            @Value("${security.jwt.secret}") String secretEnv,
            @Value("${security.jwt.accessMinutes}") long accessMinutes
    ) {
        byte[] raw;

        try {
            raw = Base64.getDecoder().decode(secretEnv);
        } catch (IllegalArgumentException e) {
            raw = secretEnv.getBytes(StandardCharsets.UTF_8);
        }
        if (raw.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 256 bits (32 bytes).");
        }
        this.key = Keys.hmacShaKeyFor(raw);
        this.accessMinutes = accessMinutes;
    }

    public String issueToken(String userId, String email) {
        var now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(accessMinutes))))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Jws<Claims> parse(String jwt) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(jwt);
    }
}
