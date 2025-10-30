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
                rebuildDatabaseFromPdfs();
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
     * 从PDF文件重建数据库(自动处理)
     */
    private void rebuildDatabaseFromPdfs() {
        log.info("开始从PDF文件重建数据库");

        List<String> pdfFiles = findPdfFiles();

        if (pdfFiles.isEmpty()) {
            throw new DatabaseException(
                "向量数据库不存在且未找到PDF文件。\n" +
                "请上传PDF文件到 " + properties.getData().getPdfDir()
            );
        }

        log.info("找到 {} 个PDF文件", pdfFiles.size());
        pdfFiles.stream().limit(5).forEach(pdf -> log.info("  - {}", new File(pdf).getName()));
        if (pdfFiles.size() > 5) {
            log.info("  ... 还有 {} 个文件", pdfFiles.size() - 5);
        }

        // 直接自动调用PDF处理及入库
        try {
            var result = preprocessorService.preprocessPdfs(null, null, pdfFiles);
            log.info("自动初始化PDF向量库结果: {}", result);
        } catch (Exception e) {
            log.error("自动初始化PDF向量库失败", e);
            throw new RuntimeException("自动初始化PDF向量库失败", e);
        }
    }

    /**
     * 查找可用的PDF文件
     */
    private List<String> findPdfFiles() {
        List<String> pdfFiles = new ArrayList<>();
        
        // 获取项目根目录
        String currentDir = System.getProperty("user.dir");
        File projectRoot = new File(currentDir).getParentFile();
        
        List<String> searchPaths = Arrays.asList(
                projectRoot.getAbsolutePath() + "/SmartTA/src/pdf",
                currentDir + "/src/pdf",
                currentDir + "/SmartTA/src/pdf",
                currentDir + "/../SmartTA/src/pdf",
                "./src/pdf",
                "./pdfs",
                "./data"
        );
        
        log.info("当前目录: {}", currentDir);
        log.info("项目根目录: {}", projectRoot.getAbsolutePath());
        log.info("开始搜索PDF文件...");
        
        for (String dirPath : searchPaths) {
            File dir = new File(dirPath);
            if (dir.exists() && dir.isDirectory()) {
                File[] pdfs = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));
                if (pdfs != null && pdfs.length > 0) {
                    log.info("✅ 在 {} 找到 {} 个PDF文件", dir.getAbsolutePath(), pdfs.length);
                    for (File pdf : pdfs) {
                        pdfFiles.add(pdf.getAbsolutePath());
                    }
                    return pdfFiles;
                } else {
                    log.info("  ⚠️  {} 存在但无PDF文件", dir.getAbsolutePath());
                }
            } else {
                log.debug("  ❌ {} 不存在", dirPath);
            }
        }
        
        log.warn("❌ 未找到PDF文件");
        return pdfFiles;
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

