package com.example.smartta.exception;

/**
 * 请求验证错误
 */
public class RequestValidationException extends SmartTAException {
    public RequestValidationException(String message) {
        super(message, "VALIDATION_ERROR");
    }
}

