"""
生成器模块 - 使用缓存的LLM和会话管理器
"""
import os
from functools import lru_cache
from langchain_openai import ChatOpenAI
from dotenv import load_dotenv
from rag_engine.config import settings
from rag_engine.conversation_manager import conversation_manager

# 加载环境变量
load_dotenv()

# === DeepSeek API Configuration ===
os.environ["OPENAI_API_KEY"] = os.getenv("DEEPSEEK_API_KEY")
os.environ["OPENAI_API_BASE"] = os.getenv("DEEPSEEK_BASE_URL")


@lru_cache(maxsize=1)
def get_llm():
    """
    获取缓存的LLM实例（使用LRU缓存避免重复创建）
    
    Returns:
        ChatOpenAI实例
    """
    return ChatOpenAI(
        model=settings.llm_model,
        temperature=settings.llm_temperature,
        max_tokens=settings.llm_max_tokens
    )


def get_answer(query: str, retrieved_chunks: list, context_code: str = "", session_id: str = "default") -> str:
    """
    Generate an answer using the DeepSeek model.
    Includes session-based conversation history.
    
    Args:
        query: 用户问题
        retrieved_chunks: 检索到的文档块
        context_code: 代码上下文
        session_id: 会话ID
    
    Returns:
        生成的答案
    """
    # 使用缓存的LLM实例
    llm = get_llm()

    # 使用会话管理器获取历史
    history_text = conversation_manager.format_history(session_id)

    # Combine course material text
    context_text = "\n\n".join([
        f"[{c['source']}, p.{c['page']}] {c['content']}" for c in retrieved_chunks
    ])

    prompt = f"""
你是一名智能助教，负责回答学生关于课程内容的问题。
你可以结合一般编程知识与课程资料作答。切记输出回答时不要用**标记任何文本。

请根据以下内容生成答案：
1. 如果课程资料中的内容与问题高度相关，请明确引用出处（例如：“见《Lecture 3 - Memory Management》第 12 页”）。
2. 如果课程资料与问题不直接相关，请说明“本回答基于一般知识，未引用课程资料”。

---
课程资料:
{context_text}

历史对话:
{history_text if history_text else "无"}

代码内容:
{context_code}

学生的问题:
{query}
---

"""

    # 调用LLM生成答案
    response = llm.invoke(prompt)

    # 使用会话管理器更新历史
    conversation_manager.append(session_id, query, response.content)

    return response.content