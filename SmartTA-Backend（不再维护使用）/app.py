"""
SmartTA Backend API
使用优化的模型管理器和会话管理器
"""
import uuid
import os
from datetime import datetime
from io import BytesIO
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from rag_engine import retriever, generator, test_generator
from fastapi import UploadFile, File, Form
from rag_engine.preprocessor import preprocess_pdfs
from rag_engine.doc_generator import generate_markdown_summary
from rag_engine.config import settings
from rag_engine.exceptions import SmartTAException
from rag_engine.conversation_manager import conversation_manager
from rag_engine.model_manager import model_manager
import logging

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler('smartta.log', encoding='utf-8')
    ]
)
logger = logging.getLogger(__name__)

"""
启动命令：uvicorn app:app --reload --host 0.0.0.0 --port 8000
"""

app = FastAPI(
    title=settings.api_title,
    max_request_size=settings.max_request_size
)

PDF_DIR = os.getenv("PDF_DIR", "./data/pdfs")

class QuestionRequest(BaseModel):
    question: str
    context_code: str = ""
    session_id: str = None  # 可选的会话ID

class AnswerResponse(BaseModel):
    answer: str
    session_id: str  # 会话ID

@app.post("/ask", response_model=AnswerResponse)
def ask(qa: QuestionRequest):
    """提问接口 - 使用优化的会话管理器"""
    try:
        # 生成或使用现有的会话ID
        session_id = qa.session_id or str(uuid.uuid4())
        logger.info(f"处理请求 - Session ID: {session_id}, Question: {qa.question[:50]}...")

        # 检索相关上下文
        retrieved_chunks = retriever.retrieve_context(qa.question)

        # 生成答案
        answer = generator.get_answer(
            query=qa.question,
            retrieved_chunks=retrieved_chunks,
            context_code=qa.context_code,
            session_id=session_id
        )

        logger.info(f"回答生成完成 - Session ID: {session_id}")
        
        # 返回答案和会话ID
        return {"answer": answer, "session_id": session_id}
    
    except Exception as e:
        logger.error(f"处理请求失败: {e}", exc_info=True)
        return JSONResponse(
            status_code=500,
            content={"answer": "抱歉，处理您的请求时出现错误。", "session_id": qa.session_id or "unknown"}
        )

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
    try:
        if file:
            logger.info(f"添加PDF文件: {file.filename}")
            # 直接处理内存中的文件
            file_content = BytesIO(await file.read())
            result = preprocess_pdfs(file_content=file_content, file_name=file.filename)
            
            # 重新加载数据库以反映更改
            model_manager.reload_database()
            logger.info(f"数据库已重新加载")
            
            return {"message": result.get("message", f"文件 {file.filename} 已处理并添加到知识库。")}

        elif directory:
            logger.info(f"添加PDF目录: {directory}")
            # 处理指定目录下的所有 PDF 文件
            if not os.path.exists(directory):
                return {"error": f"目录 {directory} 不存在。"}
            
            result = preprocess_pdfs(directory=directory)
            
            # 重新加载数据库以反映更改
            model_manager.reload_database()
            logger.info(f"数据库已重新加载")
            
            return {"message": result.get("message", f"目录 {directory} 下的所有 PDF 文件已添加到知识库。")}

        else:
            return {"error": "必须提供文件或目录路径。"}
    
    except Exception as e:
        logger.error(f"添加PDF失败: {e}", exc_info=True)
        return {"error": f"处理PDF文件时出错: {str(e)}"}

@app.get("/health")
def health_check():
    """健康检查接口"""
    try:
        is_ready = model_manager.is_initialized()
        return {
            "status": "healthy" if is_ready else "initializing",
            "timestamp": datetime.now().isoformat(),
            "model_ready": is_ready
        }
    except Exception as e:
        logger.error(f"健康检查失败: {e}")
        return JSONResponse(
            status_code=503,
            content={"status": "unhealthy", "error": str(e)}
        )


@app.post("/generate_docs")
async def generate_docs(request: Request):
    """
    接收 IntelliJ 插件上传的项目结构数据，调用 LLM 生成 Markdown 文档。
    """
    try:
        data = await request.json()
        logger.info(f"生成文档请求 - 文件数: {data.get('file_count', 'unknown')}")
        markdown = generate_markdown_summary(data)
        return {"markdown": markdown}
    except Exception as e:
        logger.error(f"生成文档失败: {e}", exc_info=True)
        return {"error": str(e)}
    
@app.post("/generate_test")
async def generate_test_endpoint(request: dict):
    """
    生成单元测试的端点
    """
    try:
        requirement = request.get("requirement", "")
        context_code = request.get("context_code", "")
        class_name = request.get("class_name", "")
        method_name = request.get("method_name", "")
        
        if not requirement or not context_code:
            return {"error": "测试需求和代码上下文不能为空"}
        
        logger.info(f"生成单元测试 - Class: {class_name}, Method: {method_name}")
        test_code = test_generator.generate_unit_test(requirement, context_code, class_name, method_name)
        
        return {
            "test_code": test_code,
            "status": "success"
        }
        
    except Exception as e:
        logger.error(f"生成测试失败: {e}", exc_info=True)
        return {"error": f"生成测试失败: {str(e)}"}


# 全局异常处理
@app.exception_handler(SmartTAException)
async def smartta_exception_handler(request: Request, exc: SmartTAException):
    """SmartTA异常处理器"""
    logger.error(f"SmartTA Exception: {exc.message} ({exc.error_code})", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={"error": exc.message, "error_code": exc.error_code}
    )


@app.exception_handler(Exception)
async def general_exception_handler(request: Request, exc: Exception):
    """通用异常处理器"""
    logger.error(f"Unexpected error: {exc}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={"error": "Internal server error", "detail": str(exc)}
    )