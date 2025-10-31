package com.example.smartta.service.extractor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * TXT文件提取器
 * 读取纯文本文件内容
 */
@Slf4j
@Component
public class TxtExtractor implements DocumentExtractor {
    
    private static final int LINES_PER_PAGE = 50; // 每50行视为一页
    
    @Override
    public List<PageContent> extractText(File file) throws IOException {
        List<PageContent> pages = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            
            StringBuilder currentPage = new StringBuilder();
            int pageNumber = 1;
            int lineCount = 0;
            String line;
            
            while ((line = reader.readLine()) != null) {
                currentPage.append(line).append("\n");
                lineCount++;
                
                // 每LINES_PER_PAGE行作为一页
                if (lineCount >= LINES_PER_PAGE) {
                    pages.add(new PageContent(pageNumber, currentPage.toString()));
                    currentPage = new StringBuilder();
                    pageNumber++;
                    lineCount = 0;
                }
            }
            
            // 添加剩余内容
            if (currentPage.length() > 0) {
                pages.add(new PageContent(pageNumber, currentPage.toString()));
            }
        }
        
        log.debug("从TXT文件 {} 提取了 {} 页内容", file.getName(), pages.size());
        return pages;
    }
    
    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".txt");
    }
}

