# 04 FastAPI 与 ASGI 架构
> 从协议到框架：ASGI 3.0 / Starlette 1.x / FastAPI 0.139 三层关系，请求生命周期与 lifespan

## 📚 目录
1. [FastAPI 2026 版本线速览](#1-fastapi-2026-版本线速览)
2. [三层架构：Pydantic / Starlette / FastAPI](#2-三层架构pydantic--starlette--fastapi)
3. [ASGI：Python 异步 Web 协议](#3-asgipython-异步-web-协议)
4. [请求生命周期：从 ASGI 到响应](#4-请求生命周期从-asgi-到响应)
5. [lifespan：应用生命周期管理](#5-lifespan应用生命周期管理)
6. [Starlette 1.0 破坏性变更与迁移](#6-starlette-10-破坏性变更与迁移)
7. [第一个 FastAPI 应用结构](#7-第一个-fastapi-应用结构)
8. [核心要点](#8-核心要点)

---

## 1. FastAPI 2026 版本线速览

**基准版本：FastAPI 0.139（2026-08）/ Python 3.10+ / Starlette 1.x / Pydantic 2.13+**

| 版本 | 时间 | 关键变化 | 对开发者的影响 |
|------|------|---------|---------------|
| 0.126 | 2025-11 | 移除 Pydantic v1 支持（保留短暂兼容） | Pydantic v2 成为唯一 |
| **0.128** | 2026-01 | **彻底移除 `pydantic.v1`**（破坏性） | 老代码 v1 写法全部失效 |
| 0.130 | 2026-02 | **响应 JSON 序列化走 Pydantic（Rust 核心），性能 2 倍+** | 指定 `response_model` 自动提速 |
| 0.131 | 2026-02 | 常规修复 | — |
| 0.13x | 2026 上半年 | 持续迭代至 0.139 | 跟随小版本 |

> 🎯 **核心要点**：2026 年的 FastAPI = **纯 Pydantic v2**。迁移注意：`int | str` 联合类型在路径/查询参数中 v2 会按 `str` 解析（v1 行为不同）——需要类型转换时用 `field_validator`。性能上**务必写 `response_model`**，白拿 Rust 序列化的 2 倍提速。

## 2. 三层架构：Pydantic / Starlette / FastAPI

```text
┌─────────────────────────────────────────────┐
│  FastAPI（0.139）                            │
│  路由/依赖注入/自动文档/OpenAPI               │  ← 你写的业务层
├─────────────────────────────────────────────┤
│  Starlette（1.1）                            │
│  ASGI 核心：请求响应、路由、中间件、lifespan  │  ← 框架底座
├─────────────────────────────────────────────┤
│  Pydantic（2.13）/ pydantic-core（Rust）      │
│  数据校验、序列化（0.130 起响应也走 Rust）     │  ← 数据层
└─────────────────────────────────────────────┘
```

| 层 | 职责 | 你接触的频率 |
|----|------|:---:|
| FastAPI | 路由装饰器、Depends、模型声明、自动文档 | 每天 |
| Starlette | ASGI 应用本体、中间件、Request/Response 对象 | 每天（间接） |
| Pydantic | 请求校验、响应序列化、Settings | 每天 |

> 💡 理解层级的意义：FastAPI 是"**DSL 层**"——它把 Starlette 的 ASGI 原语包装成声明式 API。遇到文档里没有的底层能力（如原始 ASGI scope 操作），去 Starlette 文档找；遇到校验/序列化问题，去 Pydantic 文档找。三层各查各的文档，问题定位快十倍。

## 3. ASGI：Python 异步 Web 协议

**ASGI（Asynchronous Server Gateway Interface，3.0）** = 异步时代的 WSGI：

```text
Uvicorn（服务器）←→ ASGI 协议 ←→ FastAPI 应用（Starlette）
     │                                   │
 读取 socket / TLS / HTTP 解析      处理 scope/receive/send 事件
 多个 worker 进程                   一个事件循环内并发处理请求
```

**核心概念**：ASGI 应用是一个**可调用对象**（FastAPI 实例就是），接收三个参数：

```python
# ASGI 协议本质（框架内部，一般不用手写）
async def app(scope, receive, send):
    # scope: 请求上下文（method/path/headers/query_string...）
    # receive: 异步取消息（请求体 chunk）
    # send: 异步发消息（响应头/响应体 chunk）
    if scope["type"] == "http":
        await send({"type": "http.response.start", "status": 200, "headers": [...]})
        await send({"type": "http.response.body", "body": b"hello"})
```

| 特性 | 说明 |
|------|------|
| 消息类型 | `http` / `websocket` / `lifespan` 三类事件 |
| 双通道 | receive（收）/ send（发）都是 awaitable 的异步流 |
| 中间件 | 包装 app 的另一个 ASGI 应用（洋葱模型） |
| 服务器无关 | Uvicorn / Hypercorn / Daphne 都能跑同一个 app |
| 流式 | 响应体可分块 send → SSE/流式输出（AI 场景） |

> 🎯 **核心要点**：ASGI 是"**协议**"，FastAPI 是"**框架**"，Uvicorn 是"**服务器**"——三者解耦是 Python 异步 Web 生态的设计精髓。面试答"FastAPI 为什么快"：三层各司其职（Starlette 轻量核心 + Pydantic Rust 校验 + Uvicorn uvloop）。

## 4. 请求生命周期：从 ASGI 到响应

```text
① Uvicorn 接收 socket 连接，解析 HTTP → 构造 scope
② ASGI 调用 app(scope, receive, send)
③ FastAPI 路由匹配 → 找到路径操作函数（endpoint）
④ 中间件链（洋葱）→ 依赖注入解析（Depends）→ 参数校验（Pydantic）
⑤ 执行 endpoint（async def 在事件循环内跑；def 自动丢线程池！）
⑥ 返回值 → response_model 校验/序列化（Rust，0.130+）→ HTTP 响应
⑦ 响应沿中间件链返回 → Uvicorn 写回 socket
```

**⭐ 关键机制：`def` vs `async def` 端点**：

```python
@app.get("/sync")              # def（同步）→ FastAPI 自动丢线程池执行
def sync_endpoint():
    time.sleep(1)              # 阻塞线程池线程，但事件循环不受影响
    return {"ok": True}

@app.get("/async")             # async def → 事件循环内直接执行
async def async_endpoint():
    await asyncio.sleep(1)     # 让出，不占线程
    return {"ok": True}
```

| 端点类型 | 执行位置 | 适用 |
|---------|---------|------|
| `def`（同步） | 线程池（默认 40 线程） | 同步库（PyMySQL/requests/文件）——**安全但吞吐有限** |
| `async def` | 事件循环 | 异步库（httpx/asyncpg/SQLAlchemy async）——**高吞吐** |

> ⚠️ **金科玉律**：端点内部调用了**同步阻塞**代码（time.sleep、requests、PyMySQL），必须用 `def` 写端点——FastAPI 会自动放线程池，不冻结事件循环；若在 `async def` 里调同步阻塞代码 = 冻结整个服务（02 章反模式第 1、2 条）。

## 5. lifespan：应用生命周期管理

**Starlette 1.0 起 `on_startup`/`on_shutdown` 装饰器已移除，lifespan 是唯一推荐方式**（FastAPI 内 `app.on_event` 同样弃用）。

```python
from contextlib import asynccontextmanager
import httpx, redis.asyncio as aioredis
from fastapi import FastAPI


@asynccontextmanager
async def lifespan(app: FastAPI):
    # ── 启动：创建全局资源（连接池/客户端/预热缓存） ──
    app.state.http = httpx.AsyncClient(timeout=15)          # 全局复用（03 章）
    app.state.redis = aioredis.from_url("redis://localhost:6379/0")
    print("应用启动：资源已就绪")
    yield                                            # ← 应用运行期间
    # ── 关闭：清理资源（优雅退出） ──
    await app.state.http.aclose()
    await app.state.redis.aclose()
    print("应用关闭：资源已释放")


app = FastAPI(lifespan=lifespan)                     # 挂载生命周期
```

| 资源 | 为什么放 lifespan | 不放的后果 |
|------|------------------|-----------|
| HTTP 客户端 | 连接池跨请求复用（03 章） | 每请求新建，性能差一个量级 |
| Redis 连接 | 全局单例 | 连接风暴 |
| 数据库引擎/池 | 07 章 | 每请求建连 = 灾难 |
| 模型加载（AI） | 只加载一次 | 每请求加载模型 = 秒级延迟 |

> 🎯 **核心要点**：lifespan 就是"**应用的 open/close**"——**一切跨请求复用的昂贵资源都在这里建**。AI 服务的模型预热、爬虫服务的客户端池、微服务的注册/注销，全部走 lifespan（微服务场景可配合 consul/etcd 注册）。

## 6. Starlette 1.0 破坏性变更与迁移

**Starlette 1.0.0（2026-03-22）**：近 8 年开发后的首个稳定版，**移除了一批装饰器 API**：

| 旧写法（0.x） | 新写法（1.x） | 状态 |
|--------------|--------------|:---:|
| `@app.on_event("startup")` | `lifespan` 上下文管理器 | ❌ 已移除 |
| `@app.route("/x")` | `routes=[Route("/x", handler)]` 或 FastAPI 的 `@app.get` | ❌ 已移除 |
| `@app.websocket_route()` | `routes=[WebSocketRoute(...)]` | ❌ 已移除 |
| `@app.middleware("http")` | `middleware=[Middleware(...)]` | ❌ 已移除 |
| `@app.exception_handler(...)` | `exception_handlers={...}` | ❌ 已移除 |

> ⚠️ **对你的影响**：FastAPI 内部已适配 Starlette 1.x，`@app.get`、`@app.middleware` 等 FastAPI 级写法**不受影响**；只有**绕过 FastAPI 直接用 Starlette 装饰器**的代码需要迁移。2026 年新项目直接用 lifespan + FastAPI 装饰器，无需担心。另外 Starlette 1.0 要求 **Python 3.10+**、仅依赖 anyio——依赖树更轻。

## 7. 第一个 FastAPI 应用结构

```python
# app/main.py —— 工程化目录（10 章展开）
from fastapi import FastAPI
from app.routers import users, items
from app.core.lifespan import lifespan

app = FastAPI(title="示例 API", version="1.0.0", lifespan=lifespan)

app.include_router(users.router, prefix="/users", tags=["用户"])
app.include_router(items.router, prefix="/items", tags=["商品"])
```

```bash
# 运行（开发）
uvicorn app.main:app --reload --port 8000
# 文档
# 打开 http://localhost:8000/docs（Swagger UI，自动生成）
# 打开 http://localhost:8000/redoc
```

> 💡 FastAPI 的"**自动文档**"（OpenAPI 3.x）是生产标配——联调、测试、前端生成客户端都靠它；配合 08 章把文档/健康检查/版本号一并在 /docs 暴露。

## 8. 核心要点

| 序号 | 要点 |
|:---:|------|
| 1 | 基准：FastAPI 0.139 / Starlette 1.1 / Pydantic 2.13 / Python 3.10+（2026-08） |
| 2 | 0.128 起纯 Pydantic v2（pydantic.v1 移除）；0.130 起 response_model 走 Rust 序列化（2 倍+） |
| 3 | 三层：FastAPI（DSL）/ Starlette（ASGI 核心）/ Pydantic（校验序列化） |
| 4 | ASGI 3.0 = 异步 Web 协议（scope/receive/send），服务器与应用解耦 |
| 5 | def 端点进线程池、async def 进事件循环——同步代码用 def 写端点 |
| 6 | lifespan 管全局资源生命周期（连接池/模型/客户端），on_event 已移除 |
| 7 | Starlette 1.0 移除 on_event/@app.route 等装饰器；FastAPI 级写法不受影响 |
| 8 | 新项目三件套：lifespan + include_router + response_model |

---

**下一模块**：[05-路由请求与参数校验](05-路由请求与参数校验.md) / **返回总览**：[00-Python异步与FastAPI知识体系总览](00-Python异步与FastAPI知识体系总览.md)

## 参考来源

- [FastAPI 官方文档](https://fastapi.tiangolo.com/)（0.139）
- [FastAPI Release Notes](https://github.com/fastapi/fastapi/releases)（0.126-0.131：Pydantic v1 移除 / Rust 序列化）
- [Starlette 官方文档](https://www.starlette.io/)（1.1.0；1.0.0 变更）
- [Starlette 1.0 Release Notes](https://github.com/encode/starlette/releases)（2026-03-22）
- [ASGI 规范](https://asgi.readthedocs.io/)
