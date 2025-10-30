package com.example.smartta.exception;

import lombok.Getter;

/**
 * API调用错误
 */
@Getter
public class ApiException extends SmartTAException {
    private final Integer statusCode;

    public ApiException(String message, Integer statusCode) {
        super(message, "API_ERROR");
        this.statusCode = statusCode;
    }

    public ApiException(String message) {
        this(message, null);
    }
}

