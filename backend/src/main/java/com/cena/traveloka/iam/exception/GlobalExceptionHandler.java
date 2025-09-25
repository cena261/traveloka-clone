package com.cena.traveloka.iam.exception;

import com.cena.traveloka.iam.dto.response.*;
import com.cena.traveloka.iam.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Global Exception Handler for IAM Module
 *
 * Provides centralized exception handling for all controllers in the IAM module
 * Returns consistent error responses with proper HTTP status codes
 */
@RestControllerAdvice(basePackages = "com.cena.traveloka.iam.controller")
@Slf4j
@Component("iamGlobalExceptionHandler")
public class GlobalExceptionHandler {

    // === Validation Exceptions ===

    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("Validation failed for request {} [{}]: {}",
                request.getRequestURI(), traceId, ex.getMessage());

        ValidationErrorResponse validationError = ValidationErrorResponse.create(
                request.getRequestURI(), request.getMethod(), traceId);

        // Process field errors
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationError.addFieldError(
                    fieldError.getField(),
                    fieldError.getRejectedValue(),
                    fieldError.getDefaultMessage(),
                    fieldError.getCode()
            );
        }

        // Process global errors
        for (ObjectError objectError : ex.getBindingResult().getGlobalErrors()) {
            validationError.addGlobalError(
                    objectError.getObjectName(),
                    objectError.getDefaultMessage(),
                    objectError.getCode()
            );
        }

        ErrorResponse errorResponse = ErrorResponse.validationFailed(
                "Request validation failed",
                mapToValidationErrors(validationError.getFieldErrors())
        );
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setMethod(request.getMethod());
        errorResponse.setTraceId(traceId);

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Validation failed", errorResponse, traceId));
    }

    /**
     * Handle constraint violation exceptions
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("Constraint violation for request {} [{}]: {}",
                request.getRequestURI(), traceId, ex.getMessage());

        ValidationErrorResponse validationError = ValidationErrorResponse.create(
                request.getRequestURI(), request.getMethod(), traceId);

        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            validationError.addFieldError(
                    violation.getPropertyPath().toString(),
                    violation.getInvalidValue(),
                    violation.getMessage(),
                    violation.getMessageTemplate()
            );
        }

        ErrorResponse errorResponse = ErrorResponse.validationFailed(
                "Constraint validation failed",
                mapToValidationErrors(validationError.getFieldErrors())
        );
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setMethod(request.getMethod());
        errorResponse.setTraceId(traceId);

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Constraint validation failed", errorResponse, traceId));
    }

    // === Security Exceptions ===

    /**
     * Handle authentication exceptions
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("Authentication failed for request {} [{}]: {}",
                request.getRequestURI(), traceId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.unauthorized("Authentication failed: " + ex.getMessage());
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setMethod(request.getMethod());
        errorResponse.setTraceId(traceId);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Authentication failed", errorResponse, traceId));
    }

    /**
     * Handle bad credentials exceptions
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(
            BadCredentialsException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("Bad credentials for request {} [{}]: {}",
                request.getRequestURI(), traceId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.unauthorized("Invalid credentials");
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setMethod(request.getMethod());
        errorResponse.setTraceId(traceId);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid credentials", errorResponse, traceId));
    }

    /**
     * Handle access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("Access denied for request {} [{}]: {}",
                request.getRequestURI(), traceId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.forbidden("Access denied: " + ex.getMessage());
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setMethod(request.getMethod());
        errorResponse.setTraceId(traceId);

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied", errorResponse, traceId));
    }

    // === Business Logic Exceptions ===

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("Illegal argument for request {} [{}]: {}",
                request.getRequestURI(), traceId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.badRequest(ex.getMessage());
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setMethod(request.getMethod());
        errorResponse.setTraceId(traceId);

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Bad request", errorResponse, traceId));
    }

    /**
     * Handle illegal state exceptions
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(
            IllegalStateException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("Illegal state for request {} [{}]: {}",
                request.getRequestURI(), traceId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.conflict(ex.getMessage());
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setMethod(request.getMethod());
        errorResponse.setTraceId(traceId);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Resource conflict", errorResponse, traceId));
    }

    // === Database Exceptions ===

    /**
     * Handle data integrity violation exceptions
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.error("Data integrity violation for request {} [{}]: {}",
                request.getRequestURI(), traceId, ex.getMessage());

        String message = "Data integrity constraint violation";
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("unique")) {
                message = "Resource already exists";
            } else if (ex.getMessage().contains("foreign key")) {
                message = "Referenced resource not found";
            } else if (ex.getMessage().contains("not null")) {
                message = "Required field is missing";
            }
        }

        ErrorResponse errorResponse = ErrorResponse.conflict(message);
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setMethod(request.getMethod());
        errorResponse.setTraceId(traceId);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Data constraint violation", errorResponse, traceId));
    }

    // === HTTP Exceptions ===

    /**
     * Handle request method not supported exceptions
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("Method not supported for request {} [{}]: {}",
                request.getRequestURI(), traceId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error("method_not_allowed")
                .errorDescription("HTTP method not supported: " + ex.getMethod())
                .errorCode("IAM_METHOD_NOT_ALLOWED")
                .status(405)
                .path(request.getRequestURI())
                .method(request.getMethod())
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("Method not allowed", errorResponse, traceId));
    }

    /**
     * Handle missing request parameter exceptions
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameterException(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("Missing parameter for request {} [{}]: {}",
                request.getRequestURI(), traceId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.badRequest(
                "Missing required parameter: " + ex.getParameterName());
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setMethod(request.getMethod());
        errorResponse.setTraceId(traceId);

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Missing parameter", errorResponse, traceId));
    }

    /**
     * Handle method argument type mismatch exceptions
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("Type mismatch for request {} [{}]: {}",
                request.getRequestURI(), traceId, ex.getMessage());

        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(), ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        ErrorResponse errorResponse = ErrorResponse.badRequest(message);
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setMethod(request.getMethod());
        errorResponse.setTraceId(traceId);

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid parameter type", errorResponse, traceId));
    }

    /**
     * Handle HTTP message not readable exceptions
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadableException(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("Message not readable for request {} [{}]: {}",
                request.getRequestURI(), traceId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.badRequest("Invalid JSON format or malformed request body");
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setMethod(request.getMethod());
        errorResponse.setTraceId(traceId);

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid request format", errorResponse, traceId));
    }

    /**
     * Handle no handler found exceptions (404)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.warn("No handler found for request {} [{}]: {}",
                request.getRequestURI(), traceId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.notFound("Endpoint not found: " + ex.getRequestURL());
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setMethod(request.getMethod());
        errorResponse.setTraceId(traceId);

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Endpoint not found", errorResponse, traceId));
    }

    // === Generic Exception Handler ===

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();
        log.error("Unexpected error for request {} [{}]: {}",
                request.getRequestURI(), traceId, ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.internalServerError("An unexpected error occurred");
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setMethod(request.getMethod());
        errorResponse.setTraceId(traceId);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error", errorResponse, traceId));
    }

    // === Custom Exception Handlers ===

    /**
     * Handle custom IAM exceptions (can be added as needed)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request) {

        String traceId = generateTraceId();

        // Check for specific runtime exceptions
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("not found")) {
                log.warn("Resource not found for request {} [{}]: {}",
                        request.getRequestURI(), traceId, ex.getMessage());

                ErrorResponse errorResponse = ErrorResponse.notFound(ex.getMessage());
                errorResponse.setPath(request.getRequestURI());
                errorResponse.setMethod(request.getMethod());
                errorResponse.setTraceId(traceId);

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Resource not found", errorResponse, traceId));
            }
        }

        // Fallback to generic handler
        return handleGenericException(ex, request);
    }

    // === Utility Methods ===

    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Map<String, String> mapToValidationErrors(
            List<ValidationErrorResponse.FieldError> fieldErrors) {

        Map<String, String> validationErrors = new HashMap<>();

        if (fieldErrors != null) {
            for (ValidationErrorResponse.FieldError fieldError : fieldErrors) {
                validationErrors.put(
                        fieldError.getField(),
                        fieldError.getMessage()
                );
            }
        }

        return validationErrors;
    }
}