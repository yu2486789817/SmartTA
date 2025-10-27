"""
模型管理器
使用单例模式管理嵌入模型和向量数据库，避免 repeated loading
支持自动重建数据库
"""
import threading
from typing import Optional
from langchain_community.vectorstores import FAISS
from langchain_huggingface import HuggingFaceEmbeddings
from rag_engine.config import settings
import os
import glob


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
    
    def _find_pdf_files(self):
        """查找可用的PDF文件"""
        # 获取当前工作目录的绝对路径
        current_dir = os.getcwd()
        
        # 计算项目根目录（SmartTA-Backend 的父目录）
        project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        
        pdf_dirs = [
            os.path.join(project_root, "SmartTA", "src", "pdf"),  # 项目根目录下的SmartTA/src/pdf
            os.path.join(project_root, "SmartTA", "pdf"),  # 备选路径
            os.path.join(current_dir, "src", "pdf"),      # SmartTA/src/pdf
            os.path.join(current_dir, "SmartTA", "src", "pdf"),  # 项目根目录的SmartTA/src/pdf
            os.path.join(current_dir, "..", "SmartTA", "src", "pdf"),  # 相对路径 SmartTA/src/pdf
            os.path.join(current_dir, "..", "..", "SmartTA", "src", "pdf"),  # 从SmartTA-Backend向上两级
            "./src/pdf",    # 相对路径
            "./SmartTA/src/pdf",  # 完整的相对路径
            "../SmartTA/src/pdf",  # 从SmartTA-Backend目录
            "../../SmartTA/src/pdf",  # 从SmartTA-Backend向上两级
            "./pdfs",
            "./data"
        ]
        
        print(f"当前目录: {current_dir}")
        print(f"项目根目录: {project_root}")
        print(f"开始搜索PDF文件...")
        
        for dir_path in pdf_dirs:
            abs_dir = os.path.abspath(dir_path)
            if os.path.exists(abs_dir):
                pdf_files = glob.glob(os.path.join(abs_dir, "*.pdf"))
                if pdf_files:
                    print(f"✅ 在 {abs_dir} 找到 {len(pdf_files)} 个PDF文件")
                    return pdf_files
                else:
                    print(f"  ⚠️  {abs_dir} 存在但无PDF文件")
            else:
                print(f"  ❌ {abs_dir} 不存在")
        
        print("❌ 未找到PDF文件")
        return []
    
    def _rebuild_database_from_pdfs(self):
        """从PDF文件重建数据库"""
        print("向量数据库不存在，尝试自动重建...")
        
        # 查找PDF文件
        pdf_files = self._find_pdf_files()
        
        if not pdf_files:
            raise FileNotFoundError(
                f"向量数据库不存在且未找到PDF文件。\n"
                f"请执行以下操作之一：\n"
                f"1. 上传PDF文件到 {settings.pdf_dir}\n"
                f"2. 或调用 /add_pdfs 接口上传PDF\n"
                f"3. 或手动创建数据库"
            )
        
        print(f"找到 {len(pdf_files)} 个PDF文件：")
        for pdf in pdf_files[:5]:  # 只显示前5个
            print(f"  - {os.path.basename(pdf)}")
        if len(pdf_files) > 5:
            print(f"  ... 还有 {len(pdf_files) - 5} 个文件")
        
        print("开始重建数据库...")
        
        # 导入preprocessor模块
        from rag_engine.preprocessor import preprocess_pdfs
        
        # 使用找到的PDF文件列表重建数据库
        result = preprocess_pdfs(pdf_files=pdf_files)
        
        if result.get("status") != "success":
            raise RuntimeError(f"重建数据库失败: {result.get('message')}")
        
        print(f"✅ 数据库重建完成！添加了 {result.get('added_docs', 0)} 个文档块")
    
    def _load_database(self):
        """加载向量数据库，如果不存在则自动重建"""
        if not os.path.exists(settings.db_path):
            print(f"数据库不存在: {settings.db_path}")
            # 尝试自动重建
            try:
                self._rebuild_database_from_pdfs()
            except Exception as e:
                raise FileNotFoundError(
                    f"无法加载或创建数据库: {str(e)}\n"
                    f"数据库路径: {settings.db_path}"
                )
        
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


