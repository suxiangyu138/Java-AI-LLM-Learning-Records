# 09 - MCP 协议深度解析

> 🎯 MCP = Model Context Protocol = LLM 的"USB 协议" — 让 LLM 以标准方式连接任意数据源和工具，是 Anthropic 提出的 Agent 基础设施标准

---

## 目录

1. [什么是 MCP](#1-什么是-mcp)
2. [MCP 架构](#2-mcp-架构)
3. [三大核心原语](#3-三大核心原语)
4. [MCP Server 开发](#4-mcp-server-开发)
5. [MCP vs Function Calling](#5-mcp-vs-function-calling)

---

## 1. 什么是 MCP

```text
MCP (Model Context Protocol) = Anthropic 提出的开放协议
目标：标准化 LLM 与外部数据源/工具的连接方式

类比：
  没有 MCP = 每个 LLM 应用单独对接每个数据源（M×N 集成）
  有 MCP = LLM 通过统一协议连接 → M+N 集成

  就像 USB 统一了外设连接，MCP 统一了 LLM 的工具连接
```

### MCP 解决的问题

```text
① 工具碎片化：每个 LLM 平台有自己的工具调用方式
  → MCP 提供统一标准

② 数据源接入：企业有多个数据源（数据库/API/文件系统）
  → MCP Server 一次开发，多个 LLM 复用

③ 安全与权限：没有标准化的权限控制
  → MCP 内置认证与授权机制
```

---

## 2. MCP 架构

```text
┌──────────────────────────────────────────────────┐
│              MCP Host (如 Claude Desktop)         │
│              → 用户交互界面                        │
├──────────────────────────────────────────────────┤
│              MCP Client (协议客户端)               │
│              → 连接管理/请求-响应                   │
├──────────────────────────────────────────────────┤
│              MCP Server (数据/工具提供方)           │
│  ┌──────────┬──────────┬──────────┐              │
│  │Resources │  Tools   │ Prompts  │              │
│  │(数据访问) │ (可执行)  │ (模板)   │              │
│  └──────────┴──────────┴──────────┘              │
└──────────────────────────────────────────────────┘

通信方式：
  → stdio（标准输入输出，本地进程）
  → HTTP + SSE（远程服务）
```

---

## 3. 三大核心原语

### 3.1 Resources（资源）— 数据

```text
Resources = LLM 可读取的数据

示例：
  - 文件内容：file:///docs/handbook.pdf
  - 数据库表：postgres://employees/table/salaries
  - API 数据：api://weather/forecast?city=beijing

操作：
  resources/list  — 列出可用资源
  resources/read  — 读取资源内容
```

### 3.2 Tools（工具）— 可执行操作

```json
// 工具定义（类似 Function Calling Schema）
{
  "name": "create_issue",
  "description": "在 GitHub 创建 Issue",
  "inputSchema": {
    "type": "object",
    "properties": {
      "title": {"type": "string"},
      "body": {"type": "string"},
      "repo": {"type": "string"}
    },
    "required": ["title", "repo"]
  }
}
```

```text
工具操作：
  tools/list  — 列出可用工具
  tools/call  — 调用工具
```

### 3.3 Prompts（提示模板）— 可复用 Prompt

```text
Prompts = 预定义的 Prompt 模板

示例：
  → "代码审查" Prompt 模板
  → "生成单元测试" Prompt 模板

操作：
  prompts/list  — 列出可用模板
  prompts/get   — 获取模板内容（支持参数填充）
```

---

## 4. MCP Server 开发

### 4.1 Python MCP Server

```python
from mcp.server import Server, stdio_server
from mcp.types import Tool, TextContent

# 创建 MCP Server
server = Server("my-weather-server")

@server.list_tools()
async def list_tools():
    return [
        Tool(
            name="get_weather",
            description="获取城市天气",
            inputSchema={
                "type": "object",
                "properties": {
                    "city": {"type": "string"}
                },
                "required": ["city"]
            }
        )
    ]

@server.call_tool()
async def call_tool(name, arguments):
    if name == "get_weather":
        city = arguments["city"]
        weather = fetch_weather(city)
        return [TextContent(type="text", text=weather)]

# 启动（通过 stdio）
async def main():
    async with stdio_server() as (read, write):
        await server.run(read, write)
```

### 4.2 Java MCP Server（Spring AI）

```java
// Spring AI MCP Server
@Configuration
public class McpServerConfig {
    
    @Bean
    public McpServer mcpServer() {
        return McpServer.builder()
            .serverName("java-tools")
            .tool("queryDatabase", "查询数据库", 
                  new QueryDatabaseTool())
            .resource("file:///docs/**", 
                      new FileResourceProvider())
            .build();
    }
}
```

---

## 5. MCP vs Function Calling

| 维度 | Function Calling | MCP |
|------|:---:|:---:|
| **标准化** | 各平台自定义 | 统一开放协议 |
| **复用性** | 每个应用重写 | Server一次开发多处复用 |
| **数据+工具统一** | 只有工具 | Resources(数据)+Tools(工具)+Prompts |
| **传输层** | HTTP | stdio / HTTP+SSE |
| **生态** | 平台绑定 | 跨平台开放 |
| **提出者** | OpenAI | Anthropic |

```text
MCP 解决的核心问题：
  Function Calling 解决了"LLM 调用工具"的格式问题
  MCP 进一步解决了"工具如何被多个LLM共享"的生态问题

  两者是互补关系 → MCP Server 内部仍用 Function Calling Schema 定义工具
```

---

## 核心要点回顾

- MCP = LLM 连接外部世界的标准协议（Anthropic 提出）
- 三大原语：Resources（数据）+ Tools（工具）+ Prompts（模板）
- MCP Server 一次开发 → 多个 LLM Client 复用
- MCP 与 Function Calling 互补：MCP 解决共享，FC 解决调用格式
