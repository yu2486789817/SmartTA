package com.example.smartta.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * SmartTA应用配置类
 * 统一管理应用配置，避免硬编码
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "smartta")
public class SmartTAProperties {

    private ApiConfig api = new ApiConfig();
    private ModelConfig model = new ModelConfig();
    private DeepSeekConfig deepseek = new DeepSeekConfig();
    private DataConfig data = new DataConfig();
    private RagConfig rag = new RagConfig();
    private SessionConfig session = new SessionConfig();
    private TimeoutConfig timeout = new TimeoutConfig();

    @Data
    public static class ApiConfig {
        private String title = "SmartTA Backend";
        private String version = "1.0.0";
        private int maxRequestSize = 100 * 1024 * 1024; // 100MB
    }

    @Data
    public static class ModelConfig {
        private EmbeddingConfig embedding = new EmbeddingConfig();
        private LlmConfig llm = new LlmConfig();
    }

    @Data
    public static class EmbeddingConfig {
        private String modelName = "sentence-transformers/all-mpnet-base-v2";
    }

    @Data
    public static class LlmConfig {
        private String modelName = "deepseek-chat";
        private double temperature = 0.6;
        private int maxTokens = 1024;
    }

    @Data
    public static class DeepSeekConfig {
        private String apiKey;
        private String baseUrl = "https://api.deepseek.com";
    }

    @Data
    public static class DataConfig {
        private String pdfDir = "./data/pdfs";
        private String dbPath = "./data/faiss_index";
        private String dataDir = "./data";
    }

    @Data
    public static class RagConfig {
        private int topK = 3;
        private int chunkSize = 1000;
        private int chunkOverlap = 200;
    }

    @Data
    public static class SessionConfig {
        private int maxConversationHistory = 5;
    }

    @Data
    public static class TimeoutConfig {
        private int requestTimeout = 120;
        private int connectTimeout = 10;
    }
}

