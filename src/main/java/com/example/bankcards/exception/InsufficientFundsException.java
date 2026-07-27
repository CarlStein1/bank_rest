package com.example.bankcards.exception;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(Long cardId) {
        super(
                "Недостаточно средств на карте с идентификатором "
                        + cardId
                        + " для выполнения перевода"
        );
    }
}