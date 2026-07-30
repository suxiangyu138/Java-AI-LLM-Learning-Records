# 05 - 数据库与ORM集成

> 🎯 AI 应用离不开数据持久化 — SQLAlchemy 2.0 async、连接池、Alembic 迁移。重点在异步模式下的数据库使用

---

## 目录

1. [SQLAlchemy 2.0 Async 集成](#1-sqlalchemy-20-async-集成)
2. [模型定义与关系映射](#2-模型定义与关系映射)
3. [CRUD 与异步查询](#3-crud-与异步查询)
4. [Session 管理与连接池](#4-session-管理与连接池)
5. [数据库迁移 — Alembic](#5-数据库迁移--alembic)
6. [向量数据库集成](#6-向量数据库集成)

---

## 1. SQLAlchemy 2.0 Async 集成

### 1.1 配置与连接

```python
"""
FastAPI + SQLAlchemy 2.0 async 标准配置
"""
from sqlalchemy.ext.asyncio import (
    create_async_engine,
    AsyncSession,
    async_sessionmaker,
)
from sqlalchemy.orm import DeclarativeBase
from fastapi import FastAPI, Depends
from contextlib import asynccontextmanager
from typing import Annotated

# ===== 数据库 URL =====
DATABASE_URL = "postgresql+asyncpg://user:pass@localhost:5432/aidb"

# ===== 引擎（连接池） =====
engine = create_async_engine(
    DATABASE_URL,
    echo=False,                          # 生产环境关闭 SQL 日志
    pool_size=20,                        # 常驻连接数
    max_overflow=10,                     # 溢出连接数（pool_size + max_overflow = 30 max）
    pool_recycle=3600,                   # 连接回收时间（秒）
    pool_pre_ping=True,                  # 连接前检测可用性
    connect_args={
        "server_settings": {
            "application_name": "fastapi-ai-app",
        }
    },
)

# ===== Session 工厂 =====
AsyncSessionLocal = async_sessionmaker(
    engine,
    class_=AsyncSession,
    expire_on_commit=False,              # 提交后不使对象过期
    autoflush=False,                     # 手动控制 flush
)


# ===== 基类 =====
class Base(DeclarativeBase):
    pass


# ===== 依赖注入 =====
async def get_db():
    async with AsyncSessionLocal() as session:
        try:
            yield session
            await session.commit()
        except Exception:
            await session.rollback()
            raise


DBSession = Annotated[AsyncSession, Depends(get_db)]
```

---

## 2. 模型定义与关系映射

### 2.1 AI 应用常见表结构

```python
from sqlalchemy import (
    Column, Integer, String, Text, Float,
    DateTime, ForeignKey, Boolean, JSON,
    Enum as SAEnum, Table,
)
from sqlalchemy.orm import relationship, Mapped, mapped_column
from datetime import datetime
import enum


class ModelProvider(str, enum.Enum):
    OPENAI = "openai"
    ANTHROPIC = "anthropic"
    DEEPSEEK = "deepseek"
    LOCAL = "local"


class MessageRole(str, enum.Enum):
    SYSTEM = "system"
    USER = "user"
    ASSISTANT = "assistant"
    TOOL = "tool"


# ===== 用户表 =====
class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(primary_key=True)
    username: Mapped[str] = mapped_column(String(50), unique=True, index=True)
    email: Mapped[str] = mapped_column(String(200), unique=True)
    hashed_password: Mapped[str] = mapped_column(String(200))
    is_active: Mapped[bool] = mapped_column(default=True)
    created_at: Mapped[datetime] = mapped_column(default=datetime.utcnow)
    api_keys: Mapped[list["APIKey"]] = relationship(back_populates="user", cascade="all, delete-orphan")
    conversations: Mapped[list["Conversation"]] = relationship(back_populates="user")


# ===== API Key 表 =====
class APIKey(Base):
    __tablename__ = "api_keys"

    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"))
    key_hash: Mapped[str] = mapped_column(String(64), unique=True)  # SHA256
    name: Mapped[str] = mapped_column(String(100))
    provider: Mapped[ModelProvider] = mapped_column(SAEnum(ModelProvider))
    is_active: Mapped[bool] = mapped_column(default=True)
    last_used_at: Mapped[datetime | None] = mapped_column(nullable=True)
    created_at: Mapped[datetime] = mapped_column(default=datetime.utcnow)

    user: Mapped["User"] = relationship(back_populates="api_keys")


# ===== 对话表 =====
class Conversation(Base):
    __tablename__ = "conversations"

    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    title: Mapped[str] = mapped_column(String(200))
    model: Mapped[str] = mapped_column(String(50))
    total_tokens: Mapped[int] = mapped_column(default=0)
    created_at: Mapped[datetime] = mapped_column(default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(default=datetime.utcnow, onupdate=datetime.utcnow)

    user: Mapped["User"] = relationship(back_populates="conversations")
    messages: Mapped[list["Message"]] = relationship(
        back_populates="conversation",
        cascade="all, delete-orphan",
        order_by="Message.created_at",
    )


# ===== 消息表 =====
class Message(Base):
    __tablename__ = "messages"

    id: Mapped[int] = mapped_column(primary_key=True)
    conversation_id: Mapped[int] = mapped_column(ForeignKey("conversations.id"), index=True)
    role: Mapped[MessageRole] = mapped_column(SAEnum(MessageRole))
    content: Mapped[str] = mapped_column(Text)
    tool_calls: Mapped[dict | None] = mapped_column(JSON, nullable=True)  # [{id, name, args}]
    tool_call_id: Mapped[str | None] = mapped_column(String(100), nullable=True)
    token_count: Mapped[int] = mapped_column(default=0)
    created_at: Mapped[datetime] = mapped_column(default=datetime.utcnow)

    conversation: Mapped["Conversation"] = relationship(back_populates="messages")


# ===== 文档表（RAG/知识库） =====
class Document(Base):
    __tablename__ = "documents"

    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    filename: Mapped[str] = mapped_column(String(500))
    content_type: Mapped[str] = mapped_column(String(100))
    size_bytes: Mapped[int] = mapped_column()
    chunk_count: Mapped[int] = mapped_column(default=0)
    status: Mapped[str] = mapped_column(String(20), default="uploading")  # uploading | chunking | embedding | ready | error
    metadata_: Mapped[dict] = mapped_column("metadata", JSON, default=dict)
    created_at: Mapped[datetime] = mapped_column(default=datetime.utcnow)
```

---

## 3. CRUD 与异步查询

### 3.1 Select 查询

```python
from sqlalchemy import select, func, and_, or_, desc
from sqlalchemy.orm import selectinload


class ConversationRepository:
    """对话仓库 — 异步 CRUD 封装"""

    def __init__(self, session: AsyncSession):
        self.session = session

    async def get_by_id(self, conv_id: int) -> Conversation | None:
        """获取对话 — 带 Messages 预加载"""
        stmt = (
            select(Conversation)
            .options(selectinload(Conversation.messages))  # eager load
            .where(Conversation.id == conv_id)
        )
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def list_by_user(
        self,
        user_id: int,
        page: int = 1,
        size: int = 20,
        keyword: str | None = None,
    ) -> tuple[list[Conversation], int]:
        """分页查询用户对话"""

        # 条件
        conditions = [Conversation.user_id == user_id]
        if keyword:
            conditions.append(Conversation.title.ilike(f"%{keyword}%"))

        # 总数
        count_stmt = select(func.count()).where(and_(*conditions))
        total = (await self.session.execute(count_stmt)).scalar()

        # 列表
        list_stmt = (
            select(Conversation)
            .where(and_(*conditions))
            .order_by(desc(Conversation.updated_at))
            .offset((page - 1) * size)
            .limit(size)
        )
        result = await self.session.execute(list_stmt)
        return result.scalars().all(), total

    async def create(self, user_id: int, title: str, model: str) -> Conversation:
        conv = Conversation(user_id=user_id, title=title, model=model)
        self.session.add(conv)
        await self.session.flush()  # 获取自增 ID
        return conv

    async def add_message(self, conv_id: int, role: str, content: str, token_count: int = 0) -> Message:
        msg = Message(
            conversation_id=conv_id,
            role=role,
            content=content,
            token_count=token_count,
        )
        self.session.add(msg)

        # 更新对话总 token 数和时间
        conv = await self.session.get(Conversation, conv_id)
        if conv:
            conv.total_tokens += token_count
            conv.updated_at = datetime.utcnow()

        await self.session.flush()
        return msg

    async def delete(self, conv_id: int) -> bool:
        conv = await self.session.get(Conversation, conv_id)
        if conv:
            await self.session.delete(conv)
            return True
        return False
```

### 3.2 原生 SQL 与批量操作

```python
# 批量插入（性能场景）
from sqlalchemy import insert

async def batch_insert_messages(session: AsyncSession, messages: list[dict]):
    stmt = insert(Message).values(messages)
    await session.execute(stmt)

# 原生 SQL（复杂查询）
from sqlalchemy import text

async def get_daily_stats(session: AsyncSession, user_id: int, days: int = 7):
    stmt = text("""
        SELECT
            DATE(created_at) as date,
            COUNT(*) as msg_count,
            SUM(token_count) as total_tokens,
            AVG(token_count) as avg_tokens
        FROM messages
        JOIN conversations ON messages.conversation_id = conversations.id
        WHERE conversations.user_id = :user_id
          AND messages.created_at >= NOW() - INTERVAL ':days days'
        GROUP BY DATE(created_at)
        ORDER BY date
    """)
    result = await session.execute(stmt, {"user_id": user_id, "days": days})
    return result.mappings().all()
```

---

## 4. Session 管理与连接池

### 4.1 事务装饰器

```python
from functools import wraps
from sqlalchemy.ext.asyncio import AsyncSession


def transactional(func):
    """事务装饰器：自动 commit/rollback"""

    @wraps(func)
    async def wrapper(*args, **kwargs):
        # 查找 session 参数
        session: AsyncSession = kwargs.get("session") or args[-1]

        try:
            result = await func(*args, **kwargs)
            await session.commit()
            return result
        except Exception:
            await session.rollback()
            raise

    return wrapper


# 使用
class UserService:
    @transactional
    async def create_user_with_defaults(
        self, session: AsyncSession, username: str, email: str
    ) -> User:
        user = User(username=username, email=email)
        session.add(user)
        await session.flush()

        # 创建默认 API Key
        api_key = APIKey(user_id=user.id, name="default", provider=ModelProvider.OPENAI, key_hash="...")
        session.add(api_key)

        return user
```

### 4.2 连接池监控

```python
from fastapi import FastAPI
from sqlalchemy import pool, text


@app.get("/admin/db-pool-stats")
async def db_pool_stats():
    """数据库连接池状态 — 运维监控"""
    pool = engine.pool

    return {
        "size": pool.size(),                     # 当前总连接数
        "checked_in": pool.checkedin(),          # 空闲连接数
        "checked_out": pool.checkedout(),        # 使用中连接数
        "overflow": pool.overflow(),             # 溢出连接数
        "total": pool.size() + pool.overflow(),
        "max": engine.pool.size() + engine.pool._max_overflow,
        "usage_percent": round(
            pool.checkedout() / (pool.size() + pool.overflow()) * 100, 1
        ) if (pool.size() + pool.overflow()) > 0 else 0,
    }
```

---

## 5. 数据库迁移 — Alembic

```text
Alembic = SQLAlchemy 官方迁移工具

基本命令：
  alembic init migrations                    # 初始化
  alembic revision --autogenerate -m "add users"  # 自动生成迁移
  alembic upgrade head                        # 执行迁移
  alembic downgrade -1                        # 回滚一步

关键配置（alembic.ini + env.py）：
  sqlalchemy.url = postgresql+asyncpg://...
  target_metadata = Base.metadata  (上面定义的模型)
```

```python
# migrations/env.py 关键配置 — Async 模式
from app.database import Base, DATABASE_URL
from sqlalchemy.ext.asyncio import create_async_engine

# Async 配置
connectable = create_async_engine(DATABASE_URL)

def run_migrations_online():
    """异步模式下的迁移执行"""
    connectable = create_async_engine(DATABASE_URL)

    async def run_async():
        async with connectable.connect() as connection:
            await connection.run_sync(do_run_migrations)

    asyncio.run(run_async())
```

---

## 6. 向量数据库集成

```python
"""
AI 应用特殊场景：向量数据库（Qdrant/Chroma/Milvus）
与关系数据库配合使用
"""
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams, PointStruct


class VectorStoreService:
    """向量数据库服务 — 与 PG 配合使用"""

    def __init__(self):
        self.client = QdrantClient(host="localhost", port=6333)
        self.collection = "documents"

    async def ensure_collection(self, dim: int = 1536):
        """确保集合存在"""
        collections = self.client.get_collections().collections
        names = [c.name for c in collections]
        if self.collection not in names:
            self.client.create_collection(
                collection_name=self.collection,
                vectors_config=VectorParams(size=dim, distance=Distance.COSINE),
            )

    async def upsert_chunks(
        self,
        doc_id: int,
        chunks: list[dict],
        embeddings: list[list[float]],
    ):
        """将文档块 + 向量存入 Qdrant"""
        points = []
        for i, (chunk, embedding) in enumerate(zip(chunks, embeddings)):
            points.append(PointStruct(
                id=f"{doc_id}_{i}",
                vector=embedding,
                payload={
                    "doc_id": doc_id,
                    "chunk_index": i,
                    "content": chunk["content"],
                    "metadata": chunk.get("metadata", {}),
                },
            ))

        self.client.upsert(collection_name=self.collection, points=points)

    async def search(
        self,
        query_embedding: list[float],
        top_k: int = 5,
        score_threshold: float = 0.5,
        doc_ids: list[int] | None = None,
    ):
        """向量相似度搜索"""
        query_filter = None
        if doc_ids:
            from qdrant_client.models import Filter, FieldCondition, MatchAny
            query_filter = Filter(
                must=[FieldCondition(key="doc_id", match=MatchAny(any=doc_ids))]
            )

        results = self.client.search(
            collection_name=self.collection,
            query_vector=query_embedding,
            limit=top_k,
            score_threshold=score_threshold,
            query_filter=query_filter,
        )

        return [
            {"score": r.score, "content": r.payload["content"], "metadata": r.payload.get("metadata", {})}
            for r in results
        ]
```

> 🎯 **核心要点**：SQLAlchemy 2.0 async + DeclarativeBase 是标准范式。Session 通过 DI 注入，事务用 try/commit/rollback。AI 应用特有：对话/消息表设计、向量数据库与关系数据库配合、Token 计数追踪

---

**上一模块**：[04 - 流式响应与实时通信](./04-流式响应与实时通信.md)  
**下一模块**：[06 - 认证与安全](./06-认证与安全.md)  
**返回总览**：[00 - FastAPI 知识体系总览](./00-FastAPI知识体系总览.md)
