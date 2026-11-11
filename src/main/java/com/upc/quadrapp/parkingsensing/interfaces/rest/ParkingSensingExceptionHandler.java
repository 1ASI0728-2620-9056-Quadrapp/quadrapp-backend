package com.upc.quadrapp.parkingsensing.interfaces.rest;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Set;

@RestControllerAdvice(basePackages = "com.upc.quadrapp.parkingsensing.interfaces.rest")
public class ParkingSensingExceptionHandler {

    public record ApiError(Instant timestamp, int status, String message) {}

    private static final Set<String> NOT_FOUND = Set.of(
            "Device not found.",
            "Incident not found.",
            "Parking lot not found.",
            "Space not found.",
            "Access not found."
    );

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> invalidArgument(IllegalArgumentException ex) {
        var status = NOT_FOUND.contains(ex.getMessage())
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;
        return error(status, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> businessConflict(IllegalStateException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> databaseConflict(DataIntegrityViolationException ex) {
        return error(HttpStatus.CONFLICT, "The operation conflicts with existing data.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException ex) {
        var message = ex.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getField() + ": " + field.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request body.");
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> invalidParameter(ConstraintViolationException ex) {
        return error(HttpStatus.BAD_REQUEST, "Invalid request parameter.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> malformedJson(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "Invalid JSON request.");
    }

    @ExceptionHandler({AccessDeniedException.class, SecurityException.class})
    public ResponseEntity<ApiError> forbidden(Exception ex) {
        return error(HttpStatus.FORBIDDEN, "You do not have permission for this operation.");
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiError(
                Instant.now(), status.value(), message
        ));
    }
}