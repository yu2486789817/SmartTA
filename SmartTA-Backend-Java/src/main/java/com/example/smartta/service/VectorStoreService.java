package com.example.smartta.service;

import com.example.smartta.config.SmartTAProperties;
import com.example.smartta.exception.DatabaseException;
import com.example.smartta.model.DocumentChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 向量存储服务
 * 使用简化的内存向量存储实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final SmartTAProperties properties;
    private List<DocumentChunk> documents = new ArrayList<>();
    private boolean isLoaded = false;

    /**
     * 加载向量数据库
     */
    public synchronized void loadDatabase() {
        if (isLoaded) {
            return;
        }

        String dbPath = properties.getData().getDbPath();
        Path indexFile = Paths.get(dbPath, "index.pkl");

        if (!Files.exists(indexFile)) {
            log.warn("向量数据库不存在: {}", indexFile);
            throw new DatabaseException("向量数据库不存在: " + indexFile);
        }

        try {
            log.info("加载向量数据库: {}", indexFile);
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(indexFile.toFile()))) {
                documents = (List<DocumentChunk>) ois.readObject();
            }
            isLoaded = true;
            log.info("向量数据库加载完成，文档数: {}", documents.size());
        } catch (Exception e) {
            throw new DatabaseException("加载向量数据库失败", e);
        }
    }

    /**
     * 保存向量数据库
     */
    public synchronized void saveDatabase() {
        String dbPath = properties.getData().getDbPath();
        Path indexFile = Paths.get(dbPath, "index.pkl");

        try {
            Files.createDirectories(indexFile.getParent());
            
            log.info("保存向量数据库: {}", indexFile);
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(indexFile.toFile()))) {
                oos.writeObject(documents);
            }
            log.info("向量数据库保存完成");
        } catch (Exception e) {
            throw new DatabaseException("保存向量数据库失败", e);
        }
    }

    /**
     * 相似度搜索
     *
     * @param queryEmbedding 查询向量
     * @param topK          返回的文档数量
     * @return 相似文档列表
     */
    public List<DocumentChunk> similaritySearch(float[] queryEmbedding, int topK) {
        if (!isLoaded) {
            loadDatabase();
        }

        return documents.stream()
                .map(doc -> {
                    double similarity = cosineSimilarity(queryEmbedding, doc.getEmbedding());
                    return new ScoredDocument(doc, similarity);
                })
                .sorted(Comparator.comparingDouble(ScoredDocument::getScore).reversed())
                .limit(topK)
                .map(ScoredDocument::getDocument)
                .collect(Collectors.toList());
    }

    /**
     * 添加文档
     */
    public synchronized void addDocuments(List<DocumentChunk> newDocs) {
        if (!isLoaded) {
            loadDatabase();
        }
        documents.addAll(newDocs);
    }

    /**
     * 创建新数据库
     */
    public synchronized void createDatabase(List<DocumentChunk> docs) {
        documents = new ArrayList<>(docs);
        isLoaded = true;
        saveDatabase();
    }

    /**
     * 重新加载数据库
     */
    public synchronized void reloadDatabase() {
        isLoaded = false;
        loadDatabase();
    }

    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return isLoaded && !documents.isEmpty();
    }

    /**
     * 计算余弦相似度
     */
    private double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) {
            throw new IllegalArgumentException("向量维度不匹配");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 带分数的文档
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ScoredDocument {
        private DocumentChunk document;
        private double score;
    }
}

