package com.example.bankcards.exception;

public class InactiveCardException extends RuntimeException {

    public InactiveCardException(String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
