# 03 - ASGI 协议概念

> 本体系第三课：uvicorn 工作的"语言"——scope/receive/send、异步模型——"理解 ASGI 三件套，就理解了 uvicorn 为什么存在——知道'协议'即可，不深挖实现"

---

## 📚 目录

1. [ASGI 协议的定位](#1-asgi-协议的定位)
2. [三件套：scope/receive/send](#2-三件套scopereceivesend)
3. [异步模型：并发从哪里来](#3-异步模型并发从哪里来)
4. [协议边界：HTTP/WebSocket](#4-协议边界httpwebsocket)
5. [练习 5 题](#5-练习-5-题)
6. [本节验收](#6-本节验收)

---

## 1. ASGI 协议的定位

**ASGI（Asynchronous Server Gateway Interface）= 服务器与应用之间的"通用语言"（异步版 WSGI）**：

```text
ASGI 的定位
├── 本质：一个接口规范——服务器（uvicorn）与应用（FastAPI）怎么对话
├── 类比：USB 接口——不管什么设备（应用），插上统一的接口就能通信
├── 历史：WSGI（2003 同步）→ ASGI（2019 异步）——异步时代的接口标准
└── 意义：写一次应用（ASGI 兼容），换任何 ASGI 服务器都能跑
    ——"uvicorn 是'说 ASGI 话'的服务器——FastAPI 是'说 ASGI 话'的应用——协议是普通话"
```

**协议心智**：**"ASGI 的记忆锚点：'服务器与应用的普通话'"**——"**uvicorn 负责'听 HTTP 请求'（网络层）、FastAPI 负责'处理请求'（业务层）——中间靠 ASGI 协议传递——'协议 = 分工的边界'"**（"没有协议：每个服务器要写死支持特定框架（耦合）——有协议：任何 ASGI 应用都能跑——**'协议的价值 = 生态的通用性'"**）；**了解即可的协议观**——"你写 FastAPI 时不用直接碰协议（框架封装了）——但**理解三件套（scope/receive/send）能帮你：排障（报错术语）、中间件（自定义）、理解原理（异步从哪来）**——'协议是'底层地图'——平时不看，迷路时有用'"（"深挖协议实现（消息格式细节）没必要——**'知道三件套和异步模型，本体系 03 篇就够'"**）。

## 2. 三件套：scope/receive/send

**ASGI 应用的本质 = 一个异步函数（接收三样东西）**：

```python
# ASGI 应用的最小形态（了解即可——你平时写 FastAPI 不用这样）
async def app(scope, receive, send):
    """scope：请求的信息（方法/路径/头）；receive：收消息（请求体）；send：发消息（响应）"""
    if scope["type"] == "http":
        # 收到请求 → 发响应
        await send({
            "type": "http.response.start",
            "status": 200,
            "headers": [(b"content-type", b"text/plain")],
        })
        await send({"type": "http.response.body", "body": b"Hello"})

# FastAPI 的封装：你写 @app.get("/")——框架内部翻译成 ASGI 应用
# uvicorn 的工作：把 HTTP 请求翻译成 scope/receive/send → 调你的应用
```

**三件套心智**：**"三件套的记忆：'scope 是名片、receive 是耳朵、send 是嘴巴'"**——"**scope：请求的'身份证'（方法/路径/头/查询参数）；receive：收消息（请求体/WebSocket 消息）；send：发消息（响应头/响应体）——'应用 = 看名片 → 听请求 → 说响应'"**（"理解三件套的意义：中间件（FastAPI 的中间件就是包装这三个）——**'会三件套 = 会写中间件（高级能力）'"**）；**FastAPI 的封装**——"你写的路由（@app.get）被框架翻译成 ASGI 应用——**'你几乎不直接写三件套——但报错/中间件时看得懂'"**（"FastAPI 中间件（CORS/日志——Python 异步 + FastAPI 体系有讲）本质是包了一层 scope/receive/send——**'三件套 = 中间件的底层'"**）；**生命周期消息**——"lifespan（启动/关闭事件——FastAPI 的 startup/shutdown）也是 ASGI 消息类型——**'知道 lifespan 是 ASGI 的一部分——FastAPI 的生命周期基于它'"**（"了解即可：不深究格式细节——**'知道有 http/lifespan/websocket 三类消息'"**）。

## 3. 异步模型：并发从哪里来

**uvicorn 的并发 = 单进程异步事件循环（不是多线程/多进程）**：

```text
异步并发模型
├── 传统（WSGI/Gunicorn）：一个请求占一个线程/进程——并发 = 资源堆
├── uvicorn：一个事件循环处理所有请求（async/await）
│    ├── 请求来了 → 事件循环登记（不阻塞）
│    ├── 等 IO（数据库/网络）→ 让出（处理下一个请求）
│    └── IO 完成 → 回来继续（接着处理）
└── 效果：单进程扛数千并发（IO 密集场景）——"等待不占资源"
    ——"异步的比喻：咖啡店一个服务员（事件循环）——下单（请求）后让客人等——
      同时招呼其他客人——而不是每桌配一个服务员（线程）"
```

**异步心智**：**"异步的记忆锚点：'等待不占资源'——IO 等待时去干别的"**——"**uvicorn 单进程的并发能力 = 事件循环的'同时等待'能力——'一个进程扛数千并发（IO 密集）——这就是 ASGI 时代的性能革命'"**（"对比：Gunicorn 每 worker 一个请求（同步）——并发靠进程数——**'异步是'效率'，多进程是'数量'——uvicorn 单进程 + 多 worker（05 篇）双管齐下'"**）；**异步的适用边界**——"IO 密集（Web/API/数据库）→ 异步大放异彩；CPU 密集（计算/压缩）→ 异步没用（占着循环）——**'CPU 密集任务要放线程池/进程池（FastAPI 的 def 端点自动处理）'"**（"`async def` vs `def` 的选择（02 篇）——**'IO 用 async、CPU 用 def（线程池兜底）'"**）；**事件循环的实现**——"uvicorn 用 asyncio（标准）或 uvloop（standard 扩展——加速版——06 篇）——**'uvloop = 事件循环的'涡轮增压'——装 standard 就有'"**。

## 4. 协议边界：HTTP/WebSocket

**uvicorn 支持的协议——知道边界**：

```text
uvicorn 协议支持
├── HTTP/1.1：完全支持（Web 的基础协议——主力）
├── WebSocket：支持（实时通信——聊天/推送——ASGI 时代的特色）
├── HTTP/2：支持有限（不是完整实现）——要完整 HTTP/2 用 Hypercorn
└── 边界的意义：
    "WebSocket 是 uvicorn 的'独门'（对比 WSGI 服务器不支持）——
     HTTP/2 是别人的主场（知道即可）"
```

**协议边界心智**：**"协议边界的记忆：'HTTP/1.1 + WebSocket 是主场，HTTP/2 是客场'"**——"**WebSocket（长连接双向通信）是 ASGI 服务器相对 WSGI 的升级——'FastAPI 的 WebSocket 端点（实时功能）依赖 uvicorn 的 WebSocket 支持'"**（"聊天/实时推送场景：uvicorn + FastAPI WebSocket——**'异步服务器 = 实时应用的底座'"**）；**HTTP/2 的现状**——"uvicorn 的 HTTP/2 支持有限（实验性）——**'需要 HTTP/2（性能/多路复用）→ 换 Hypercorn 或前面加 Nginx（05 篇反代解决）'"**（"现实的姿势：Nginx 做 HTTP/2 终止 + uvicorn 跑 HTTP/1.1——**'反代把协议问题解决了（05 篇）'"**）；**SSE（服务器推送）**——"SSE（Server-Sent Events——单向推送）在 HTTP/1.1 上可行（FastAPI 的 StreamingResponse）——**'流式响应 = uvicorn 的流式能力（长连接）'"**（"了解即可：知道 uvicorn 支持流式/长连接——**'流式/SSE/WebSocket = 异步服务器的三件特色菜'"**）。

## 5. 练习 5 题

1. ASGI 是什么？（服务器与应用的普通话）
2. 三件套各是什么？（名片/耳朵/嘴巴）
3. 异步并发从哪来？（事件循环——等待不占资源）
4. 异步的适用边界？（IO 密集主场/CPU 密集要线程池）
5. uvicorn 的协议边界？（HTTP/1.1+WebSocket 主场/HTTP/2 客场）

## 6. 本节验收

**验收动作**：① 给同学讲三件套（scope/receive/send——3 分钟）；② 画异步模型图（事件循环 + 等待让出）；③ 写一个 FastAPI WebSocket 端点并跑通（验证 uvicorn 的 WebSocket）；④ 默写协议边界——**"三件套 + 异步模型 + 边界 = 懂协议"**——**练习纪律**：报错里出现 scope/receive 术语时能对上号——"协议术语是排障的字典"。

> 🎯 **核心要点**：ASGI = 服务器与应用的普通话（**写一次应用任何 ASGI 服务器能跑——协议的价值 = 生态通用性**）；**三件套（scope 名片/receive 耳朵/send 嘴巴——中间件的底层——lifespan 也是 ASGI 消息）**；**异步模型（事件循环——等待不占资源——单进程扛数千并发——IO 用 async/CPU 用 def 线程池）**；协议边界（**HTTP/1.1 + WebSocket 主场/HTTP/2 客场——Nginx 反代解决——流式/SSE/WebSocket 是异步服务器三特色菜**）——"理解三件套，就理解了 uvicorn 为什么存在"。

---

**上一模块**：[02-快速开始.md](./02-快速开始.md) / **下一模块**：[04-启动方式.md](./04-启动方式.md)
