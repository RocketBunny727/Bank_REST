package com.example.bankcards.exception;

public class CardAlredyExistsException extends RuntimeException {
    public CardAlredyExistsException(String message) {
        super(message);
    }
}
