package com.example.smartta.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 回答响应模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResponse {
    private String answer;
    private String sessionId;
}

