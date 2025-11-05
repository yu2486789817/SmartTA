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
    private final GitCommitMessageService gitCommitMessageService;
    private final ModelManager modelManager;

    /**
     * 提问接口 - 使用优化的会话管理器
     */
    @PostMapping("/ask")
    public ResponseEntity<AnswerResponse> ask(@RequestBody QuestionRequest request) {
        try {
            // 生成或使用现有的会话ID
            // 如果前端未提供会话ID，则使用一个默认的会话ID，以便累积历史对话
            // 注意：这会导致所有未提供sessionId的请求共享同一个历史。
            // 在生产环境中，通常需要前端提供唯一的sessionId或通过其他方式管理用户会话。
            String sessionId = request.getSessionId() != null ? 
                    request.getSessionId() : "default-smartta-session";
            
            log.info("处理请求 - 会话ID: {}, 问题: {}...", 
                    sessionId, 
                    request.getQuestion().length() > 50 ? 
                            request.getQuestion().substring(0, 50) + "..." : 
                            request.getQuestion());

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

            log.info("回答生成完成 - 会话ID: {}", sessionId);

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
     * 添加文档文件到知识库（支持PDF、DOCX、TXT、PPTX等）
     */
    @PostMapping("/add_documents")
    public ResponseEntity<Map<String, Object>> addDocuments(
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String directory) {
        try {
            Map<String, Object> result;
            
            if (file != null && !file.isEmpty()) {
                log.info("添加文档文件: {}", file.getOriginalFilename());
                result = preprocessorService.preprocessDocuments(file, null, null);
                
                // 重新加载数据库以反映更改
                modelManager.reloadDatabase();
                log.info("数据库已重新加载");
                
                return ResponseEntity.ok(result);
            } 
            else if (directory != null && !directory.isEmpty()) {
                log.info("添加文档目录: {}", directory);
                result = preprocessorService.preprocessDocuments(null, directory, null);
                
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
            log.error("添加文档失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "处理文档文件时出错：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 添加PDF文件到知识库（保留以兼容旧版本）
     * @deprecated 使用 /add_documents 替代
     */
    @Deprecated
    @PostMapping("/add_pdfs")
    public ResponseEntity<Map<String, Object>> addPdfs(
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String directory) {
        return addDocuments(file, directory);
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            boolean isReady = modelManager.isInitialized();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", isReady ? "健康" : "初始化中");
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("model_ready", isReady);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("健康检查失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "不可用");
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

            log.info("生成单元测试 - 类名: {}, 方法名: {}", className, methodName);
            String testCode = testGeneratorService.generateUnitTest(
                    requirement, contextCode, className, methodName);

            Map<String, Object> response = new HashMap<>();
            response.put("test_code", testCode);
            response.put("status", "成功");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("生成测试失败", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "生成测试失败：" + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 生成Git提交消息
     */
    @PostMapping("/generate_commit_message")
    public ResponseEntity<Map<String, Object>> generateCommitMessage(
            @RequestBody Map<String, String> request) {
        try {
            String gitDiff = request.getOrDefault("git_diff", "");

            if (gitDiff.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Git diff内容不能为空");
                return ResponseEntity.badRequest().body(error);
            }

            log.info("生成Git提交消息 - 差异大小: {} 字符", gitDiff.length());
            String commitMessage = gitCommitMessageService.generateCommitMessage(gitDiff);

            Map<String, Object> response = new HashMap<>();
            response.put("commit_message", commitMessage);
            response.put("status", "成功");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("生成提交消息失败", e);

            Map<String, Object> error = new HashMap<>();
            error.put("error", "生成提交消息失败：" + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
