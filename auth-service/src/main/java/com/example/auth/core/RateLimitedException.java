package com.example.auth.core;

public class RateLimitedException extends RuntimeException {
    private final int retryAfterSeconds;
    public RateLimitedException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
    public int getRetryAfterSeconds() { return retryAfterSeconds; }
}
