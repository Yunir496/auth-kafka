package com.example.auth.security;

import io.jsonwebtoken.*; import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets; import java.time.*; import java.util.Date;
import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key; private final long accessMinutes;
    public JwtService(@Value("${security.jwt.secret}") String secret,
                      @Value("${security.jwt.accessMinutes}") long accessMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessMinutes = accessMinutes;
    }
    public String issueToken(String userId, String email) {
        var now = Instant.now();
        return Jwts.builder().subject(userId).claim("email", email)
                .issuedAt(Date.from(now)).expiration(Date.from(now.plus(Duration.ofMinutes(accessMinutes))))
                .signWith(key, Jwts.SIG.HS256).compact();
    }
    public Jws<Claims> parse(String jwt){ return Jwts.parser().verifyWith(key).build().parseSignedClaims(jwt); }
}
