package com.example.bankcards.exception;

public class CardNotFoundException extends RuntimeException {

    public CardNotFoundException(Long cardId) {
        super("Карта с идентификатором " + cardId + " не найдена");
    }
}