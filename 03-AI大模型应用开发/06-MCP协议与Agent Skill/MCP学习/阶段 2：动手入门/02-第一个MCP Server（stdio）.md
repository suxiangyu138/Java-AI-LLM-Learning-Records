# 02 第一个 MCP Server（stdio）

> 用 fastmcp 写一个 30 行的最小 Server：`@mcp.tool` 注册工具、docstring 自动生成 Schema、stdio 传输本地运行——并用原始 JSON-RPC 验证它真的在工作。

## 📚 目录

1. [最小 Server：30 行跑通](#1-最小-server30-行跑通)
2. [装饰器魔法：签名→Schema](#2-装饰器魔法签名schema)
3. [stdio 传输原理](#3-stdio-传输原理)
4. [运行与验证](#4-运行与验证)
5. [TypeScript 对照实现](#5-typescript-对照实现)
6. [易错点与规范](#6-易错点与规范)

## 1. 最小 Server：30 行跑通

```python
# server.py
from fastmcp import FastMCP
from datetime import datetime

# 1) 创建 Server 实例（名字会出现在客户端工具列表）
mcp = FastMCP("demo-server")

# 2) 注册工具：装饰器 + 函数即工具
@mcp.tool
def get_current_time(timezone: str = "UTC") -> str:
    """返回指定时区的当前时间（IANA 时区名，如 Asia/Shanghai）。"""
    return datetime.now().isoformat()

@mcp.tool
def add(a: int, b: int) -> int:
    """两数相加。"""
    return a + b

if __name__ == "__main__":
    mcp.run()   # 默认 stdio 传输
```

**跑起来**：

```bash
cd mcp-demo
uv run python server.py
```

**预期**：程序"卡住"不动——这不是 bug，Server 正在 stdio 上等待 JSON-RPC 请求。Ctrl+C 退出。

## 2. 装饰器魔法：签名→Schema

`@mcp.tool` 自动完成三件事：

| 函数元素 | 生成的 Schema 部分 | 示例 |
|---|---|---|
| 参数名+类型注解 | `properties` | `timezone: str` → `{"type": "string"}` |
| 默认值 | `default` | `timezone: str = "UTC"` → 默认 UTC |
| **docstring** | `description` | 工具说明（客户端展示给模型看） |
| 返回类型注解 | `outputSchema` | `-> str` → 文本结果 |

**因此三个规范直接决定工具质量**：

1. **docstring 必须写清楚"做什么、何时用"**——模型靠它决定是否调用该工具。
2. **类型注解要具体**——`str` 能用，但 `Literal["UTC", "Asia/Shanghai"]` 能让模型不传错值。
3. **参数默认值减少模型负担**——可省略的参数别设为必填。

> 💡 查看生成的 Schema：`uv run python -c "from server import mcp; print(mcp.get_tools())"`——这是理解"工具即 Schema"最快的方式。

## 3. stdio 传输原理

```text
客户端（Claude Code/自定义 Client）
   │  子进程启动: python server.py
   ▼
stdin ←──── JSON-RPC 请求行（单行 JSON）
stdout ────→ JSON-RPC 响应行
stderr      日志（不参与协议）
```

| 要点 | 说明 |
|---|---|
| 协议 | JSON-RPC 2.0，每消息一行 JSON |
| 传输 | 客户端以子进程方式启动 Server，独占 stdin/stdout |
| 日志 | 只能写 stderr（写 stdout 会污染协议流！） |
| 范围 | 仅本机（无法远程） |
| 2026-07 规范 | 无会话无握手，每请求带版本/能力（阶段 1 的 04 篇） |

**这就是为什么**：`print()` 调试会弄坏 Server（打印进了 stdout = 发给客户端的协议流），必须用 `print(..., file=sys.stderr)` 或 logging。

## 4. 运行与验证

**方法一：直接手发 JSON-RPC（理解协议最直观）**

```bash
# 与 Server 进程对话：发送 tools/list 请求
printf '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | uv run python server.py
```

**预期输出**（一行 JSON）：

```json
{"jsonrpc":"2.0","id":1,"result":{"tools":[{"name":"get_current_time","description":"返回指定时区的当前时间...","inputSchema":{"properties":{"timezone":{"default":"UTC","type":"string"}},"type":"object"}}]}}
```

**方法二：用 mcp CLI（官方 SDK 自带，更友好）**

```bash
uv run mcp dev server.py
# 打开本地调试面板，可视化浏览工具、手动调用
```

**方法三：MCP Inspector**（05 篇专章，这里先知道入口）

```bash
npx @modelcontextprotocol/inspector uv -- run server.py
```

## 5. TypeScript 对照实现

```typescript
// server.ts —— 等价功能的 TS 版本
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

const server = new McpServer({ name: "demo-server", version: "1.0.0" });

server.tool(
  "get_current_time",
  "返回指定时区的当前时间（IANA 时区名）",
  { timezone: z.string().default("UTC") },
  async ({ timezone }) => ({
    content: [{ type: "text", text: new Date().toISOString() }],
  })
);

const transport = new StdioServerTransport();
await server.connect(transport);
```

运行与验证：

```bash
npx tsx server.ts   # 开发运行（或先 tsc 编译再 node dist/server.js）
printf '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | npx tsx server.ts
```

**Python vs TS 对照**：

| 维度 | Python（fastmcp） | TS（官方 SDK） |
|---|---|---|
| 工具注册 | `@mcp.tool` 装饰器 | `server.tool(name, desc, schema, handler)` |
| 参数 Schema | 自动（类型注解+docstring） | 手动写 Zod schema |
| 返回格式 | 直接返回值 | `{content: [...]}` 结构化对象 |
| 错误返回 | 抛异常（SDK 捕获） | `{content, isError: true}` |

## 6. 易错点与规范

| 易错点 | 现象 | 正确做法 |
|---|---|---|
| `print()` 调试 | 客户端收不到响应/解析失败 | 日志写 stderr：`print(..., file=sys.stderr)` |
| 忘写 docstring | 工具描述为空，模型不会调用 | 每个工具必写"做什么+何时用" |
| 参数全必填 | 模型调用失败率高 | 可省略参数给默认值 |
| 阻塞长任务 | 工具调用卡死超时 | 用 `async def` 或快进快出 |
| 修改文件后忘重启 | 客户端缓存旧工具列表 | 改代码后重启 Server 进程 |
| 端口/路径字符集 | Windows 中文路径报错 | 项目放纯英文路径，终端用 UTF-8 |

**动手规范（本阶段就养成）**：

1. 工具函数保持"无状态、可重入"（阶段 3 讲生产约束）。
2. 每个工具只做一件事（参数即决策负担）。
3. 验证顺序固定：`tools/list` → 手发调用 → Inspector 可视化。

> 🎯 **核心要点**：第一个 Server 的本质是"把一个普通 Python 函数暴露成模型可调用的工具"——`@mcp.tool` 替你完成了 Schema 生成与 JSON-RPC 协议处理；stdio 模式下 Server 就是"读 stdin 回 stdout 的过滤器"。亲手用 `printf` 发一次 `tools/list`，你就看穿了 MCP 的最小本质：**协议只是换了个格式的 RPC**。

---

**参考来源**：
- [MCP Server 开发完全教程：FastMCP 3.0 + TypeScript SDK 从零跑通](https://ofox.ai/zh/blog/mcp-server-development-chinese-complete-guide/)
- [How to Build Your Own MCP Server: 2026 Edition](https://timewell.jp/en/columns/mcp-server-self-build-guide)

---

**下一模块**：[03-第一个MCP Client](03-第一个MCP Client.md) / **返回总览**：[00-阶段总览与学习路径](00-阶段总览与学习路径.md)
