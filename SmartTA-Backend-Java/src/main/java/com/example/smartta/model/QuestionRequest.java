package com.example.smartta.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 提问请求模型
 */
@Data
public class QuestionRequest {
    private String question;
    @JsonProperty("context_code")
    private String contextCode = "";
    private String sessionId; // 可选的会话ID
}

