# 第11步：AI全栈开发基础

> **阶段目标：** 掌握FastAPI搭建AI Web服务，理解前后端交互模式，能用现代UI框架构建AI应用界面  
> **预计学时：** 2-3周（每天3-4小时）  
> **前置要求：** API调用基础 + Python编程  

---

## 📚 目录

- [11.1 AI全栈导论](#111-ai全栈导论)
- [11.2 FastAPI核心](#112-fastapi核心)
- [11.3 AI API服务设计](#113-ai-api服务设计)
- [11.4 前端基础与AI UI](#114-前端基础与ai-ui)
- [11.5 构建Chat UI](#115-构建chat-ui)
- [11.6 阶段练习](#116-阶段练习)

---

## 11.1 AI全栈导论

### 11.1.1 AI应用的技术栈

```
┌─────────────────────────────────────────────┐
│            前端 (Frontend)                   │
│  HTML/CSS/JS · React/Vue · Tailwind CSS     │
│  Streamlit · Gradio · Chainlit              │
├─────────────────────────────────────────────┤
│           API 层 (Backend)                   │
│  FastAPI · Flask · Node.js Express          │
│  认证鉴权 · 限流 · 日志 · 监控               │
├─────────────────────────────────────────────┤
│          AI 服务层 (AI Service)              │
│  LLM调用 · RAG检索 · Agent执行              │
│  Prompt管理 · 向量数据库                     │
├─────────────────────────────────────────────┤
│          数据层 (Data Layer)                 │
│  PostgreSQL · Redis · Milvus · MinIO        │
└─────────────────────────────────────────────┘
```

### 11.1.2 为什么需要后端？

```
用户浏览器 ──HTTP──→ FastAPI后端 ──API──→ OpenAI/Claude
                           │
                           ├──→ 向量数据库 (RAG检索)
                           ├──→ PostgreSQL (用户数据)
                           ├──→ Redis (缓存/会话)
                           └──→ 文件存储 (上传文件)

不能直接从前端调用LLM API的原因：
1. API Key暴露风险
2. 无法做访问控制和限流
3. 不能集成内部数据（RAG）
4. 无法记录和审计
5. CORS跨域限制
```

---

## 11.2 FastAPI核心

### 11.2.1 最小应用

```python
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import Optional, List
import uvicorn

app = FastAPI(
    title="AI Chat API",
    description="一个简单的AI聊天接口",
    version="1.0.0",
)

# ========== 请求/响应模型 ==========
class ChatRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=10000, 
                         description="用户消息")
    model: str = Field(default="gpt-4o-mini", description="模型名称")
    temperature: float = Field(default=0.7, ge=0, le=2)
    max_tokens: int = Field(default=1024, ge=1, le=4096)

class ChatResponse(BaseModel):
    reply: str
    model: str
    tokens_used: int

# ========== API 路由 ==========
@app.get("/")
async def root():
    return {"message": "AI Chat API 运行中", "version": "1.0.0"}

@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """聊天接口"""
    try:
        # TODO: 调用LLM
        reply = f"收到你的消息: {request.message}"
        return ChatResponse(
            reply=reply,
            model=request.model,
            tokens_used=len(request.message),
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/health")
async def health_check():
    """健康检查 — Kubernetes就绪探针"""
    return {"status": "healthy"}

# 启动: uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

### 11.2.2 中间件与依赖注入

```python
# ========== CORS中间件 ==========
from fastapi.middleware.cors import CORSMiddleware

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],  # 前端地址
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ========== 认证中间件 ==========
from fastapi import Depends, Header

async def verify_api_key(x_api_key: str = Header(None)):
    """API Key验证"""
    valid_keys = {"sk-test-key-123"}  # 实际应从数据库读取
    if x_api_key not in valid_keys:
        raise HTTPException(status_code=401, detail="无效的API Key")
    return x_api_key

@app.post("/secure-chat")
async def secure_chat(
    request: ChatRequest,
    api_key: str = Depends(verify_api_key),
):
    """需要认证的聊天接口"""
    return {"reply": f"[已验证] {request.message}"}

# ========== 请求日志中间件 ==========
import time
import logging

logger = logging.getLogger(__name__)

@app.middleware("http")
async def log_requests(request, call_next):
    start = time.time()
    response = await call_next(request)
    duration = time.time() - start
    
    logger.info(
        f"{request.method} {request.url.path} "
        f"→ {response.status_code} ({duration:.3f}s)"
    )
    
    return response
```

### 11.2.3 LLM调用集成

```python
# ========== 依赖注入：LLM客户端 ==========
from functools import lru_cache

@lru_cache()
def get_llm_client():
    """单例模式获取LLM客户端"""
    return LLMAPIClient(APIConfigManager())

@app.post("/v1/chat/completions")
async def chat_completions(
    request: ChatRequest,
    client: LLMAPIClient = Depends(get_llm_client),
):
    """OpenAI兼容的聊天接口"""
    try:
        messages = [{"role": "user", "content": request.message}]
        
        response = client.chat(
            messages=messages,
            model=request.model,
            temperature=request.temperature,
            max_tokens=request.max_tokens,
        )
        
        return {
            "choices": [{
                "message": {"role": "assistant", "content": response.content},
                "finish_reason": response.finish_reason,
            }],
            "usage": {
                "prompt_tokens": response.usage["prompt_tokens"],
                "completion_tokens": response.usage["completion_tokens"],
                "total_tokens": response.usage["prompt_tokens"] + response.usage["completion_tokens"],
            },
            "model": response.model,
        }
    except RateLimitError:
        raise HTTPException(status_code=429, detail="请求过于频繁，请稍后重试")
    except InvalidAPIKeyError:
        raise HTTPException(status_code=401, detail="API Key无效")
    except Exception as e:
        logger.error(f"LLM调用失败: {e}")
        raise HTTPException(status_code=500, detail="AI服务暂时不可用")

# ========== 流式响应（SSE）==========
from fastapi.responses import StreamingResponse
import asyncio

@app.post("/v1/chat/stream")
async def chat_stream(request: ChatRequest):
    """流式聊天接口"""
    async def generate():
        # 模拟流式输出
        words = "这是一段流式输出的示例文本，每个词逐个发送。".split()
        for word in words:
            yield f"data: {word}\n\n"
            await asyncio.sleep(0.1)
        yield "data: [DONE]\n\n"
    
    return StreamingResponse(
        generate(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",  # Nginx禁用缓冲
        },
    )
```

---

## 11.3 AI API服务设计

### 11.3.1 项目结构

```
ai-service/
├── app/
│   ├── __init__.py
│   ├── main.py              # FastAPI应用入口
│   ├── config.py            # 配置管理
│   │
│   ├── api/                 # API路由层
│   │   ├── __init__.py
│   │   ├── v1/
│   │   │   ├── __init__.py
│   │   │   ├── chat.py      # 聊天接口
│   │   │   ├── rag.py       # RAG接口
│   │   │   └── agent.py     # Agent接口
│   │   └── deps.py          # 公共依赖
│   │
│   ├── core/                # 核心业务逻辑
│   │   ├── llm_client.py    # LLM客户端
│   │   ├── rag_service.py   # RAG服务
│   │   ├── agent_service.py # Agent服务
│   │   └── prompt_manager.py
│   │
│   ├── models/              # 数据模型
│   │   ├── request.py       # 请求模型
│   │   ├── response.py      # 响应模型
│   │   └── database.py      # 数据库模型
│   │
│   └── utils/               # 工具函数
│       ├── auth.py
│       ├── logger.py
│       └── cost_tracker.py
│
├── tests/                   # 测试
├── alembic/                 # 数据库迁移
├── .env                     # 环境变量
├── Dockerfile
├── docker-compose.yml
└── requirements.txt
```

### 11.3.2 完整AI服务示例

```python
# app/api/v1/rag.py
from fastapi import APIRouter, Depends, HTTPException
from app.models.request import RAGQueryRequest
from app.models.response import RAGQueryResponse
from app.core.rag_service import RAGService
from app.api.deps import get_rag_service, get_current_user

router = APIRouter(prefix="/rag", tags=["RAG"])

@router.post("/query", response_model=RAGQueryResponse)
async def rag_query(
    request: RAGQueryRequest,
    rag_service: RAGService = Depends(get_rag_service),
    user: str = Depends(get_current_user),
):
    """
    RAG查询接口
    
    工作流程：
    1. 在知识库中检索相关文档
    2. 用检索到的文档增强Prompt
    3. 调用LLM生成回答
    4. 返回答案+引用来源
    """
    try:
        result = await rag_service.query(
            question=request.question,
            knowledge_base=request.knowledge_base,
            top_k=request.top_k,
            user_id=user,
        )
        
        return RAGQueryResponse(
            answer=result["answer"],
            sources=result["sources"],
            tokens_used=result["tokens_used"],
            latency_ms=result["latency_ms"],
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"RAG查询失败: {e}")

@router.post("/knowledge-base/{kb_name}/upload")
async def upload_document(
    kb_name: str,
    file: UploadFile,
    rag_service: RAGService = Depends(get_rag_service),
):
    """上传文档到知识库"""
    # 验证文件类型
    allowed_types = ['.pdf', '.docx', '.txt', '.md', '.csv']
    ext = os.path.splitext(file.filename)[1].lower()
    if ext not in allowed_types:
        raise HTTPException(400, f"不支持的文件类型: {ext}")
    
    # 保存并索引
    content = await file.read()
    result = await rag_service.index_document(
        kb_name=kb_name,
        filename=file.filename,
        content=content,
    )
    
    return {"status": "success", "chunks_indexed": result["chunk_count"]}
```

---

## 11.4 前端基础与AI UI

### 11.4.1 AI应用前端方案对比

```
方案选择：

1. Streamlit
   - 纯Python，零前端知识
   - 适合：快速原型、内部工具、数据Dashboard
   - 不适合：复杂交互、定制化UI

2. Gradio
   - 纯Python，HuggingFace官方推荐
   - 适合：模型Demo、简单交互界面
   - 不适合：复杂多页面应用

3. Chainlit
   - 专为LLM应用设计
   - 适合：Chatbot、Agent界面
   - 不适合：非对话型应用

4. React + Tailwind CSS
   - 完全定制化
   - 适合：生产级产品
   - 需要前端知识
```

### 11.4.2 Streamlit快速搭建

```python
# app.py — AI聊天应用
import streamlit as st
import requests

st.set_page_config(
    page_title="AI助手",
    page_icon="🤖",
    layout="wide",
)

# ========== 侧边栏：设置 ==========
with st.sidebar:
    st.title("⚙️ 设置")
    model = st.selectbox(
        "选择模型",
        ["gpt-4o", "gpt-4o-mini", "gpt-4-turbo"],
    )
    temperature = st.slider("Temperature", 0.0, 2.0, 0.7, 0.1)
    max_tokens = st.slider("Max Tokens", 64, 4096, 1024, 64)
    
    st.divider()
    st.caption(f"当前模型: {model}")
    if st.button("清空对话"):
        st.session_state.messages = []
        st.rerun()

# ========== 主界面：对话 ==========
st.title("🤖 AI 聊天助手")

# 初始化对话历史
if "messages" not in st.session_state:
    st.session_state.messages = []

# 显示历史消息
for msg in st.session_state.messages:
    with st.chat_message(msg["role"]):
        st.markdown(msg["content"])

# 用户输入
if prompt := st.chat_input("输入你的问题..."):
    # 显示用户消息
    st.session_state.messages.append({"role": "user", "content": prompt})
    with st.chat_message("user"):
        st.markdown(prompt)
    
    # 调用后端API
    with st.chat_message("assistant"):
        with st.spinner("思考中..."):
            try:
                response = requests.post(
                    "http://localhost:8000/v1/chat/completions",
                    json={
                        "message": prompt,
                        "model": model,
                        "temperature": temperature,
                        "max_tokens": max_tokens,
                    },
                    headers={"X-API-Key": "sk-test-key-123"},
                )
                
                if response.status_code == 200:
                    data = response.json()
                    reply = data["choices"][0]["message"]["content"]
                    st.markdown(reply)
                    st.session_state.messages.append(
                        {"role": "assistant", "content": reply}
                    )
                    
                    # 显示Token使用
                    usage = data["usage"]
                    st.caption(
                        f"输入: {usage['prompt_tokens']} tokens | "
                        f"输出: {usage['completion_tokens']} tokens"
                    )
                else:
                    st.error(f"API错误: {response.status_code}")
            except Exception as e:
                st.error(f"连接失败: {e}")

# 启动: streamlit run app.py
```

### 11.4.3 Gradio — 模型Demo利器

```python
import gradio as gr

def chat_fn(message, history, model, temperature):
    """聊天函数 — 对接后端API"""
    # 转换history格式
    messages = []
    for h in history:
        messages.append({"role": "user", "content": h[0]})
        messages.append({"role": "assistant", "content": h[1]})
    messages.append({"role": "user", "content": message})
    
    # 调用API
    response = requests.post(
        "http://localhost:8000/v1/chat/stream",
        json={
            "message": message,
            "model": model,
            "temperature": temperature,
        },
        stream=True,
    )
    
    # 流式返回
    full_response = ""
    for chunk in response.iter_content(chunk_size=None):
        if chunk:
            text = chunk.decode().replace("data: ", "").strip()
            if text != "[DONE]":
                full_response += text
                yield full_response

# 构建Gradio界面
with gr.Blocks(title="AI Chat", theme=gr.themes.Soft()) as demo:
    gr.Markdown("# 🤖 AI 聊天助手")
    
    with gr.Row():
        with gr.Column(scale=3):
            chatbot = gr.Chatbot(height=500)
            msg = gr.Textbox(placeholder="输入消息...", label="")
            clear = gr.Button("清空")
        
        with gr.Column(scale=1):
            model = gr.Dropdown(
                ["gpt-4o", "gpt-4o-mini"],
                value="gpt-4o-mini",
                label="模型",
            )
            temperature = gr.Slider(0, 2, 0.7, label="Temperature")
    
    msg.submit(chat_fn, [msg, chatbot, model, temperature], chatbot)
    clear.click(lambda: None, None, chatbot)

demo.launch(server_name="0.0.0.0", server_port=7860)
```

---

## 11.5 构建Chat UI

### 11.5.1 核心交互模式

```
AI Chat UI 的三种核心模式：

1. 请求-响应 (Request-Response)
   用户发送 → 等待 → 完整回复显示
   简单但慢（用户要等）

2. 流式输出 (Streaming)
   用户发送 → 实时显示生成的每个Token
   体验好，用户感知速度快
   需要：SSE / WebSocket / ReadableStream

3. 中断生成 (Stop Generation)
   用户觉得方向不对 → 点停止 → 截断输出
   所有好的Chat UI都有这个功能
```

### 11.5.2 原生HTML/CSS/JS Chat UI

```html
<!-- chat-ui.html — 纯原生实现，无框架依赖 -->
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AI Chat</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, sans-serif; background: #1a1a2e; color: #eee; }
        
        .container {
            max-width: 800px; margin: 0 auto; height: 100vh;
            display: flex; flex-direction: column;
        }
        
        .header {
            padding: 16px; background: #16213e;
            border-bottom: 1px solid #0f3460;
            text-align: center;
        }
        
        .messages {
            flex: 1; overflow-y: auto; padding: 20px;
        }
        
        .message {
            margin: 12px 0; padding: 12px 16px;
            border-radius: 12px; max-width: 80%;
            line-height: 1.6; white-space: pre-wrap;
        }
        
        .user { background: #0f3460; margin-left: auto; }
        .assistant { background: #16213e; margin-right: auto; }
        
        .input-area {
            padding: 16px; display: flex; gap: 12px;
            border-top: 1px solid #0f3460;
        }
        
        #userInput {
            flex: 1; padding: 12px; border-radius: 8px;
            border: 1px solid #0f3460; background: #1a1a2e;
            color: #eee; font-size: 14px; resize: none;
        }
        
        button {
            padding: 12px 24px; border: none; border-radius: 8px;
            cursor: pointer; font-weight: 600;
        }
        
        #sendBtn { background: #e94560; color: white; }
        #stopBtn { background: #555; color: white; display: none; }
        
        .streaming { border-left: 2px solid #e94560; animation: pulse 1.5s infinite; }
        @keyframes pulse { 50% { border-color: transparent; } }
    </style>
</head>
<body>
    <div class="container">
        <div class="header"><h2>🤖 AI 聊天助手</h2></div>
        
        <div class="messages" id="messages"></div>
        
        <div class="input-area">
            <textarea id="userInput" rows="2" placeholder="输入问题..."
                      onkeydown="if(event.key==='Enter'&&!event.shiftKey){event.preventDefault();sendMessage()}">
            </textarea>
            <button id="sendBtn" onclick="sendMessage()">发送</button>
            <button id="stopBtn" onclick="stopGeneration()">停止</button>
        </div>
    </div>
    
    <script>
        let abortController = null;
        const API_URL = 'http://localhost:8000';
        
        function addMessage(role, content) {
            const div = document.createElement('div');
            div.className = `message ${role}`;
            div.textContent = content;
            document.getElementById('messages').appendChild(div);
            document.getElementById('messages').scrollTop = 
                document.getElementById('messages').scrollHeight;
            return div;
        }
        
        async function sendMessage() {
            const input = document.getElementById('userInput');
            const message = input.value.trim();
            if (!message) return;
            
            input.value = '';
            input.disabled = true;
            
            addMessage('user', message);
            
            const assistantDiv = addMessage('assistant', '');
            assistantDiv.classList.add('streaming');
            
            document.getElementById('sendBtn').style.display = 'none';
            document.getElementById('stopBtn').style.display = 'block';
            
            abortController = new AbortController();
            
            try {
                const response = await fetch(`${API_URL}/v1/chat/stream`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-API-Key': 'sk-test-key-123',
                    },
                    body: JSON.stringify({ message }),
                    signal: abortController.signal,
                });
                
                const reader = response.body.getReader();
                const decoder = new TextDecoder();
                let fullText = '';
                
                while (true) {
                    const { done, value } = await reader.read();
                    if (done) break;
                    
                    const chunk = decoder.decode(value, { stream: true });
                    const text = chunk.replace('data: ', '').trim();
                    if (text === '[DONE]') break;
                    
                    fullText += text;
                    assistantDiv.textContent = fullText;
                    document.getElementById('messages').scrollTop = 
                        document.getElementById('messages').scrollHeight;
                }
                
                assistantDiv.classList.remove('streaming');
            } catch (err) {
                if (err.name !== 'AbortError') {
                    assistantDiv.textContent = '❌ 连接错误: ' + err.message;
                }
            } finally {
                document.getElementById('sendBtn').style.display = 'block';
                document.getElementById('stopBtn').style.display = 'none';
                input.disabled = false;
                input.focus();
                abortController = null;
            }
        }
        
        function stopGeneration() {
            if (abortController) {
                abortController.abort();
                abortController = null;
            }
        }
    </script>
</body>
</html>
```

---

## 11.6 阶段练习

### 练习1：FastAPI AI服务
搭建一个支持聊天、RAG查询、健康检查的FastAPI服务。

### 练习2：Streamlit UI
用Streamlit给练习1的API搭建一个用户界面。

### 练习3：全栈AI应用
完成一个端到端的AI应用：
- FastAPI后端（集成LLM调用）
- 前端界面（支持流式输出）
- 错误处理和重试

---

> **✅ 阶段完成检查清单：**
> - [ ] 能用FastAPI搭建带认证的AI API服务
> - [ ] 理解CORS、中间件、依赖注入
> - [ ] 能用Streamlit快速搭建AI应用UI
> - [ ] 理解流式输出的前后端实现
> - [ ] 知道AI应用的标准项目结构
> - [ ] 完成3个阶段练习
>
> **下一步：** [第12步：后端与API集成](../12-后端与API集成/README.md)
