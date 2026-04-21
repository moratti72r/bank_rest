package com.example.bankcards.exception;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ExceptHandler {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Hidden
    @ExceptionHandler({MethodArgumentNotValidException.class, IllegalArgumentException.class, ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrResponse handleValidationExceptions(Exception ex) {
        return new ErrResponse("BAD REQUEST",
                ex instanceof MethodArgumentNotValidException
                        ? ((MethodArgumentNotValidException) ex).getAllErrors()
                        .stream()
                        .map(DefaultMessageSourceResolvable::getDefaultMessage)
                        .collect(Collectors.joining(", "))
                        : ex.getMessage(),
                400,
                LocalDateTime.now().format(formatter));
    }

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrResponse handleBadCredentials(Exception ex) {
        return new ErrResponse("UNAUTHORIZED",
                ex.getMessage(),
                401,
                LocalDateTime.now().format(formatter));
    }

    @ExceptionHandler({AccessDeniedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrResponse handleAccessDenied(Exception ex) {
        return new ErrResponse("FORBIDDEN",
                ex.getMessage(),
                403,
                LocalDateTime.now().format(formatter));
    }

    @ExceptionHandler(NotFoundEntityException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrResponse handleNotFound(NotFoundEntityException ex) {
        return new ErrResponse("NOT_FOUND",
                ex.getMessage(),
                404,
                LocalDateTime.now().format(formatter));
    }

    @ExceptionHandler({UniqueValueException.class, BusinessLogicException.class, InactiveCardException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrResponse handleConflict(Exception ex) {
        return new ErrResponse("CONFLICT",
                ex.getMessage(),
                409,
                LocalDateTime.now().format(formatter));
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrResponse handleGeneral(Throwable ex) {
        return new ErrResponse("INTERNAL_SERVER_ERROR",
                ex.getMessage(),
                500,
                LocalDateTime.now().format(formatter));
    }
}
