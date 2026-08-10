# aiohttp 知识体系总览
> 一句话定位：Python 异步 HTTP 客户端 + 服务端双件套——asyncio 生态的 HTTP 地基，写爬虫、网关、Agent 工具层绕不开的异步请求库。

## 📚 目录
1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [常见误区](#5-常见误区)
6. [一周计划](#6-一周计划)
7. [自测题](#7-自测题)
8. [参考来源](#8-参考来源)

## 1. 知识体系导图

```text
aiohttp（客户端 + 服务端双件套）
├── 01 是什么（定位/2026基线/与httpx-requests对比）
├── 02 安装与快速开始（十分钟双端跑通）
├── 03 异步核心与事件循环（await语义/并发模型/uvloop）
├── 客户端四部曲
│   ├── 04 ClientSession 与连接管理（连接池/超时/CookieJar）
│   ├── 05 请求与响应处理（参数/流式/编码/重定向）
│   └── 06 WebSocket 与流式通信（ws_connect/心跳/SSE）
├── 07 服务端 Web 框架（路由/中间件/静态资源）
├── 08 并发限流与性能（gather+信号量/背压/压测）
├── 09 测试与调试（pytest-aiohttp/TraceConfig）
└── 10 生产实战与自测（并发下载器/毕业验收）
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 01 | 是什么 | 双件套定位、2026 基线 3.14.3、vs httpx | 所有人 |
| 02 | 安装与快速开始 | 锁版本、首个请求、首个服务 | 新手 |
| 03 | 异步核心 | 协程模型、事件循环配合、反模式 | 必须 |
| 04 | ClientSession | 连接池、复用、超时、代理、Cookie | 必须 |
| 05 | 请求与响应 | 参数全解、流式下载、编码、重定向 | 必须 |
| 06 | WebSocket | 双向通信、心跳、流式上传 | 进阶 |
| 07 | 服务端框架 | 路由、中间件、WebSocket 服务端 | 进阶 |
| 08 | 并发与性能 | 限流、背压、压测方法论 | 必须 |
| 09 | 测试与调试 | pytest-aiohttp、aioresponses、Trace | 进阶 |
| 10 | 实战与自测 | 并发下载器、面试、20 题自测 | 毕业 |

## 3. 学习路线推荐

**先理解本体系在整座知识库中的位置**。aiohttp 属于「Web & 异步（基座模块）」——它是地基而非应用：爬虫体系里 requests 是同步入门，aiohttp 是异步进阶；FastAPI 体系里 Starlette 负责路由，aiohttp 双件套提供底层 HTTP 与 WebSocket 能力；Python 异步体系讲编程模型本身，这里讲模型在 HTTP 上的具体实现。学完本体系，你会同时获得三样东西：一个并发 HTTP 客户端（爬虫/批量调用/AI 工具层）、一个轻量异步 Web 服务端（网关/推送/内部 API）、以及对 asyncio 并发模型的实战级理解——后者是面试区分度所在。

**路线二：并发进阶（3-5 天）**——路线一 + 03 → 08。目标：掌握信号量限流、连接池调优、背压设计，处理千级并发不被打死。

**路线三：生产实战（一周）**——路线二 + 06 → 07 → 09。目标：双端都会用，能写服务端 API、WebSocket 网关，会测试会调试。

## 4. 核心概念速查

| 概念 | 一句话 |
|------|-------|
| ClientSession | 客户端会话对象，管理连接池与 Cookie，必须 `async with` |
| TCPConnector | 底层连接器，`limit`/`limit_per_host` 控制并发连接数 |
| ClientTimeout | 超时配置：`connect`/`sock_connect`/`sock_read`/`total` 四段分离 |
| raise_for_status | 手动检查 HTTP 状态码，4xx/5xx 抛 ClientResponseError |
| StreamReader | 流式响应体，`iter_chunked()` 大文件分块下载 |
| ws_connect | 客户端 WebSocket 入口，`ws.receive()` 循环收消息 |
| web.Application | 服务端应用对象，挂路由与中间件 |
| RouteTableDef | 装饰器式路由声明，`@routes.get('/path')` |
| middleware | 洋葱模型中间件，请求前/响应后统一处理 |
| Signal（信号量） | `asyncio.Semaphore` 并发限流，爬虫必备 |
| CookieJar | Cookie 存储，`unsafe=True` 允许跨域；3.14 前有反序列化 RCE |
| pytest-aiohttp | 官方测试插件，提供 `asyncio` fixture 与 TestClient |
| TraceConfig | 请求追踪回调：开始/结束/异常/重定向四个钩子 |
| uvloop | 更快的事件循环，与 aiohttp 无缝配合 |

## 5. 常见误区

**误区一：`requests.get()` 习惯了，直接 `aiohttp.get()`**。aiohttp 没有模块级请求函数——一切从 ClientSession 开始，这是连接复用的前提。

**误区二：每次请求新建一个 session**。建 session 就是建连接池，高频创建等于放弃复用，性能直接退化到 requests 水平。

**误区三：不 `async with` 关 session**。不关 session 连接不释放，长期运行泄漏文件描述符，进程悄悄挂掉。

**误区四：忘写 `raise_for_status()`**。aiohttp 默认 4xx/5xx 不抛异常，只返回响应对象，不检查就静默吞掉错误。

**误区五：超时只设一个 total**。不设 `sock_read` 的话，服务器吊死连接你的请求就永远挂着——每个请求都要四段超时齐备。

**误区六：服务端回调里写阻塞代码**。`time.sleep()`、同步 `requests`、大文件读盘都会卡死整个事件循环，所有请求一起等待。

**误区七：老教程的 `BasicAuth`、`auth=` 参数**。3.14 已弃用，aiohttp 4.0 移除，用 `encode_basic_auth()` + headers。

**误区八：把服务端当同步框架写**。处理函数里 sleep、同步读文件、同步库调用——事件循环冻结，全部连接一起卡死；服务端代码与客户端代码遵循同一条异步纪律。

七个误区的共同根源只有一个：**把 aiohttp 当成了 requests 的异步版**。requests 是「调用一次完事」的函数式库，aiohttp 是「管理全生命周期」的组件化库——session 要建要关、请求要进上下文、错误要显式检查、并发要自己限流。带着 requests 的心智模型写 aiohttp，七个坑轮流踩；换成「组件 + 生命周期」的心智模型，一切行为都有了解释。

## 6. 一周计划

**先讲投入产出**：这套体系的正确学习姿态是「写代码验证概念」——异步代码只靠读是学不会的，每个概念（连接复用、信号量、心跳）都要用最小脚本跑一遍。预算每天 40-60 分钟，周末两天的实战日投入两小时完成下载器项目。七天下来你的能力清单是：能写并发爬虫（替代 requests 脚本）、能自建内部 API 与推送网关、能排查「为什么这么慢」「为什么连不上」、面试能讲清异步原理。这份能力对应的是 AI 应用开发里的高频需求——批量调用 LLM API 的并发层、Agent 工具层的 HTTP 封装、实时推送的 WebSocket 服务，全部落在 aiohttp 的射程内。
|:---:|------|------|
| Day1 | 01 + 02：双端跑通 | 一个 GET + 一个 hello 服务 |
| Day2 | 03 + 04：异步模型与连接管理 | 写清 session 生命周期 |
| Day3 | 05：请求响应全解 | 流式下载大文件脚本 |
| Day4 | 08：并发限流 | 信号量限流并发爬虫 |
| Day5 | 06：WebSocket | 实时行情客户端 |
| Day6 | 07 + 09：服务端与测试 | 小 API + pytest 用例 |
| Day7 | 10：综合实战 + 自测 | 并发下载器 + 20 题 |

## 7. 自测题

1. 为什么 aiohttp 没有 `aiohttp.get()` 这样的模块级函数？
2. `async with ClientSession()` 的关闭会做什么，不关会怎样？
3. `limit` 与 `limit_per_host` 的区别？
4. 4xx 响应不抛异常是 bug 还是设计？
5. 流式下载大文件用什么方法，为什么？
6. 服务端路由怎么按方法（GET/POST）区分？
7. 中间件怎么给所有响应加统一 header？
8. WebSocket 心跳机制怎么配？
9. 并发 1000 请求怎么限流到 50？
10. pytest-aiohttp 和已弃用的 `aiohttp.pytest_plugin` 什么关系？

## 8. 参考来源

- [aiohttp 官方文档](https://docs.aiohttp.org/)
- [aiohttp Changelog（3.13.3+）](https://docs.aiohttp.org/en/v3.13.3/changes.html)
- [aio-libs/aiohttp GitHub](https://github.com/aio-libs/aiohttp)
- [PEPY 下载统计（累计 100 亿+）](https://pepy.tech/projects/aiohttp)
- [aio-libs 安全通告（CVE-2026-34993/47265）](https://app.opencve.io/cve/?vendor=aio-libs)
- [pytest-aiohttp 官方插件](https://github.com/pytest-dev/pytest-aiohttp)

---

**下一模块**：[01-aiohttp是什么](01-aiohttp是什么.md) → 从定位与 2026 基线开始。
