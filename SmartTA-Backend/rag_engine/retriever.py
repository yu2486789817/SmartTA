"""
检索模块 - 使用缓存的模型管理器
"""
from rag_engine.model_manager import model_manager
from rag_engine.config import settings


def retrieve_context(query, top_k=None):
    """
    检索上下文，使用缓存的数据库实例（避免重复加载）
    
    Args:
        query: 查询文本
        top_k: 返回的文档数量，默认使用配置值
    
    Returns:
        匹配的文档列表
    """
    if top_k is None:
        top_k = settings.top_k
    
    # 使用缓存的数据库实例
    db = model_manager.get_db()
    docs = db.similarity_search(query, k=top_k)

    # 返回字典列表，每个包含 source、page、content
    return [
        {
            "source": getattr(d, "metadata", {}).get("source", "unknown"),
            "page": getattr(d, "metadata", {}).get("page", "?"),
            "content": d.page_content
        }
        for d in docs
    ]


# 保留旧的load_db函数以兼容旧代码（已弃用）
def load_db():
    """已弃用：请使用model_manager.get_db()"""
    return model_manager.get_db()


if __name__ == "__main__":
    print(retrieve_context("操作系统中的虚拟内存机制"))
