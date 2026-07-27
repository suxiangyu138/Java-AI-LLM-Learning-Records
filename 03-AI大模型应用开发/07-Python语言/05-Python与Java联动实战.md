# 05 - Python 与 Java 联动实战

> 🎯 最优架构 = Java 管业务 + Python 跑 AI。三种联动方式：HTTP、进程调用、gRPC

## 1. FastAPI 服务（推荐）

```python
from fastapi import FastAPI
from pydantic import BaseModel
app = FastAPI()

class ChatRequest(BaseModel):
    prompt: str
    max_tokens: int = 512

@app.post("/chat")
async def chat(req: ChatRequest):
    result = llm.generate(req.prompt, req.max_tokens)
    return {"response": result}

# 启动：uvicorn server:app --port 8000
```

```java
// Java 端调用
RestTemplate rest = new RestTemplate();
String resp = rest.postForObject(
    "http://localhost:8000/chat",
    Map.of("prompt", "解释Java多态", "max_tokens", 512),
    String.class
);
```

## 2. 进程调用

```java
// Java 直接执行 Python 脚本
ProcessBuilder pb = new ProcessBuilder(
    "python", "scripts/embed.py", "--text", "Hello World"
);
Process p = pb.start();
String output = new String(p.getInputStream().readAllBytes());
```

```python
# scripts/embed.py
import argparse, json
parser = argparse.ArgumentParser()
parser.add_argument("--text", required=True)
args = parser.parse_args()

vec = embed_model.encode(args.text)
print(json.dumps(vec))  # stdout → Java 读取
```

## 3. gRPC（高性能内部通信）

```protobuf
service LLMService {
  rpc Chat (ChatRequest) returns (ChatResponse);
  rpc ChatStream (ChatRequest) returns (stream ChatChunk);
}
```

## 4. 混合架构推荐

```text
┌────────────────────────────────────────────┐
│  Java 后端（业务层）                         │
│  → 用户管理/权限/订单/数据库/缓存             │
│  → HTTP Client → Python AI 服务             │
├────────────────────────────────────────────┤
│  Python AI 服务（推理层）                     │
│  → FastAPI + LangChain + vLLM               │
│  → 模型推理 / RAG 检索 / Agent              │
└────────────────────────────────────────────┘

通信：HTTP/REST（通用）或 gRPC（高性能）
序列化：JSON（调试友好）或 Protobuf（高性能）
```
