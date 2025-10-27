"""
模型管理器
使用单例模式管理嵌入模型和向量数据库，避免 repeated loading
"""
import threading
from typing import Optional
from langchain_community.vectorstores import FAISS
from langchain_huggingface import HuggingFaceEmbeddings
from rag_engine.config import settings
import os


class ModelManager:
    """
    单例模型管理器
    负责管理和复用嵌入模型和向量数据库
    """
    
    _instance: Optional['ModelManager'] = None
    _lock = threading.Lock()
    
    def __new__(cls):
        """线程安全的单例实现"""
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
                    cls._instance._initialized = False
        return cls._instance
    
    def __init__(self):
        """初始化管理器"""
        if not self._initialized:
            self._embeddings: Optional[HuggingFaceEmbeddings] = None
            self._db: Optional[FAISS] = None
            self._init_lock = threading.Lock()
            self._initialized = True
    
    def initialize(self):
        """懒加载模型和数据库"""
        with self._init_lock:
            if self._embeddings is None:
                self._load_embeddings()
            if self._db is None:
                self._load_database()
    
    def _load_embeddings(self):
        """加载嵌入模型"""
        print(f"加载嵌入模型: {settings.embedding_model}")
        self._embeddings = HuggingFaceEmbeddings(
            model_name=settings.embedding_model
        )
    
    def _load_database(self):
        """加载向量数据库"""
        if not os.path.exists(settings.db_path):
            raise FileNotFoundError(f"数据库路径不存在: {settings.db_path}")
        
        print(f"加载向量数据库: {settings.db_path}")
        self._db = FAISS.load_local(
            settings.db_path,
            self._embeddings,
            allow_dangerous_deserialization=True
        )
    
    def get_db(self) -> FAISS:
        """获取向量数据库实例"""
        if self._db is None:
            self.initialize()
        return self._db
    
    def get_embeddings(self) -> HuggingFaceEmbeddings:
        """获取嵌入模型实例"""
        if self._embeddings is None:
            self.initialize()
        return self._embeddings
    
    def reload_database(self):
        """重新加载数据库（用于更新后刷新）"""
        with self._init_lock:
            self._db = None
            self._load_database()
    
    def is_initialized(self) -> bool:
        """检查是否已初始化"""
        return self._db is not None and self._embeddings is not None


# 全局模型管理器实例
model_manager = ModelManager()


