package com.example.auth.code;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
        redis.opsForHash().putAll(key, map);
        redis.expire(key, Duration.ofMinutes(ttlMinutes));
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

    private String key(String email) { return "verify:" + email.toLowerCase(); }
}
