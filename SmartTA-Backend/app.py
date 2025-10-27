import uuid
import os
from io import BytesIO
from fastapi import FastAPI, Request
from pydantic import BaseModel
from collections import defaultdict, deque
from rag_engine import retriever, generator
from fastapi import UploadFile, File, Form
from rag_engine.preprocessor import preprocess_pdfs
from rag_engine.doc_generator import generate_markdown_summary

"""
启动命令：uvicorn app:app --reload --host 0.0.0.0 --port 8000
"""

app = FastAPI(title="SmartTA Backend" , max_request_size=100 * 1024 * 1024)

PDF_DIR = os.getenv("PDF_DIR", "./data/pdfs")
# Global conversation history
conversation_history = defaultdict(lambda: deque(maxlen=5))

class QuestionRequest(BaseModel):
    question: str
    context_code: str = ""
    session_id: str = None  # Allow None for auto-generation

class AnswerResponse(BaseModel):
    answer: str
    session_id: str  # Include session_id in the response

@app.post("/ask", response_model=AnswerResponse)
def ask(qa: QuestionRequest):
    # Auto-generate session_id if not provided
    session_id = qa.session_id or str(uuid.uuid4())
    history = conversation_history[session_id]

    # Retrieve course materials
    retrieved_chunks = retriever.retrieve_context(qa.question)

    # Generate answer
    answer = generator.get_answer(
        query=qa.question,
        retrieved_chunks=retrieved_chunks,
        context_code=qa.context_code
    )

    # Update conversation history
    history.append((qa.question, answer))

    # Return the answer and session_id
    return {"answer": answer, "session_id": session_id}

@app.post("/add_pdfs")
async def add_pdfs(
    file: UploadFile = File(None),
    directory: str = Form(None)
):
    """
    添加新的 PDF 文件到知识库。
    - 如果提供了 `file`，直接处理上传的文件（内存中）。
    - 如果提供了 `directory`，处理目录下的所有 PDF 文件。
    """
    if file:
        # 直接处理内存中的文件
        file_content = BytesIO(await file.read())
        preprocess_pdfs(file_content=file_content, file_name=file.filename)
        return {"message": f"文件 {file.filename} 已处理并添加到知识库。"}

    elif directory:
        # 处理指定目录下的所有 PDF 文件
        if not os.path.exists(directory):
            return {"error": f"目录 {directory} 不存在。"}
        preprocess_pdfs(directory=directory)
        return {"message": f"目录 {directory} 下的所有 PDF 文件已添加到知识库。"}

    else:
        return {"error": "必须提供文件或目录路径。"}

@app.post("/generate_docs")
async def generate_docs(request: Request):
    """
    接收 IntelliJ 插件上传的项目结构数据，调用 LLM 生成 Markdown 文档。
    """
    try:
        data = await request.json()
        markdown = generate_markdown_summary(data)
        return {"markdown": markdown}
    except Exception as e:
        return {"error": str(e)}