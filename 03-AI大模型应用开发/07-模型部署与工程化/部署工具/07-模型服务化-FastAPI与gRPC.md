# 07 - 模型服务化：FastAPI 与 gRPC

> 🎯 模型部署的最后一公里 — FastAPI 快速搭建 REST API、gRPC 高性能通信、SSE 流式输出

### FastAPI REST 服务

```python
from fastapi import FastAPI
from pydantic import BaseModel
import uvicorn

app = FastAPI()

class ChatRequest(BaseModel):
    prompt: str
    max_tokens: int = 512
    temperature: float = 0.7

@app.post("/chat")
async def chat(req: ChatRequest):
    response = llm.generate(req.prompt, req.max_tokens, req.temperature)
    return {"response": response}

# 流式输出 (SSE)
from fastapi.responses import StreamingResponse
@app.post("/chat/stream")
async def chat_stream(req: ChatRequest):
    async def generate():
        async for token in llm.generate_stream(req.prompt):
            yield f"data: {token}\n\n"
    return StreamingResponse(generate(), media_type="text/event-stream")

uvicorn.run(app, host="0.0.0.0", port=8000, workers=4)
```

### gRPC 高性能通信

```protobuf
service LLMService {
  rpc Chat (ChatRequest) returns (ChatResponse);
  rpc ChatStream (ChatRequest) returns (stream ChatChunk);
}
```

### 并发与性能

| 方案 | 协议 | 序列化 | 延迟 | 适用 |
|------|:---:|------|:---:|------|
| FastAPI | HTTP/1.1 | JSON | 低 | 通用 |
| FastAPI SSE | HTTP/1.1 | Text | 低 | 流式输出 |
| gRPC | HTTP/2 | Protobuf | 极低 | 内部服务 |

```python
# 异步并发处理
from concurrent.futures import ThreadPoolExecutor
executor = ThreadPoolExecutor(max_workers=10)

@app.post("/chat")
async def chat(req: ChatRequest):
    loop = asyncio.get_event_loop()
    result = await loop.run_in_executor(executor, llm.generate, req.prompt)
    return {"response": result}
```

---

## 6. LLM 服务化的生产要点（2026）

**流式输出（SSE）的完整实现**：

```python
from fastapi import FastAPI
from fastapi.responses import StreamingResponse

app = FastAPI()

@app.post("/v1/chat/completions")
async def chat(req: dict):
    async def generate():
        async for token in llm.stream(req["messages"]):   # 引擎流式接口
            yield f"data: {token}\n\n"                    # SSE 格式
        yield "data: [DONE]\n\n"
    return StreamingResponse(generate(), media_type="text/event-stream")
# 客户端：fetch 流式读取 / OpenAI SDK 原生支持
```

**生产服务化四件套**：

| 要素 | 说明 |
|------|------|
| 流式 | SSE（首 token 先行，感知延迟关键） |
| 并发 | 异步（asyncio）+ 引擎自身批处理（vLLM 连续批处理） |
| 超时 | 生成超时（长响应）+ 空闲超时 |
| OpenAI 兼容 | /v1/chat/completions 标准化（vLLM 原生提供，别自造） |

**gRPC vs REST 选择（2026）**：

```text
REST + SSE：LLM 标准（OpenAI 兼容生态、流式友好）→ 默认
gRPC：内部服务间高吞吐（非流式场景）、强类型
流式 gRPC：服务端流（高吞吐流式）——框架支持成熟后可选
```

> 🎯 **核心要点**：LLM 服务化 = "**OpenAI 兼容 API + SSE 流式 + 引擎批处理**"三件套——**"别自造 API 格式"（vLLM 原生 OpenAI 兼容）是 2026 铁律**；感知延迟靠流式（TTFT），吞吐靠引擎（批处理）。

---

## 7. 服务化架构速查（2026）

**LLM 服务的完整调用链**：

```text
客户端（前端/后端）
  → API 网关（鉴权/限流/路由）
  → 推理服务（FastAPI 包装 vLLM/Ollama）
  → 推理引擎（批处理/KV Cache）
  → GPU

关键：推理服务应该"薄"——业务逻辑在网关/应用层，
      推理服务只做"模型调用 + 流式转发"
```

**并发与性能速查**：

| 要素 | 最佳实践 |
|------|---------|
| 并发模型 | asyncio 异步（FastAPI 原生） |
| 引擎对接 | 直接调 vLLM HTTP API（别再包一层） |
| 限流 | 网关层（按用户/按 token） |
| 背压 | 引擎排队时返回 429（让客户端重试） |
| 超时 | 生成超时 60-120s（长响应） |

**REST vs gRPC 决策表**：

```text
对外/流式 → REST + SSE（OpenAI 兼容，生态标准）
内部非流式高吞吐 → gRPC（protobuf 强类型）
流式高吞吐（内部）→ gRPC 服务端流
结论：2026 默认 REST + SSE（别为"高性能"自造协议）
```

> 🎯 **核心要点**：服务化架构 = "**薄服务（只做转发）+ 网关（鉴权限流）+ 引擎直连（不重复包装）**"——**vLLM 已提供 OpenAI 兼容 API，业务服务别再造轮子**。

---

## 8. 完整服务化示例速查

**一个完整的 LLM 推理服务**（FastAPI + vLLM）：

```python
from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse
import httpx

app = FastAPI()
VLLM_URL = "http://vllm-service:8000/v1/chat/completions"

@app.post("/v1/chat/completions")
async def chat(req: dict):
    # ① 参数校验
    if not req.get("messages"):
        raise HTTPException(400, "messages 必填")
    # ② 流式转发到 vLLM（薄服务：只转发不加工）
    async with httpx.AsyncClient(timeout=120) as client:
        async with client.stream("POST", VLLM_URL, json={**req, "stream": True}) as resp:
            async def gen():
                async for chunk in resp.aiter_bytes():
                    yield chunk
            return StreamingResponse(gen(), media_type="text/event-stream")

@app.get("/health")
async def health():
    return {"status": "ok"}
```

**生产化检查清单**：

```text
□ OpenAI 兼容 API（/v1/chat/completions + /v1/models）
□ 流式（SSE）与非流式双支持
□ 参数透传（temperature/max_tokens 等）
□ 鉴权（API Key 校验）
□ 限流（按用户/按 token）
□ 日志（request_id + 输入输出记录，注意脱敏）
□ 超时（生成 120s）+ 错误码规范（429/500）
□ 健康检查（/health + /metrics）
```

> 🎯 **核心要点**：薄服务模式 = "**校验 → 流式转发 → 透传**"——业务逻辑放网关/应用层，推理服务保持纯净；八项生产化检查清单逐项过一遍即可上线。

---

## 9. 常见问题速查

| 问题 | 原因 | 解法 |
|------|------|------|
| 流式无输出 | SSE 格式错误/缓冲 | 确认 yield + media_type + 关闭代理缓冲 |
| 并发慢 | 引擎无批处理 | 用 vLLM（连续批处理）而非单请求引擎 |
| 超时中断 | 长响应 | 生成超时 120s + 客户端重试策略 |
| 内存泄漏 | 请求对象未释放 | 检查异步任务生命周期 |
| 模型切换 | 需重载 | 多模型实例 + 网关路由（灰度思路） |

**性能验证**：

```text
① 单请求 TTFT/TPOT（流式实测）
② 并发压测（50/100/200 并发 → P95）
③ 引擎 vs 服务层瓶颈定位（指标分层）
④ 长上下文压测（KV 显存上限验证）
```

> 🎯 **核心要点**：服务化排障 = "**流式（SSE 格式/缓冲）+ 引擎（批处理）+ 超时（120s）**"三大件——**"服务层慢先查引擎是否批处理"**是 LLM 服务性能排查的第一问。
