"""
配置管理模块
统一管理应用配置，避免硬编码
"""
import os
from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    """应用配置类"""
    
    # === API配置 ===
    api_title: str = "SmartTA Backend"
    api_version: str = "1.0.0"
    max_request_size: int = 100 * 1024 * 1024  # 100MB
    api_host: str = "0.0.0.0"
    api_port: int = 8000
    
    # === 模型配置 ===
    embedding_model: str = "sentence-transformers/all-mpnet-base-v2"
    llm_model: str = "deepseek-chat"
    llm_temperature: float = 0.6
    llm_max_tokens: int = 1024
    
    # === DeepSeek API配置 ===
    deepseek_api_key: Optional[str] = None
    deepseek_base_url: Optional[str] = None
    
    # === 数据路径配置 ===
    pdf_dir: str = "./data/pdfs"
    db_path: str = "./data/faiss_index"
    data_dir: str = "./data"
    
    # === RAG参数配置 ===
    top_k: int = 3
    chunk_size: int = 1000
    chunk_overlap: int = 200
    
    # === 会话配置 ===
    max_conversation_history: int = 5
    
    # === 超时配置 ===
    request_timeout: int = 120
    connect_timeout: int = 10
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = False
        extra = "ignore"

    def __post_init__(self):
        """配置后处理"""
        # 设置环境变量用于Langchain
        if self.deepseek_api_key:
            os.environ["OPENAI_API_KEY"] = self.deepseek_api_key
        if self.deepseek_base_url:
            os.environ["OPENAI_API_BASE"] = self.deepseek_base_url


# 全局配置实例
settings = Settings()

