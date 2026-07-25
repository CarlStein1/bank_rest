package com.example.bankcards.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Данные для создания заявки на блокировку карты")
public record CreateCardBlockRequest(

        @Schema(
                description = "Причина блокировки карты",
                example = "Карта была потеряна"
        )
        @Size(max = 500)
        String reason
) {
}