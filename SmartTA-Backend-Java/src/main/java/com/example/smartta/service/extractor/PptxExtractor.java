package com.example.smartta.service.extractor;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PPTX文件提取器
 * 使用Apache POI提取PowerPoint演示文稿文本内容
 */
@Slf4j
@Component
public class PptxExtractor implements DocumentExtractor {
    
    @Override
    public List<PageContent> extractText(File file) throws IOException {
        List<PageContent> pages = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(file);
             XMLSlideShow ppt = new XMLSlideShow(fis)) {
            
            List<XSLFSlide> slides = ppt.getSlides();
            
            for (int i = 0; i < slides.size(); i++) {
                XSLFSlide slide = slides.get(i);
                StringBuilder slideText = new StringBuilder();
                
                // 提取幻灯片中的所有文本
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape) {
                        XSLFTextShape textShape = (XSLFTextShape) shape;
                        String text = textShape.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            slideText.append(text).append("\n");
                        }
                    }
                }
                
                if (slideText.length() > 0) {
                    pages.add(new PageContent(i + 1, slideText.toString()));
                }
            }
        }
        
        log.debug("从PPTX文件 {} 提取了 {} 页内容", file.getName(), pages.size());
        return pages;
    }
    
    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".pptx");
    }
}

