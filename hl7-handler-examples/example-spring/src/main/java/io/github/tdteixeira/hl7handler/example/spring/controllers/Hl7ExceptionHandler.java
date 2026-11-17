package io.github.tdteixeira.hl7handler.example.spring.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.tdteixeira.hl7handler.Hl7Exception;

@RestControllerAdvice
public class Hl7ExceptionHandler {

    @ExceptionHandler({Hl7Exception.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleInvalidInput(RuntimeException exception) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(exception.getMessage()));
    }
}
