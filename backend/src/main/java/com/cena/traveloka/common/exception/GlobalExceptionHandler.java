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

/**
 * Global exception handler for centralized error handling across the application.
 * Provides standardized error responses for various exception types.
 *
 * Features:
 * - Handles validation errors with field-specific details
 * - Manages business exceptions with custom error codes
 * - Processes entity not found scenarios
 * - Sanitizes generic errors for production
 * - Includes correlation ID support for request tracking
 * - Logs errors for debugging and monitoring
 * - Returns standardized ErrorResponse format
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles validation errors from @Valid annotation
     *
     * @param ex MethodArgumentNotValidException containing validation errors
     * @param request WebRequest for extracting request path
     * @return ResponseEntity with 400 status and validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {

        logger.warn("Validation error: {}", ex.getMessage());

        // Get the first field error for primary error response
        FieldError fieldError = ex.getBindingResult().getFieldErrors().get(0);

        // Collect all field error messages for comprehensive error description
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

    /**
     * Handles entity not found exceptions
     *
     * @param ex EntityNotFoundException
     * @param request WebRequest for extracting request path
     * @return ResponseEntity with 404 status and error details
     */
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

    /**
     * Handles business rule violations
     *
     * @param ex BusinessException with custom error code
     * @param request WebRequest for extracting request path
     * @return ResponseEntity with 400 status and business error details
     */
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

    /**
     * Handles custom validation exceptions
     *
     * @param ex ValidationException containing custom validation errors
     * @param request WebRequest for extracting request path
     * @return ResponseEntity with 400 status and validation error details
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, WebRequest request) {

        logger.warn("Custom validation error: {}", ex.getMessage());

        ErrorResponse.Builder errorBuilder = ErrorResponse.builder()
            .code("VALIDATION_FAILED")
            .path(extractPath(request));

        // Handle different types of validation errors
        if (ex.isSingleFieldError()) {
            // Single field validation error
            errorBuilder.field(ex.getField()).message(ex.getMessage());
        } else if (ex.hasFieldErrors() && !ex.hasGeneralErrors()) {
            // Multiple field errors only
            String fieldErrorsMessage = ex.getFieldErrors().entrySet().stream()
                .map(entry -> String.format("%s: %s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(", "));
            errorBuilder.message("Field validation errors: " + fieldErrorsMessage);
        } else if (ex.hasGeneralErrors() && !ex.hasFieldErrors()) {
            // General validation errors only
            String generalErrorsMessage = String.join(", ", ex.getGeneralErrors());
            errorBuilder.message("Validation errors: " + generalErrorsMessage);
        } else {
            // Mixed field and general errors
            errorBuilder.message(ex.getMessage());
        }

        ErrorResponse errorResponse = errorBuilder.build();
        HttpHeaders headers = createResponseHeaders(request);
        return new ResponseEntity<>(errorResponse, headers, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles constraint violation exceptions (Bean Validation)
     *
     * @param ex ConstraintViolationException
     * @param request WebRequest for extracting request path
     * @return ResponseEntity with 400 status and constraint violation details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {

        logger.warn("Constraint violation: {}", ex.getMessage());

        // Collect all constraint violation messages
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

    /**
     * Handles all other runtime exceptions
     *
     * @param ex Generic RuntimeException
     * @param request WebRequest for extracting request path
     * @return ResponseEntity with 500 status and sanitized error message
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            RuntimeException ex, WebRequest request) {

        logger.error("Unexpected error: {}", ex.getMessage(), ex);

        // Sanitize error message for production (don't expose internal details)
        String sanitizedMessage = "An unexpected error occurred";

        ErrorResponse errorResponse = ErrorResponse.builder()
            .code("INTERNAL_SERVER_ERROR")
            .message(sanitizedMessage)
            .path(extractPath(request))
            .build();

        HttpHeaders headers = createResponseHeaders(request);
        return new ResponseEntity<>(errorResponse, headers, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles all other general exceptions
     *
     * @param ex Generic Exception
     * @param request WebRequest for extracting request path
     * @return ResponseEntity with 500 status and sanitized error message
     */
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

    /**
     * Extracts the request path from WebRequest description
     *
     * @param request WebRequest containing request details
     * @return Cleaned request path or default value
     */
    private String extractPath(WebRequest request) {
        String description = request.getDescription(false);
        if (description != null && description.startsWith("uri=")) {
            return description.substring(4); // Remove "uri=" prefix
        }
        return description;
    }

    /**
     * Creates response headers including correlation ID if present
     *
     * @param request WebRequest for extracting headers
     * @return HttpHeaders with appropriate response headers
     */
    private HttpHeaders createResponseHeaders(WebRequest request) {
        HttpHeaders headers = new HttpHeaders();

        // Include correlation ID in response if present in request
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId != null) {
            headers.add("X-Correlation-ID", correlationId);
        }

        return headers;
    }
}