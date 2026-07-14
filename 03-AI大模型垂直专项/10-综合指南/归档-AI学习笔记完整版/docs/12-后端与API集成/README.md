# 第12步：后端与API集成

> **阶段目标：** 掌握Python脚本与前端界面的集成方法，能够设计并实现多Provider多服务协同的完整后端架构  
> **预计学时：** 1-2周（每天3-4小时）  
> **前置要求：** FastAPI基础 + 多Provider API调用  

---

## 📚 目录

- [12.1 系统架构设计](#121-系统架构设计)
- [12.2 数据库集成](#122-数据库集成)
- [12.3 异步处理与任务队列](#123-异步处理与任务队列)
- [12.4 缓存策略](#124-缓存策略)
- [12.5 外部服务集成](#125-外部服务集成)
- [12.6 错误处理与容错](#126-错误处理与容错)
- [12.7 阶段练习](#127-阶段练习)

---

## 12.1 系统架构设计

### 12.1.1 企业级AI应用架构

```
                        ┌──────────────┐
                        │   用户/客户端  │
                        └──────┬───────┘
                               ↓
                    ┌──────────────────────┐
                    │   API Gateway        │
                    │   (Nginx/Traefik)    │
                    │   路由/限流/认证      │
                    └──────────┬───────────┘
                               ↓
            ┌──────────────────┼──────────────────┐
            ↓                  ↓                  ↓
    ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
    │  Chat Service│  │  RAG Service │  │ Agent Service│
    │  (FastAPI)   │  │  (FastAPI)   │  │  (FastAPI)   │
    └──────┬───────┘  └──────┬───────┘  └──────┬───────┘
           ↓                 ↓                 ↓
    ┌──────────────────────────────────────────────────┐
    │              消息队列 (Celery/Redis)               │
    └──────────────────────────────────────────────────┘
           ↓                 ↓                 ↓
    ┌──────────┐  ┌──────────────┐  ┌──────────────┐
    │ OpenAI   │  │   Milvus     │  │  PostgreSQL  │
    │ Anthropic│  │   (向量DB)   │  │   Redis      │
    │ DeepSeek │  │   Chroma     │  │   MinIO      │
    └──────────┘  └──────────────┘  └──────────────┘
```

### 12.1.2 后端项目结构

```python
"""
ai-backend/
├── app/
│   ├── main.py
│   ├── config.py
│   ├── api/
│   │   ├── v1/
│   │   │   ├── chat.py
│   │   │   ├── rag.py
│   │   │   ├── agent.py
│   │   │   └── admin.py
│   │   └── deps.py
│   ├── core/
│   │   ├── llm/              # LLM调用层
│   │   │   ├── base.py       # 抽象基类
│   │   │   ├── openai.py
│   │   │   ├── anthropic.py
│   │   │   └── router.py     # 智能路由（选最佳模型）
│   │   ├── rag/              # RAG服务层
│   │   ├── agent/            # Agent服务层
│   │   └── security/         # 安全认证
│   ├── models/               # 数据库模型（SQLAlchemy）
│   ├── schemas/              # Pydantic Schema
│   ├── tasks/                # Celery异步任务
│   └── utils/
├── migrations/               # Alembic数据库迁移
├── tests/
├── docker-compose.yml
└── Dockerfile
"""
```

---

## 12.2 数据库集成

### 12.2.1 SQLAlchemy + PostgreSQL

```python
# ========== 数据库配置 ==========
from sqlalchemy import create_engine, Column, String, Integer, Float, DateTime, Text, JSON
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, Session
from sqlalchemy.sql import func
from contextlib import contextmanager
import os

DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql://user:pass@localhost:5432/ai_app"
)

engine = create_engine(DATABASE_URL, pool_size=20, max_overflow=10)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

@contextmanager
def get_db() -> Session:
    """数据库会话上下文管理器"""
    db = SessionLocal()
    try:
        yield db
        db.commit()
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()

# ========== 数据模型 ==========
class User(Base):
    __tablename__ = "users"
    
    id = Column(Integer, primary_key=True, autoincrement=True)
    username = Column(String(50), unique=True, nullable=False, index=True)
    api_key_hash = Column(String(64), nullable=False)
    tier = Column(String(20), default="free")  # free/pro/enterprise
    quota_total = Column(Integer, default=10000)  # Token配额
    quota_used = Column(Integer, default=0)
    created_at = Column(DateTime, server_default=func.now())
    updated_at = Column(DateTime, onupdate=func.now())

class ChatHistory(Base):
    __tablename__ = "chat_history"
    
    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, nullable=False, index=True)
    session_id = Column(String(64), nullable=False, index=True)
    role = Column(String(20), nullable=False)  # user/assistant/system
    content = Column(Text, nullable=False)
    model = Column(String(50))
    prompt_tokens = Column(Integer, default=0)
    completion_tokens = Column(Integer, default=0)
    cost = Column(Float, default=0.0)
    latency_ms = Column(Integer)
    metadata_ = Column("metadata", JSON)
    created_at = Column(DateTime, server_default=func.now())

class APICallLog(Base):
    __tablename__ = "api_call_logs"
    
    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, index=True)
    provider = Column(String(20))  # openai/anthropic/deepseek
    model = Column(String(50))
    endpoint = Column(String(100))
    status = Column(String(20))  # success/error/rate_limited
    prompt_tokens = Column(Integer)
    completion_tokens = Column(Integer)
    cost = Column(Float)
    latency_ms = Column(Integer)
    error_message = Column(Text)
    created_at = Column(DateTime, server_default=func.now())

# 创建表
Base.metadata.create_all(bind=engine)
```

### 12.2.2 用户配额管理

```python
class QuotaManager:
    """Token配额管理器"""
    
    def __init__(self, db: Session):
        self.db = db
    
    def check_quota(self, user_id: int, estimated_tokens: int) -> bool:
        """检查用户是否有足够配额"""
        user = self.db.query(User).filter(User.id == user_id).first()
        if not user:
            return False
        
        remaining = user.quota_total - user.quota_used
        return remaining >= estimated_tokens
    
    def deduct_quota(self, user_id: int, tokens_used: int) -> bool:
        """扣减配额"""
        user = self.db.query(User).filter(User.id == user_id).first()
        if not user:
            return False
        
        if user.quota_used + tokens_used > user.quota_total:
            return False
        
        user.quota_used += tokens_used
        self.db.commit()
        return True
    
    def get_usage_report(self, user_id: int) -> dict:
        """获取用量报告"""
        user = self.db.query(User).filter(User.id == user_id).first()
        
        # 今日用量
        today = func.date(func.now())
        today_calls = self.db.query(APICallLog).filter(
            APICallLog.user_id == user_id,
            func.date(APICallLog.created_at) == today,
        ).all()
        
        today_tokens = sum(c.prompt_tokens + c.completion_tokens for c in today_calls)
        today_cost = sum(c.cost for c in today_calls)
        
        return {
            "quota_total": user.quota_total,
            "quota_used": user.quota_used,
            "quota_remaining": user.quota_total - user.quota_used,
            "today_tokens": today_tokens,
            "today_cost": today_cost,
            "today_calls": len(today_calls),
        }
```

---

## 12.3 异步处理与任务队列

### 12.3.1 Celery异步任务

```python
# ========== Celery配置 ==========
from celery import Celery

celery_app = Celery(
    "ai_tasks",
    broker="redis://localhost:6379/0",   # 消息代理
    backend="redis://localhost:6379/1",  # 结果后端
)

celery_app.conf.update(
    task_serializer="json",
    accept_content=["json"],
    result_serializer="json",
    timezone="Asia/Shanghai",
    enable_utc=True,
    task_annotations={
        '*': {'max_retries': 3}
    },
)

# ========== 异步任务定义 ==========
@celery_app.task(bind=True, max_retries=3, default_retry_delay=60)
def process_document_indexing(self, file_path: str, kb_name: str):
    """
    异步文档索引任务
    
    为什么需要异步？
    - 大文件处理可能耗时几分钟
    - 不应该让HTTP请求一直等待
    - 失败后可以自动重试
    """
    try:
        # 1. 加载文档
        docs = DocumentLoader.load(file_path)
        
        # 2. 分割
        chunks = splitter.split_documents(docs)
        
        # 3. 生成Embedding
        embeddings = encoder.encode([c.page_content for c in chunks])
        
        # 4. 存入向量数据库
        vector_store.add_documents(chunks, embeddings)
        
        return {
            "status": "completed",
            "chunks": len(chunks),
            "file": file_path,
        }
    except Exception as e:
        # 自动重试
        self.retry(exc=e)

@celery_app.task
def batch_llm_evaluation(prompts: List[str], model: str):
    """批量LLM评估任务"""
    results = []
    for prompt in prompts:
        result = llm_client.chat(prompt, model=model)
        results.append(result)
    return results

@celery_app.task
def send_usage_report(user_id: int, email: str):
    """定时发送用量报告"""
    # 生成并发送邮件
    pass

# ========== FastAPI集成 ==========
@app.post("/documents/upload")
async def upload_document(
    file: UploadFile,
    kb_name: str,
    background_tasks: BackgroundTasks,
):
    """上传文档（触发异步索引）"""
    # 保存文件
    file_path = f"/tmp/uploads/{file.filename}"
    with open(file_path, "wb") as f:
        f.write(await file.read())
    
    # 提交异步任务
    task = process_document_indexing.delay(file_path, kb_name)
    
    # 也可以用FastAPI的BackgroundTasks（简单场景）
    # background_tasks.add_task(process_document_indexing, file_path, kb_name)
    
    return {
        "status": "processing",
        "task_id": task.id,
        "message": "文档正在索引中，请稍后查询结果"
    }

@app.get("/tasks/{task_id}")
async def get_task_status(task_id: str):
    """查询异步任务状态"""
    task = celery_app.AsyncResult(task_id)
    
    response = {"task_id": task_id, "status": task.status}
    
    if task.status == "SUCCESS":
        response["result"] = task.result
    elif task.status == "FAILURE":
        response["error"] = str(task.info)
    
    return response
```

### 12.3.2 定时任务

```python
# ========== Celery Beat 定时任务 ==========
from celery.schedules import crontab

celery_app.conf.beat_schedule = {
    # 每小时清理过期会话
    'cleanup-expired-sessions': {
        'task': 'app.tasks.maintenance.cleanup_expired_sessions',
        'schedule': crontab(minute=0),  # 每小时整点
    },
    # 每天凌晨生成日报
    'daily-usage-report': {
        'task': 'app.tasks.reports.generate_daily_report',
        'schedule': crontab(hour=0, minute=0),
    },
    # 每5分钟检查模型健康状态
    'health-check': {
        'task': 'app.tasks.monitoring.check_model_health',
        'schedule': crontab(minute='*/5'),
    },
}

# 启动: celery -A app.tasks beat --loglevel=info
# Worker: celery -A app.tasks worker --loglevel=info --concurrency=4
```

---

## 12.4 缓存策略

### 12.4.1 Redis缓存集成

```python
import redis
import json
import hashlib
from functools import wraps
from typing import Optional, Callable

# 连接Redis
cache = redis.Redis(
    host='localhost',
    port=6379,
    db=0,
    decode_responses=True,
)

class AICache:
    """
    AI应用缓存管理器
    
    缓存什么？
    ✅ 相同Prompt的回答（短期内不变）
    ✅ Embedding向量（模型输出固定）
    ✅ 用户Session信息
    ✅ Token计数结果
    
    不缓存什么？
    ❌ 需要实时性的回答
    ❌ 高temperature的创意输出
    ❌ 流式输出（内容不完整）
    """
    
    def __init__(self, redis_client: redis.Redis, ttl: int = 3600):
        self.cache = redis_client
        self.ttl = ttl  # 默认1小时
    
    def _make_key(self, prefix: str, *args, **kwargs) -> str:
        """生成缓存键"""
        raw = f"{prefix}:{json.dumps(args, sort_keys=True)}:{json.dumps(kwargs, sort_keys=True)}"
        return hashlib.md5(raw.encode()).hexdigest()
    
    def cache_llm_response(self, prompt: str, model: str, 
                           temperature: float = 0) -> Optional[str]:
        """
        缓存LLM回答
        
        只缓存temperature=0的确定性输出
        高temperature的输出每次都不同，不值得缓存
        """
        if temperature > 0.1:
            return None  # 不缓存随机输出
        
        key = self._make_key("llm", prompt, model, temperature)
        return self.cache.get(key)
    
    def set_llm_response(self, prompt: str, model: str,
                         temperature: float, response: str):
        """存储LLM回答"""
        if temperature > 0.1:
            return
        
        key = self._make_key("llm", prompt, model, temperature)
        self.cache.setex(key, self.ttl, response)
    
    def cache_embedding(self, text: str, model: str) -> Optional[List[float]]:
        """缓存Embedding向量（始终缓存，因为模型输出是确定的）"""
        key = self._make_key("emb", text, model)
        cached = self.cache.get(key)
        if cached:
            return json.loads(cached)
        return None
    
    def set_embedding(self, text: str, model: str, embedding: List[float]):
        """存储Embedding"""
        key = self._make_key("emb", text, model)
        self.cache.setex(key, 86400 * 7, json.dumps(embedding))  # 7天

# ========== 装饰器：自动缓存 ==========
def ai_cache(ttl: int = 3600):
    """缓存装饰器"""
    def decorator(func: Callable):
        @wraps(func)
        async def wrapper(*args, **kwargs):
            # 生成缓存键
            key_parts = [func.__name__] + [str(a) for a in args] + \
                        [f"{k}={v}" for k, v in sorted(kwargs.items())]
            key = hashlib.md5(":".join(key_parts).encode()).hexdigest()
            
            # 检查缓存
            cached = cache.get(key)
            if cached:
                return json.loads(cached)
            
            # 执行函数
            result = await func(*args, **kwargs)
            
            # 存入缓存
            cache.setex(key, ttl, json.dumps(result))
            
            return result
        return wrapper
    return decorator


# ========== 缓存分层策略 ==========
"""
L1: 内存缓存 (Python dict/lru_cache)
    - 速度最快
    - 容量最小
    - 适合：配置信息、模型元数据

L2: Redis缓存
    - 速度快
    - 可共享（多进程/多实例）
    - 适合：LLM回答、Embedding、Session

L3: 数据库 (PostgreSQL)
    - 速度中等
    - 持久化
    - 适合：用户数据、对话历史、日志
"""
```

---

## 12.5 外部服务集成

### 12.5.1 Webhook集成

```python
# ========== Webhook：企业IM集成 ==========
@app.post("/webhook/slack")
async def slack_webhook(request: Request):
    """Slack机器人集成"""
    body = await request.json()
    
    # Slack事件验证
    if body.get("type") == "url_verification":
        return {"challenge": body["challenge"]}
    
    # 处理消息
    event = body.get("event", {})
    if event.get("type") == "app_mention":
        user_message = event.get("text", "").replace("<@BOT_ID>", "").strip()
        
        # 异步处理
        process_slack_message.delay(
            channel=event["channel"],
            user=event["user"],
            message=user_message,
        )
    
    return {"ok": True}

@app.post("/webhook/wechat")
async def wechat_webhook(request: Request):
    """企业微信集成"""
    # 类似实现
    pass

@app.post("/webhook/dingtalk")
async def dingtalk_webhook(request: Request):
    """钉钉集成"""
    pass
```

### 12.5.2 第三方数据源集成

```python
class ExternalDataSource:
    """外部数据源管理器"""
    
    def __init__(self):
        self.sources = {}
    
    async def sync_confluence(self, space_key: str):
        """同步Confluence文档"""
        import httpx
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"https://confluence.example.com/rest/api/space/{space_key}/content",
                auth=("user", os.getenv("CONFLUENCE_TOKEN")),
            )
            return response.json()
    
    async def sync_notion(self, database_id: str):
        """同步Notion数据库"""
        pass
    
    async def sync_sharepoint(self, site_url: str):
        """同步SharePoint"""
        pass
    
    async def sync_all(self):
        """全量同步所有数据源"""
        tasks = [
            self.sync_confluence("AI"),
            self.sync_notion("project-db"),
        ]
        results = await asyncio.gather(*tasks, return_exceptions=True)
        return results
```

---

## 12.6 错误处理与容错

### 12.6.1 全局异常处理

```python
from fastapi import Request
from fastapi.responses import JSONResponse

# ========== 自定义异常 ==========
class AppException(Exception):
    """应用基础异常"""
    def __init__(self, message: str, status_code: int = 500, 
                 error_code: str = "INTERNAL_ERROR"):
        self.message = message
        self.status_code = status_code
        self.error_code = error_code

class QuotaExceeded(AppException):
    def __init__(self):
        super().__init__("Token配额已用完", 429, "QUOTA_EXCEEDED")

class ModelUnavailable(AppException):
    def __init__(self, model: str):
        super().__init__(f"模型 {model} 暂时不可用", 503, "MODEL_UNAVAILABLE")

# ========== 全局异常处理器 ==========
@app.exception_handler(AppException)
async def app_exception_handler(request: Request, exc: AppException):
    return JSONResponse(
        status_code=exc.status_code,
        content={
            "error": {
                "code": exc.error_code,
                "message": exc.message,
                "type": type(exc).__name__,
            }
        },
    )

@app.exception_handler(Exception)
async def general_exception_handler(request: Request, exc: Exception):
    """捕获所有未处理的异常"""
    logger.error(f"未处理异常: {exc}", exc_info=True)
    
    return JSONResponse(
        status_code=500,
        content={
            "error": {
                "code": "INTERNAL_ERROR",
                "message": "服务器内部错误，请稍后重试",
                # 生产环境不暴露详细错误信息
                "detail": str(exc) if os.getenv("DEBUG") == "true" else None,
            }
        },
    )
```

### 12.6.2 熔断器模式

```python
from datetime import datetime, timedelta
import threading

class CircuitBreaker:
    """
    熔断器 — 防止级联故障
    
    状态转换:
    CLOSED ──失败次数达到阈值──→ OPEN
       ↑                           ↓
       └──── 超时后尝试 ──── HALF_OPEN
    """
    
    def __init__(self, failure_threshold: int = 5, 
                 timeout: int = 60):
        self.failure_threshold = failure_threshold
        self.timeout = timeout  # OPEN状态持续时间
        self.failure_count = 0
        self.last_failure_time = None
        self.state = "CLOSED"
        self.lock = threading.Lock()
    
    def call(self, func: Callable, *args, **kwargs):
        """通过熔断器调用函数"""
        with self.lock:
            if self.state == "OPEN":
                if self._should_retry():
                    self.state = "HALF_OPEN"
                    logger.info("熔断器: OPEN → HALF_OPEN (尝试恢复)")
                else:
                    raise AppException("服务暂时不可用(熔断)", 503, "CIRCUIT_OPEN")
            
            try:
                result = func(*args, **kwargs)
                
                # 成功 → 重置
                if self.state == "HALF_OPEN":
                    self.state = "CLOSED"
                    self.failure_count = 0
                    logger.info("熔断器: HALF_OPEN → CLOSED (已恢复)")
                
                return result
                
            except Exception as e:
                self.failure_count += 1
                self.last_failure_time = datetime.now()
                
                if self.failure_count >= self.failure_threshold:
                    self.state = "OPEN"
                    logger.error(f"熔断器: CLOSED → OPEN (连续失败{self.failure_count}次)")
                
                raise
    
    def _should_retry(self) -> bool:
        """判断是否该尝试恢复"""
        if self.last_failure_time is None:
            return True
        return datetime.now() - self.last_failure_time > timedelta(seconds=self.timeout)


# 使用示例
openai_breaker = CircuitBreaker(failure_threshold=3, timeout=30)

@app.post("/chat")
async def chat(request: ChatRequest):
    try:
        # 通过熔断器调用LLM
        response = openai_breaker.call(
            client.chat.completions.create,
            model=request.model,
            messages=[...],
        )
        return response
    except AppException as e:
        raise HTTPException(status_code=e.status_code, detail=e.message)
```

---

## 12.7 阶段练习

### 练习1：数据库集成
用SQLAlchemy+SQLite（或PostgreSQL）为AI应用添加用户管理和对话历史存储。

### 练习2：异步任务
用Celery+Redis实现文档索引的异步处理和进度查询。

### 练习3：熔断器
为你的LLM调用实现熔断器模式，测试服务不可用时的降级行为。

---

> **✅ 阶段完成检查清单：**
> - [ ] 能用SQLAlchemy设计AI应用的数据模型
> - [ ] 理解异步任务处理的必要性并能实现
> - [ ] 掌握了Redis缓存在AI场景中的应用
> - [ ] 了解熔断器、重试、降级等容错模式
> - [ ] 完成3个阶段练习
>
> **下一步：** [第13步：实时交互与多模态](../13-实时交互与多模态/README.md)
