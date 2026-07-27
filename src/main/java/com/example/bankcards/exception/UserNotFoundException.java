package com.example.bankcards.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("Пользователь с идентификатором " + userId + " не найден");
    }

    public UserNotFoundException(String login) {
        super("Пользователь с логином '" + login + "' не найден");
    }
}