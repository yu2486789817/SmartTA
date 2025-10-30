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
 * 测试生成器服务
 * 根据用户需求和Java代码生成单元测试
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestGeneratorService {

    private final SmartTAProperties properties;
    private ChatLanguageModel llm;

    @PostConstruct
    public void init() {
        log.info("初始化测试生成器LLM模型");
        
        String apiKey = properties.getDeepseek().getApiKey();
        String baseUrl = properties.getDeepseek().getBaseUrl();
        
        this.llm = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName("deepseek-chat")
                .temperature(0.3) // 降低温度以获得更确定的测试代码
                .maxTokens(2048)
                .timeout(Duration.ofSeconds(properties.getTimeout().getRequestTimeout()))
                .build();
    }

    /**
     * 生成单元测试
     *
     * @param requirement 测试需求描述
     * @param contextCode 代码上下文
     * @param className   类名
     * @param methodName  方法名
     * @return 生成的测试代码
     */
    public String generateUnitTest(String requirement, String contextCode, 
                                   String className, String methodName) {
        log.info("生成单元测试 - Class: {}, Method: {}", className, methodName);

        String prompt = String.format("""
                你是一个专业的Java开发工程师，专门编写高质量的JUnit单元测试。
                
                任务：根据用户的需求描述和提供的Java代码，生成针对性的JUnit 单元测试。
                
                代码上下文：
                类名：%s
                方法名：%s
                
                用户测试需求：
                %s
                
                Java源代码：
                ```java
                %s
                ```
                
                请生成完整的JUnit 5测试类，要求：
                
                1.只输出Java测试代码，不要任何解释或markdown标记
                
                2.使用JUnit 5 (Jupiter)
                
                3.包含必要的import语句
                
                4.测试类名格式：%sTest
                
                5.针对用户描述的具体场景编写测试方法
                
                6.使用有意义的测试方法名称，描述测试场景
                
                7.包含必要的断言(assertions)
                
                8.处理边界条件和异常情况
                
                9.使用适当的测试注解(@Test, @BeforeEach等)
                
                10.如果需要，使用Mockito进行mock（但不要过度使用）
                
                生成的测试代码：
                """,
                className, methodName, requirement, contextCode, className
        );

        String testCode = llm.generate(prompt);
        log.info("单元测试生成完成");
        return testCode;
    }
}

