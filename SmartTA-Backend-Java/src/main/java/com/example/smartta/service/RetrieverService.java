package com.example.smartta.service;

import com.example.smartta.config.SmartTAProperties;
import com.example.smartta.model.DocumentChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 检索服务
 * 使用缓存的模型管理器进行向量检索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrieverService {

    private final SmartTAProperties properties;
    private final ModelManager modelManager;

    /**
     * 检索上下文
     *
     * @param query 查询文本
     * @return 匹配的文档列表
     */
    public List<Map<String, String>> retrieveContext(String query) {
        return retrieveContext(query, null);
    }

    /**
     * 检索上下文
     *
     * @param query 查询文本
     * @param topK  返回的文档数量，为null时使用配置值
     * @return 匹配的文档列表
     */
    public List<Map<String, String>> retrieveContext(String query, Integer topK) {
        if (topK == null) {
            topK = properties.getRag().getTopK();
        }

        log.debug("检索上下文，查询: {}, topK: {}", query, topK);

        // 获取查询向量
        float[] queryEmbedding = modelManager.getEmbeddingService().embed(query);

        // 执行相似度搜索
        List<DocumentChunk> docs = modelManager.getVectorStore()
                .similaritySearch(queryEmbedding, topK);

        // 转换为Map格式返回
        return docs.stream()
                .map(doc -> {
                    Map<String, String> result = new HashMap<>();
                    result.put("source", doc.getSource() != null ? doc.getSource() : "unknown");
                    result.put("page", doc.getPage() != null ? doc.getPage() : "?");
                    result.put("content", doc.getContent());
                    return result;
                })
                .collect(Collectors.toList());
    }
}

