package com.example.bankcards.exception;

public class MissingExpiryDateException extends RuntimeException {
    public MissingExpiryDateException(String message) {
        super(message);
    }
}
