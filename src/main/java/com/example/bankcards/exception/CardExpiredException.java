package com.example.bankcards.exception;

public class CardExpiredException
        extends RuntimeException {

    public CardExpiredException(Long cardId) {
        super(
                "Срок действия карты с идентификатором "
                        + cardId
                        + " истёк"
        );
    }
}