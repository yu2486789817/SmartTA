package com.example.smartta.service.extractor;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 文档提取器接口
 * 定义从不同类型文档中提取文本的通用方法
 */
public interface DocumentExtractor {
    
    /**
     * 从文件中提取文本内容
     * 
     * @param file 要处理的文件
     * @return 提取的文本段落列表，每个元素代表一个逻辑单元（如PDF的一页，DOCX的一段等）
     * @throws IOException 如果文件读取失败
     */
    List<PageContent> extractText(File file) throws IOException;
    
    /**
     * 检查该提取器是否支持指定的文件类型
     * 
     * @param fileName 文件名
     * @return 如果支持返回true，否则返回false
     */
    boolean supports(String fileName);
    
    /**
     * 页面内容类，表示文档的一个逻辑单元
     */
    class PageContent {
        private final int pageNumber;
        private final String content;
        
        public PageContent(int pageNumber, String content) {
            this.pageNumber = pageNumber;
            this.content = content;
        }
        
        public int getPageNumber() {
            return pageNumber;
        }
        
        public String getContent() {
            return content;
        }
    }
}

