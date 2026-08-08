# 08 - FastAPI × AI 应用实战

> 🎯 用 FastAPI 构建 AI 应用四大件：MCP Server、RAG API、Agent 后端、LLM 统一代理（OpenAI-Compatible API）

---

## 目录

1. [实战一：MCP Server](#1-实战一mcp-server)
2. [实战二：RAG 检索增强生成 API](#2-实战二rag-检索增强生成-api)
3. [实战三：Agent 后端服务](#3-实战三agent-后端服务)
4. [实战四：LLM 统一代理](#4-实战四llm-统一代理openai-compatible-api)

---

## 1. 实战一：MCP Server

### 1.1 完整 MCP Server 实现

```python
"""
用 FastAPI 构建 MCP Server — Streamable HTTP 传输
符合 MCP 2025-03-26 规范
"""
from fastapi import FastAPI, Request
from fastapi.responses import StreamingResponse, JSONResponse
from pydantic import BaseModel
from typing import Any
import json
import asyncio

app = FastAPI(title="Weather MCP Server", version="1.0.0")


# ===== MCP 数据模型 =====
class JSONRPCRequest(BaseModel):
    jsonrpc: str = "2.0"
    id: int | str | None = None
    method: str
    params: dict[str, Any] | None = None


class JSONRPCResponse(BaseModel):
    jsonrpc: str = "2.0"
    id: int | str | None = None
    result: Any | None = None


class JSONRPCError(BaseModel):
    jsonrpc: str = "2.0"
    id: int | str | None = None
    error: dict


# ===== 工具注册表 =====
MCP_TOOLS = [
    {
        "name": "get_weather",
        "description": "获取指定城市的实时天气信息。适用：用户询问天气、出行规划",
        "inputSchema": {
            "type": "object",
            "properties": {
                "city": {"type": "string", "description": "城市名称"},
                "unit": {
                    "type": "string",
                    "enum": ["celsius", "fahrenheit"],
                    "default": "celsius",
                },
            },
            "required": ["city"],
        },
        "annotations": {
            "readOnlyHint": True,
            "destructiveHint": False,
            "idempotentHint": True,
        },
    },
    {
        "name": "get_forecast",
        "description": "获取城市未来天气预报",
        "inputSchema": {
            "type": "object",
            "properties": {
                "city": {"type": "string"},
                "days": {"type": "integer", "minimum": 1, "maximum": 7, "default": 3},
            },
            "required": ["city"],
        },
        "annotations": {"readOnlyHint": True, "destructiveHint": False, "idempotentHint": True},
    },
    {
        "name": "send_weather_alert",
        "description": "发送天气预警通知。⚠️ 会向所有订阅用户推送通知！",
        "inputSchema": {
            "type": "object",
            "properties": {
                "city": {"type": "string", "description": "预警城市"},
                "level": {
                    "type": "string",
                    "enum": ["yellow", "orange", "red"],
                    "description": "预警级别：yellow=注意, orange=警惕, red=危险",
                },
                "message": {"type": "string", "description": "预警内容"},
            },
            "required": ["city", "level", "message"],
        },
        "annotations": {"readOnlyHint": False, "destructiveHint": True, "idempotentHint": False},
    },
]


# ===== MCP 请求处理 =====
@app.post("/mcp")
async def mcp_endpoint(request: Request):
    """MCP Streamable HTTP — 统一入口"""

    body = await request.json()
    rpc = JSONRPCRequest(**body)

    match rpc.method:
        case "initialize":
            return handle_initialize(rpc)
        case "tools/list":
            return handle_list_tools(rpc)
        case "tools/call":
            return await handle_call_tool(rpc)
        case "resources/list":
            return handle_list_resources(rpc)
        case "resources/read":
            return await handle_read_resource(rpc)
        case "ping":
            return JSONRPCResponse(id=rpc.id, result={}).model_dump()
        case _:
            return JSONRPCError(
                id=rpc.id,
                error={"code": -32601, "message": f"Method not found: {rpc.method}"},
            ).model_dump()


def handle_initialize(rpc: JSONRPCRequest) -> dict:
    """MCP 初始化握手"""
    return {
        "jsonrpc": "2.0",
        "id": rpc.id,
        "result": {
            "protocolVersion": rpc.params.get("protocolVersion", "2024-11-05"),
            "capabilities": {
                "tools": {"listChanged": True},
                "resources": {"subscribe": False, "listChanged": False},
                "logging": {},
            },
            "serverInfo": {
                "name": "weather-mcp-server",
                "version": "1.0.0",
            },
            "instructions": "此 Server 提供天气查询、预报和预警功能。",
        },
    }


def handle_list_tools(rpc: JSONRPCRequest) -> dict:
    return {
        "jsonrpc": "2.0",
        "id": rpc.id,
        "result": {"tools": MCP_TOOLS},
    }


async def handle_call_tool(rpc: JSONRPCRequest) -> dict:
    """工具调用 — 路由到具体实现"""
    tool_name = rpc.params["name"]
    arguments = rpc.params.get("arguments", {})

    try:
        match tool_name:
            case "get_weather":
                result = await get_weather(arguments)
            case "get_forecast":
                result = await get_forecast(arguments)
            case "send_weather_alert":
                result = await send_weather_alert(arguments)
            case _:
                result = {"isError": True, "content": [{"type": "text", "text": f"未知工具: {tool_name}"}]}

        return {"jsonrpc": "2.0", "id": rpc.id, "result": result}

    except Exception as e:
        return {
            "jsonrpc": "2.0",
            "id": rpc.id,
            "result": {
                "content": [{"type": "text", "text": f"❌ 执行失败: {str(e)}"}],
                "isError": True,
            },
        }


# ===== 工具实现 =====
async def get_weather(args: dict) -> dict:
    city = args["city"]
    unit = args.get("unit", "celsius")
    # ... 调用天气 API
    temp = 25 if unit == "celsius" else 77
    return {
        "content": [
            {"type": "text", "text": f"{city} 当前天气：晴，{temp}°{'C' if unit == 'celsius' else 'F'}"}
        ],
        "isError": False,
    }


async def get_forecast(args: dict) -> dict:
    city = args["city"]
    days = args.get("days", 3)
    return {
        "content": [{"type": "text", "text": f"{city} 未来{days}天预报：晴→多云→小雨"}],
        "isError": False,
    }


async def send_weather_alert(args: dict) -> dict:
    city = args["city"]
    level = args["level"]
    message = args["message"]
    # ... 发送通知
    emoji = {"yellow": "⚠️", "orange": "🔶", "red": "🔴"}
    return {
        "content": [{"type": "text", "text": f"{emoji[level]} 预警已发送到 {city}：{message}"}],
        "isError": False,
    }


def handle_list_resources(rpc: JSONRPCRequest) -> dict:
    return {
        "jsonrpc": "2.0",
        "id": rpc.id,
        "result": {
            "resources": [
                {
                    "uri": "weather://config/cities",
                    "name": "支持的城市列表",
                    "mimeType": "application/json",
                }
            ]
        },
    }


async def handle_read_resource(rpc: JSONRPCRequest) -> dict:
    return {
        "jsonrpc": "2.0",
        "id": rpc.id,
        "result": {
            "contents": [
                {
                    "uri": rpc.params["uri"],
                    "mimeType": "application/json",
                    "text": '["北京", "上海", "广州", "深圳"]',
                }
            ]
        },
    }
```

---

## 2. 实战二：RAG 检索增强生成 API

### 2.1 完整 RAG 服务

```python
"""
RAG 服务 — 文档上传 + 向量检索 + LLM 生成
"""
from fastapi import FastAPI, UploadFile, File, Depends, HTTPException
from fastapi.responses import StreamingResponse
import httpx
import json
import numpy as np

app = FastAPI(title="RAG API", version="1.0.0")

# 全局服务（实际应用中通过 DI 管理）
vector_store = VectorStoreService()
llm_client = httpx.AsyncClient()


# ===== 文档管理 =====
@app.post("/api/documents/upload")
async def upload_document(
    file: UploadFile = File(...),
    user: CurrentUser = Depends(get_current_user),
    background_tasks: BackgroundTasks = BackgroundTasks(),
):
    """上传文档 → 后台处理（分块 + Embedding + 入库）"""
    # 读取内容
    content = await file.read()
    text = content.decode("utf-8")

    # 保存文档元信息到 PG
    doc = await doc_service.create(
        user_id=user.id,
        filename=file.filename,
        content_type=file.content_type,
        size_bytes=len(content),
        status="processing",
    )

    # 后台处理
    background_tasks.add_task(process_document, doc.id, text)

    return {
        "doc_id": doc.id,
        "status": "processing",
        "message": "文档已接收，正在向量化处理中",
    }


@app.get("/api/documents")
async def list_documents(
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=100),
    user: CurrentUser = Depends(get_current_user),
):
    """列出用户的文档"""
    docs, total = await doc_service.list_by_user(user.id, page, size)
    return {
        "items": [{"id": d.id, "filename": d.filename, "status": d.status, "chunk_count": d.chunk_count} for d in docs],
        "total": total,
        "page": page,
    }


@app.delete("/api/documents/{doc_id}")
async def delete_document(
    doc_id: int,
    user: CurrentUser = Depends(get_current_user),
):
    """删除文档及其向量"""
    doc = await doc_service.get_by_id(doc_id)
    if not doc or doc.user_id != user.id:
        raise HTTPException(status_code=404, detail="文档不存在")
    await doc_service.delete(doc_id)
    # 同时删除向量数据
    vector_store.delete_by_doc_id(doc_id)
    return {"message": "已删除"}


# ===== RAG 检索 + 生成 =====
@app.post("/api/rag/chat")
async def rag_chat(
    request: ChatRequest,
    user: CurrentUser = Depends(get_current_user),
):
    """RAG 对话：检索相关文档 + LLM 生成回答"""

    query = request.messages[-1]["content"]

    # Step 1: 向量检索
    query_embedding = await embedding_service.embed(query)
    chunks = await vector_store.search(
        query_embedding,
        top_k=request.top_k or 5,
        score_threshold=request.score_threshold or 0.5,
    )

    if not chunks:
        return {"answer": "未找到相关文档片段，请上传相关文档或更换问题重试。", "sources": []}

    # Step 2: 构建 RAG Prompt
    context = "\n\n---\n\n".join([
        f"[来源 {i+1}] {c['content']}"
        for i, c in enumerate(chunks)
    ])

    system_prompt = f"""你是一个知识库助手。请基于以下文档内容回答问题。

## 文档内容
{context}

## 回答要求
- 只基于提供的文档内容回答
- 如果文档内容不足以回答，请明确说明
- 引用具体来源（使用 [来源 N] 格式）
- 保持回答简洁准确"""

    # Step 3: 调用 LLM
    response = await llm_client.post(
        f"{LLM_BASE_URL}/v1/chat/completions",
        json={
            "model": "gpt-4o",
            "messages": [
                {"role": "system", "content": system_prompt},
                *request.messages,
            ],
            "temperature": 0.3,  # RAG 用低温度保证准确性
        },
        headers={"Authorization": f"Bearer {LLM_API_KEY}"},
    )

    data = response.json()

    return {
        "answer": data["choices"][0]["message"]["content"],
        "sources": [
            {"content": c["content"][:200], "score": c["score"], "metadata": c.get("metadata", {})}
            for c in chunks
        ],
        "tokens": data.get("usage"),
    }
```

---

## 3. 实战三：Agent 后端服务

### 3.1 Agent 状态机

```python
"""
AI Agent 后端 — 状态机 + 工具编排 + 流式事件
"""
from enum import Enum
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from sse_starlette.sse import EventSourceResponse
import asyncio


class AgentState(str, Enum):
    """Agent 执行状态"""
    IDLE = "idle"
    PLANNING = "planning"       # 正在规划步骤
    EXECUTING = "executing"     # 正在执行工具
    WAITING_CONFIRM = "waiting_confirm"  # 等待用户确认
    DONE = "done"
    ERROR = "error"


class AgentService:
    """Agent 核心逻辑"""

    def __init__(self, llm_client, tool_registry):
        self.llm = llm_client
        self.tools = tool_registry

    async def run(self, task: str, event_queue: asyncio.Queue, session: dict):
        """Agent 主循环 — 规划 → 执行 → 反思"""
        messages = [{"role": "user", "content": task}]
        state = AgentState.PLANNING

        for iteration in range(10):  # 最多 10 轮
            # Step 1: LLM 规划下一步
            await event_queue.put({
                "event": "state_change",
                "data": json.dumps({"state": state.value, "iteration": iteration}),
            })

            response = await self.llm.post(
                f"{LLM_BASE_URL}/v1/chat/completions",
                json={
                    "model": "gpt-4o",
                    "messages": messages,
                    "tools": self.tools.get_openai_schemas(),
                    "tool_choice": "auto",
                },
            )

            msg = response.json()["choices"][0]["message"]

            # 不需要工具 → 完成
            if not msg.get("tool_calls"):
                await event_queue.put({
                    "event": "done",
                    "data": json.dumps({"answer": msg["content"]}),
                })
                return msg["content"]

            # 需要工具 → 执行
            for tool_call in msg["tool_calls"]:
                tool_name = tool_call["function"]["name"]
                args = json.loads(tool_call["function"]["arguments"])

                # 推送状态
                state = AgentState.EXECUTING
                await event_queue.put({
                    "event": "tool_start",
                    "data": json.dumps({"tool": tool_name, "args": args}),
                })

                # 执行工具
                try:
                    result = await self.tools.execute(tool_name, args)
                except Exception as e:
                    result = f"Error: {str(e)}"
                    await event_queue.put({
                        "event": "tool_error",
                        "data": json.dumps({"tool": tool_name, "error": str(e)}),
                    })

                await event_queue.put({
                    "event": "tool_end",
                    "data": json.dumps({"tool": tool_name, "result": str(result)[:500]}),
                })

                # 将工具调用结果加入对话
                messages.append(msg)
                messages.append({
                    "role": "tool",
                    "tool_call_id": tool_call["id"],
                    "content": str(result),
                })


# ===== SSE 端点：Agent 进度推送 =====
agent_service = AgentService(llm_client, tool_registry)


@app.get("/api/agent/{session_id}/run")
async def agent_run(session_id: str, task: str, request: Request):
    """Agent 执行 — SSE 进度推送"""

    async def run_with_events():
        queue = asyncio.Queue()

        # 启动 Agent 执行
        run_task = asyncio.create_task(
            agent_service.run(task, queue, {"session_id": session_id})
        )

        try:
            while True:
                if await request.is_disconnected():
                    run_task.cancel()
                    break

                try:
                    event = await asyncio.wait_for(queue.get(), timeout=30.0)
                except asyncio.TimeoutError:
                    yield {"event": "heartbeat", "data": ""}
                    continue

                yield event

                data = json.loads(event["data"])
                if event["event"] in ("done", "error"):
                    break
        finally:
            run_task.cancel()

    return EventSourceResponse(run_with_events())
```

---

## 4. 实战四：LLM 统一代理（OpenAI-Compatible API）

### 4.1 多模型代理

```python
"""
LLM 统一代理 — 对外暴露 OpenAI-Compatible API
对内路由到 OpenAI / Anthropic / DeepSeek / 本地模型
"""
from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse


@app.post("/v1/chat/completions")
async def chat_completions(
    request: ChatCompletionRequest,
    api_key: str = Depends(api_key_header),
):
    """
    OpenAI-Compatible Chat Completions 端点
    支持的 model 格式：
      • openai/gpt-4o
      • anthropic/claude-sonnet-5
      • deepseek/deepseek-v4-pro
    """

    # 解析模型提供者
    provider, model = parse_model_string(request.model)  # "openai/gpt-4o" → ("openai", "gpt-4o")

    # 路由到不同提供者
    match provider:
        case "openai":
            handler = openai_handler
        case "anthropic":
            handler = anthropic_handler
        case "deepseek":
            handler = deepseek_handler
        case _:
            raise HTTPException(400, f"不支持的模型提供者: {provider}")

    if request.stream:
        return StreamingResponse(
            handler.stream(request, model),
            media_type="text/event-stream",
        )
    else:
        return await handler.call(request, model)


# ===== Anthropic Adapter =====
class AnthropicAdapter:
    """OpenAI 请求 → Anthropic 请求"""

    BASE_URL = "https://api.anthropic.com/v1/messages"

    @staticmethod
    def convert_messages(openai_messages: list[dict]) -> tuple[str | None, list[dict]]:
        """转换消息格式"""
        system = None
        messages = []

        for msg in openai_messages:
            role = msg["role"]
            if role == "system":
                system = msg["content"]
            elif role in ("user", "assistant"):
                messages.append({"role": role, "content": msg["content"]})
            elif role == "tool":
                # Anthropic 格式不同，需要特殊处理
                pass

        return system, messages

    @staticmethod
    async def call(request: ChatCompletionRequest, model: str) -> dict:
        system, messages = AnthropicAdapter.convert_messages(request.messages)

        body = {
            "model": model,
            "max_tokens": request.max_tokens or 4096,
            "messages": messages,
            "temperature": request.temperature,
        }
        if system:
            body["system"] = system

        async with httpx.AsyncClient() as client:
            resp = await client.post(
                AnthropicAdapter.BASE_URL,
                headers={"x-api-key": ANTHROPIC_API_KEY, "anthropic-version": "2023-06-01"},
                json=body,
                timeout=120.0,
            )

            data = resp.json()

            # Anthropic 响应 → OpenAI 格式
            return {
                "id": data["id"],
                "object": "chat.completion",
                "created": int(datetime.now().timestamp()),
                "model": request.model,
                "choices": [{
                    "index": 0,
                    "message": {
                        "role": "assistant",
                        "content": data["content"][0]["text"],
                    },
                    "finish_reason": data.get("stop_reason", "stop"),
                }],
                "usage": {
                    "prompt_tokens": data["usage"]["input_tokens"],
                    "completion_tokens": data["usage"]["output_tokens"],
                    "total_tokens": data["usage"]["input_tokens"] + data["usage"]["output_tokens"],
                },
            }

    @staticmethod
    async def stream(request: ChatCompletionRequest, model: str):
        """Anthropic 流式转换"""
        system, messages = AnthropicAdapter.convert_messages(request.messages)

        async with httpx.AsyncClient() as client:
            async with client.stream(
                "POST",
                AnthropicAdapter.BASE_URL,
                headers={"x-api-key": ANTHROPIC_API_KEY, "anthropic-version": "2023-06-01"},
                json={"model": model, "max_tokens": request.max_tokens or 4096, "messages": messages, "stream": True},
            ) as resp:
                async for line in resp.aiter_lines():
                    if line.startswith("data: "):
                        data = json.loads(line[6:])
                        # Anthropic SSE → OpenAI SSE 格式
                        if data.get("type") == "content_block_delta":
                            delta_text = data["delta"].get("text", "")
                            yield f"data: {json.dumps({'choices': [{'delta': {'content': delta_text}}]})}\n\n"
                yield "data: [DONE]\n\n"


# 注册适配器
from functools import partial

async def anthropic_handler(request, model):
    return await AnthropicAdapter.call(request, model)

# 同理实现 DeepSeekAdapter、OpenAIAdapter...
```

> 🎯 **核心要点**：FastAPI 在 AI 应用中的四大实战角色 — MCP Server（协议实现）、RAG API（检索+生成）、Agent 后端（状态机+SSE推送）、LLM 代理（多模型统一接入）。核心能力是异步流式 + Pydantic 模型校验 + SSE/WebSocket 实时通信

---

**上一模块**：[07 - 测试与部署](./07-测试与部署.md)  
**返回总览**：[00 - FastAPI 知识体系总览](./00-FastAPI知识体系总览.md)
