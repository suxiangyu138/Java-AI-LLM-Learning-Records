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
