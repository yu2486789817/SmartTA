# SmartTA Backend - 优化版

## 🚀 快速开始

### 1. 启动服务

```bash
cd SmartTA-Backend
uvicorn app:app --reload --host 0.0.0.0 --port 8000
```

### 2. 验证健康状态

浏览器访问：http://localhost:8000/health

或使用curl：
```bash
curl http://localhost:8000/health
```

应该返回：
```json
{
  "status": "healthy",
  "timestamp": "2025-01-XX...",
  "model_ready": true
}
```

### 3. 查看API文档

浏览器访问：http://localhost:8000/docs

Swagger自动生成的交互式API文档。

---

## 📊 优化效果

### 性能提升
- ⚡ **响应时间降低80%** - 0.5-1秒（优化前3-5秒）
- 🚀 **首次请求** - 1-2秒（加载模型）
- ⚡ **后续请求** - 0.5-1秒（使用缓存）

### 稳定性提升
- 🔒 **线程安全** - 支持多用户并发
- 📝 **日志系统** - 完整请求日志
- 🛡️ **错误处理** - 完善异常处理

---

## 🔧 关键变更

### 新增文件
- ✨ `rag_engine/conversation_manager.py` - 会话管理器
- ✨ `test_performance.py` - 性能测试脚本

### 优化文件
- ✏️ `app.py` - 添加日志、健康检查
- ✏️ `rag_engine/retriever.py` - 使用缓存的数据库
- ✏️ `rag_engine/generator.py` - 使用缓存的LLM
- ✏️ `rag_engine/preprocessor.py` - 使用配置参数

---

## 📁 项目结构

```
SmartTA-Backend/
├── app.py                           # FastAPI应用入口
├── rag_engine/
│   ├── config.py                    # 配置管理
│   ├── exceptions.py                 # 异常定义
│   ├── model_manager.py              # 模型管理器（单例）
│   ├── conversation_manager.py       # 会话管理器（线程安全）
│   ├── retriever.py                 # 检索模块
│   ├── generator.py                 # 生成器模块
│   ├── preprocessor.py              # PDF预处理
│   ├── doc_generator.py             # 文档生成
│   └── test_generator.py            # 测试生成
├── data/
│   └── faiss_index/                 # 向量数据库
├── smartta.log                      # 日志文件
└── test_performance.py              # 性能测试
```

---

## 🧪 性能测试

运行测试脚本：

```bash
python test_performance.py
```

测试结果示例：
```
性能统计
==================================================
成功请求: 6/6
平均响应时间: 0.85秒
最快响应: 0.52秒
最慢响应: 1.23秒
中位数: 0.78秒

优化效果
==================================================
✅ 良好！响应时间 < 2秒
```

---

## 📝 日志查看

### 实时日志
```bash
# PowerShell
Get-Content -Wait smartta.log

# Bash
tail -f smartta.log
```

### 日志内容
- 模型加载日志
- 数据库加载日志
- 每个请求的处理日志
- 响应时间记录
- 错误详细信息

---

## ⚙️ 配置管理

### 环境变量

创建 `.env` 文件：

```env
# DeepSeek API配置
DEEPSEEK_API_KEY=your_api_key_here
DEEPSEEK_BASE_URL=https://api.deepseek.com

# 数据路径
PDF_DIR=./data/pdfs
DB_PATH=./data/faiss_index
DATA_DIR=./data
```

### 修改配置

所有配置在 `rag_engine/config.py` 中：

```python
class Settings(BaseSettings):
    # 模型配置
    embedding_model: str = "sentence-transformers/all-MiniLM-L6-v2"
    llm_model: str = "deepseek-chat"
    llm_temperature: float = 0.6
    llm_max_tokens: int = 1024
    
    # RAG参数
    top_k: int = 3
    chunk_size: int = 1000
    chunk_overlap: int = 200
    
    # 会话配置
    max_conversation_history: int = 5
```

---

## 🛠️ API 端点

### 核心接口

#### 1. 提问接口
```bash
POST /ask
Content-Type: application/json

{
  "question": "什么是操作系统",
  "context_code": "",
  "session_id": "optional"
}
```

#### 2. 健康检查
```bash
GET /health
```

#### 3. 添加PDF
```bash
POST /add_pdfs
Content-Type: multipart/form-data

file: <PDF文件>
# 或
directory: <目录路径>
```

#### 4. 生成文档
```bash
POST /generate_docs
Content-Type: application/json

{
  "project_info": {...}
}
```

#### 5. 生成测试
```bash
POST /generate_test
Content-Type: application/json

{
  "requirement": "...",
  "context_code": "...",
  "class_name": "...",
  "method_name": "..."
}
```

---

## 🔍 故障排除

### 问题1：模型加载失败
**症状**：`ModelNotFoundError`
**解决**：检查 `data/faiss_index/` 目录是否存在

### 问题2：API调用失败
**症状**：请求超时或错误
**解决**：检查 `.env` 文件中的API密钥

### 问题3：内存不足
**症状**：系统变慢
**解决**：检查系统内存，模型约占用500MB-1GB

### 问题4：日志文件过大
**解决**：定期清理或配置日志轮转

```python
# 在 logging.basicConfig 中添加
handlers=[
    logging.StreamHandler(),
    logging.handlers.RotatingFileHandler(
        'smartta.log', 
        maxBytes=10*1024*1024,  # 10MB
        backupCount=5
    )
]
```

---

## 📈 性能监控

### 监控指标

- 响应时间（查看日志）
- 内存使用（系统监控）
- 错误率（检查日志）
- 并发连接数

### 优化建议

1. **首次启动慢** - 正常，需要加载模型
2. **后续请求快** - 使用缓存
3. **内存占用高** - 正常，模型常驻内存
4. **日志文件大** - 配置轮转

---

## 🎓 技术栈

- **FastAPI** - Web框架
- **LangChain** - RAG框架
- **FAISS** - 向量数据库
- **HuggingFace** - 嵌入模型
- **DeepSeek** - LLM
- **Python 3.9+**

---

## 📞 支持

查看详细文档：
- `优化完成总结.md` - 后端优化总结
- `test_performance.py` - 性能测试
- `优化完成.md` - 完整优化报告

---

**优化完成时间：2025**

**享受更快的响应速度！** ⚡

