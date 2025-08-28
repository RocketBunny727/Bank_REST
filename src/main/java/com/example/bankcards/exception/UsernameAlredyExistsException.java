package com.example.bankcards.exception;

public class UsernameAlredyExistsException extends RuntimeException {
    public UsernameAlredyExistsException(String message) {
        super(message);
    }
}
