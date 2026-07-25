package com.example.bankcards.dto.request;

import com.example.bankcards.entity.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Данные для создания пользователя")
public record CreateUserRequest(

        @Schema(
                description = "Имя пользователя",
                example = "Иван"
        )
        @NotBlank
        @Size(max = 50)
        String firstName,

        @Schema(
                description = "Отчество пользователя",
                example = "Иванович"
        )
        @Size(max = 50)
        String middleName,

        @Schema(
                description = "Фамилия пользователя",
                example = "Иванов"
        )
        @NotBlank
        @Size(max = 50)
        String lastName,

        @Schema(
                description = "Логин пользователя",
                example = "ivanov"
        )
        @NotBlank
        @Size(min = 3, max = 50)
        String login,

        @Schema(
                description = "Пароль пользователя",
                example = "Password123"
        )
        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @Schema(
                description = "Роль пользователя",
                example = "USER"
        )
        @NotNull
        UserRole role
) {
}