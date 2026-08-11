# 03 FastAPI 服务化改造

> Level2 的承重墙：把 Level1 的 Streamlit 直连代码改造成 FastAPI 服务——路由、依赖注入、Pydantic 校验、SSE 流式、错误契约。服务化之后，界面层与能力层彻底解耦，测试与部署才有抓手。

## 📚 目录

1. [目标与验收](#1-目标与验收)
2. [从脚本到服务：架构转变](#2-从脚本到服务架构转变)
3. [路由与依赖注入](#3-路由与依赖注入)
4. [Pydantic 校验与错误契约](#4-pydantic-校验与错误契约)
5. [SSE 流式接口](#5-sse-流式接口)
6. [服务化后的 Streamlit](#6-服务化后的-streamlit)
7. [文档上传接口与 CORS](#7-文档上传接口与-cors)
8. [常见坑](#8-常见坑)

---

## 1. 目标与验收

本模块的产出：FastAPI 服务跑通——`POST /api/chat` 对话接口（含 SSE 流式版）、`POST /api/documents` 上传接口、`GET /health` 健康检查。验收标准：**curl 能调通三个接口**；**接口文档（/docs）自动生成且字段正确**；**能讲清依赖注入在测试中的价值**（07 篇会替换依赖做测试）。版本基线（2026-08）：FastAPI 0.139、Pydantic 2.13、Starlette 1.1、Uvicorn 0.51——详细机制回仓库「Python 异步 + FastAPI」体系，本模块聚焦项目落地。

## 2. 从脚本到服务：架构转变

Level1 的代码形态是"脚本直接调能力"（Streamlit 脚本里创建 client、调模型）。服务化的本质是**把能力封装成 HTTP 契约**，界面与能力解耦。转变的收益三个：**可测试**（接口是稳定的契约，测试打接口而非测脚本）；**可复用**（Streamlit、curl、未来前端都用同一套接口）；**可部署**（Uvicorn 进程就是服务，Docker 化顺理成章）。转变的动作：能力层代码（rag/、repository/）基本原样迁移进新目录，新增的是**api 层**——HTTP 与业务的边界。

异步的选择：FastAPI 是异步框架，但**同步能力（embedding、本地检索）不要强行 async**——用 `def` 定义路由（FastAPI 自动丢线程池），用 `async def` 处理 IO 密集型（调大模型、读库）。一句话：**IO 异步、计算同步**，这个选型在 Level2 规模下最优，Level3 高并发再上全异步。

## 3. 路由与依赖注入

路由层的核心是**依赖注入（Depends）**：组件通过声明获得依赖，而不是在代码里 import 单例——这让测试可以替换依赖：

```python
# app/api/chat.py
from fastapi import APIRouter, Depends
from app.services.chat_service import ChatService
from app.schemas import ChatRequest, ChatResponse

router = APIRouter(prefix="/api/chat", tags=["chat"])

def get_chat_service() -> ChatService:      # 依赖工厂
    return ChatService()                     # 07 篇测试时替换为 Mock 版本

@router.post("", response_model=ChatResponse)
async def chat(req: ChatRequest, service: ChatService = Depends(get_chat_service)):
    answer, citations = await service.ask(req.session_id, req.question)
    return ChatResponse(answer=answer, citations=citations)
```

三个要点：**APIRouter 分模块**（chat/documents 各自文件，main.py 里 include_router 挂载）；**依赖用 Depends 声明**（不直接 `from app.services.chat_service import chat_service_instance`——测试无法替换实例）；**response_model 必写**（接口契约显式化，/docs 自动出文档，响应校验自动生效）。main.py 入口最小化：创建 app、注册路由、挂中间件、加 /health——**main.py 超过 50 行就是分层失败了**。

## 4. Pydantic 校验与错误契约

请求校验与错误响应是接口的"门禁"：**非法输入在入口拦截，错误在出口统一格式**。请求校验用 Pydantic 模型（自动 422 返回）：

```python
# app/schemas.py
from pydantic import BaseModel, Field

class ChatRequest(BaseModel):
    session_id: str = Field(min_length=1, max_length=64, description="会话 ID")
    question: str = Field(min_length=1, max_length=2000, description="用户问题")

class ChatResponse(BaseModel):
    answer: str
    citations: list[str] = []
```

错误契约的统一设计（BizError 模式）：业务异常一律抛自定义异常，由全局异常处理器转为统一 JSON 格式：

```python
# app/core/exceptions.py
class BizError(Exception):
    def __init__(self, code: str, message: str, status: int = 400):
        self.code, self.message, self.status = code, message, status

class DocNotFoundError(BizError): ...      # 文档不存在
class SessionNotFoundError(BizError): ...  # 会话不存在
```

main.py 里注册全局处理器，捕获 BizError 返回 `{"code": ..., "message": ...}`（HTTP 400），其他异常记录日志后返回 500 通用格式。**错误契约的价值**：前端与测试只认 `code` 字段做分支，不解析中文 message——这在 07 篇测试、09 篇排错里都会用到。

**应用生命周期（lifespan）**：FastAPI 推荐的启动/关闭钩子是 lifespan 上下文管理器——启动时初始化数据库（04 篇 init_db）、加载 embedding/重排模型（06 篇）、构建 BM25 索引；关闭时优雅释放（关连接池、清缓存）：

```python
# app/main.py
from contextlib import asynccontextmanager
from fastapi import FastAPI
from app.repository.db import init_db
from app.rag.ingest import load_models
from app.api import chat, documents

@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db()                  # 启动：建表
    load_models()                    # 启动：预加载模型（首问不卡顿）
    yield
    # 关闭：释放资源（示例：embedding 模型卸载）

app = FastAPI(title="KBQA 知识库问答", version="1.0.0", lifespan=lifespan)
app.include_router(chat.router)
app.include_router(documents.router)
```

启动预热的价值：**模型加载最慢的 30-60 秒发生在启动期而不是首问期**——没有 lifespan 预热，第一个用户请求会卡成"事故现场"；有预热，健康检查通过即服务就绪。这是"上线体验"与"Demo 体验"的又一个分水岭。

## 5. SSE 流式接口

流式输出（打字机体验）在服务层的实现是 **SSE（Server-Sent Events）**——HTTP 长连接逐段推送文本。FastAPI 用 StreamingResponse 实现：

```python
from fastapi.responses import StreamingResponse

@router.post("/stream")
async def chat_stream(req: ChatRequest, service: ChatService = Depends(get_chat_service)):
    async def event_stream():
        async for chunk in service.ask_stream(req.session_id, req.question):
            yield f"data: {chunk}\n\n"      # SSE 协议：data: 前缀 + 双换行
    return StreamingResponse(event_stream(), media_type="text/event-stream")
```

要点：**生成器函数 yield 逐段**（内部是 05 篇 LangGraph 的流式回调或逐 token 调用）；**SSE 协议格式是 data: 前缀 + 双换行**（前端 EventSource 直接消费）；**结束标记**——流结束后发 `data: [DONE]`，前端据此收尾。SSE vs WebSocket 的选型：**单向推送用 SSE**（服务端生成→客户端展示，AI 对话就是这个场景），双向交互才用 WebSocket——这是 2026 年的共识选型，面试常问。

## 6. 服务化后的 Streamlit

Level1 的 Streamlit 直连代码改造为**只调 API 的瘦客户端**：

```python
import httpx, streamlit as st

API = "http://localhost:8000"

def ask(session_id: str, question: str) -> str:
    with httpx.Client(timeout=60) as client:
        resp = client.post(f"{API}/api/chat", json={"session_id": session_id, "question": question})
        resp.raise_for_status()
        return resp.json()["answer"]
```

改造收益：**界面层零业务逻辑**（Streamlit 只负责渲染与输入收集）；**接口即文档**（前端开发不依赖后端代码）；**会话 ID 由前端生成**（uuid4，传给接口，服务端按 ID 存取历史）。瘦客户端版 Streamlit 放 `client/` 目录，作为独立可启动进程（`streamlit run client/app.py`）——这也为 Level3 换成正式前端预留了契约。

启动与验证：`uv run uvicorn app.main:app --reload`（开发热重载）→ 浏览器开 http://localhost:8000/docs（自动接口文档，可直接在页面上试调所有接口）→ curl 冒烟三连（/health、/api/chat、/api/documents）。**/docs 是服务化改造的免费赠品**——FastAPI 根据 response_model 自动生成，测试接口、向别人演示、接口对接都靠它；它也是"契约即文档"的证据：模型定义对了，文档自动对。

## 7. 文档上传接口与 CORS

文档上传接口是服务化的又一个标准形态——**文件接收 + 业务处理 + 结果返回**三段式：

```python
# app/api/documents.py
from fastapi import APIRouter, File, UploadFile
from app.services.doc_service import DocService

router = APIRouter(prefix="/api/documents", tags=["documents"])

@router.post("")
async def upload(file: UploadFile = File(...), service: DocService = Depends(...)):
    content = (await file.read()).decode("utf-8", errors="replace")
    doc = await service.ingest(file.filename, content)   # 04/06 篇：落库+分块+入库
    return {"document_id": doc.id, "chunk_count": doc.chunk_count}
```

两个细节：**上传大小限制**（`max_size` 校验在 service 层——Demo 期不设也行，但文档里写明边界）；**UploadFile 的流式读取**（大文件不要 `await file.read()` 全量进内存——分块读或限大小，边界写进 06 篇分块的输入侧）。**CORS**：Streamlit（8501 端口）调 FastAPI（8000）属跨域，需在 main.py 挂 CORS 中间件（允许来源写 8501 与生产域名，白名单不是 `*`）。上传接口跑通后，Level1 的"上传文档 → 问答"闭环在服务化形态下完整复现——界面层只做转发，全部业务在服务层。

## 8. 常见坑

**async 滥用**：同步能力包 async 导致阻塞事件循环——`def` 路由（线程池）处理 CPU 型，`async def` 只给 IO 型。

**Depends 形同虚设**：写了 Depends 但函数内部又直接 import 单例——统一从参数取，测试才有缝可插。

**错误处理散落**：每个路由 try/except 各写各的——集中到全局处理器，路由层只抛异常。

**SSE 流式响应被缓冲**：Nginx 等反代未关缓冲会吞掉逐段输出——本地测试正常、部署后卡顿先查反代（09 篇）。

**响应模型与实现脱节**：改了内部字段忘改 response_model——接口契约以 response_model 为准，改字段必改模型，测试打接口兜底。

**请求体超大导致超时**：长文档直接进对话接口——上传走 /api/documents（分块处理），对话接口只收问题文本（Field 上限 2000），两种数据流的边界在接口设计上就划清。

**开发环境热重载异常**：--reload 与多 worker 互斥（--reload 只能单 worker）——开发单 worker 热重载、部署多 worker 不重载，两个启动命令分开写进 README。

**接口设计三连问**（每个新接口上线前自检）：**请求是否最小**（客户端只需传必要的字段）；**响应是否稳定**（response_model 固定，加字段不删字段）；**错误是否可处理**（错误契约 code 字段齐全，客户端可分支）。三问过完的接口才是"契约"，不过完的接口只是"临时路径"——契约意识是接口设计与测试（07 篇）的共同地基。接口变更的升级策略：**加字段兼容、删字段破坏**——要删字段先标记废弃（deprecated）过渡一个版本，前端有缓冲期；破坏性变更集中在版本号升级时做（/v2 前缀是最后手段）。

> 🎯 **核心要点**：服务化改造的核心产出是**稳定的 HTTP 契约**——请求校验在入口、错误格式在出口、依赖注入留测试缝、SSE 管流式。契约定好，界面、测试、部署三方都只认契约，项目从此"接口化生长"。

---

**下一模块**：[04 数据持久化与多会话](./04-数据持久化与多会话.md) | **返回总览**：[Level2 总览](./00-Level2%20工程化完整项目%20总览.md)

【参考来源】
- [FastAPI 官方文档](https://fastapi.tiangolo.com/)
- [FastAPI SSE Streaming 实现](https://fastapi.tiangolo.com/advanced/custom-response/#streamingresponse)
- [Python 异步 + FastAPI 体系（本仓库）](https://github.com/suxiangyu138/Java-AI-LLM-Learning-Records)
