package com.example.bankcards.dto.request;

import com.example.bankcards.entity.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Данные для обновления пользователя")
public record UpdateUserRequest(

        @Schema(
                description = "Новое имя пользователя",
                example = "Иван"
        )
        @NotBlank
        @Size(max = 50)
        String firstName,

        @Schema(
                description = "Новое отчество пользователя",
                example = "Иванович"
        )
        @Size(max = 50)
        String middleName,

        @Schema(
                description = "Новая фамилия пользователя",
                example = "Иванов"
        )
        @NotBlank
        @Size(max = 50)
        String lastName,

        @Schema(
                description = "Новая роль пользователя",
                example = "USER"
        )
        @NotNull
        UserRole role
) {
}