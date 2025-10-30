package com.example.smartta.controller;

import com.example.smartta.model.AnswerResponse;
import com.example.smartta.model.QuestionRequest;
import com.example.smartta.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

/**
 * SmartTA主控制器
 * 处理所有REST API端点
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SmartTAController {

    private final RetrieverService retrieverService;
    private final GeneratorService generatorService;
    private final PreprocessorService preprocessorService;
    private final TestGeneratorService testGeneratorService;
    private final DocGeneratorService docGeneratorService;
    private final ModelManager modelManager;

    /**
     * 提问接口 - 使用优化的会话管理器
     */
    @PostMapping("/ask")
    public ResponseEntity<AnswerResponse> ask(@RequestBody QuestionRequest request) {
        try {
            // 生成或使用现有的会话ID
            String sessionId = request.getSessionId() != null ? 
                    request.getSessionId() : UUID.randomUUID().toString();
            
            log.info("处理请求 - Session ID: {}, Question: {}...", 
                    sessionId, 
                    request.getQuestion().length() > 50 ? 
                            request.getQuestion().substring(0, 50) + "..." : 
                            request.getQuestion());
            log.info("收到 contextCode: {}", request.getContextCode());
            // 检索相关上下文
            List<Map<String, String>> retrievedChunks = 
                    retrieverService.retrieveContext(request.getQuestion());

            // 生成答案
            String answer = generatorService.getAnswer(
                    request.getQuestion(),
                    retrievedChunks,
                    request.getContextCode(),
                    sessionId
            );

            log.info("回答生成完成 - Session ID: {}", sessionId);

            return ResponseEntity.ok(new AnswerResponse(answer, sessionId));

        } catch (Exception e) {
            log.error("处理请求失败", e);
            String sessionId = request.getSessionId() != null ? 
                    request.getSessionId() : "unknown";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AnswerResponse("抱歉，处理您的请求时出现错误。", sessionId));
        }
    }

    /**
     * 添加PDF文件到知识库
     */
    @PostMapping("/add_pdfs")
    public ResponseEntity<Map<String, Object>> addPdfs(
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String directory) {
        try {
            Map<String, Object> result;
            
            if (file != null && !file.isEmpty()) {
                log.info("添加PDF文件: {}", file.getOriginalFilename());
                result = preprocessorService.preprocessPdfs(file, null, null);
                
                // 重新加载数据库以反映更改
                modelManager.reloadDatabase();
                log.info("数据库已重新加载");
                
                return ResponseEntity.ok(result);
            } 
            else if (directory != null && !directory.isEmpty()) {
                log.info("添加PDF目录: {}", directory);
                result = preprocessorService.preprocessPdfs(null, directory, null);
                
                // 重新加载数据库以反映更改
                modelManager.reloadDatabase();
                log.info("数据库已重新加载");
                
                return ResponseEntity.ok(result);
            } 
            else {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "必须提供文件或目录路径。");
                return ResponseEntity.badRequest().body(error);
            }
            
        } catch (Exception e) {
            log.error("添加PDF失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "处理PDF文件时出错: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            boolean isReady = modelManager.isInitialized();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", isReady ? "healthy" : "initializing");
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("model_ready", isReady);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("健康检查失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "unhealthy");
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    /**
     * 生成项目文档
     */
    @PostMapping("/generate_docs")
    public ResponseEntity<Map<String, Object>> generateDocs(
            @RequestBody Map<String, Object> projectInfo) {
        try {
            log.info("生成文档请求 - 文件数: {}", projectInfo.get("file_count"));
            String markdown = docGeneratorService.generateMarkdownSummary(projectInfo);
            
            Map<String, Object> response = new HashMap<>();
            response.put("markdown", markdown);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("生成文档失败", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 生成单元测试
     */
    @PostMapping("/generate_test")
    public ResponseEntity<Map<String, Object>> generateTest(
            @RequestBody Map<String, String> request) {
        try {
            String requirement = request.getOrDefault("requirement", "");
            String contextCode = request.getOrDefault("context_code", "");
            String className = request.getOrDefault("class_name", "");
            String methodName = request.getOrDefault("method_name", "");

            if (requirement.isEmpty() || contextCode.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "测试需求和代码上下文不能为空");
                return ResponseEntity.badRequest().body(error);
            }

            log.info("生成单元测试 - Class: {}, Method: {}", className, methodName);
            String testCode = testGeneratorService.generateUnitTest(
                    requirement, contextCode, className, methodName);

            Map<String, Object> response = new HashMap<>();
            response.put("test_code", testCode);
            response.put("status", "success");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("生成测试失败", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "生成测试失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

