package com.example.auth.code;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VerificationCodeStore {

    private final StringRedisTemplate redis;
    private final long ttlMinutes;
    private final int attempts;

    public VerificationCodeStore(StringRedisTemplate redis,
                                 @Value("${codes.ttlMinutes}") long ttlMinutes,
                                 @Value("${codes.attempts}") int attempts) {
        this.redis = redis;
        this.ttlMinutes = ttlMinutes;
        this.attempts = attempts;
    }

    public void putCode(String email, String code) {
        String key = key(email);
        Map<String, String> map = new HashMap<>();
        map.put("code", code);
        map.put("attemptsLeft", String.valueOf(attempts));
        map.put("lastSentAt", String.valueOf(Instant.now().getEpochSecond()));
        redis.opsForHash().putAll(key, map);
        redis.expire(key, Duration.ofMinutes(ttlMinutes));
    }


    public int secondsLeftToResend(String email, int windowSeconds) {
        String key = key(email);
        Object v = redis.opsForHash().get(key, "lastSentAt");
        if (v == null) return 0;
        long last = Long.parseLong(v.toString());
        long now = Instant.now().getEpochSecond();
        long diff = now - last;
        long left = windowSeconds - diff;
        return (int) Math.max(0, left);
    }

    public boolean verifyAndConsume(String email, String code) {
        String key = key(email);
        List<Object> vals = redis.opsForHash().multiGet(key, List.of("code", "attemptsLeft"));
        if (vals == null || vals.get(0) == null) return false;
        String stored = (String) vals.get(0);
        int left = Integer.parseInt((String) vals.get(1));
        if (left <= 0) return false;
        if (!stored.equals(code)) {
            redis.opsForHash().put(key, "attemptsLeft", String.valueOf(left - 1));
            return false;
        }
        redis.delete(key);
        return true;
    }

    public boolean hasActiveCode(String email) {
        return Boolean.TRUE.equals(redis.hasKey(key(email)));
    }

    private String key(String email) { return "verify:" + email.toLowerCase(); }
}
