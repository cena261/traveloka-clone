package com.cena.traveloka.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // 4xx
    NOT_FOUND(401, HttpStatus.NOT_FOUND, "Resource not found"),
    VALIDATION_ERROR(402, HttpStatus.BAD_REQUEST, "Validation error"),
    BAD_REQUEST(403, HttpStatus.BAD_REQUEST, "Bad request"),
    INVALID_REQUEST(404, HttpStatus.BAD_REQUEST, "Invalid request"),
    ALREADY_EXISTS(405, HttpStatus.CONFLICT, "Resource already exists"),
    CONFLICT(406, HttpStatus.CONFLICT, "Conflict"),
    FORBIDDEN(407, HttpStatus.FORBIDDEN, "Forbidden"),
    UNAUTHORIZED(408, HttpStatus.UNAUTHORIZED, "Unauthorized"),
    RESOURCE_NOT_FOUND(409, HttpStatus.NOT_FOUND, "Resource not found"),
    RATE_LIMIT_EXCEEDED(410, HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded"),
    AUTHENTICATION_FAILED(411, HttpStatus.UNAUTHORIZED, "Authentication failed"),
    ACCESS_DENIED(412, HttpStatus.FORBIDDEN, "Access denied"),
    METHOD_NOT_ALLOWED(413, HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed"),

    // 5xx
    INTERNAL_SERVER_ERROR(501, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    EXTERNAL_SERVICE_ERROR(502, HttpStatus.BAD_GATEWAY, "External service error")
    ;
    private final int code;
    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(int code, HttpStatus status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
