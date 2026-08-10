# 01-aiohttp是什么
> 定位：Python 异步 HTTP 客户端 + 服务端双件套，asyncio 官方生态里最成熟的 HTTP 库——客户端发起并发请求，服务端处理高并发连接，一个库统治两端。

## 📚 目录
1. [双件套：客户端与服务端](#1-双件套客户端与服务端)
2. [解决什么问题](#2-解决什么问题)
3. [2026 基线](#3-2026-基线)
4. [替代品对比](#4-替代品对比)
5. [生态位置](#5-生态位置)
6. [五条边界](#6-五条边界)
7. [练习](#7-练习)

## 1. 双件套：客户端与服务端

多数 HTTP 库只做一件事：requests 只发请求，Flask 只收请求。aiohttp 的特殊之处在于它同时提供完整的客户端与服务端两套 API，且全部建立在 asyncio 之上——这是它从 2012 年立项至今的核心设计。

客户端侧由 `ClientSession` 统领：管理连接池、Cookie、超时、重试，支持流式上传下载、WebSocket 双向通信，是爬虫、Agent 工具层、网关调用的标准选择。服务端侧由 `web.Application` 统领：路由、中间件、静态资源、WebSocket 服务端一应俱全，能支撑数万并发连接，是轻量 API 服务、推送网关、SSE 中转的可靠底座。

两套 API 共享同一个异步内核与事件循环，客户端和服务端可以跑在同一个进程里，这为「代理/中转」类应用提供了天然形态：一个 aiohttp 服务端收请求，内部用 aiohttp 客户端转发上游，全程异步、零线程切换。

**核心构成**：客户端家族以 `ClientSession` 为门面，下属 `TCPConnector`（连接池）、`ClientTimeout`（超时四段）、`CookieJar`（Cookie 管理）、`ClientResponse`（响应对象）、`StreamReader`（流式读取）、`TraceConfig`（请求追踪）——六个组件管住一次请求从发起到收尾的每一环。服务端家族以 `web.Application` 为门面，下属 `router`（路由表）、`web.Request`/`web.Response`（请求响应对象）、`middleware`（中间件）、`web.WebSocketResponse`（WebSocket 响应）、`web.StreamResponse`（流式响应）、生命周期钩子 `on_startup`/`on_cleanup`——从建应用到收请求到回响应自成体系。这套 API 结构十年稳定：早期学过的 `ClientSession` 写法在 3.14 依然原样可用，变化只在细节（弃用项）而非骨架——学的是一套长期有效的知识。

## 2. 解决什么问题

**requests 的阻塞问题**。requests 是同步库，一个请求要等网络 I/O 完成才返回。100 个请求要么串行（慢），要么开线程池（资源开销大、GIL 抢锁）。aiohttp 把等待时间还给事件循环：1000 个并发请求在一个线程内轮流推进，网络等待期间循环去干别的活。

**服务端的高并发**。同步 Web 服务每连接一个线程，万级连接就是万级线程，内存爆炸。aiohttp 服务端用事件循环处理成千上万个连接，每个连接只是一份协程状态，内存占用低一个数量级。

**全栈异步的黏合**。AI 应用里 vLLM 推理、Redis、数据库驱动都是异步的（`asyncpg`、`redis.asyncio`），如果 HTTP 层是同步的，异步链路就会断在中间——要么被迫 `to_thread` 切换线程，要么阻塞循环。aiohttp 让整条链路从发起到回包全异步，这是它无法被替换的场景。

一个画面：

```python
async def fetch(session, url):
    async with session.get(url) as resp:
        return await resp.text()

async def main():
    async with aiohttp.ClientSession() as session:
        results = await asyncio.gather(
            *(fetch(session, f"https://api.example.com/item/{i}") for i in range(100)))
```

100 个请求同时进行，一个线程，一份连接池。

## 3. 2026 基线

aiohttp 当前最新稳定版 **3.14.3（2026-07-22）**，3.14.0（2026-06-01）是本代里程碑。几个必须知道的基线事实：

**Python 3.14 与自由线程**。3.14.0 起官方支持 Python 3.14，并支持 free-threading（no-GIL）构建；同时移除了 Python 3.9 支持。3.14 弃用了 asyncio 的 policy 系统，aiohttp 相应调整，老教程里 `asyncio.set_event_loop_policy()` 的写法在 3.14 已不可用。

**安全基线**。2026-06-02 披露两个高危 CVE，3.14.0 修复：CVE-2026-34993（`CookieJar.load()` 反序列化不可信输入可致任意代码执行，CVSS 6.4）与 CVE-2026-47265（请求级 `cookies` 参数在跨域重定向后继续发送，导致敏感 Cookie 泄漏，CVSS 7.5）。另外 3.13.3（2026-01-03）修复了代理连接复用导致 Authorization 头泄漏。结论：生产环境必须 ≥3.14.0。

**弃用信号**。`BasicAuth` 类与 `auth=`/`proxy_auth=` 参数已弃用（4.0 移除），改用 `encode_basic_auth()` 生成 Authorization 头；`aiohttp.pytest_plugin` 已弃用，改用 pytest-aiohttp。

**规模**：GitHub 约 16.4K stars，PyPI 累计下载超 100 亿次，近 30 天约 5.5 亿次——Python 异步 HTTP 的第一库。许可证 Apache-2.0。

## 4. 替代品对比

| 维度 | aiohttp | httpx | requests |
|------|---------|-------|----------|
| 异步 | 原生 asyncio | 同步+异步双模式 | 不支持（阻塞） |
| 服务端 | 完整 Web 框架 | 无 | 无 |
| WebSocket | 客户端+服务端全支持 | 仅客户端（依赖第三方） | 不支持 |
| 连接池 | 原生内置 | 原生内置 | 靠 Session 复用 |
| 生态年龄 | 2012 年起，最成熟 | 较新，API 更现代 | 同步事实标准 |
| 与 asyncio 深度集成 | 最深（StreamReader/背压） | 异步是适配层 | 无关 |

选择逻辑：纯客户端且要同步/异步双模式、追求 API 现代感——httpx 更顺手；需要服务端、WebSocket 双向、与 asyncio 深度绑定（背压、流式、底层控制）——aiohttp 是唯一答案。服务端对比 FastAPI：FastAPI 在 Starlette 之上加 Pydantic 校验与 OpenAPI 文档，适合对外 API 业务；aiohttp 服务端更底、更轻，适合内部网关、代理、WebSocket 长连接场景。两者不是竞争关系，而是不同层。

## 5. 生态位置

aiohttp 是 Python 异步生态的「地基件」：大量知名项目直接依赖它——Home Assistant（智能家居）、Jupyter、Bitwarden 客户端、以及无数爬虫与 Agent 工具链。AI 场景里，调用外部 API 的异步层、多模型网关的转发层、实时推送的 WebSocket 层，aiohttp 都是默认选项之一。它与 [[../../强烈推荐（做项目必用，简历加分）/loguru/00-loguru总览|loguru]] 搭配是异步程序的日志标准姿势，与 [[../../../01-Python语言/Python%20异步%20+%20FastAPI/00-Python异步与FastAPI总览|Python 异步 + FastAPI]] 体系是同一门课的两半：那边讲异步编程模型本身，这里讲异步 HTTP 的具体实现。

## 6. 五条边界

**不是数据库驱动**——aiohttp 只管 HTTP；异步数据库请用 asyncpg、aiomysql、redis.asyncio，由你组合。

**不是全功能业务框架**——没有 Django 式的 ORM、Admin、迁移工具；它是 HTTP 层，业务层自己搭。

**不处理同步代码**——同步阻塞调用写在协程里会卡死事件循环，这是它的运行前提而非缺陷。

**不是安全框架**——认证、限流、CORS 需要中间件自己实现或借助第三方；它只负责按协议收发。

**不是性能银弹**——异步解决并发 I/O 密集问题；CPU 密集任务它同样无能为力，该上进程池还是得上。

## 7. 练习

1. 用一句话向别人解释 aiohttp 和 requests 的本质区别。
2. aiohttp 与 httpx 相比，服务端和 WebSocket 能力意味着什么使用场景？
3. 为什么说 3.14.0 是安全分水岭？列两个 CVE。
4. 全栈异步的「黏合」价值在 AI 应用里指什么？
5. 说出 aiohttp 的五条边界中的任意三条。

> 🎯 **核心要点**：aiohttp = 异步 HTTP 双件套，客户端服务端一套 API；2026 基线 3.14.3，Python 3.14 支持，两大 CVE 已修复；它与 httpx 是选择关系，与 FastAPI 是层次关系。

---

**下一模块**：[02-安装与快速开始](02-安装与快速开始.md)｜**返回总览**：[00-aiohttp总览](00-aiohttp总览.md)
