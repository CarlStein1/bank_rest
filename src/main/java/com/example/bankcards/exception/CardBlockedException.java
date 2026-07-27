package com.example.bankcards.exception;

public class CardBlockedException extends RuntimeException {

    public CardBlockedException(Long cardId) {
        super(
                "Операция недоступна: карта с идентификатором "
                        + cardId
                        + " заблокирована"
        );
    }
}