package com.example.bankcards.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(
        name = "ApiErrorResponse",
        description = "Единый формат ошибки REST API"
)
public record ApiErrorResponse(
        @Schema(
                description = "Время возникновения ошибки",
                example = "2026-07-28T19:34:52.426"
        )
        LocalDateTime timestamp,

        @Schema(
                description = "HTTP-статус ошибки",
                example = "404"
        )
        int status,

        @Schema(
                description = "Наименование HTTP-ошибки",
                example = "Not Found"
        )
        String error,

        @Schema(
                description = "Сообщение для пользователя",
                example = "Карта с идентификатором 15 не найдена"
        )
        String message,

        @Schema(
                description = "Путь HTTP-запроса",
                example = "/api/cards/15"
        )
        String path
) {
}
