package com.example.auth.web;

import com.example.auth.core.AuthService;
import com.example.auth.security.JwtService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Validated
public class AuthController {

    private final JwtService jwt;
    private final AuthService auth;

    public AuthController(JwtService jwt, AuthService auth) {
        this.jwt = jwt;
        this.auth = auth;
    }

    public record EmailDto(@NotBlank @Email String email) {}
    public record VerifyDto(@NotBlank @Email String email, @NotBlank String code) {}
    public record TokenDto(String accessToken) {}

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@RequestBody EmailDto body) {
        auth.register(body.email());
    }

    @PostMapping("/auth/verify")
    public TokenDto verify(@RequestBody VerifyDto body) {
        String token = auth.verify(body.email(), body.code(), jwt);
        return new TokenDto(token);
    }

    @GetMapping("/secure/ping")
    public Map<String, String> ping(Authentication authn) {
        return Map.of("pong", "ok", "userId", authn.getName(), "email", String.valueOf(authn.getDetails()));
    }

    // dev-эндпоинт из прошлой версии можно оставить/удалить по желанию
}
