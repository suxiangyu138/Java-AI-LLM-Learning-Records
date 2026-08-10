# 07 - 与 FastAPI 配合

> 本体系第七课：uvicorn 在 FastAPI 生态里的位置——三件套组合、分工、常见用法——"FastAPI + uvicorn = 2026 年 Python API 开发的标准组合——一个写、一个跑"

---

## 📚 目录

1. [FastAPI 生态全景](#1-fastapi-生态全景)
2. [组合的分工](#2-组合的分工)
3. [常见配合用法](#3-常见配合用法)
4. [与全栈体系的衔接](#4-与全栈体系的衔接)
5. [练习 5 题](#5-练习-5-题)
6. [本节验收](#6-本节验收)

---

## 1. FastAPI 生态全景

**FastAPI 应用的完整生态——uvicorn 是"运行时"那一环**：

```text
FastAPI 生态（从写代码到跑起来）
├── FastAPI：框架——路由/参数/文档（怎么写接口）
├── Pydantic：数据校验——请求/响应模型（数据对不对）
├── uvicorn：服务器——把代码跑起来（本体系）
├── SQLAlchemy/异步驱动：数据库（数据存取）
└── 其他：中间件/测试/部署工具（Python 异步 + FastAPI 体系的完整图）
    ——"FastAPI 是'身体'，Pydantic 是'骨架'，uvicorn 是'心脏'——各司其职"
```

**生态心智**：**"生态的记忆：'FastAPI 写、Pydantic 验、uvicorn 跑'"**——"**三件套的分工：FastAPI 管'接口怎么定义'、Pydantic 管'数据怎么校验'、uvicorn 管'请求怎么处理'——'缺一不可——但只有 uvicorn 是'跑起来'的那一个'"**（"FastAPI 生态的完整知识在 `../../../01-Python语言/Python%20异步%20+%20FastAPI/` 体系（全栈地图）——**'本体系是'心脏'的说明书'"**）；**生态的安装**——"`pip install fastapi` 不含 uvicorn（要单独装）——**'FastAPI 教程让你另装 uvicorn——因为它不是 FastAPI 的一部分（01 篇误区一）'"**（"`uvicorn[standard]` + `fastapi` 是标准组合——**'两个独立库，组合使用'"**）。

## 2. 组合的分工

**FastAPI 与 uvicorn 的分工——"写"与"跑"的边界**：

```text
分工边界
├── FastAPI（写）：
│    路由（@app.get）——接口定义
│    参数校验（Pydantic）——数据契约
│    文档（/docs）——自动 API 文档
├── uvicorn（跑）：
│    监听端口——接收 HTTP 请求
│    ASGI 协议——翻译请求给 FastAPI（03 篇）
│    并发处理——异步事件循环（03 篇）
└── 边界：FastAPI 不知道"网络"长什么样；uvicorn 不知道"业务"是什么
    ——"FastAPI 管'逻辑'，uvicorn 管'网络'——分工让各自专注"
```

**分工心智**：**"分工的记忆：'FastAPI 管逻辑、uvicorn 管网络——中间靠 ASGI 协议'"**——"**调试的价值：报错先分'哪一侧'——路由 404 是 FastAPI 的问题、连接失败是 uvicorn/网络的问题——'分工会定位（09 篇排障的前提）'"**（"报错定位三问：服务起了吗（uvicorn）、路由对了吗（FastAPI）、网络通了吗（Nginx/防火墙）——**'排障先分侧'"**）；**换服务器的可能**——"FastAPI 应用可以换服务器（Hypercorn 等——ASGI 兼容）——**'但 uvicorn 是默认与最优（性能 + 生态——01 篇）'"**（"组合的稳定性：FastAPI + uvicorn 是官方推荐组合（文档示例全是 uvicorn）——**'跟官方走，别自己发明'"**）。

## 3. 常见配合用法

**FastAPI + uvicorn 的常见配合姿势**：

```python
# 配合一：生命周期（FastAPI 的启动/关闭——uvicorn 负责触发）
from contextlib import asynccontextmanager
from fastapi import FastAPI

@asynccontextmanager
async def lifespan(app: FastAPI):
    # 启动时（连接池/加载模型——uvicorn 启动时执行）
    print("服务启动——准备资源")
    yield
    # 关闭时（释放资源——uvicorn 停机时执行）
    print("服务关闭——释放资源")

app = FastAPI(lifespan=lifespan)
# 配合二：WebSocket（uvicorn 的 WebSocket 支持——03 篇）
# from fastapi import WebSocket
# @app.websocket("/ws")——实时通信（聊天/推送）

# 配合三：流式响应（StreamingResponse——长连接——uvicorn 流式能力）
# from fastapi.responses import StreamingResponse
# @app.get("/stream")——SSE/流式输出（LLM 对话流——AI 场景常用）
```

**配合心智**：**"配合的记忆：'生命周期 + WebSocket + 流式——三个高频配合'"**——"**生命周期（lifespan）：启动时初始化（连接池/模型加载——AI 场景加载模型）、关闭时清理——'uvicorn 的启动/停机信号驱动 lifespan（03 篇的 lifespan 消息）'"**（"AI 场景的典型：uvicorn 启动时加载 LLM 模型（一次加载反复用）——**'lifespan = 资源的生命周期管理'"**）；**WebSocket 与流式**——"聊天/实时推送用 WebSocket（uvicorn 支持——03 篇）；LLM 流式输出（token 流）用 StreamingResponse（SSE——逐字输出）——**'AI 应用（LLM API 服务）的标配：uvicorn + 流式'"**（"LLM 部署场景：FastAPI + uvicorn 包装模型推理（`模型推理与部署/` 体系有讲）——**'uvicorn 是 LLM API 服务的常见底座'"**）；**配合的工程**——"启动命令/配置写进项目文档（04 篇——README 的启动姿势）——**'组合的用法要文档化（团队交接）'"**。

## 4. 与全栈体系的衔接

**本体系与 Python 异步 + FastAPI 体系的分工**：

```text
分工（两套体系）
├── Python 异步 + FastAPI 体系（全栈地图——11 篇）：
│    asyncio 原理/路由参数/依赖注入/数据库/部署/压测——"FastAPI 全景"
└── 本体系（uvicorn 单库——了解即可）：
     uvicorn 是什么/启动/部署/调优——"心脏的说明书"
    ——"全栈体系是'整车手册'，本体系是'发动机手册'——看整车时发动机章节用本体系"
```

**衔接心智**：**"衔接的记忆：'本体系是 FastAPI 全景里的'服务器'章节的展开'"**——"**学习顺序：先 FastAPI 全景（会写接口）→ 本体系（懂服务器）→ 回全景的部署篇（完整部署）——'两套体系互补，不冲突'"**（"内容的重叠管理：部署/压测在全景有、本体系也有——**'本体系聚焦 uvicorn 视角（服务器侧），全景聚焦 FastAPI 视角（应用侧）'"**）；**衔接的落地**——"看全景的部署篇遇到 uvicorn 参数 → 查本体系 04/05/06 篇——**'本体系是全景的'查字典'"**（"看本体系遇到 FastAPI 概念 → 查全景——**'两套体系互相引用'"**）。

**配合的实战场景**（uvicorn 在真实项目里的典型出现）："**① LLM API 服务**——FastAPI + uvicorn 包装模型推理（加载一次模型 + 流式输出——07 篇配合的 AI 版——`模型推理与部署/` 体系的落地底座）；**② 微服务网关后端**——每个服务一个 uvicorn（Nginx 统一入口——05 篇）；**③ 异步任务平台**——uvicorn 扛 WebSocket 长连接（实时推送）——**'三个场景 = uvicorn 的典型出场——'服务器是应用的底座'"**（"配合的本质：uvicorn 让 FastAPI 的异步能力真正兑现——**'没有 uvicorn，FastAPI 的 async def 只是语法'"**）。

**配合的版本兼容**（两库一起升级的注意）："**FastAPI 与 uvicorn 独立发版**——FastAPI 更新不强制 uvicorn 更新（反之亦然）——**'两库独立演进，组合稳定'"**（"升级的姿势：分别看各自 release notes——**'组合升级 = 各自评估，别一起盲升'"**）；**配合的常见版本坑**——"FastAPI 新特性需要新 uvicorn（如新的 WebSocket 特性）——**'新特性报错先查另一方的版本（09 篇版本排障）'"**（"锁版本区间（`>=0.49,<1.0`——01 篇）保证组合稳定——**'锁版本 = 组合的复现保障'"**）。

## 5. 练习 5 题

1. FastAPI 生态三件套？（FastAPI 写/Pydantic 验/uvicorn 跑）
2. 分工的边界？（FastAPI 管逻辑/uvicorn 管网络）
3. 排障先分侧的三问？（服务起没/路由对没/网络通没）
4. 三个高频配合？（生命周期/WebSocket/流式）
5. 与全栈体系的分工？（发动机手册 vs 整车手册）

## 6. 本节验收

**验收动作**：① 给 FastAPI 应用加 lifespan（启动/关闭打印）；② 写一个 WebSocket 端点跑通；③ 写一个流式响应（StreamingResponse）体验；④ 画"FastAPI 生态分工图"——**"生态位置 + 分工 + 配合 = 懂配合"**——**练习纪律**：报错先分侧——"排障先分侧（uvicorn vs FastAPI）"。

> 🎯 **核心要点**：FastAPI 生态（**三件套：FastAPI 写/Pydantic 验/uvicorn 跑——只有 uvicorn 是'跑起来'的那个——fastapi 不含 uvicorn 要另装**）；**分工（FastAPI 管逻辑/uvicorn 管网络——排障先分侧三问）**；**三个高频配合（生命周期 lifespan 驱动资源/WebSocket 实时/StreamingResponse 流式——LLM API 服务的标配底座）**；与全栈体系衔接（**发动机手册 vs 整车手册——互相引用互补**）——"FastAPI + uvicorn = 2026 年 Python API 开发的标准组合——一个写、一个跑"。

---

**上一模块**：[06-性能与调优.md](./06-性能与调优.md) / **下一模块**：[08-日志与监控.md](./08-日志与监控.md)
