package com.example.bankcards.exception;

public class MissingCardNumberException extends RuntimeException {
    public MissingCardNumberException(String message) {
        super(message);
    }
}
