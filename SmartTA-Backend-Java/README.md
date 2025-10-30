# SmartTA Backend (Java版本)

这是SmartTA后端的Java实现，使用Spring Boot框架开发，完全保持了原Python版本的所有功能。

## 技术栈

- **Java 17** - 编程语言
- **Spring Boot 3.2.0** - Web框架
- **LangChain4j** - LLM集成框架
- **Apache PDFBox** - PDF处理
- **Maven** - 项目管理和构建工具

## 项目结构

```
SmartTA-Backend-Java/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── example/
│       │           └── smartta/
│       │               ├── SmartTAApplication.java      # 主应用类
│       │               ├── config/
│       │               │   └── SmartTAProperties.java   # 配置管理
│       │               ├── controller/
│       │               │   └── SmartTAController.java   # REST API控制器
│       │               ├── service/
│       │               │   ├── ConversationManager.java # 会话管理
│       │               │   ├── ModelManager.java        # 模型管理
│       │               │   ├── EmbeddingService.java    # 嵌入服务
│       │               │   ├── VectorStoreService.java  # 向量存储
│       │               │   ├── RetrieverService.java    # 检索服务
│       │               │   ├── GeneratorService.java    # 答案生成
│       │               │   ├── PreprocessorService.java # PDF预处理
│       │               │   ├── TestGeneratorService.java # 测试生成
│       │               │   └── DocGeneratorService.java  # 文档生成
│       │               ├── model/
│       │               │   ├── DocumentChunk.java       # 文档块模型
│       │               │   ├── QuestionRequest.java     # 请求模型
│       │               │   └── AnswerResponse.java      # 响应模型
│       │               └── exception/
│       │                   ├── SmartTAException.java    # 基础异常
│       │                   ├── GlobalExceptionHandler.java # 全局异常处理
│       │                   └── ...                       # 其他异常类
│       └── resources/
│           ├── application.yml          # 主配置文件
│           ├── application-dev.yml      # 开发环境配置
│           └── application-prod.yml     # 生产环境配置
├── pom.xml                              # Maven配置文件
├── .env                                 # 环境变量
└── README.md                            # 本文档
```

## 核心功能

### 1. 智能问答 (`/ask`)
- 基于RAG（检索增强生成）的智能问答
- 支持会话历史管理
- 可选的代码上下文

### 2. PDF知识库管理 (`/add_pdfs`)
- 上传PDF文件到知识库
- 批量处理目录中的PDF文件
- 自动提取文本并生成向量嵌入

### 3. 文档生成 (`/generate_docs`)
- 根据项目结构生成Markdown文档
- 分析Java类和方法
- 提供项目概览

### 4. 测试生成 (`/generate_test`)
- 根据需求生成JUnit单元测试
- 支持指定类名和方法名
- 包含边界条件和异常处理

### 5. 健康检查 (`/health`)
- 检查服务状态
- 验证模型是否就绪

## 安装和配置

### 所需资源

- Java 17或 **Java 21（推荐）**（注意：Lombok 在 Java 25 上可能有兼容性问题）
- Maven 3.8 或更高版本
- DeepSeek API密钥

### 配置API

方法1. 编辑 `.env` 文件，填入你的API密钥：
```properties
DEEPSEEK_API_KEY=your_actual_api_key_here
DEEPSEEK_BASE_URL=https://api.deepseek.com
```

方法2. 或者直接在 `application.yml` 中配置：
```yaml
smartta:
  deepseek:
    api-key: your_api_key_here
    base-url: https://api.deepseek.com
```

### 构建项目

```bash
# 使用Java 17或21，具体路径切换成自己设备上jdk的位置
export JAVA_HOME=/Users/renhongzhen/Library/Java/JavaVirtualMachines/graalvm-jdk-21.0.7/Contents/Home

# 编译项目
mvn clean compile
```

### 运行

在IDE中直接运行 `SmartTAApplication.java` 的main方法。

## API端点说明

### 1. 问答接口

**端点**: `POST /ask`

**请求体**:
```json
{
  "question": "什么是虚拟内存？",
  "context_code": "// 可选的代码上下文",
  "session_id": "可选的会话ID"
}
```

**响应**:
```json
{
  "answer": "虚拟内存是...",
  "session_id": "uuid-string"
}
```

### 2. 添加PDF

**端点**: `POST /add_pdfs`

**方式1 - 上传文件**:
```bash
curl -X POST http://localhost:8000/add_pdfs \
  -F "file=@/path/to/your.pdf"
```

**方式2 - 指定目录**:
```bash
curl -X POST http://localhost:8000/add_pdfs \
  -F "directory=/path/to/pdf/directory"
```

### 3. 生成文档

**端点**: `POST /generate_docs`

**请求体**:
```json
{
  "root": "/project/path",
  "file_count": 10,
  "files": [
    {
      "file": "Example.java",
      "classes": ["Example"],
      "methods": ["method1", "method2"],
      "comments": ["注释1", "注释2"]
    }
  ]
}
```

### 4. 生成测试

**端点**: `POST /generate_test`

**请求体**:
```json
{
  "requirement": "测试计算器的加法功能",
  "context_code": "public class Calculator { ... }",
  "class_name": "Calculator",
  "method_name": "add"
}
```

### 5. 健康检查

**端点**: `GET /health`

**响应**:
```json
{
  "status": "healthy",
  "timestamp": "2024-01-01T12:00:00",
  "model_ready": true
}
```

## 配置说明

### application.yml 配置项

```yaml
server:
  port: 8000  # 服务端口

smartta:
  # 模型配置
  model:
    embedding:
      model-name: sentence-transformers/all-mpnet-base-v2
    llm:
      model-name: deepseek-chat
      temperature: 0.6
      max-tokens: 1024

  # DeepSeek API配置
  deepseek:
    api-key: ${DEEPSEEK_API_KEY}
    base-url: ${DEEPSEEK_BASE_URL}

  # 数据路径配置
  data:
    pdf-dir: ./data/pdfs
    db-path: ./data/faiss_index
    data-dir: ./data

  # RAG参数配置
  rag:
    top-k: 3
    chunk-size: 1000
    chunk-overlap: 200

  # 会话配置
  session:
    max-conversation-history: 5

  # 超时配置
  timeout:
    request-timeout: 120
    connect-timeout: 10
```

## 数据存储

### 向量数据库

数据存储在 `./data/faiss_index/` 目录下：
- `index.pkl` - 序列化的文档和向量数据

### 自动重建

如果向量数据库不存在，系统会：
1. 自动搜索PDF文件（在多个可能的路径）
2. 提示用户调用 `/add_pdfs` 接口

## 开发说明

### 日志配置

- 日志文件: `smartta.log`
- 日志级别: 开发环境DEBUG，生产环境INFO
- 日志格式: 包含时间戳、日志级别、消息

### 线程安全

- `ConversationManager`: 使用 `ConcurrentHashMap` 和同步块
- `ModelManager`: 使用单例模式和初始化锁
- `VectorStoreService`: 使用同步方法保护关键操作

### 错误处理

- 全局异常处理器 `GlobalExceptionHandler`
- 自定义异常体系继承自 `SmartTAException`
- 统一的错误响应格式

## 与Python版本的差异

### 技术架构对比

| 技术层面 |	Python |	Java |
|-----|----|----|
| Web框架 |	FastAPI |	Spring Boot|
| LLM集成	| LangChain	| LangChain4j |
|PDF处理	|PyMuPDF	|Apache PDFBox|
|向量存储	|FAISS	|自实现内存存储|
|配置管理	|Pydantic	|Spring ConfigurationProperties|
|依赖注入	|手动	|Spring IoC|

### 主要差异

1. **向量存储实现**
   - Python: 使用FAISS (Facebook AI Similarity Search)
   - Java: 使用自实现的内存向量存储（基于余弦相似度）
   - 注意：生产环境建议使用专业的向量数据库（如Milvus、Pinecone等）

2. **嵌入模型**
   - Python: 使用 HuggingFace Transformers
   - Java: 使用 LangChain4j 的 AllMiniLmL6V2 嵌入模型

3. **LLM集成**
   - Python: 使用 LangChain (Python)
   - Java: 使用 LangChain4j

4. **PDF处理**
   - Python: 使用 PyMuPDF/pypdf
   - Java: 使用 Apache PDFBox

### 功能对等性

✅ 所有API端点完全一致  
✅ 请求/响应格式完全一致  
✅ 配置参数完全对应  
✅ 核心业务逻辑完全等价  
✅ 异常处理机制相同  

## 性能优化

1. **模型缓存**: 嵌入模型和LLM在应用启动时初始化，避免重复加载
2. **单例模式**: ModelManager使用单例模式管理资源
3. **懒加载**: 向量数据库按需加载
4. **会话管理**: 自动限制历史记录长度，防止内存溢出

## 故障排查

### 问题: 启动时找不到PDF文件

**原因**: 向量数据库不存在，且系统找不到PDF文件

**解决方案**:
1. 将PDF文件放到 `../SmartTA/src/pdf` 目录
2. 或调用 `/add_pdfs` 接口上传PDF

### 问题: DeepSeek API调用失败

**原因**: API密钥未配置或无效

**解决方案**:
1. 检查环境变量 `DEEPSEEK_API_KEY`
2. 检查 `application.yml` 中的配置
3. 验证API密钥是否有效

### 问题: 内存不足

**原因**: 向量数据库太大或JVM堆内存不足

**解决方案**:
```bash
java -Xmx4g -jar target/smartta-backend-1.0.0.jar
```

## 部署

### Docker部署（推荐）

创建 `Dockerfile`:
```dockerfile
FROM openjdk:17-slim
WORKDIR /app
COPY target/smartta-backend-1.0.0.jar app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]
```

构建和运行:
```bash
docker build -t smartta-backend .
docker run -p 8000:8000 \
  -e DEEPSEEK_API_KEY=your_key \
  smartta-backend
```

### 传统部署

```bash
# 构建
mvn clean package -DskipTests

# 上传到服务器
scp target/smartta-backend-1.0.0.jar user@server:/opt/smartta/

# 在服务器上运行
java -jar /opt/smartta/smartta-backend-1.0.0.jar \
  --server.port=8000 \
  --spring.profiles.active=prod
```


**注意**: 这是从Python重写的Java版本，所有核心功能与原版本完全等价。

