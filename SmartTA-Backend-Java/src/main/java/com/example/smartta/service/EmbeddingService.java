package com.example.smartta.service;

import com.example.smartta.config.SmartTAProperties;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * 嵌入服务
 * 负责文本向量化
 */
@Slf4j
@Service
public class EmbeddingService {

    private final SmartTAProperties properties;
    private EmbeddingModel embeddingModel;

    public EmbeddingService(SmartTAProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        log.info("初始化嵌入模型：{}", properties.getModel().getEmbedding().getModelName());
        // 使用 LangChain4j 提供的轻量级嵌入模型
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        log.info("嵌入模型初始化完成");
    }

    /**
     * 对文本进行向量化
     *
     * @param text 输入文本
     * @return 向量数组
     */
    public float[] embed(String text) {
        Embedding embedding = embeddingModel.embed(text).content();
        return embedding.vector();
    }

    /**
     * 批量向量化
     *
     * @param texts 文本列表
     * @return 向量数组列表
     */
    public float[][] embedAll(java.util.List<String> texts) {
        return texts.stream()
                .map(this::embed)
                .toArray(float[][]::new);
    }
}

