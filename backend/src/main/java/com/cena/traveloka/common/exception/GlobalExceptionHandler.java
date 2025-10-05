package com.cena.traveloka.common.exception;

import com.cena.traveloka.common.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {

        logger.warn("Validation error: {}", ex.getMessage());

        FieldError fieldError = ex.getBindingResult().getFieldErrors().get(0);

        String allErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> String.format("%s: %s", error.getField(), error.getDefaultMessage()))
            .collect(Collectors.joining(", "));

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code("VALIDATION_FAILED")
            .message(allErrors)
            .field(fieldError.getField())
            .path(extractPath(request))
            .build();

        HttpHeaders headers = createResponseHeaders(request);
        return new ResponseEntity<>(errorResponse, headers, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex, WebRequest request) {

        logger.warn("Entity not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code("ENTITY_NOT_FOUND")
            .message(ex.getMessage())
            .path(extractPath(request))
            .build();

        HttpHeaders headers = createResponseHeaders(request);
        return new ResponseEntity<>(errorResponse, headers, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, WebRequest request) {

        logger.warn("Business exception [{}]: {}", ex.getErrorCode(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code(ex.getErrorCode())
            .message(ex.getMessage())
            .path(extractPath(request))
            .build();

        HttpHeaders headers = createResponseHeaders(request);
        return new ResponseEntity<>(errorResponse, headers, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, WebRequest request) {

        logger.warn("Custom validation error: {}", ex.getMessage());

        ErrorResponse.Builder errorBuilder = ErrorResponse.builder()
            .code("VALIDATION_FAILED")
            .path(extractPath(request));

        if (ex.isSingleFieldError()) {
            errorBuilder.field(ex.getField()).message(ex.getMessage());
        } else if (ex.hasFieldErrors() && !ex.hasGeneralErrors()) {
            String fieldErrorsMessage = ex.getFieldErrors().entrySet().stream()
                .map(entry -> String.format("%s: %s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(", "));
            errorBuilder.message("Field validation errors: " + fieldErrorsMessage);
        } else if (ex.hasGeneralErrors() && !ex.hasFieldErrors()) {
            String generalErrorsMessage = String.join(", ", ex.getGeneralErrors());
            errorBuilder.message("Validation errors: " + generalErrorsMessage);
        } else {
            errorBuilder.message(ex.getMessage());
        }

        ErrorResponse errorResponse = errorBuilder.build();
        HttpHeaders headers = createResponseHeaders(request);
        return new ResponseEntity<>(errorResponse, headers, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {

        logger.warn("Constraint violation: {}", ex.getMessage());

        String violations = ex.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.joining(", "));

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code("CONSTRAINT_VIOLATION")
            .message("Validation failed: " + violations)
            .path(extractPath(request))
            .build();

        HttpHeaders headers = createResponseHeaders(request);
        return new ResponseEntity<>(errorResponse, headers, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            RuntimeException ex, WebRequest request) {

        logger.error("Unexpected error: {}", ex.getMessage(), ex);

        String sanitizedMessage = "An unexpected error occurred";

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code("INTERNAL_SERVER_ERROR")
            .message(sanitizedMessage)
            .path(extractPath(request))
            .build();

        HttpHeaders headers = createResponseHeaders(request);
        return new ResponseEntity<>(errorResponse, headers, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(
            Exception ex, WebRequest request) {

        logger.error("Unexpected exception: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code("INTERNAL_SERVER_ERROR")
            .message("An unexpected error occurred")
            .path(extractPath(request))
            .build();

        HttpHeaders headers = createResponseHeaders(request);
        return new ResponseEntity<>(errorResponse, headers, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String extractPath(WebRequest request) {
        String description = request.getDescription(false);
        if (description != null && description.startsWith("uri=")) {
            return description.substring(4); // Remove "uri=" prefix
        }
        return description;
    }

    private HttpHeaders createResponseHeaders(WebRequest request) {
        HttpHeaders headers = new HttpHeaders();

        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId != null) {
            headers.add("X-Correlation-ID", correlationId);
        }

        return headers;
    }
}