package com.upc.quadrapp.prediction.interfaces.rest;

import com.upc.quadrapp.prediction.domain.model.exceptions.PredictionNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice(basePackages = "com.upc.quadrapp.prediction.interfaces.rest")
public class PredictionExceptionHandler {

    public record ApiError(Instant timestamp, int status, String message) {}

    @ExceptionHandler(PredictionNotFoundException.class)
    public ResponseEntity<ApiError> notFound(PredictionNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> invalidArgument(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> invalidState(IllegalStateException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> dataIntegrity() {
        return error(
                HttpStatus.CONFLICT,
                "The requested data conflicts with existing records."
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accessDenied() {
        return error(
                HttpStatus.FORBIDDEN,
                "You do not have permission for this operation."
        );
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiError(Instant.now(), status.value(), message));
    }
}
