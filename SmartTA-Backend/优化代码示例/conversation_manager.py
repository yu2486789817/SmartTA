"""
会话管理器
线程安全的会话历史管理
"""
import threading
from collections import deque
from typing import Dict, Optional
from config import settings


class ConversationManager:
    """
    线程安全的会话管理器
    管理多用户的对话历史
    """
    
    def __init__(self):
        self._histories: Dict[str, deque] = {}
        self._lock = threading.Lock()
    
    def append(self, session_id: str, query: str, answer: str):
        """
        添加对话历史
        
        Args:
            session_id: 会话ID
            query: 用户问题
            answer: 回答
        """
        with self._lock:
            if session_id not in self._histories:
                self._histories[session_id] = deque(
                    maxlen=settings.max_conversation_history
                )
            self._histories[session_id].append((query, answer))
    
    def get(self, session_id: str) -> deque:
        """
        获取会话历史
        
        Args:
            session_id: 会话ID
        
        Returns:
            对话历史deque
        """
        with self._lock:
            return self._histories.get(session_id, deque())
    
    def clear(self, session_id: str):
        """清除指定会话的历史"""
        with self._lock:
            if session_id in self._histories:
                self._histories[session_id].clear()
    
    def format_history(self, session_id: str) -> str:
        """
        格式化对话历史为文本
        
        Args:
            session_id: 会话ID
        
        Returns:
            格式化的对话文本
        """
        history = self.get(session_id)
        if not history:
            return "无"
        
        return "\n".join([
            f"User: {q}\nSmartTA: {a}" for q, a in history
        ])
    
    def cleanup_old_sessions(self, max_sessions: int = 100):
        """
        清理旧会话（防止内存泄漏）
        
        Args:
            max_sessions: 最大保留会话数
        """
        with self._lock:
            if len(self._histories) > max_sessions:
                # 保留最近的会话
                sorted_sessions = sorted(
                    self._histories.items(),
                    key=lambda x: len(x[1])
                )[-max_sessions:]
                self._histories = dict(sorted_sessions)


# 全局会话管理器
conversation_manager = ConversationManager()

