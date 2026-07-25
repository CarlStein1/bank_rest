package com.example.bankcards.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Комментарий при обработке заявки на блокировку")
public record ProcessCardBlockRequest(

        @Schema(
                description = "Комментарий администратора",
                example = "Заявка одобрена, карта заблокирована"
        )
        @Size(max = 500)
        String adminComment
) {
}