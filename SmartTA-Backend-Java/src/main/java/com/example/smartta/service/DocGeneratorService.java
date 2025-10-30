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
 * 文档生成器服务
 * 将项目扫描信息转化为 Markdown 文档
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocGeneratorService {

    private final SmartTAProperties properties;
    private ChatLanguageModel llm;

    @PostConstruct
    public void init() {
        log.info("初始化文档生成器LLM模型");
        
        String apiKey = properties.getDeepseek().getApiKey();
        String baseUrl = properties.getDeepseek().getBaseUrl();
        
        this.llm = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName("deepseek-chat")
                .temperature(0.6)
                .maxTokens(1024)
                .timeout(Duration.ofSeconds(properties.getTimeout().getRequestTimeout()))
                .build();
    }

    /**
     * 生成Markdown文档
     *
     * @param projectInfo 项目扫描信息
     * @return Markdown文档
     */
    public String generateMarkdownSummary(Map<String, Object> projectInfo) {
        log.info("生成项目文档");

        String root = (String) projectInfo.getOrDefault("root", "Unknown Project");
        Integer fileCount = (Integer) projectInfo.getOrDefault("file_count", 0);
        List<Map<String, Object>> files = (List<Map<String, Object>>) projectInfo.getOrDefault("files", List.of());

        // 构造简洁摘要
        StringBuilder summaryBuilder = new StringBuilder();
        summaryBuilder.append("项目路径: ").append(root).append("\n");
        summaryBuilder.append("共包含 ").append(fileCount).append(" 个 Java 文件。\n");

        // 限制前10个文件，防止过长
        int limit = Math.min(files.size(), 10);
        for (int i = 0; i < limit; i++) {
            Map<String, Object> f = files.get(i);
            summaryBuilder.append("\n文件名: ").append(f.get("file")).append("\n");
            
            List<String> classes = (List<String>) f.getOrDefault("classes", List.of());
            summaryBuilder.append("类: ")
                    .append(classes.isEmpty() ? "无" : String.join(", ", classes))
                    .append("\n");
            
            List<String> methods = (List<String>) f.getOrDefault("methods", List.of());
            summaryBuilder.append("方法: ")
                    .append(methods.isEmpty() ? "无" : String.join(", ", methods))
                    .append("\n");
            
            List<String> comments = (List<String>) f.getOrDefault("comments", List.of());
            if (!comments.isEmpty()) {
                summaryBuilder.append("注释摘要:\n");
                int commentLimit = Math.min(comments.size(), 3);
                for (int j = 0; j < commentLimit; j++) {
                    summaryBuilder.append("- ").append(comments.get(j)).append("\n");
                }
            }
        }

        String summaryData = summaryBuilder.toString();

        // 构建 Prompt
        String prompt = """
                你是一名专业的Java架构师，请根据以下扫描到的项目结构生成一份结构化、
                简洁明了的 Markdown 项目文档，包括每个类的功能说明、主要方法、注释摘要，
                请不要使用**标记任何文本。文档应包含项目概述、模块结构，
                以及总体设计简介。
                
                输出格式示例：
                # 项目概述
                ## 模块结构
                ### 类说明
                ...
                
                以下是扫描数据:
                """ + summaryData;

        String markdown = llm.generate(prompt);
        log.info("项目文档生成完成");
        return markdown;
    }
}

