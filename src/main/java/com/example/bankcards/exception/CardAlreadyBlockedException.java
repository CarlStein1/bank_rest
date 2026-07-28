package com.example.bankcards.exception;

public class CardAlreadyBlockedException
        extends RuntimeException {

    public CardAlreadyBlockedException(Long cardId) {
        super(
                "Карта с идентификатором "
                        + cardId
                        + " уже заблокирована"
        );
    }
}