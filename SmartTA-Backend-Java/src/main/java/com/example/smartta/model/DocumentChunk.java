package com.example.smartta.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 文档块模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String source;
    private String page;
    private String content;
    private float[] embedding;
}

