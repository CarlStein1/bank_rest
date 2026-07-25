package com.example.bankcards.dto.response;

import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Информация о пользователе")
public record UserResponse(

        @Schema(
                description = "Идентификатор пользователя",
                example = "1"
        )
        Long id,

        @Schema(
                description = "Имя пользователя",
                example = "Иван"
        )
        String firstName,

        @Schema(
                description = "Отчество пользователя",
                example = "Иванович"
        )
        String middleName,

        @Schema(
                description = "Фамилия пользователя",
                example = "Иванов"
        )
        String lastName,

        @Schema(
                description = "Логин пользователя",
                example = "ivanov"
        )
        String login,

        @Schema(
                description = "Роль пользователя",
                example = "USER"
        )
        UserRole role
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getMiddleName(),
                user.getLastName(),
                user.getLogin(),
                user.getRole()
        );
    }
}