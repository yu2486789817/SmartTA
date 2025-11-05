package com.example.smartta.service;

import com.example.smartta.config.SmartTAProperties;
import com.example.smartta.exception.DatabaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 模型管理器
 * 单例模式管理嵌入模型和向量数据库
 * 支持自动重建数据库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelManager {

    private final SmartTAProperties properties;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final @Lazy PreprocessorService preprocessorService;
    
    private volatile boolean initialized = false;

    @PostConstruct
    public void initialize() {
        log.info("初始化模型管理器");
        
        try {
            // 嵌入服务已经通过 @PostConstruct 自动初始化
            
            // 尝试加载向量数据库
            String dbPath = properties.getData().getDbPath();
            Path indexFile = Paths.get(dbPath, "index.pkl");
            
            if (!Files.exists(indexFile)) {
                log.warn("向量数据库不存在，尝试自动重建");
                rebuildDatabaseFromDocuments();
            } else {
                vectorStoreService.loadDatabase();
            }
            
            initialized = true;
            log.info("模型管理器初始化完成");
            
        } catch (Exception e) {
            log.error("模型管理器初始化失败", e);
            throw new RuntimeException("模型管理器初始化失败", e);
        }
    }

    /**
     * 从文档文件重建数据库（自动处理）
     */
    private void rebuildDatabaseFromDocuments() {
        log.info("开始从文档文件重建数据库");

        List<String> documentFiles = findDocumentFiles();

        if (documentFiles.isEmpty()) {
            throw new DatabaseException(
                "向量数据库不存在且未找到可用文档。\n" +
                "请在目录 " + properties.getData().getPdfDir() + " 中放置 PDF、DOCX、TXT 或 PPTX 文件"
            );
        }

        log.info("找到 {} 个文档文件", documentFiles.size());
        documentFiles.stream().limit(5).forEach(doc -> log.info("  - {}", new File(doc).getName()));
        if (documentFiles.size() > 5) {
            log.info("  ... 还有 {} 个文件", documentFiles.size() - 5);
        }

        // 直接自动调用文档处理及入库
        try {
            var result = preprocessorService.preprocessDocuments(null, null, documentFiles);
            log.info("自动初始化文档向量库结果：{}", result);
        } catch (Exception e) {
            log.error("自动初始化文档向量库失败", e);
            throw new RuntimeException("自动初始化文档向量库失败", e);
        }
    }

    /**
     * 查找可用的文档文件
     */
    private List<String> findDocumentFiles() {
        List<String> documentFiles = new ArrayList<>();
        
        // 直接使用 application.yml 中的配置路径
        String configDataDir = properties.getData().getDataDir();
        String configPdfDir = properties.getData().getPdfDir();

        // 将配置的路径放入一个列表中进行检查，可以根据需要调整优先级
        List<String> searchPaths = Arrays.asList(
                configDataDir,
                configPdfDir
        );
        
        log.info("开始根据配置搜索文档文件...");
        log.info("配置的数据目录: {}", configDataDir);
        log.info("配置的PDF目录: {}", configPdfDir);

        for (String dirPath : searchPaths) {
            if (dirPath == null || dirPath.isEmpty()) {
                continue;
            }
            File dir = new File(dirPath);
            if (dir.exists() && dir.isDirectory()) {
                File[] documents = dir.listFiles((d, name) -> {
                    String lower = name.toLowerCase();
                    return lower.endsWith(".pdf") || lower.endsWith(".docx")
                            || lower.endsWith(".txt") || lower.endsWith(".pptx");
                });
                if (documents != null && documents.length > 0) {
                    log.info("✅ 在配置的目录 {} 中找到 {} 个文档文件", dir.getAbsolutePath(), documents.length);
                    for (File document : documents) {
                        documentFiles.add(document.getAbsolutePath());
                    }
                    // 找到后即可返回，避免重复加载
                    return documentFiles;
                } else {
                    log.info("  ⚠️  配置的目录 {} 存在，但没有找到支持的文档文件", dir.getAbsolutePath());
                }
            } else {
                log.debug("  ❌ 配置的目录 {} 不存在", dirPath);
            }
        }
        
        log.warn("❌ 在所有配置的目录中均未找到可用的文档文件");
        return documentFiles;
    }

    /**
     * 重新加载数据库
     */
    public void reloadDatabase() {
        vectorStoreService.reloadDatabase();
    }

    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return initialized && vectorStoreService.isInitialized();
    }

    public VectorStoreService getVectorStore() {
        return vectorStoreService;
    }

    public EmbeddingService getEmbeddingService() {
        return embeddingService;
    }
}
