package com.example.bankcards.exception;

public class CardAlreadyActiveException
        extends RuntimeException {

    public CardAlreadyActiveException(Long cardId) {
        super(
                "Карта с идентификатором "
                        + cardId
                        + " уже активна"
        );
    }
}