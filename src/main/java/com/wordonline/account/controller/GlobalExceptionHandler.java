package com.wordonline.account.controller;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<String>> handleValidationExceptions(WebExchangeBindException ex) {
        String message = String.join(" | ",
                ex.getFieldErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage
                ).toList());
        return Mono.just(ResponseEntity.badRequest().body(message));
    }

    private final static String INVALID_REQUEST_MESSAGE = "요청이 잘못됐습니다.";

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        log.trace(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(e.getMessage());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<String> handleAuthorizationDeniedException(
            AuthorizationDeniedException e) {
        log.trace(e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Unauthorized");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(MethodArgumentNotValidException e) {
        log.trace(e.getMessage());
        return ResponseEntity.badRequest().body(INVALID_REQUEST_MESSAGE);
    }

    /**
     * Without this the handler below would answer every ResponseStatusException with 500, so a
     * rejected refresh token would read as a server fault instead of the 401 the contract
     * states.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException e) {
        log.trace(e.getMessage());
        return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleException(Exception e) {
        log.error("[ERROR] {}", e.getMessage(), e);
        return ResponseEntity.internalServerError().build();
    }
}