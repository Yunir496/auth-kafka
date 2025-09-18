package com.example.auth.web;

import com.example.auth.core.RateLimitedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage() == null ? "Bad request" : ex.getMessage()));
    }

    @ExceptionHandler(RateLimitedException.class)
    public ResponseEntity<Map<String, Object>> tooMany(RateLimitedException ex) {
        var headers = new HttpHeaders();
        headers.add("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
        return new ResponseEntity<>(
                Map.of("error", ex.getMessage(),
                        "retryAfterSeconds", ex.getRetryAfterSeconds()),
                headers,
                HttpStatus.TOO_MANY_REQUESTS
        );
    }
}
