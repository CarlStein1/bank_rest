package com.example.bankcards.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Данные для перевода между своими картами")
public record TransferRequest(

        @Schema(
                description = "Идентификатор карты списания",
                example = "1"
        )
        @NotNull
        @Positive
        Long fromCardId,

        @Schema(
                description = "Идентификатор карты зачисления",
                example = "2"
        )
        @NotNull
        @Positive
        Long toCardId,

        @Schema(
                description = "Сумма перевода",
                example = "500.00"
        )
        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount
) {
}