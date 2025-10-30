package com.example.smartta.model;

import lombok.Data;

/**
 * 提问请求模型
 */
@Data
public class QuestionRequest {
    private String question;
    private String contextCode = "";
    private String sessionId; // 可选的会话ID
}

