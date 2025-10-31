package com.example.smartta.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SmartTAException.class)
    public ResponseEntity<Map<String, Object>> handleSmartTAException(SmartTAException ex) {
        log.error("SmartTA 异常：{} ({})", ex.getMessage(), ex.getErrorCode(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", ex.getMessage());
        response.put("error_code", ex.getErrorCode());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        log.error("未预期的系统错误", ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "服务器内部错误");
        response.put("detail", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

