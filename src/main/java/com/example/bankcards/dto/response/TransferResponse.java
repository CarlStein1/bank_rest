package com.example.bankcards.dto.response;

import com.example.bankcards.entity.Card;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Результат перевода между картами")
public record TransferResponse(

        @Schema(
                description = "Идентификатор карты списания",
                example = "1"
        )
        Long fromCardId,

        @Schema(
                description = "Идентификатор карты зачисления",
                example = "2"
        )
        Long toCardId,

        @Schema(
                description = "Сумма перевода",
                example = "500.00"
        )
        BigDecimal amount,

        @Schema(
                description = "Баланс карты списания после перевода",
                example = "1500.00"
        )
        BigDecimal fromCardBalance,

        @Schema(
                description = "Баланс карты зачисления после перевода",
                example = "3000.00"
        )
        BigDecimal toCardBalance
) {

    public static TransferResponse of(
            Card fromCard,
            Card toCard,
            BigDecimal amount
    ) {
        return new TransferResponse(
                fromCard.getId(),
                toCard.getId(),
                amount,
                fromCard.getBalance(),
                toCard.getBalance()
        );
    }
}