package com.cena.traveloka.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleApp(AppException ex, HttpServletRequest req) {
        var code = ex.getCode();
        return ResponseEntity.status(code.getStatus()).body(
                ErrorResponse.builder()
                        .code(code.name())
                        .message(ex.getMessage())
                        .status(code.getStatus().value())
                        .path(req.getRequestURI())
                        .timestamp(OffsetDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ValidationError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toValidationError).toList();

        var code = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity.status(code.getStatus()).body(
                ErrorResponse.builder()
                        .code(code.name())
                        .message(code.getDefaultMessage())
                        .status(code.getStatus().value())
                        .path(req.getRequestURI())
                        .timestamp(OffsetDateTime.now())
                        .errors(details)
                        .build()
        );
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, HttpServletRequest req) {
        var code = ErrorCode.BAD_REQUEST;
        return ResponseEntity.status(code.getStatus()).body(
                ErrorResponse.builder()
                        .code(code.name())
                        .message(ex.getMessage())
                        .status(code.getStatus().value())
                        .path(req.getRequestURI())
                        .timestamp(OffsetDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConflict(DataIntegrityViolationException ex, HttpServletRequest req) {
        var code = ErrorCode.CONFLICT;
        return ResponseEntity.status(code.getStatus()).body(
                ErrorResponse.builder()
                        .code(code.name())
                        .message("Data integrity violation")
                        .status(code.getStatus().value())
                        .path(req.getRequestURI())
                        .timestamp(OffsetDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOthers(Exception ex, HttpServletRequest req) {
        var code = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(code.getStatus()).body(
                ErrorResponse.builder()
                        .code(code.name())
                        .message(code.getDefaultMessage())
                        .status(code.getStatus().value())
                        .path(req.getRequestURI())
                        .timestamp(OffsetDateTime.now())
                        .build()
        );
    }

    private ValidationError toValidationError(FieldError fe) {
        return new ValidationError(fe.getField(), fe.getDefaultMessage());
    }
}
