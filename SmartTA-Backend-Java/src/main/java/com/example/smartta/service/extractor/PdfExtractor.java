package com.example.smartta.service.extractor;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF文件提取器
 * 使用Apache PDFBox提取PDF文本内容
 */
@Slf4j
@Component
public class PdfExtractor implements DocumentExtractor {
    
    @Override
    public List<PageContent> extractText(File file) throws IOException {
        List<PageContent> pages = new ArrayList<>();
        
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();
            
            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document);
                
                if (pageText != null && !pageText.trim().isEmpty()) {
                    pages.add(new PageContent(page, pageText));
                }
            }
        }
        
        log.debug("从PDF文件 {} 提取了 {} 页内容", file.getName(), pages.size());
        return pages;
    }
    
    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".pdf");
    }
}

