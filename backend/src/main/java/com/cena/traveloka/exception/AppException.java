package com.cena.traveloka.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    private final ErrorCode code;

    public AppException(ErrorCode code) {
        super(code.getDefaultMessage());
        this.code = code;
    }

    public AppException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
