package com.example.bankcards.exception;

public class LoginAlreadyExistsException extends RuntimeException {
    public LoginAlreadyExistsException(String login) {
        super("Логин " + login + " уже существует");
    }
}
