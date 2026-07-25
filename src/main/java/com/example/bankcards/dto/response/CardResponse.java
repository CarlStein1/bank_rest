package com.example.bankcards.dto.response;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Информация о банковской карте")
public record CardResponse(

        @Schema(
                description = "Идентификатор карты",
                example = "10"
        )
        Long id,

        @Schema(
                description = "Идентификатор владельца карты",
                example = "1"
        )
        Long userId,

        @Schema(
                description = "Маскированный номер карты",
                example = "**** **** **** 1234"
        )
        String maskedNumber,

        @Schema(
                description = "Срок действия карты",
                example = "2031-07-31"
        )
        LocalDate expirationDate,

        @Schema(
                description = "Статус карты",
                example = "ACTIVE"
        )
        CardStatus status,

        @Schema(
                description = "Баланс карты",
                example = "12500.50"
        )
        BigDecimal balance
) {

    public static CardResponse from(Card card, String maskedNumber) {
        return new CardResponse(
                card.getId(),
                card.getUser().getId(),
                maskedNumber,
                card.getExpirationDate(),
                card.getStatus(),
                card.getBalance()
        );
    }
}