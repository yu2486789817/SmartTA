package com.example.smartta.service;

import com.example.smartta.config.SmartTAProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 线程安全的会话管理器
 * 管理多用户的对话历史
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationManager {

    private final SmartTAProperties properties;
    private final Map<String, Deque<ConversationPair>> histories = new ConcurrentHashMap<>();

    /**
     * 添加对话历史
     *
     * @param sessionId 会话ID
     * @param query     用户问题
     * @param answer    回答
     */
    public void append(String sessionId, String query, String answer) {
        histories.computeIfAbsent(sessionId, k -> {
            ArrayDeque<ConversationPair> deque = new ArrayDeque<>();
            return deque;
        });

        Deque<ConversationPair> history = histories.get(sessionId);
        
        synchronized (history) {
            history.addLast(new ConversationPair(query, answer));
            
            // 限制历史记录长度
            int maxHistory = properties.getSession().getMaxConversationHistory();
            while (history.size() > maxHistory) {
                history.removeFirst();
            }
        }
    }

    /**
     * 获取会话历史
     *
     * @param sessionId 会话ID
     * @return 对话历史
     */
    public Deque<ConversationPair> get(String sessionId) {
        return histories.getOrDefault(sessionId, new ArrayDeque<>());
    }

    /**
     * 清除指定会话的历史
     *
     * @param sessionId 会话ID
     */
    public void clear(String sessionId) {
        Deque<ConversationPair> history = histories.get(sessionId);
        if (history != null) {
            synchronized (history) {
                history.clear();
            }
        }
    }

    /**
     * 格式化对话历史为文本
     *
     * @param sessionId 会话ID
     * @return 格式化的对话文本
     */
    public String formatHistory(String sessionId) {
        Deque<ConversationPair> history = get(sessionId);
        
        if (history.isEmpty()) {
            return "无";
        }

        StringBuilder sb = new StringBuilder();
        synchronized (history) {
            for (ConversationPair pair : history) {
                sb.append("User: ").append(pair.getQuery()).append("\n");
                sb.append("SmartTA: ").append(pair.getAnswer()).append("\n");
            }
        }
        
        return sb.toString().trim();
    }

    /**
     * 清理旧会话（防止内存泄漏）
     *
     * @param maxSessions 最大保留会话数
     */
    public void cleanupOldSessions(int maxSessions) {
        if (histories.size() > maxSessions) {
            log.info("清理旧会话，当前会话数: {}", histories.size());
            
            // 简单策略：移除最早的会话
            int toRemove = histories.size() - maxSessions;
            histories.keySet().stream()
                    .limit(toRemove)
                    .forEach(histories::remove);
            
            log.info("清理完成，剩余会话数: {}", histories.size());
        }
    }

    /**
     * 对话对
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ConversationPair {
        private String query;
        private String answer;
    }
}

