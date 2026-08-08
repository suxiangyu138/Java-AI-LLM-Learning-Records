# 05 MCP 协议接入工程

> 定位：工具接入的"USB-C 接口"——MCP 协议本质、2026-07-28 新版（无状态）、stdio/HTTP 双传输、LangGraph 接入（2026-08 基准）

## 📚 目录

1. [MCP 是什么：为什么需要它](#1-mcp-是什么为什么需要它)
2. [协议本质：客户端-服务器-工具](#2-协议本质客户端-服务器-工具)
3. [2026-07-28 新版：无状态革命](#3-2026-07-28-新版无状态革命)
4. [双传输：stdio 与 Streamable HTTP](#4-双传输stdio-与-streamable-http)
5. [Python 接入代码](#5-python-接入代码)
6. [LangGraph 集成](#6-langgraph-集成)
7. [选型与常见坑](#7-选型与常见坑)

## 1. MCP 是什么：为什么需要它

MCP（Model Context Protocol）= **AI 应用连接外部工具/数据的开放标准**（Anthropic 2024-11 发起，2025-12 捐赠 Linux 基金会 AAIF）。

```text
没有 MCP 之前：每个工具一套接入（HTTP 的写 HTTP 适配、数据库的写数据库驱动、
              每个框架再包一层）→ N 个工具 × M 个框架 = N×M 适配

有了 MCP 之后：工具实现一次 MCP 服务器，任何 MCP 客户端（Claude/OpenAI/LangGraph...）
              直接发现和调用 → N + M 适配
```

| 2026 生态事实 | 数据 |
|--------------|------|
| 采用 | OpenAI、Google、主流编码工具全接入 |
| 下载量 | 1.5 亿+ |
| 公开服务器 | 7000+ 公网 IP，约 20 万实例 |
| 治理 | Linux 基金会 AAIF（2025-12 捐赠） |

> 🎯 **核心要点**：MCP 的价值 = **"工具接口的 USB-C"**——一次实现、处处可用。你阶段 1 手写的工具注册表，MCP 化之后就是"可被任何 AI 客户端发现的标准化工具"。

## 2. 协议本质：客户端-服务器-工具

```text
MCP 客户端（你的 Agent/框架）
  │  ① initialize（握手/能力协商）
  │  ② tools/list（发现工具：名字/描述/schema）
  │  ③ tools/call（调用工具：参数 → 结果）
  ▼
MCP 服务器（工具的实现者：文件系统/数据库/第三方 API...）
```

| 概念 | 说明 |
|------|------|
| 工具 | 服务器暴露的可调用函数（JSON Schema 声明） |
| 资源 | 可读取的数据（文件、文档） |
| 提示词（已弃用） | 模板提示（2026 新版弃用） |
| 传输 | stdio（本地）/ Streamable HTTP（远程） |

## 3. 2026-07-28 新版：无状态革命

AAIF 发布协议史上最大修订（2026-07-28）：

| 变化 | 说明 | 影响 |
|------|------|------|
| **无状态请求中心** | 去掉会话/初始化握手；能力、协议版本、身份进每请求元数据 | 可部署在普通负载均衡器后（无需粘滞会话） |
| 新能力 | 多轮请求协商、**MCP Apps**（沙箱 iframe UI）、**MCP Tasks**（长任务）、`server/discover`、确定性缓存 | 企业基础设施就绪 |
| **弃用清单** | Sampling、Roots、Logging、HTTP+SSE 传输、动态客户端注册 | 12 个月弃用窗口 |
| **安全强化** | OAuth 2.1/OIDC 授权 + **强制 iss 校验**（SEP 2468） | 关闭 OAuth mix-up 攻击类 |

> ⚠️ **2026 弃用即信号**：看到教程教 HTTP+SSE 传输或 Sampling → 已过时。新项目用 Streamable HTTP + OAuth 2.1。

## 4. 双传输：stdio 与 Streamable HTTP

| 传输 | 适用 | 特点 | 2026 状态 |
|------|------|------|:---:|
| **stdio** | 本地子进程（CLI 工具、开发环境） | 进程间管道，零网络面 | 本地标准；**远程暴露 = 高危** |
| **Streamable HTTP** | 远程服务器（云部署） | REST 风格、负载均衡友好 | ✅ 云端标准（取代 HTTP+SSE） |

```text
选型：本地工具/开发 → stdio（启动即连）
     远程服务/生产 → Streamable HTTP（无状态设计就是为了它）
```

> ⚠️ **stdio 安全红线**：stdio 服务器**绝不能暴露到网络**——2026 年最大的 MCP 安全事故（06 篇详述）。

## 5. Python 接入代码

```python
"""mcp_client.py — MCP 客户端接入（Python SDK，新旧协议过渡期双支持）"""
import asyncio
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

async def use_mcp_stdio(server_command: str, args: list[str]) -> None:
    """连接本地 stdio MCP 服务器"""
    server = StdioServerParameters(command=server_command, args=args)

    async with stdio_client(server) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()

            # ① 发现工具
            tools = await session.list_tools()
            for t in tools.tools:
                print(f"工具: {t.name} - {t.description[:50]}")
                print(f"  schema: {t.inputSchema}")

            # ② 调用工具
            result = await session.call_tool("get_weather", {"city": "北京"})
            print(f"结果: {result.content}")

# asyncio.run(use_mcp_stdio("python", ["weather_server.py"]))
```

```python
"""mcp_http.py — Streamable HTTP 客户端（远程服务器）"""
from mcp import ClientSession
from mcp.client.streamable_http import streamablehttp_client
from mcp.types import OAuth2Parameters  # 新版：OAuth 2.1 授权

async def use_mcp_http(url: str, token: str) -> None:
    async with streamablehttp_client(url, token=token) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            tools = await session.list_tools()
            result = await session.call_tool("search", {"query": "AI Agent"})
            print(result.content)
```

| 接入要点 | 说明 |
|---------|------|
| 生命周期 | `initialize → list_tools → call_tool`（与手写协议五步同构！） |
| 新旧协议 | SDK 过渡期双支持；生产跟随 2026-07 新版 |
| 鉴权 | 远程必须 OAuth 2.1（token 换入） |

> 💡 **心智迁移**：MCP 的 list_tools/call_tool 就是阶段 1 的 tools 声明/execute_tool——**你已经会了，只是格式标准化了**。

## 6. LangGraph 集成

```python
"""langgraph_mcp.py — LangGraph 中接入 MCP 工具"""
from langchain_mcp_adapters.tools import load_mcp_tools
from langgraph.prebuilt import ToolNode
from langchain.agents import create_agent
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client
from contextlib import AsyncExitStack


async def build_mcp_agent():
    # 1. 连接 MCP 服务器
    server = StdioServerParameters(command="python", args=["weather_server.py"])
    stack = AsyncExitStack()
    read, write = await stack.enter_async_context(stdio_client(server))
    session = await stack.enter_async_context(ClientSession(read, write))
    await session.initialize()

    # 2. MCP 工具 → LangChain 工具
    mcp_tools = await load_mcp_tools(session)

    # 3. 与本地工具合并，交给 create_agent（阶段 3 技能复用）
    agent = create_agent(
        model=model,
        tools=[*mcp_tools, local_tool],     # MCP + 本地混合
        system_prompt="你是一个多源工具助手。",
    )
    result = agent.invoke({"messages": [("user", "北京天气如何？")]})
    return result["messages"][-1].content
```

| 集成收益 | 说明 |
|---------|------|
| 工具混合 | MCP 工具与本地 @tool 无差别使用 |
| 自动注入 | Supervisor/Handoffs 应用自动获得 MCP 工具 |
| 复用框架能力 | 熔断/评估/可观测照常工作（工具就是工具） |

## 7. 选型与常见坑

| 坑 | 现象 | 修复 |
|----|------|------|
| stdio 服务器暴露公网 | 巨大攻击面（06 篇） | 远程一律 Streamable HTTP + 鉴权 |
| 忘 initialize | 调用报错 | 连接后必须先握手 |
| 工具名冲突 | MCP 工具与本地重名 | 加载时重命名/前缀 |
| 旧传输教程 | HTTP+SSE 已弃用 | 用 Streamable HTTP |
| 无鉴权远程 | 数据裸奔 | OAuth 2.1 + iss 校验 |
| 长任务阻塞 | 调用卡死 | MCP Tasks（新能力）/ 异步 |

**选型主线**：

```text
工具在本地/开发 → stdio（快、零配置）
工具在云端/生产 → Streamable HTTP + OAuth（2026 标准）
工具是第三方 → 优先找现成 MCP 服务器（7000+ 可选），但要过安全审查（06 篇）
```

> 🎯 **核心要点**：MCP 接入 = 三行核心代码（initialize/list_tools/call_tool）+ 一个传输选择 + 一份安全审查。你已经会协议循环，MCP 只是"标准化的工具接口"——**最难的不是接入，是信任**（06 篇的安全基线）。

---

**返回总览**：[00-阶段总览：进阶](00-阶段总览：进阶.md) / **上一模块**：[04-多 Agent 状态与上下文工程](04-多%20Agent%20状态与上下文工程.md) / **下一模块**：[06-MCP 安全](06-MCP%20安全.md)
