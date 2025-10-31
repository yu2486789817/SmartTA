package com.example.smartta.service;

import com.example.smartta.config.SmartTAProperties;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;

/**
 * Git提交消息生成服务
 * 分析git diff并生成规范的提交消息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitCommitMessageService {

    private final SmartTAProperties properties;
    private ChatLanguageModel llm;

    @PostConstruct
    public void init() {
        log.info("初始化Git提交消息生成服务");
        
        String apiKey = properties.getDeepseek().getApiKey();
        String baseUrl = properties.getDeepseek().getBaseUrl();
        
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("DEEPSEEK_API_KEY 未配置");
        }
        
        this.llm = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(properties.getModel().getLlm().getModelName())
                .temperature(0.3) // 降低温度以获得更一致的输出
                .maxTokens(512) // 提交消息不需要太长
                .timeout(Duration.ofSeconds(properties.getTimeout().getRequestTimeout()))
                .build();
        
        log.info("Git提交消息生成服务初始化完成");
    }

    /**
     * 根据git diff生成提交消息
     *
     * @param gitDiff git diff内容
     * @return 生成的提交消息
     */
    public String generateCommitMessage(String gitDiff) {
        log.info("开始生成Git提交消息，差异大小：{} 字符", gitDiff.length());

        if (gitDiff == null || gitDiff.trim().isEmpty()) {
            log.warn("Git diff为空");
            return "chore: 更新文件";
        }

        // 构建提示词
        String prompt = buildPrompt(gitDiff);

        // 调用LLM生成提交消息
        String commitMessage = llm.generate(prompt);

        log.info("提交消息生成完成");
        return commitMessage.trim();
    }

    /**
     * 构建LLM提示词
     */
    private String buildPrompt(String gitDiff) {
        return String.format("""
                你是一个Git提交消息生成助手。请根据提供的git diff内容，生成一个符合Conventional Commits规范的提交消息。
                
                **规范说明：**
                提交消息格式应为：`<type>(<scope>): <subject>`
                
                - **type**: 提交类型，必须是以下之一：
                  - feat: 新功能
                  - fix: 修复bug
                  - docs: 文档变更
                  - style: 代码格式调整（不影响代码功能）
                  - refactor: 重构（既不是新功能也不是bug修复）
                  - perf: 性能优化
                  - test: 添加或修改测试
                  - build: 构建系统或外部依赖变更
                  - ci: CI配置文件和脚本变更
                  - chore: 其他不修改src或test文件的变更
                  - revert: 回滚之前的提交
                
                - **scope**: （可选）影响范围，如模块名、组件名等
                - **subject**: 简短描述，不超过50个字符，使用中文
                
                **要求：**
                1. 仔细分析diff内容，识别变更的主要意图
                2. 选择最合适的type
                3. 如果有明确的模块或组件，添加scope
                4. subject要简洁明了，准确描述变更内容
                5. 只输出提交消息本身，不要有任何额外的解释或标记
                6. 如果有多个重要变更，只关注最主要的那个
                
                **示例：**
                - feat(auth): 添加用户登录功能
                - fix(api): 修复空指针异常
                - docs: 更新README文档
                - refactor(service): 优化数据查询逻辑
                - style: 统一代码格式
                
                ---
                **Git Diff内容：**
                ```
                %s
                ```
                
                请直接输出提交消息（不要包含代码块标记）：
                """, gitDiff);
    }
}

