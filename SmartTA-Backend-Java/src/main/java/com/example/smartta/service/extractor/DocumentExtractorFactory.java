package com.example.smartta.service.extractor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档提取器工厂
 * 根据文件类型选择合适的提取器
 */
@Slf4j
@Component
public class DocumentExtractorFactory {
    
    private final List<DocumentExtractor> extractors;
    
    public DocumentExtractorFactory(List<DocumentExtractor> extractors) {
        this.extractors = extractors;
        log.info("初始化文档提取器工厂，注册了 {} 个提取器", extractors.size());
    }
    
    /**
     * 根据文件名获取合适的提取器
     * 
     * @param fileName 文件名
     * @return 对应的提取器，如果没有找到则返回null
     */
    public DocumentExtractor getExtractor(String fileName) {
        for (DocumentExtractor extractor : extractors) {
            if (extractor.supports(fileName)) {
                log.debug("为文件 {} 选择提取器: {}", fileName, extractor.getClass().getSimpleName());
                return extractor;
            }
        }
        log.warn("没有找到支持文件类型的提取器: {}", fileName);
        return null;
    }
    
    /**
     * 检查是否支持指定的文件类型
     * 
     * @param fileName 文件名
     * @return 如果支持返回true，否则返回false
     */
    public boolean isSupported(String fileName) {
        return getExtractor(fileName) != null;
    }
    
    /**
     * 获取所有支持的文件扩展名
     * 
     * @return 支持的文件扩展名列表
     */
    public List<String> getSupportedExtensions() {
        return List.of(".pdf", ".docx", ".txt", ".pptx");
    }
}

