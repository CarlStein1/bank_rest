package com.example.bankcards.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Возникновение ошибки при запросе на сервер")
public record ApiErrorResponse(
        @Schema(description = "Время возникновения ошибки")
        LocalDateTime timestamp,

        @Schema(description = "Статус ошибки", example = "404")
        int status,

        @Schema(description = "Наименование ошибки", example = "Not Found")
        String error,

        @Schema(description = "Сообщение для пользователя", example = "Карта с идентификатором 15 не найдена")
        String message,

        @Schema(description = "Путь HTTP-запроса", example = "/api/cards/15")
        String path

) {
}
