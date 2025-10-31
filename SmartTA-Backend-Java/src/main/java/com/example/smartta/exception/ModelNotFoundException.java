package com.example.smartta.exception;

/**
 * 模型未找到异常
 */
public class ModelNotFoundException extends SmartTAException {
    public ModelNotFoundException(String modelPath) {
        super("模型文件未找到：" + modelPath, "MODEL_NOT_FOUND");
    }
}

