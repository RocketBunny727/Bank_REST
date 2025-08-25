package com.example.bankcards.exception;

public class AcessDeniedException extends RuntimeException {
    public AcessDeniedException(String message) {
        super(message);
    }
}
