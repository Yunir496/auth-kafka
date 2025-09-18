package com.example.auth.web;

import com.example.auth.core.AuthService;
import com.example.auth.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Auth", description = "Email-коды, верификация и JWT")
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

    public record EmailDto(
            @Schema(example = "yunir@example.com")
            @NotBlank @Email String email) {}

    public record VerifyDto(
            @Schema(example = "yunir@example.com")
            @NotBlank @Email String email,
            @Schema(example = "123456", minLength = 6, maxLength = 6)
            @NotBlank String code) {}

    public record TokenDto(
            @Schema(example = "eyJhbGciOiJIUzI1NiJ9...") String accessToken) {}

    @Operation(summary = "Старт регистрации: сгенерировать и отправить код",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Код отправлен"),
                    @ApiResponse(responseCode = "429", description = "Сработал антиспам"),
                    @ApiResponse(responseCode = "400", description = "Некорректный email")
            })
    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@RequestBody @Valid EmailDto body) {
        auth.register(body.email());
    }

    @Operation(summary = "Переотправить код (под антиспамом)",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Код переотправлен"),
                    @ApiResponse(responseCode = "429", description = "Сработал антиспам"),
                    @ApiResponse(responseCode = "400", description = "Нет активной заявки на email")
            })
    @PostMapping("/auth/resend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resend(@RequestBody @Valid EmailDto body) {
        auth.resend(body.email());
    }

    @Operation(summary = "Проверить код и получить JWT",
            responses = {
                    @ApiResponse(responseCode = "200", description = "JWT выдан",
                            content = @Content(schema = @Schema(implementation = TokenDto.class))),
                    @ApiResponse(responseCode = "400", description = "Неверный/просроченный код")
            })
    @PostMapping("/auth/verify")
    public TokenDto verify(@RequestBody @Valid VerifyDto body) {
        String token = auth.verify(body.email(), body.code(), jwt);
        return new TokenDto(token);
    }

    @Operation(summary = "Закрытый эндпоинт (JWT обязателен)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/secure/ping")
    public Map<String, String> ping(Authentication authn) {
        return Map.of(
                "pong", "ok",
                "userId", authn.getName(),
                "email", String.valueOf(authn.getDetails()));
    }
}
