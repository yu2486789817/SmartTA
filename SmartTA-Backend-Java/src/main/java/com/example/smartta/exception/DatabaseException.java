package com.example.smartta.exception;

/**
 * 数据库错误
 */
public class DatabaseException extends SmartTAException {
    public DatabaseException(String message) {
        super(message, "DATABASE_ERROR");
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, "DATABASE_ERROR", cause);
    }
}

