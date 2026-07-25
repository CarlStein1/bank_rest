package com.example.bankcards.dto.response;

import com.example.bankcards.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Результат успешной авторизации")
public record AuthResponse(

        @Schema(
                description = "JWT-токен доступа"
        )
        String token,

        @Schema(description = "Данные авторизованного пользователя")
        UserResponse user
) {

    public static AuthResponse from(User user, String token) {
        return new AuthResponse(
                token,
                UserResponse.from(user)
        );
    }
}