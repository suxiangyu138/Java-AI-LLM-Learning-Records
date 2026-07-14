# AI 项目企业级部署与运维

> **核心认知**：AI 应用从 Demo 到生产，需要解决高并发、流式响应、异步任务、监控告警四大工程问题。
> **适用阶段**：阶段四/六 — 微调完成后、综合项目上线前

---

## 1. 高并发处理

### 1.1 LLM 调用的瓶颈

```
瓶颈不在你的服务器，而在 LLM API：
- API 响应延迟：2-10 秒（生成式模型）
- API 并发限制：通常 10-50 RPM（不同平台差异大）
- Token 成本：每次调用 0.001-0.1 元

策略：请求队列 + 连接池 + 缓存
```

### 1.2 异步任务队列（Celery）

```python
# tasks.py
from celery import Celery
import time

app = Celery('ai_tasks', broker='redis://localhost:6379/0')

@app.task(bind=True, max_retries=3, default_retry_delay=10)
def process_llm_request(self, prompt: str, model: str = "deepseek"):
    """异步 LLM 调用（不阻塞 API 响应）"""
    try:
        response = llm_client.chat(prompt, model=model)
        # 结果存入 Redis
        redis_client.setex(
            f"task:{self.request.id}",
            3600,
            response
        )
        return response
    except RateLimitError as e:
        # 速率限制 → 指数退避重试
        countdown = 2 ** self.request.retries * 5
        raise self.retry(exc=e, countdown=countdown)
    except Exception as e:
        logger.error(f"LLM task failed: {e}")
        raise
```

### 1.3 Python 异步方案（FastAPI + asyncio）

```python
import asyncio
from fastapi import FastAPI, BackgroundTasks
from contextlib import asynccontextmanager

app = FastAPI()

class LLMPool:
    """LLM 连接池——控制并发数"""

    def __init__(self, max_concurrent: int = 5):
        self.semaphore = asyncio.Semaphore(max_concurrent)

    async def chat(self, prompt: str) -> str:
        async with self.semaphore:
            # 实际 LLM 调用
            return await self._call_llm_async(prompt)


pool = LLMPool(max_concurrent=5)

@app.post("/api/chat")
async def chat(prompt: str, background_tasks: BackgroundTasks):
    """异步对话接口"""
    response = await pool.chat(prompt)
    return {"response": response}


@app.post("/api/chat/async")
async def chat_async(prompt: str, background_tasks: BackgroundTasks):
    """提交任务，立即返回 task_id，后续轮询结果"""
    task_id = str(uuid.uuid4())
    background_tasks.add_task(
        process_and_store, task_id, prompt
    )
    return {"task_id": task_id, "status": "processing"}


@app.get("/api/task/{task_id}")
async def get_result(task_id: str):
    result = redis_client.get(f"task:{task_id}")
    if result:
        return {"status": "completed", "result": result}
    return {"status": "processing"}
```

---

## 2. 流式响应（SSE）

### 2.1 FastAPI SSE

```python
from fastapi.responses import StreamingResponse
import json


@app.post("/api/chat/stream")
async def chat_stream(prompt: str):
    """流式对话接口（SSE）"""
    async def generate():
        stream = llm_client.chat_stream(prompt)
        for chunk in stream:
            yield f"data: {json.dumps({'content': chunk})}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(
        generate(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",  # 禁用 Nginx 缓冲
        }
    )
```

### 2.2 Spring Boot SSE（WebFlux）

```java
@GetMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> streamChat(@RequestParam String prompt) {
    return llmService.chatStream(prompt)
            .map(content -> ServerSentEvent.<String>builder()
                    .data(content)
                    .build())
            .timeout(Duration.ofSeconds(60))  // 超时保护
            .onErrorResume(e -> Flux.just(
                    ServerSentEvent.<String>builder()
                            .event("error")
                            .data("流式输出中断: " + e.getMessage())
                            .build()));
}
```

---

## 3. 缓存策略

### 3.1 多层缓存架构

```
请求 → L1 本地缓存 (Caffeine) → L2 分布式缓存 (Redis) → LLM API
       命中率 ~40%                 命中率 ~20%             命中剩余 40%
       延迟 < 1ms                  延迟 ~2ms              延迟 2-10s
```

### 3.2 语义缓存（同级问题命中）

```python
import hashlib
from sklearn.metrics.pairwise import cosine_similarity


class SemanticCache:
    """语义缓存——相似问题直接返回缓存结果"""

    def __init__(self, embed_model, threshold: float = 0.92):
        self.embed_model = embed_model
        self.threshold = threshold
        self.cache: dict[str, dict] = {}  # hash → {embedding, response}

    def get(self, query: str) -> Optional[str]:
        query_emb = self.embed_model.encode([query])[0]

        for key, entry in self.cache.items():
            cached_emb = entry["embedding"]
            similarity = cosine_similarity([query_emb], [cached_emb])[0][0]
            if similarity >= self.threshold:
                return entry["response"]

        return None

    def set(self, query: str, response: str):
        embedding = self.embed_model.encode([query])[0]
        key = hashlib.md5(query.encode()).hexdigest()
        self.cache[key] = {"embedding": embedding, "response": response}
```

### 3.3 精确缓存 + 语义缓存组合

```python
class HybridCache:
    """精确匹配 + 语义相似度 组合缓存"""

    def __init__(self):
        self.exact_cache = {}     # MD5(query) → response
        self.semantic_cache = SemanticCache(embed_model, threshold=0.92)

    def get(self, query: str) -> Optional[str]:
        query_hash = hashlib.md5(query.encode()).hexdigest()
        if query_hash in self.exact_cache:
            return self.exact_cache[query_hash]
        return self.semantic_cache.get(query)

    def set(self, query: str, response: str):
        query_hash = hashlib.md5(query.encode()).hexdigest()
        self.exact_cache[query_hash] = response
        self.semantic_cache.set(query, response)
```

---

## 4. 成本优化

### 4.1 模型路由（简单问题用小模型）

```python
class ModelRouter:
    """智能路由：简单问题用小模型，复杂问题用大模型"""

    CLASSIFIER_PROMPT = """判断以下用户问题的复杂度，只回答一个字母：
S = 简单（闲聊/问候/已知事实查询）
M = 中等（需要推理/多步骤分析）
H = 复杂（需要深度分析/代码生成/长文本创作）

问题：{query}
复杂度："""

    def __init__(self):
        self.small_model = "deepseek-chat"       # ¥0.001/千token
        self.large_model = "deepseek-reasoner"   # ¥0.004/千token

    def route(self, query: str) -> str:
        # 快速规则判断
        if len(query) < 50 and any(kw in query for kw in ["你好", "hi", "谢谢", "再见"]):
            return self.small_model

        # LLM 判断复杂度
        complexity = llm_client.chat(
            self.CLASSIFIER_PROMPT.format(query=query),
            model=self.small_model  # 用便宜模型判断
        ).strip()

        return self.large_model if complexity == "H" else self.small_model
```

### 4.2 Token 消耗追踪

```python
import tiktoken


class CostTracker:
    """实时追踪 Token 消耗与费用"""

    def __init__(self):
        self.encoder = tiktoken.get_encoding("cl100k_base")
        self.total_tokens = 0
        self.total_cost = 0.0
        self.calls = 0

        # 各模型单价（元/千token）
        self.pricing = {
            "deepseek-chat": {"input": 0.001, "output": 0.002},
            "deepseek-reasoner": {"input": 0.004, "output": 0.016},
            "qwen-plus": {"input": 0.002, "output": 0.006},
        }

    def count_tokens(self, text: str) -> int:
        return len(self.encoder.encode(text))

    def record(self, model: str, input_tokens: int, output_tokens: int):
        price = self.pricing.get(model, {"input": 0.002, "output": 0.008})
        cost = (input_tokens * price["input"] + output_tokens * price["output"]) / 1000
        self.total_tokens += input_tokens + output_tokens
        self.total_cost += cost
        self.calls += 1

    def report(self) -> dict:
        return {
            "total_calls": self.calls,
            "total_tokens": self.total_tokens,
            "total_cost_yuan": round(self.total_cost, 4),
            "avg_cost_per_call": round(self.total_cost / max(self.calls, 1), 6),
        }
```

---

## 5. 安全防护

### 5.1 Prompt 注入防御

```python
import re


class PromptInjectionGuard:
    """Prompt 注入检测与防御"""

    # 已知的注入模式
    INJECTION_PATTERNS = [
        r"忽略(所有|之前的|以上)(指令|规则|限制)",
        r"ignore (all |previous |above )(instructions?|rules?|constraints?)",
        r"你是.*(现在|从现在开始).*你是",
        r"you are now.*you are",
        r"<\|im_start\|>", r"<\|im_end\|>",
        r"\[INST\].*\[/INST\]",
        r"system:\s*$",  # 尝试覆盖 system prompt
    ]

    @classmethod
    def detect(cls, user_input: str) -> tuple[bool, str]:
        """检测是否包含注入尝试"""
        for pattern in cls.INJECTION_PATTERNS:
            if re.search(pattern, user_input, re.IGNORECASE):
                return True, f"检测到疑似 Prompt 注入: {pattern}"
        return False, ""

    @classmethod
    def sanitize(cls, user_input: str) -> str:
        """清洗用户输入中的特殊标记"""
        # 移除 LLM 控制标记
        cleaned = re.sub(r'<\|[^|]+\|>', '', user_input)
        cleaned = re.sub(r'\[/?INST\]', '', cleaned)
        # 截断过长输入
        if len(cleaned) > 4000:
            cleaned = cleaned[:4000] + "...(已截断)"
        return cleaned
```

### 5.2 结构化输出验证

```python
def safe_json_generation(prompt: str, expected_schema: dict) -> dict:
    """安全生成 JSON——验证输出符合预期格式"""
    full_prompt = f"""{prompt}

请严格按照以下 JSON Schema 输出，不要包含任何其他内容：
{json.dumps(expected_schema, ensure_ascii=False, indent=2)}"""

    response = llm_client.chat(full_prompt)

    try:
        # 提取 JSON
        match = re.search(r'```(?:json)?\s*(\{[\s\S]*?\})\s*```', response)
        if match:
            result = json.loads(match.group(1))
        else:
            result = json.loads(response)

        # 验证字段
        jsonschema.validate(result, expected_schema)
        return result

    except (json.JSONDecodeError, jsonschema.ValidationError) as e:
        raise ValueError(f"LLM 输出不符合预期格式: {e}")
```

---

## 6. 监控与告警

### 6.1 关键指标

```python
# 需要监控的指标
METRICS = {
    "latency_p50": "P50 响应延迟",
    "latency_p99": "P99 响应延迟",
    "error_rate": "错误率",
    "token_per_minute": "每分钟 Token 消耗",
    "cache_hit_rate": "缓存命中率",
    "rate_limit_hits": "限流触发次数",
}

import time
from collections import defaultdict
from contextlib import contextmanager


class LLMMetrics:
    """LLM 调用监控"""

    def __init__(self):
        self.latencies: list[float] = []
        self.errors = 0
        self.total = 0
        self.tokens_used = 0

    @contextmanager
    def measure(self):
        """上下文管理器——自动记录调用指标"""
        start = time.time()
        try:
            yield
        except Exception:
            self.errors += 1
            raise
        finally:
            self.total += 1
            self.latencies.append(time.time() - start)

    def snapshot(self) -> dict:
        if not self.latencies:
            return {}
        sorted_lat = sorted(self.latencies)
        return {
            "total_requests": self.total,
            "error_rate": f"{self.errors / max(self.total, 1):.1%}",
            "p50_latency": f"{sorted_lat[len(sorted_lat)//2]:.2f}s",
            "p99_latency": f"{sorted_lat[int(len(sorted_lat)*0.99)]:.2f}s",
            "avg_latency": f"{sum(self.latencies)/len(self.latencies):.2f}s",
        }
```

### 6.2 日志追踪

```python
import logging
import uuid

# 为每个请求生成 trace_id
logger = logging.getLogger("llm-service")


def log_llm_call(prompt: str, response: str, model: str, latency: float):
    trace_id = str(uuid.uuid4())[:8]
    logger.info(f"[{trace_id}] model={model} latency={latency:.2f}s "
                f"prompt_len={len(prompt)} resp_len={len(response)}")

    # 关键信息脱敏后记录
    if "error" in response.lower():
        logger.warning(f"[{trace_id}] Possible error in response: {response[:200]}")

# 生产环境建议使用 LangSmith / LangFuse
# pip install langfuse
# 接入 LangFuse 自动追踪 LLM 调用链路
```

---

## 7. 健康检查与优雅关闭

```python
from fastapi import FastAPI
import signal

app = FastAPI()

@app.get("/health")
def health():
    return {
        "status": "ok",
        "llm_api": check_llm_api(),       # LLM API 是否可达
        "redis": check_redis(),            # Redis 连接状态
        "queue_size": get_queue_size(),   # 任务队列积压数量
    }

@app.on_event("shutdown")
def graceful_shutdown():
    """优雅关闭——等待正在处理的请求完成"""
    logger.info("Shutting down...")
    pool.drain(timeout=30)  # 等待进行中的请求完成
    logger.info("Shutdown complete")


# 数据库连接池监控
@app.get("/health/db-pool")
def db_pool_status():
    return {
        "active": db_pool.active_connections,
        "idle": db_pool.idle_connections,
        "pending": db_pool.pending_requests,
        "max": db_pool.max_connections,
    }
```

---

## 快速调试检查清单

- [ ] 异步任务队列（Celery/Redis）是否正常启动？
- [ ] SSE 接口是否禁用了反向代理缓冲？（`X-Accel-Buffering: no`）
- [ ] 敏感信息（API Key、用户数据）是否在日志中脱敏？
- [ ] 是否设置了请求超时？（LLM 调用默认 30s，流式 60s）
- [ ] 健康检查接口是否可用？`/health` 返回 200？
- [ ] 缓存过期时间是否合理？（太短没效果，太长结果过时）
