package com.example.smartta.exception;

/**
 * 配置错误
 */
public class ConfigurationException extends SmartTAException {
    public ConfigurationException(String message) {
        super(message, "CONFIGURATION_ERROR");
    }
}

