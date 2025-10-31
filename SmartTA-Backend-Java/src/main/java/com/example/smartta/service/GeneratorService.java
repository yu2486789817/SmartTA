package com.example.smartta.service;

import com.example.smartta.config.SmartTAProperties;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 生成器服务
 * 使用缓存的LLM和会话管理器生成答案
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeneratorService {

    private final SmartTAProperties properties;
    private final ConversationManager conversationManager;
    
    private ChatLanguageModel llm;

    @PostConstruct
    public void init() {
        log.info("初始化LLM模型：{}", properties.getModel().getLlm().getModelName());
        
        String apiKey = properties.getDeepseek().getApiKey();
        String baseUrl = properties.getDeepseek().getBaseUrl();
        
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("DEEPSEEK_API_KEY 未配置");
        }
        
        this.llm = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(properties.getModel().getLlm().getModelName())
                .temperature(properties.getModel().getLlm().getTemperature())
                .maxTokens(properties.getModel().getLlm().getMaxTokens())
                .timeout(Duration.ofSeconds(properties.getTimeout().getRequestTimeout()))
                .build();
        
        log.info("LLM模型初始化完成");
    }

    /**
     * 生成答案
     *
     * @param query           用户问题
     * @param retrievedChunks 检索到的文档块
     * @param contextCode     代码上下文
     * @param sessionId       会话ID
     * @return 生成的答案
     */
    public String getAnswer(String query, List<Map<String, String>> retrievedChunks,
                           String contextCode, String sessionId) {
        
        log.info("生成答案 - 会话ID: {}, 问题: {}", sessionId, 
                query.length() > 50 ? query.substring(0, 50) + "..." : query);

        // 获取历史对话
        String historyText = conversationManager.formatHistory(sessionId);

        // 组合课程资料文本
        String contextText = retrievedChunks.stream()
                .map(chunk -> String.format("[%s，第%s页] %s",
                        chunk.get("source"),
                        chunk.get("page"),
                        chunk.get("content")))
                .collect(Collectors.joining("\n\n"));

        // 构建提示词
        String prompt = String.format("""
                你是一名智能助教，负责回答学生关于课程内容的问题。
                你可以结合一般编程知识与课程资料作答。切记输出回答时不要用**标记任何文本。
                
                请根据以下内容生成答案：
                1. 如果课程资料中的内容与问题高度相关，请明确引用出处（例如："见《Lecture 3 - Memory Management》第 12 页"）。
                2. 如果课程资料与问题不直接相关，请说明"本回答基于一般知识，未引用课程资料"。
                
                ---
                课程资料:
                %s
                
                历史对话:
                %s
                
                代码内容:
                %s
                
                学生的问题:
                %s
                ---
                """,
                contextText,
                historyText != null && !historyText.equals("无") ? historyText : "无",
                contextCode != null && !contextCode.isEmpty() ? contextCode : "无",
                query
        );

        // 调用LLM生成答案
        String answer = llm.generate(prompt);

        // 更新历史记录
        conversationManager.append(sessionId, query, answer);

        log.info("答案生成完成 - 会话ID: {}", sessionId);
        return answer;
    }
}

