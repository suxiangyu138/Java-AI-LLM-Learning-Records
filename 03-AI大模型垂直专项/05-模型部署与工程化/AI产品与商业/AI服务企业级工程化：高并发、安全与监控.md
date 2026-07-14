# ⚙️ AI 服务企业级工程化：高并发、安全与监控

> **核心摘要**：将 AI 原型变为生产级服务，需解决高并发处理、成本优化、安全防护与监控运维四大挑战。本文提供异步任务队列、限流、缓存、模型路由、Prompt 注入防御、Prometheus 监控等完整的工程化方案及 Spring Boot 集成实践。

> **前置阅读**：[[大模型API调用实践]]、[[AI项目成本优化与安全防护]]

---

## 目录

1. [工程化全景](#1-工程化全景)
2. [高并发处理](#2-高并发处理)
3. [成本优化](#3-成本优化)
4. [安全防护](#4-安全防护)
5. [监控运维](#5-监控运维)
6. [部署检查清单](#6-部署检查清单)

---

## 1. 工程化全景

```
负载均衡 (Nginx/K8s) → AI 服务实例集群 → 缓存 (Redis) / 消息队列 / 数据库
```

关键关注点：高并发下如何避免 API 限流、如何控制成本、如何防御安全攻击、如何监控服务状态。

---

## 2. 高并发处理

### 2.1 异步任务队列

使用 Celery 处理耗时 AI 任务，避免阻塞 Web 服务：

```python
from celery import Celery
from openai import OpenAI

app = Celery("ai_tasks", broker="redis://localhost:6379/0")
client = OpenAI(api_key="sk-xxx", base_url="https://api.deepseek.com/v1")

@app.task(bind=True, max_retries=3)
def generate_report(self, prompt, params=None):
    try:
        response = client.chat.completions.create(
            model="deepseek-chat",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.3
        )
        return response.choices[0].message.content
    except Exception as e:
        self.retry(exc=e, countdown=60)

# 调用
result = generate_report.delay("分析本季度系统性能趋势")
```

### 2.2 连接池 + 流式响应

```python
import httpx

class AIClientPool:
    """AI API 连接池——复用连接，减少开销"""
    def __init__(self, base_url, api_key, pool_size=10):
        self.client = httpx.AsyncClient(
            base_url=base_url,
            headers={"Authorization": f"Bearer {api_key}"},
            limits=httpx.Limits(max_connections=pool_size, max_keepalive_connections=pool_size)
        )

    async def stream_chat(self, model, messages):
        async with self.client.stream("POST", "/v1/chat/completions",
            json={"model": model, "messages": messages, "stream": True}) as response:
            async for line in response.aiter_lines():
                if line.startswith("data: ") and line != "data: [DONE]":
                    import json
                    chunk = json.loads(line[6:])
                    if chunk["choices"][0]["delta"].get("content"):
                        yield chunk["choices"][0]["delta"]["content"]
```

### 2.3 限流与排队

```python
from collections import deque
import time, asyncio

class RateLimiter:
    """滑动窗口限流器"""
    def __init__(self, max_requests, window_seconds):
        self.max_requests = max_requests
        self.window = window_seconds
        self.requests = deque()

    async def acquire(self):
        while True:
            now = time.time()
            while self.requests and self.requests[0] < now - self.window:
                self.requests.popleft()
            if len(self.requests) < self.max_requests:
                self.requests.append(now)
                return
            await asyncio.sleep(self.requests[0] + self.window - now + 0.1)
```

> **注意**：对于 Java 后端，推荐使用 **Sentinel** 或 **Resilience4j** 实现限流与熔断，替代 Python 版本的滑动窗口实现。

---

## 3. 成本优化

### 3.1 Prompt 缓存

```python
import hashlib, redis

class PromptCache:
    def __init__(self, redis_client, ttl=3600):
        self.redis = redis_client
        self.ttl = ttl

    def _key(self, prompt):
        return f"cache:{hashlib.md5(prompt.encode()).hexdigest()}"

    def get(self, prompt):
        return self.redis.get(self._key(prompt))

    def set(self, prompt, answer):
        self.redis.setex(self._key(prompt), self.ttl, answer)
```

### 3.2 模型路由

```python
class ModelRouter:
    ROUTES = {"easy": "deepseek-chat", "hard": "deepseek-reasoner"}

    def classify(self, prompt):
        if len(prompt) < 50:
            return "easy"
        if any(kw in prompt for kw in ["分析", "架构", "设计"]):
            return "hard"
        return "easy"

    def route(self, prompt, client):
        level = self.classify(prompt)
        return client.chat.completions.create(
            model=self.ROUTES[level],
            messages=[{"role": "user", "content": prompt}],
            max_tokens=256 if level == "easy" else 2048
        )
```

### 3.3 成本计算

```python
PRICING = {
    "deepseek-chat": {"input": 0.001, "output": 0.002},
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
class PromptGuard:
    INJECTION_PATTERNS = [
        r"忽略.*指令", r"ignore.*instruction",
        r"你.*是.*新.*角色", r"forget.*previous",
    ]

    @classmethod
    def detect(cls, user_input):
        for pattern in cls.INJECTION_PATTERNS:
            if re.search(pattern, user_input, re.IGNORECASE):
                return True, f"检测到潜在注入: {pattern}"
        return False, "安全"

    @classmethod
    def sanitize(cls, user_input):
        if len(user_input) > 8000:
            user_input = user_input[:8000] + "..."
        return re.sub(r'[\x00-\x08\x0b\x0c\x0e-\x1f]', '', user_input)
```

### 4.2 内容安全与访问控制

```python
class ContentFilter:
    SENSITIVE_KEYWORDS = []  # 根据业务需求配置

    @classmethod
    def check_output(cls, text):
        for kw in cls.SENSITIVE_KEYWORDS:
            if kw in text:
                return False, f"输出包含敏感词"
        return True, "安全"
```

### 4.3 接口限流（装饰器模式）

```python
def rate_limit(max_per_minute=30):
    records = {}
    def decorator(f):
        def wrapper(*args, **kwargs):
            user_ip = request.remote_addr
            now = time.time()
            if user_ip not in records:
                records[user_ip] = []
            records[user_ip] = [t for t in records[user_ip] if now - t < 60]
            if len(records[user_ip]) >= max_per_minute:
                return jsonify({"error": "Too many requests"}), 429
            records[user_ip].append(now)
            return f(*args, **kwargs)
        return wrapper
    return decorator
```

---

## 5. 监控运维

### 5.1 结构化日志

```python
import logging, time, uuid

logger = logging.getLogger("ai_service")
handler = logging.StreamHandler()
handler.setFormatter(logging.Formatter(
    '{"time": "%(asctime)s", "level": "%(levelname)s", '
    '"request_id": "%(request_id)s", "message": "%(message)s"}'
))

def log_api_call(model, input_tokens, output_tokens, latency_ms):
    logger.info(f"API call | model={model} | input={input_tokens} | "
                f"output={output_tokens} | latency={latency_ms}ms")
```

### 5.2 Prometheus 指标

```python
from prometheus_client import Counter, Histogram, generate_latest

request_count = Counter("ai_requests_total", "Total AI requests", ["model", "status"])
request_latency = Histogram("ai_request_latency_seconds", "AI request latency", ["model"])

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

| 检查项 | 要求 |
|---|---|
| 环境变量 | API Key 使用环境变量/Secrets，禁止硬编码 |
| 健康检查 | 提供 `/health` 端点 |
| 优雅关闭 | SIGTERM 后等待进行中请求完成 |
| 超时设置 | API 调用超时 30s，防止连接挂死 |
| 重试策略 | 指数退避，最多 3 次 |
| 降级方案 | 主模型不可用时切换到备用模型 |
| 日志轮转 | 按天切割，保留 30 天 |

---

## 核心要点回顾

- 高并发处理三件套：异步任务（Celery）+ 连接池（httpx）+ 限流器（滑动窗口）
- 成本优化三板斧：Prompt 缓存（Redis）、模型路由（难易分级）、用量计量
- 安全防护三层：输入检测（注入防御）、内容过滤（敏感词）、接口限流
- 监控体系：结构化日志 + Prometheus 指标（请求量/延迟/成功率）
- 部署必备：健康检查、优雅关闭、降级方案、日志轮转

## 参考资料

1. Celery 文档：https://docs.celeryq.dev
2. Prometheus Python Client：https://github.com/prometheus/client_python
3. Sentinel（Java 限流）：https://sentinelguard.io
4. Resilience4j：https://resilience4j.readme.io
