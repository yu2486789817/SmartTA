package com.example.smartta.service;

import com.example.smartta.config.SmartTAProperties;
import com.example.smartta.model.DocumentChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * PDF预处理服务
 * 使用缓存的模型管理器处理PDF文件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreprocessorService {

    private final SmartTAProperties properties;
    private final ModelManager modelManager;

    /**
     * 预处理PDF文件并增量更新向量数据库
     *
     * @param file      上传的文件
     * @param directory 目录路径
     * @param pdfFiles  PDF文件路径列表
     * @return 处理结果
     */
    public Map<String, Object> preprocessPdfs(MultipartFile file, String directory, List<String> pdfFiles) {
        List<DocumentChunk> allDocs = new ArrayList<>();

        try {
            // 单文件模式（上传）
            if (file != null && !file.isEmpty()) {
                log.info("处理上传的PDF文件: {}", file.getOriginalFilename());
                
                // 保存临时文件
                String dataDir = properties.getData().getDataDir();
                Path tempPath = Paths.get(dataDir, file.getOriginalFilename());
                Files.createDirectories(tempPath.getParent());
                file.transferTo(tempPath.toFile());

                // 处理PDF
                List<DocumentChunk> docs = processPdfFile(tempPath.toFile(), file.getOriginalFilename());
                allDocs.addAll(docs);

                // 删除临时文件
                Files.deleteIfExists(tempPath);
            }
            // PDF文件列表模式（自动重建）
            else if (pdfFiles != null && !pdfFiles.isEmpty()) {
                for (String pdfPath : pdfFiles) {
                    try {
                        File pdfFile = new File(pdfPath);
                        log.info("处理: {}", pdfFile.getName());
                        List<DocumentChunk> docs = processPdfFile(pdfFile, pdfFile.getName());
                        allDocs.addAll(docs);
                    } catch (Exception e) {
                        String errorMsg = "Failed to process " + pdfPath + ": " + e.getMessage();
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

                File[] pdfs = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));
                if (pdfs == null || pdfs.length == 0) {
                    return createErrorResult("No PDF files found in " + directory);
                }

                for (File pdf : pdfs) {
                    try {
                        log.info("处理: {}", pdf.getName());
                        List<DocumentChunk> docs = processPdfFile(pdf, pdf.getName());
                        allDocs.addAll(docs);
                    } catch (Exception e) {
                        String errorMsg = "Failed to process " + pdf.getName() + ": " + e.getMessage();
                        log.error(errorMsg, e);
                        return createErrorResult(errorMsg);
                    }
                }
            } else {
                return createErrorResult("No valid input provided");
            }

            if (allDocs.isEmpty()) {
                return createErrorResult("No text extracted from PDFs");
            }

            // 更新或创建向量数据库
            String dbPath = properties.getData().getDbPath();
            Path indexFile = Paths.get(dbPath, "index.pkl");

            if (Files.exists(indexFile)) {
                // 增量更新
                modelManager.getVectorStore().addDocuments(allDocs);
                modelManager.getVectorStore().saveDatabase();
            } else {
                // 新建数据库
                modelManager.getVectorStore().createDatabase(allDocs);
            }

            String message = "Database updated successfully. Added " + allDocs.size() + " new document chunks.";
            log.info(message);
            
            return createSuccessResult(message, allDocs.size());

        } catch (Exception e) {
            log.error("预处理PDF失败", e);
            return createErrorResult(e.getMessage());
        }
    }

    /**
     * 处理单个PDF文件
     */
    private List<DocumentChunk> processPdfFile(File pdfFile, String fileName) throws IOException {
        List<DocumentChunk> chunks = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();

            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document);

                if (pageText != null && !pageText.trim().isEmpty()) {
                    // 分块处理
                    List<String> textChunks = splitText(pageText);
                    
                    for (String chunk : textChunks) {
                        // 生成嵌入向量
                        float[] embedding = modelManager.getEmbeddingService().embed(chunk);
                        
                        DocumentChunk doc = new DocumentChunk();
                        doc.setSource(fileName);
                        doc.setPage(String.valueOf(page));
                        doc.setContent(chunk);
                        doc.setEmbedding(embedding);
                        
                        chunks.add(doc);
                    }
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
        result.put("status", "success");
        result.put("message", message);
        result.put("added_docs", addedDocs);
        return result;
    }

    private Map<String, Object> createErrorResult(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "error");
        result.put("message", message);
        result.put("added_docs", 0);
        return result;
    }
}

