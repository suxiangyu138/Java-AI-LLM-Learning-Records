# Python 异步 + FastAPI 知识体系总览
> Python 异步深潜（事件循环/并发模式/生态）+ FastAPI 全栈工程（0.139）——"爬虫之外的异步服务端"

## 📚 目录
1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [体系定位与分工](#4-体系定位与分工)
5. [核心概念速查](#5-核心概念速查)
6. [参考来源](#6-参考来源)

## 1. 知识体系导图

```text
Python 异步 + FastAPI ——10 篇（FastAPI 0.139 / Starlette 1.1 / Uvicorn 0.51 / Python 3.14 基准，2026-08）
│
├─ 异步内核 ───────────────────────────
│   ├─ 00 总览（本文）
│   ├─ 01 asyncio 事件循环与异步核心（运行模型/任务取消/超时/3.14 变化）
│   └─ 02 并发控制与异步设计模式（信号量/扇出/背压/线程桥接）
│
├─ 异步生态 ───────────────────────────
│   └─ 03 异步 IO 生态实战（aiohttp/httpx/asyncpg/兼容矩阵）
│
├─ FastAPI 核心 ───────────────────────
│   ├─ 04 FastAPI 与 ASGI 架构（三层/请求生命周期/lifespan）
│   ├─ 05 路由请求与参数校验（Pydantic v2/校验/响应模型）
│   └─ 06 依赖注入与认证授权（Depends/OAuth2/JWT/角色权限）
│
├─ FastAPI 工程 ───────────────────────
│   ├─ 07 异步数据库集成（SQLAlchemy async/连接池/事务）
│   ├─ 08 中间件异常处理与可观测性（CORS/追踪/日志/OTel）
│   └─ 09 生产部署与性能优化（uvicorn 部署/压测/调优）
│
└─ 实战层 ─────────────────────────────
│   └─ 10 实战项目与面试冲刺（综合项目/避坑/自测/面试）
```

> 🎯 一句话定位：本体系覆盖 Python 异步编程的**工程级全链路**——先吃透 asyncio 事件循环与并发控制（爬虫高并发、AI 服务并发的共同地基），再掌握 FastAPI 0.139 的现代 Web 服务开发（依赖注入/异步 DB/生产部署），最终能交付高性能异步 API。

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 00 | [总览（本文）](00-Python异步与FastAPI知识体系总览.md) | 导图、定位、速查 | 所有人 |
| 01 | [asyncio 事件循环与异步核心](01-asyncio事件循环与异步核心.md) | 运行模型/Task/取消/超时/3.14 变化 | 入门必读 |
| 02 | [并发控制与异步设计模式](02-并发控制与异步设计模式.md) | 信号量/扇出/背压/队列/线程桥接 | 重点 |
| 03 | [异步 IO 生态实战](03-异步IO生态实战.md) | aiohttp/httpx/asyncpg/兼容矩阵 | 重点 |
| 04 | [FastAPI 与 ASGI 架构](04-FastAPI与ASGI架构.md) | 三层/ASGI/生命周期/lifespan | 入门必读 |
| 05 | [路由请求与参数校验](05-路由请求与参数校验.md) | 参数三源/Pydantic v2/响应模型 | 重点 |
| 06 | [依赖注入与认证授权](06-依赖注入与认证授权.md) | Depends/yield 依赖/OAuth2/JWT | 重点 |
| 07 | [异步数据库集成](07-异步数据库集成.md) | SQLAlchemy async/连接池/事务/N+1 | 重点 |
| 08 | [中间件异常处理与可观测性](08-中间件异常处理与可观测性.md) | 中间件/CORS/异常/追踪/OTel | 进阶 |
| 09 | [生产部署与性能优化](09-生产部署与性能优化.md) | uvicorn 部署/压测/性能调优 | 进阶 |
| 10 | [实战项目与面试冲刺](10-实战项目与面试冲刺.md) | 综合项目/避坑/自测/面试 | 收尾 |

## 3. 学习路线推荐

| 路线 | 人群 | 路径 |
|------|------|------|
| 快速上手（1 天） | 会 Flask 想转 FastAPI | 04 → 05 → 06 → 09 → 10 |
| 完整学习（3 天） | 系统学习 | 00 → 01 → 02 → 03 → 04 → 05 → 06 → 07 → 08 → 09 → 10 |
| 异步深潜（补课） | 会用 FastAPI 但异步薄弱 | 01 → 02 → 03 → 07 |
| AI 服务开发 | 给 LLM/Agent 写后端 | 01 → 04 → 06 → 07 → 09（配合流式响应） |

> 💡 完成标志：**能解释"await 时事件循环在干什么"+ 独立开发带异步 DB、JWT 认证、生产部署的 FastAPI 服务 + 压测后说清性能瓶颈在哪**——达标后即可无缝衔接 AI 应用开发（LangChain 服务、RAG 后端、Agent 网关）。

## 4. 体系定位与分工

```text
与 Python高级语法/07-协程与asyncio深度.md 的分工：
  高级语法 07 = 语言层入门（协程 vs 线程、async/await 语法、Task 初识）
  本体系     = 工程层深潜（事件循环内部、并发控制模式、异步生态、FastAPI 全栈）

与 Python生态/05-异步编程与AI并发模式.md 的分工：
  生态 05 = AI 视角概览（异步在 AI 并发中的应用模式）
  本体系 = 体系化深潜（语言机制 + Web 框架全链路）

与阶段 6 爬虫框架 的衔接：
  阶段 6 = Scrapy 内置异步引擎（框架内）
  本体系 = 自己写异步代码（事件循环手控），爬虫调度器/自定义下载器会用到
```

> 🎯 认知起点：**异步的收益来自"等待时干别的活"，不是"代码跑得快"**。IO 密集（网络/数据库/磁盘）场景收益巨大，CPU 密集场景需要多进程/多线程配合——这个判断贯穿整个体系。

## 5. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| 事件循环 | 单线程调度器，轮转执行"就绪的协程"（Python 3.14 起支持自由线程构建下的多循环并行） |
| 协程 | async def 函数，执行到 await 让出控制权 |
| Task | 协程的调度单元，并发的最小单位 |
| Future | 异步结果的占位符（Task 是其子类） |
| await | 挂起当前协程，等目标完成（事件循环趁机跑别的） |
| TaskGroup | 结构化并发（3.11+），任务组内一个失败全体取消 |
| asyncio.timeout | 超时上下文管理器（3.11+），超时抛 TimeoutError |
| to_thread | 阻塞代码桥接：丢进线程池执行，不冻结事件循环 |
| 背压（backpressure） | 生产者快于消费者时，控制流入速度（队列上限/信号量） |
| ASGI | Python 异步 Web 网关协议（ASGI 3.0，Starlette 1.x 实现） |
| Starlette | FastAPI 底层的 ASGI 框架（1.1.0，2026-05-23） |
| FastAPI | 基于 Starlette + Pydantic 的现代 Web 框架（0.139，2026-08） |
| Pydantic | 数据校验库（2.13.4），FastAPI 0.130 起响应序列化走 Rust 核心（2 倍+） |
| lifespan | 应用生命周期上下文（Starlette 1.0 起唯一推荐，on_event 已移除） |
| Depends | FastAPI 依赖注入：声明式获取会话/认证/配置 |
| SQLAlchemy async | 2.0 异步 ORM（create_async_engine/AsyncSession） |
| Uvicorn | ASGI 服务器（0.51.0），生产配 uvloop + httptools |
| MissingGreenlet | 异步 ORM 中触发懒加载的经典错误 |

## 6. 参考来源

- [FastAPI 官方文档](https://fastapi.tiangolo.com/)（0.139）
- [FastAPI Release Notes（0.126-0.131：Pydantic v1 移除 / Rust 序列化）](https://github.com/fastapi/fastapi/releases)
- [Starlette 官方文档](https://www.starlette.io/)（1.1.0；1.0.0 = 2026-03-22 首个稳定版，on_event 移除）
- [Uvicorn GitHub](https://github.com/encode/uvicorn)（0.51.0，2026-07-08）
- [Python 官方 asyncio 文档](https://docs.python.org/3/library/asyncio.html)（3.14）
- [Python 官方：asyncio and free-threaded Python](https://docs.python.org/3/library/asyncio-threading.html)
- [Pydantic 官方文档](https://docs.pydantic.dev/)（2.13.4）
- [SQLAlchemy 2.0 async 官方文档](https://docs.sqlalchemy.org/en/20/orm/extensions/asyncio.html)
- [Python高级语法/07-协程与asyncio深度.md](../Python高级语法/07-协程与asyncio深度.md)（语言层入门）
- [阶段 6：爬虫框架](../../12-Python爬虫/阶段%206：爬虫框架（工程化，写大型爬虫）/00-阶段6爬虫框架总览.md)（异步并发另一视角）

---

**下一模块**：[01-asyncio 事件循环与异步核心](01-asyncio事件循环与异步核心.md)
