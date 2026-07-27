package com.example.bankcards.exception;

public class DuplicateBlockRequestException extends RuntimeException {

    public DuplicateBlockRequestException(Long cardId) {
        super(
                "Для карты с идентификатором " + cardId
                + " уже существует необработанный запрос на блокировку"
        );
    }
}