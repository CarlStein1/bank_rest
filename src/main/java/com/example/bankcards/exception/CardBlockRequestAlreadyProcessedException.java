package com.example.bankcards.exception;

public class CardBlockRequestAlreadyProcessedException
        extends RuntimeException {

    public CardBlockRequestAlreadyProcessedException(
            Long requestId
    ) {
        super(
                "Заявка на блокировку с идентификатором "
                        + requestId
                        + " уже была обработана"
        );
    }
}