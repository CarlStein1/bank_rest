package com.example.bankcards.exception;

public class CardAccessDeniedException extends RuntimeException {

    public CardAccessDeniedException(Long cardId) {
        super("Отсутствует доступ к карте с идентификатором " + cardId);
    }
}