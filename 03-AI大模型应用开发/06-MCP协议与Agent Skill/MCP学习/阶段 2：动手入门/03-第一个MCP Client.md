# 03 第一个 MCP Client

> 用官方 Python SDK 写一个最小 Client：以子进程方式启动 Server、连接 stdio、列出工具、调用工具——完成"MCP 全链路第一次闭环"。

## 📚 目录

1. [Client 的角色与最小模型](#1-client-的角色与最小模型)
2. [最小 Client：连接与调用](#2-最小-client连接与调用)
3. [全链路闭环演示](#3-全链路闭环演示)
4. [TypeScript 对照实现](#4-typescript-对照实现)
5. [Client 关键 API 速查](#5-client-关键-api-速查)
6. [易错点与规范](#6-易错点与规范)

## 1. Client 的角色与最小模型

```text
MCP Host（如 Claude Code）
 └── Client（连接一个 Server）──── 子进程/HTTP ────> Server（工具/资源/提示）
```

- Client 负责：以子进程方式启动 stdio Server（或连 HTTP）、发送 JSON-RPC、解析响应、把工具暴露给上层 LLM。
- 本阶段用官方 `mcp` SDK 的 `ClientSession`——它是协议层最小实现，比 fastmcp 的客户端更接近本质。
- 注意：Client 自己不带 LLM。真实产品里 LLM 在 Client 之上（Host 层），本阶段用"写死参数"代替模型决策。

## 2. 最小 Client：连接与调用

```python
# client.py —— 依赖官方 SDK（uv add mcp）
import asyncio
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

async def main():
    # 1) 定义要启动的 Server：与 claude mcp add 的配置同构
    params = StdioServerParameters(
        command="uv",
        args=["run", "python", "server.py"],   # 阶段2 的 02 篇 Server
        env=None,                                # 可注入环境变量
    )

    # 2) 启动子进程并建立 stdio 会话
    async with stdio_client(params) as (read, write):
        async with ClientSession(read, write) as session:
            # 3) 初始化（v1 协议需 initialize；2026-07 规范为请求级协商）
            await session.initialize()

            # 4) 列出工具
            tools = await session.list_tools()
            print("可用工具:", [t.name for t in tools.tools])

            # 5) 调用工具（写死参数 = 模拟模型决策）
            result = await session.call_tool("add", {"a": 3, "b": 4})
            print("add(3,4) =", result.content[0].text)

            result = await session.call_tool("get_current_time", {"timezone": "Asia/Shanghai"})
            print("上海时间 =", result.content[0].text)

if __name__ == "__main__":
    asyncio.run(main())
```

**跑起来**：

```bash
cd mcp-demo
uv run python client.py
```

**预期输出**：

```text
可用工具: ['get_current_time', 'add']
add(3,4) = 7
上海时间 = 2026-08-09T18:35:12.345678+08:00
```

## 3. 全链路闭环演示

```text
client.py ──stdio──> uv run python server.py
   │                     │
   ├─ initialize() ──────┤  协议初始化（版本/能力协商）
   ├─ tools/list ────────┤  返回 get_current_time / add
   └─ tools/call(add) ───┤  JSON-RPC 调用 → 函数执行 → 结果回传
```

**这一刻的意义**：你刚刚完成了一次完整的 MCP 调用闭环——工具发现（list）→ 工具调用（call）→ 结果返回。这个闭环就是 MCP 一切高级玩法的地基：换成 LLM 决策、加上 HTTP 远程、套上认证，就是生产级系统。

## 4. TypeScript 对照实现

```typescript
// client.ts
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";

const transport = new StdioClientTransport({
  command: "uv",
  args: ["run", "python", "server.py"],
});

const client = new Client({ name: "demo-client", version: "1.0.0" });

await client.connect(transport);

const tools = await client.listTools();
console.log("可用工具:", tools.tools.map((t) => t.name));

const result = await client.callTool({ name: "add", arguments: { a: 3, b: 4 } });
console.log("add(3,4) =", result.content[0].text);

await client.close();
```

```bash
npx tsx client.ts
```

**Python vs TS 对照**：

| 维度 | Python | TS |
|---|---|---|
| 会话创建 | `stdio_client` 上下文 + `ClientSession` | `StdioClientTransport` + `Client` |
| 初始化 | `session.initialize()` | `client.connect()` 自动 |
| 列工具 | `session.list_tools()` | `client.listTools()` |
| 调工具 | `session.call_tool(name, args)` | `client.callTool({name, arguments})` |
| 关闭 | 上下文管理器自动 | `client.close()` |

## 5. Client 关键 API 速查

| API | 作用 | 返回 |
|---|---|---|
| `StdioServerParameters` | 描述要启动的 Server（command/args/env） | — |
| `stdio_client(params)` | 启动子进程，返回 (read, write) 流 | 异步上下文 |
| `ClientSession(read, write)` | 在流上建立 JSON-RPC 会话 | 会话对象 |
| `session.initialize()` | v1 协议握手（版本/能力协商） | Server 信息 |
| `session.list_tools()` | tools/list | 工具列表 |
| `session.call_tool(name, args)` | tools/call | 调用结果（content/isError） |
| `session.list_resources()` / `list_prompts()` | 资源/提示列表 | 对应结果 |
| `session.read_resource(uri)` | 读取资源内容 | 内容对象 |

## 6. 易错点与规范

| 易错点 | 现象 | 正确做法 |
|---|---|---|
| Server 路径错误 | 子进程启动失败（FileNotFoundError） | 用绝对路径或先 `cd` 到项目目录 |
| Server 报错但无输出 | Client 静默失败 | 先单独跑 `uv run python server.py` 验证 |
| 忘记 `initialize()` | 调用报协议错误 | v1 必须先 initialize |
| 同步代码里用 async API | 报错 `await` 外使用 | 全部异步或用 `asyncio.run` |
| 调不存在的工具 | 报 tool 未找到 | 先 list_tools 看名字拼写 |
| Windows 下 `uv` 命令找不到 | 子进程启动失败 | command 改 `uv.exe` 绝对路径或 `python` |

**Client 验证顺序（固定套路）**：

1. 先 `tools/list` 确认 Server 活着、工具全。
2. 再调一个最简单的工具（happy path）。
3. 最后故意传错参数（验证错误处理，05 篇讲透）。

> 🎯 **核心要点**：Client 的最小模型只有三个动作——**启动/连接 Server、list 工具、call 工具**；本阶段的 Client 用写死参数代替 LLM，但接口与生产完全一致。跑通闭环后你会看到：MCP 的价值不在协议本身，而在"工具发现是标准化的"——任何 Server 一次实现，任何 Client 免配置接入，这就是 USB-C 类比的实际含义。

---

**参考来源**：
- [How to Build Your Own MCP Server: A Complete Guide (2026 Edition)](https://timewell.jp/en/columns/mcp-server-self-build-guide)
- [MCP Integration Development Guide 2026](https://www.contextstudios.ai/it/guides/mcp-integration-development-guide-2026)

---

**下一模块**：[04-Streamable HTTP传输动手](04-Streamable HTTP传输动手.md) / **返回总览**：[00-阶段总览与学习路径](00-阶段总览与学习路径.md)
