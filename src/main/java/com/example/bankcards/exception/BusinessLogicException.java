package com.example.bankcards.exception;

public class BusinessLogicException extends RuntimeException {

    public BusinessLogicException(String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
