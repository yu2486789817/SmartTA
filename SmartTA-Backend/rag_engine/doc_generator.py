# rag_engine/doc_generator.py

from typing import Dict, Any
from langchain_openai import ChatOpenAI
from dotenv import load_dotenv

load_dotenv()

def generate_markdown_summary(project_info: Dict[str, Any]) -> str:
    """
    将项目扫描信息转化为 LLM 可理解的 prompt，并生成 Markdown 文档。
    """
    root = project_info.get("root", "Unknown Project")
    file_count = project_info.get("file_count", 0)
    files = project_info.get("files", [])

    # 构造简洁摘要
    summary_parts = [f"项目路径: {root}", f"共包含 {file_count} 个 Java 文件。"]
    for f in files[:10]:  # 限制前10个文件，防止过长
        summary_parts.append(f"\n文件名: {f.get('file')}")
        summary_parts.append(f"类: {', '.join(f.get('classes', [])) or '无'}")
        summary_parts.append(f"方法: {', '.join(f.get('methods', [])) or '无'}")
        if f.get("comments"):
            summary_parts.append("注释摘要:\n- " + "\n- ".join(f.get("comments", [])[:3]))

    # 构建 Prompt
    prompt = (
        "你是一名专业的Java架构师，请根据以下扫描到的项目结构生成一份结构化、"
        "简洁明了的 Markdown 项目文档，包括每个类的功能说明、主要方法、注释摘要，"
        "请不要使用**标记任何文本。文档应包含项目概述、模块结构，"
        "以及总体设计简介。\n\n"
        "输出格式示例：\n"
        "# 项目概述\n## 模块结构\n### 类说明\n..."
        "\n\n以下是扫描数据:\n"
        + "\n".join(summary_parts)
    )

    llm = ChatOpenAI(
        model="deepseek-chat",
        temperature=0.6,
        max_tokens=1024
    )
    # 调用已有 generator 模块（统一 LLM 接口）
    answer = response = llm.invoke(prompt)

    return answer.content
