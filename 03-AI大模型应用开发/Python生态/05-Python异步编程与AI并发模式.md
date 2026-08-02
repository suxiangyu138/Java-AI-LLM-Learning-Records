# 05 - Python 异步编程与 AI 并发模式

> 🎯 LLM API 调用是 IO 密集型 — asyncio 能让你同时发 100 个请求而不用等任何一个。本章覆盖 asyncio + LLM 并发调用的实战模式：semaphore 限流、gather 并发、流式处理回退方案

---

## 目录

1. [为什么 AI 应用需要异步](#1-为什么-ai-应用需要异步)
2. [asyncio 基础：LLM 场景三件套](#2-asyncio-基础llm-场景三件套)
3. [LLM 并发调用模式](#3-llm-并发调用模式)
4. [流式处理与并发结合](#4-流式处理与并发结合)
5. [FastAPI 集成：异步 Web 服务](#5-fastapi-集成异步-web-服务)

---

## 1. 为什么 AI 应用需要异步

```text
同步模式（一个接一个）：
  查GPT → 等3秒 → 查Claude → 等2秒 → 查Gemini → 等1.5秒
  总耗时 = 6.5 秒

异步模式（同时发三个请求）：
  查GPT ═══3秒═══╗
  查Claude ══2秒══╬→ 三者并发，同时等
  查Gemini ═1.5s═╝
  总耗时 = max(3, 2, 1.5) = 3 秒
```

LLM API 延迟 0.5-10 秒 → IO 密集型场景 → **asyncio 是标配**。

---

## 2. asyncio 基础：LLM 场景三件套

```python
import asyncio

# ① async def + await：定义和等待协程
async def call_llm(prompt: str) -> str:
    # 模拟 LLM API 调用
    await asyncio.sleep(2)   # 假设 2 秒响应
    return f"Response for: {prompt[:20]}..."

# ② asyncio.gather：并发执行多个协程
async def batch_queries(prompts: list[str]) -> list[str]:
    results = await asyncio.gather(
        *[call_llm(p) for p in prompts]
    )
    return results

# ③ asyncio.create_task：创建后台任务（不阻塞当前流程）
task = asyncio.create_task(call_llm("long query"))
# 主流程继续... 稍后 await task
```

---

## 3. LLM 并发调用模式

### 3.1 Semaphore 限流（防 API 限流/爆内存）

```python
async def call_with_limit(prompts: list[str], max_concurrent: int = 10):
    sem = asyncio.Semaphore(max_concurrent)

    async def bounded_call(prompt):
        async with sem:          # 获取许可（满 10 个就等）
            return await call_llm(prompt)

    results = await asyncio.gather(*[bounded_call(p) for p in prompts])
    return results
```

### 3.2 超时兜底（一个慢不拖全部）

```python
async def call_with_timeout(prompt: str, timeout: float = 30.0):
    try:
        return await asyncio.wait_for(
            call_llm(prompt), timeout=timeout
        )
    except asyncio.TimeoutError:
        return "请求超时，请重试"
```

### 3.3 多模型投票（同时调多个模型，取最优）

```python
async def multi_model_vote(prompt: str) -> str:
    models = [call_gpt(prompt), call_claude(prompt), call_gemini(prompt)]
    results = await asyncio.gather(*models, return_exceptions=True)
    # 过滤异常结果，投票/取最一致的
    valid = [r for r in results if not isinstance(r, Exception)]
    return max(set(valid), key=valid.count)   # 多数投票
```

---

## 4. 流式处理与并发结合

```python
# 流式 SSE 消费（不阻塞其他请求）
async def stream_llm(prompt: str, queue: asyncio.Queue):
    async for chunk in llm_client.stream(prompt):
        await queue.put(chunk)
    await queue.put(None)  # 结束标志

async def consumer(queue):
    async for chunk in iter_queue(queue):
        yield f"data: {chunk}\n\n"
```

---

## 5. FastAPI 集成：异步 Web 服务

```python
from fastapi import FastAPI
from contextlib import asynccontextmanager

# 启动时预加载连接池；全局共享 semaphore
@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.semaphore = asyncio.Semaphore(20)
    yield

app = FastAPI(lifespan=lifespan)

@app.post("/chat")
async def chat(prompt: str):
    async with app.state.semaphore:
        # FastAPI 自动处理 asyncio — 不阻塞其他请求
        response = await call_llm(prompt)
    return {"response": response}

@app.post("/batch")
async def batch(prompts: list[str]):
    # 同时处理多 prompt
    results = await call_with_limit(prompts, max_concurrent=10)
    return {"results": results}
```

**追问：** FastAPI 为什么适合 AI 服务？→ 原生 asyncio + 自动 OpenAPI 文档 + 流式 SSE 支持 + 与 Pydantic 无缝集成。

---

> 🎯 **核心要点**：AI 应用的 Python 并发三件套 — **① asyncio.gather 并发 ② Semaphore 限流（防 API 限流/爆内存）③ FastAPI 异步端点（不阻塞其他请求）**。所有 LLM SDK（openai/anthropic）都提供 async 客户端，务必用 `AsyncOpenAI` 而不是在同步方法里 `asyncio.run()`。

**下一模块**：[06-AI应用评估与可观测性](06-AI应用评估与可观测性.md) / **返回总览**：[00-生态总览](00-Python生态知识体系总览.md)
