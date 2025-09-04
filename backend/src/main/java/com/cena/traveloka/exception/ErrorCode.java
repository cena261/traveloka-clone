package com.cena.traveloka.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // 4xx
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation error"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Bad request"),
    CONFLICT(HttpStatus.CONFLICT, "Conflict"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized"),

    // 5xx
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
