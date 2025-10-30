package com.example.smartta.exception;

import lombok.Getter;

/**
 * SmartTA基础异常
 */
@Getter
public class SmartTAException extends RuntimeException {
    private final String errorCode;

    public SmartTAException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public SmartTAException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}

