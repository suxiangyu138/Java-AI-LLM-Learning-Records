# AI 服务企业级工程化：高并发、安全与监控

> **所属阶段**：阶段六 — 企业级工程实践（持续）
> **前置知识**：大模型 API 调用、Web 后端基础
> **核心目标**：将 AI 原型变为生产级服务

---

## 1. 工程化全景

```
                     ┌──────────────┐
                     │   负载均衡     │
                     │  Nginx/K8s   │
                     └──────┬───────┘
                            │
              ┌─────────────┼─────────────┐
              │             │             │
        ┌─────▼─────┐ ┌────▼────┐ ┌─────▼─────┐
        │ AI 服务实例1│ │ AI 服务2 │ │ AI 服务实例3│
        └─────┬─────┘ └────┬────┘ └─────┬─────┘
              │             │             │
              └─────────────┼─────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
   ┌────▼────┐      ┌──────▼──────┐      ┌─────▼─────┐
   │  Redis  │      │  消息队列     │      │   MySQL   │
   │  缓存    │      │  RabbitMQ    │      │  持久化    │
   └─────────┘      └─────────────┘      └───────────┘
```

---

## 2. 高并发处理

### 2.1 异步任务队列

```python
# 使用 Celery 处理耗时 AI 任务
# tasks.py
from celery import Celery
from openai import OpenAI
import os

app = Celery("ai_tasks", broker=os.getenv("CELERY_BROKER", "redis://localhost:6379/0"))

client = OpenAI(api_key=os.getenv("API_KEY"), base_url=os.getenv("API_BASE"))

@app.task(bind=True, max_retries=3)
def generate_report(self, prompt, params=None):
    """异步生成报告任务"""
    try:
        response = client.chat.completions.create(
            model="deepseek-chat",
            messages=[{"role": "user", "content": prompt}],
            temperature=params.get("temperature", 0.3) if params else 0.3
        )
        return response.choices[0].message.content
    except Exception as e:
        self.retry(exc=e, countdown=60)  # 1 分钟后重试

# 调用
result = generate_report.delay("分析本季度系统性能趋势")
# 通过 result.id 轮询状态
```

### 2.2 流式响应 + 连接池

```python
import httpx
from typing import AsyncGenerator

class AIClientPool:
    """AI API 连接池 — 复用连接，减少开销"""

    def __init__(self, base_url, api_key, pool_size=10):
        self.client = httpx.AsyncClient(
            base_url=base_url,
            headers={"Authorization": f"Bearer {api_key}"},
            timeout=60.0,
            limits=httpx.Limits(
                max_connections=pool_size,
                max_keepalive_connections=pool_size
            )
        )

    async def stream_chat(self, model, messages) -> AsyncGenerator[str, None]:
        """异步流式对话"""
        async with self.client.stream(
            "POST", "/v1/chat/completions",
            json={"model": model, "messages": messages, "stream": True}
        ) as response:
            async for line in response.aiter_lines():
                if line.startswith("data: ") and line != "data: [DONE]":
                    import json
                    chunk = json.loads(line[6:])
                    if chunk["choices"][0]["delta"].get("content"):
                        yield chunk["choices"][0]["delta"]["content"]

    async def close(self):
        await self.client.aclose()
```

### 2.3 限流与排队

```python
import asyncio
import time
from collections import deque

class RateLimiter:
    """滑动窗口限流器"""

    def __init__(self, max_requests, window_seconds):
        self.max_requests = max_requests
        self.window = window_seconds
        self.requests = deque()

    async def acquire(self):
        while True:
            now = time.time()
            # 清理过期记录
            while self.requests and self.requests[0] < now - self.window:
                self.requests.popleft()

            if len(self.requests) < self.max_requests:
                self.requests.append(now)
                return

            # 排队等待
            wait_time = self.requests[0] + self.window - now + 0.1
            await asyncio.sleep(wait_time)


# 使用
limiter = RateLimiter(max_requests=10, window_seconds=60)  # 每分钟 10 次

async def rate_limited_chat(prompt):
    await limiter.acquire()
    return client.chat.completions.create(...)
```

---

## 3. 成本优化

### 3.1 Prompt 缓存

```python
import hashlib
import redis

class PromptCache:
    """Redis 缓存相似问题的回答"""

    def __init__(self, redis_client, ttl=3600):
        self.redis = redis_client
        self.ttl = ttl

    def _key(self, prompt):
        return f"cache:{hashlib.md5(prompt.encode()).hexdigest()}"

    def get(self, prompt):
        return self.redis.get(self._key(prompt))

    def set(self, prompt, answer):
        self.redis.setex(self._key(prompt), self.ttl, answer)


# 使用
cache = PromptCache(redis_client)

def cached_chat(prompt, client):
    cached = cache.get(prompt)
    if cached:
        return cached.decode()
    answer = client.chat.completions.create(...)
    cache.set(prompt, answer)
    return answer
```

### 3.2 模型路由

```python
class ModelRouter:
    """根据问题复杂度路由到不同模型 — 控制成本"""

    ROUTES = {
        "easy": "deepseek-chat",      # 简单问题用小模型
        "medium": "deepseek-chat",
        "hard": "deepseek-reasoner",  # 复杂问题用推理模型
    }

    def classify(self, prompt):
        """快速判断问题复杂度"""
        if len(prompt) < 50 and "?" not in prompt:
            return "easy"
        if any(kw in prompt for kw in ["分析", "架构", "设计", "方案"]):
            return "hard"
        return "medium"

    def route(self, prompt, client):
        level = self.classify(prompt)
        model = self.ROUTES[level]
        # 简单问题可降低 max_tokens
        return client.chat.completions.create(
            model=model,
            messages=[{"role": "user", "content": prompt}],
            max_tokens=256 if level == "easy" else 2048
        )
```

### 3.3 成本计算

```python
PRICING = {
    "deepseek-chat": {"input": 0.001, "output": 0.002},   # 元/1K tokens
    "qwen-turbo": {"input": 0.003, "output": 0.006},
    "gpt-4o": {"input": 0.015, "output": 0.060},
}

def estimate_cost(model, input_tokens, output_tokens):
    price = PRICING.get(model, PRICING["deepseek-chat"])
    return (input_tokens * price["input"] + output_tokens * price["output"]) / 1000
```

---

## 4. 安全防护

### 4.1 Prompt 注入防御

```python
import re

class PromptGuard:
    """Prompt 注入检测"""

    INJECTION_PATTERNS = [
        r"忽略.*指令",
        r"ignore.*instruction",
        r"system:\s*",
        r"你.*是.*新.*角色",
        r"forget.*previous",
        r"切换.*身份",
    ]

    @classmethod
    def detect(cls, user_input):
        for pattern in cls.INJECTION_PATTERNS:
            if re.search(pattern, user_input, re.IGNORECASE):
                return True, f"检测到潜在注入: 匹配模式 '{pattern}'"
        return False, "安全"

    @classmethod
    def sanitize(cls, user_input):
        """清理用户输入"""
        # 截断过长输入
        if len(user_input) > 8000:
            user_input = user_input[:8000] + "..."
        # 移除控制字符
        user_input = re.sub(r'[\x00-\x08\x0b\x0c\x0e-\x1f]', '', user_input)
        return user_input

# 在 API 入口使用
@app.route("/chat", methods=["POST"])
def chat():
    user_input = request.json.get("prompt")

    # 注入检测
    is_attack, reason = PromptGuard.detect(user_input)
    if is_attack:
        return jsonify({"error": "Invalid input"}), 400

    # 清理
    safe_input = PromptGuard.sanitize(user_input)
    # ...
```

### 4.2 内容安全

```python
class ContentFilter:
    """敏感内容过滤"""

    SENSITIVE_KEYWORDS = [
        # 政治敏感词列表（根据业务需求配置）
    ]

    @classmethod
    def check_output(cls, text):
        """检测模型输出是否包含敏感内容"""
        for kw in cls.SENSITIVE_KEYWORDS:
            if kw in text:
                return False, f"输出包含敏感词"
        return True, "安全"
```

### 4.3 访问控制

```python
from functools import wraps
import time

def rate_limit(max_per_minute=30):
    """装饰器：接口级限流"""
    records = {}

    def decorator(f):
        @wraps(f)
        def wrapper(*args, **kwargs):
            user_ip = request.remote_addr
            now = time.time()

            if user_ip not in records:
                records[user_ip] = []

            # 清理 1 分钟前的记录
            records[user_ip] = [t for t in records[user_ip] if now - t < 60]

            if len(records[user_ip]) >= max_per_minute:
                return jsonify({"error": "Too many requests"}), 429

            records[user_ip].append(now)
            return f(*args, **kwargs)
        return wrapper
    return decorator

@app.route("/api/chat", methods=["POST"])
@rate_limit(max_per_minute=30)
def chat():
    # ...
```

---

## 5. 监控运维

### 5.1 日志与追踪

```python
import logging
import time
import uuid
from contextvars import ContextVar

request_id_var = ContextVar("request_id", default="unknown")

# 结构化日志
logger = logging.getLogger("ai_service")
handler = logging.StreamHandler()
handler.setFormatter(logging.Formatter(
    '{"time": "%(asctime)s", "level": "%(levelname)s", '
    '"request_id": "%(request_id)s", "message": "%(message)s"}'
))
# 添加 filter 注入 request_id
logging.getLogger().addFilter(
    lambda r: setattr(r, "request_id", request_id_var.get()) or True
)

def log_api_call(model, input_tokens, output_tokens, latency_ms):
    logger.info(f"API call | model={model} | input={input_tokens} | "
                f"output={output_tokens} | latency={latency_ms}ms")
```

### 5.2 关键指标

```python
# 使用 Prometheus 暴露指标
from prometheus_client import Counter, Histogram, generate_latest

request_count = Counter("ai_requests_total", "Total AI requests",
                         ["model", "status"])
request_latency = Histogram("ai_request_latency_seconds",
                             "AI request latency", ["model"])

@app.route("/metrics")
def metrics():
    return generate_latest()

# 在请求中埋点
@request_latency.labels(model="deepseek-chat").time()
def call_llm(prompt):
    try:
        result = client.chat.completions.create(...)
        request_count.labels(model="deepseek-chat", status="success").inc()
        return result
    except Exception:
        request_count.labels(model="deepseek-chat", status="error").inc()
        raise
```

---

## 6. 部署检查清单

| 检查项 | 内容 |
|--------|------|
| 环境变量 | API Key 不硬编码，使用环境变量 / Secrets |
| 健康检查 | `/health` 端点返回服务状态 |
| 优雅关闭 | 收到 SIGTERM 后等待进行中请求完成 |
| 超时设置 | API 调用超时 30s，防止连接挂死 |
| 重试策略 | 指数退避，最多 3 次 |
| 降级方案 | 主模型不可用时切换到备用模型 |
| 日志轮转 | 按天切割，保留 30 天 |
