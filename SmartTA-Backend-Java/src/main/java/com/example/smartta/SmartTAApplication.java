package com.example.smartta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * SmartTA Backend Application
 * 使用优化的模型管理器和会话管理器
 * 
 * 启动命令：mvn spring-boot:run
 * 或者：java -jar target/smartta-backend-1.0.0.jar
 */
@SpringBootApplication
@EnableConfigurationProperties
public class SmartTAApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartTAApplication.class, args);
    }
}

