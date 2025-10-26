# generator.py
import os
from langchain_openai import ChatOpenAI
from dotenv import load_dotenv
from collections import defaultdict, deque

load_dotenv()

# === DeepSeek API Configuration ===
os.environ["OPENAI_API_KEY"] = os.getenv("DEEPSEEK_API_KEY")
os.environ["OPENAI_API_BASE"] = os.getenv("DEEPSEEK_BASE_URL")

# Global conversation history
conversation_history = defaultdict(lambda: deque(maxlen=5))

def get_answer(query: str, retrieved_chunks: list, context_code: str = "", session_id: str = "default") -> str:
    """
    Generate an answer using the DeepSeek model.
    Includes session-based conversation history.
    """
    llm = ChatOpenAI(
        model="deepseek-chat",
        temperature=0.6,
        max_tokens=1024
    )

    # Retrieve session-specific history
    history = conversation_history[session_id]
    history_text = "\n".join([
        f"User: {q}\nSmartTA: {a}" for q, a in history
    ])

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

    # Generate response
    response = llm.invoke(prompt)

    # Update session history
    conversation_history[session_id].append((query, response.content))

    return response.content