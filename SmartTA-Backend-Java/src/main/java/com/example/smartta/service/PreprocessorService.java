package com.example.smartta.service;

import com.example.smartta.config.SmartTAProperties;
import com.example.smartta.model.DocumentChunk;
import com.example.smartta.service.extractor.DocumentExtractor;
import com.example.smartta.service.extractor.DocumentExtractorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 文档预处理服务
 * 支持多种文件格式（PDF、DOCX、TXT、PPTX等）的处理和向量化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreprocessorService {

    private final SmartTAProperties properties;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final DocumentExtractorFactory extractorFactory;

    /**
     * 预处理文档文件并增量更新向量数据库
     * 支持多种文件格式：PDF、DOCX、TXT、PPTX
     *
     * @param file      上传的文件
     * @param directory 目录路径
     * @param docFiles  文档文件路径列表
     * @return 处理结果
     */
    public Map<String, Object> preprocessDocuments(MultipartFile file, String directory, List<String> docFiles) {
        List<DocumentChunk> allDocs = new ArrayList<>();

        try {
            // 单文件模式（上传）
            if (file != null && !file.isEmpty()) {
                String fileName = file.getOriginalFilename();
                log.info("处理上传的文档文件：{}", fileName);
                
                // 检查文件类型是否支持
                if (!extractorFactory.isSupported(fileName)) {
                    String supportedFormats = String.join("、", extractorFactory.getSupportedExtensions());
                    return createErrorResult("不支持的文件类型。支持的格式：" + supportedFormats);
                }

                // 使用绝对路径，防止相对路径在不同工作目录下失效
                String dataDir = properties.getData().getDataDir();
                // 若为相对路径，则转换为绝对路径（本地开发或部署都一致可靠）
                Path dataDirPath = Paths.get(dataDir).toAbsolutePath();
                Files.createDirectories(dataDirPath);
                Path tempPath = dataDirPath.resolve(fileName);

                log.info("文档将临时保存到：{}", tempPath.toAbsolutePath());
                file.transferTo(tempPath.toFile());

                // 处理文档
                List<DocumentChunk> docs = processDocumentFile(tempPath.toFile(), fileName);
                allDocs.addAll(docs);

                // 删除临时文件
                Files.deleteIfExists(tempPath);
            }
            // 文档文件列表模式（自动重建）
            else if (docFiles != null && !docFiles.isEmpty()) {
                for (String docPath : docFiles) {
                    try {
                        File docFile = new File(docPath);
                        log.info("处理文档：{}", docFile.getName());
                        
                        if (!extractorFactory.isSupported(docFile.getName())) {
                            log.warn("跳过不支持的文件类型：{}", docFile.getName());
                            continue;
                        }
                        
                        List<DocumentChunk> docs = processDocumentFile(docFile, docFile.getName());
                        allDocs.addAll(docs);
                    } catch (Exception e) {
                        String errorMsg = "处理文档失败：" + docPath + "，原因：" + e.getMessage();
                        log.error(errorMsg, e);
                        return createErrorResult(errorMsg);
                    }
                }
            }
            // 目录模式
            else if (directory != null && !directory.isEmpty()) {
                File dir = new File(directory);
                if (!dir.exists()) {
                    return createErrorResult("目录 " + directory + " 不存在。");
                }

                File[] docs = dir.listFiles((d, name) -> extractorFactory.isSupported(name));
                if (docs == null || docs.length == 0) {
                    String supportedFormats = String.join("、", extractorFactory.getSupportedExtensions());
                    return createErrorResult("目录中没有找到支持的文档文件。支持的格式：" + supportedFormats);
                }

                log.info("在目录 {} 中找到 {} 个支持的文档文件", directory, docs.length);
                
                for (File doc : docs) {
                    try {
                        log.info("处理文档：{}", doc.getName());
                        List<DocumentChunk> docChunks = processDocumentFile(doc, doc.getName());
                        allDocs.addAll(docChunks);
                    } catch (Exception e) {
                        String errorMsg = "处理文档失败：" + doc.getName() + "，原因：" + e.getMessage();
                        log.error(errorMsg, e);
                        return createErrorResult(errorMsg);
                    }
                }
            } else {
                return createErrorResult("未提供有效的输入来源");
            }

            if (allDocs.isEmpty()) {
                return createErrorResult("未能从文档中提取到有效文本");
            }

            // 更新或创建向量数据库
            String dbPath = properties.getData().getDbPath();
            Path indexFile = Paths.get(dbPath, "index.pkl");

            if (Files.exists(indexFile)) {
                // 增量更新
                vectorStoreService.addDocuments(allDocs);
                vectorStoreService.saveDatabase();
            } else {
                // 新建数据库
                vectorStoreService.createDatabase(allDocs);
            }

            String message = "向量数据库更新成功，共新增 " + allDocs.size() + " 个文档片段。";
            log.info(message);
            
            return createSuccessResult(message, allDocs.size());

        } catch (Exception e) {
            log.error("预处理文档失败", e);
            return createErrorResult(e.getMessage());
        }
    }
    
    /**
     * 兼容旧API的方法
     * @deprecated 使用 preprocessDocuments 替代
     */
    @Deprecated
    public Map<String, Object> preprocessPdfs(MultipartFile file, String directory, List<String> pdfFiles) {
        return preprocessDocuments(file, directory, pdfFiles);
    }

    /**
     * 处理单个文档文件（支持多种格式）
     */
    private List<DocumentChunk> processDocumentFile(File docFile, String fileName) throws IOException {
        List<DocumentChunk> chunks = new ArrayList<>();
        
        // 获取合适的提取器
        DocumentExtractor extractor = extractorFactory.getExtractor(fileName);
        if (extractor == null) {
            throw new IOException("不支持的文件类型：" + fileName);
        }
        
        // 提取文本内容
        List<DocumentExtractor.PageContent> pages = extractor.extractText(docFile);
        
        // 处理每一页内容
        for (DocumentExtractor.PageContent page : pages) {
            String pageText = page.getContent();
            
            if (pageText != null && !pageText.trim().isEmpty()) {
                // 分块处理
                List<String> textChunks = splitText(pageText);
                
                for (String chunk : textChunks) {
                    // 生成嵌入向量
                    float[] embedding = embeddingService.embed(chunk);
                    
                    DocumentChunk doc = new DocumentChunk();
                    doc.setSource(fileName);
                    doc.setPage(String.valueOf(page.getPageNumber()));
                    doc.setContent(chunk);
                    doc.setEmbedding(embedding);
                    
                    chunks.add(doc);
                }
            }
        }

        log.info("从 {} 提取了 {} 个文档块", fileName, chunks.size());
        return chunks;
    }

    /**
     * 分割文本为块
     */
    private List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();
        int chunkSize = properties.getRag().getChunkSize();
        int chunkOverlap = properties.getRag().getChunkOverlap();

        if (text.length() <= chunkSize) {
            chunks.add(text);
            return chunks;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            start += chunkSize - chunkOverlap;
        }

        return chunks;
    }

    private Map<String, Object> createSuccessResult(String message, int addedDocs) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "成功");
        result.put("message", message);
        result.put("added_docs", addedDocs);
        return result;
    }

    private Map<String, Object> createErrorResult(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "失败");
        result.put("message", message);
        result.put("added_docs", 0);
        return result;
    }
}

