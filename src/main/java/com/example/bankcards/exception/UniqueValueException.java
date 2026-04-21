package com.example.bankcards.exception;

public class UniqueValueException extends RuntimeException {

    public UniqueValueException(String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
