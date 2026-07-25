package com.example.bankcards.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Данные для авторизации")
public record LoginRequest(

        @Schema(
                description = "Логин пользователя",
                example = "ivanov"
        )
        @NotBlank
        @Size(min = 3, max = 50)
        String login,

        @Schema(
                description = "Пароль пользователя",
                example = "StrongPass123"
        )
        @NotBlank
        @Size(min = 8, max = 100)
        String password
) {
}