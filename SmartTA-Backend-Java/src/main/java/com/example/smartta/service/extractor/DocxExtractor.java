package com.example.smartta.service.extractor;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * DOCX文件提取器
 * 使用Apache POI提取Word文档文本内容
 */
@Slf4j
@Component
public class DocxExtractor implements DocumentExtractor {
    
    private static final int PARAGRAPHS_PER_PAGE = 10; // 每10个段落视为一页
    
    @Override
    public List<PageContent> extractText(File file) throws IOException {
        List<PageContent> pages = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {
            
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            StringBuilder currentPage = new StringBuilder();
            int pageNumber = 1;
            int paragraphCount = 0;
            
            for (XWPFParagraph paragraph : paragraphs) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    currentPage.append(text).append("\n");
                    paragraphCount++;
                    
                    // 每PARAGRAPHS_PER_PAGE个段落作为一页
                    if (paragraphCount >= PARAGRAPHS_PER_PAGE) {
                        pages.add(new PageContent(pageNumber, currentPage.toString()));
                        currentPage = new StringBuilder();
                        pageNumber++;
                        paragraphCount = 0;
                    }
                }
            }
            
            // 添加剩余内容
            if (currentPage.length() > 0) {
                pages.add(new PageContent(pageNumber, currentPage.toString()));
            }
        }
        
        log.debug("从DOCX文件 {} 提取了 {} 页内容", file.getName(), pages.size());
        return pages;
    }
    
    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".docx");
    }
}

