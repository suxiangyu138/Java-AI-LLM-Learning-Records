# 🔌 MCP 协议深度解析（Java 后端 + AI 全栈实战版）

> **核心摘要**：MCP（Model Context Protocol）是 Anthropic 提出的开放协议，为 AI 模型与外部工具、数据源之间提供标准化连接方式，相当于"AI 领域的 USB-C 接口"。本文深入 MCP 架构设计、三大原语（Tools、Resources、Prompts）、JSON-RPC 2.0 通信机制及 Java/Python 实现方案。

**前置阅读**：[[MCP（Model Context Protocol，模型上下文协议）]] | [[基于大模型的RAG应用开发与优化——Data Agent开发核心知识点大全]]

---

## 目录

1. [MCP 是什么](#一mcp-是什么)
2. [MCP 架构](#二mcp-架构)
3. [三大原语](#三mcp-三大原语primitives)
4. [通信机制](#四mcp-通信机制)
5. [生命周期](#五mcp-生命周期)
6. [Python 实现](#六python-实现-mcp-server最小示例)
7. [Java 实现](#七java-实现-mcp-serverspringboot)
8. [与 Function Calling 对比](#八mcp-vs-传统-function-calling)
9. [生态与应用场景](#九mcp-生态与应用场景)
10. [面试核心要点](#十面试核心要点)

---

## 一、MCP 是什么

**MCP（Model Context Protocol）** 是 Anthropic 提出的开放协议，为 AI 模型与外部工具、数据源之间提供标准化连接方式。

```
之前：每个 LLM 应用独立接入工具
  LLM App → 自定义连接 → 工具 A（定制代码，不可复用）

MCP 后：标准协议统一连接
  LLM App ↔ MCP Client ↔ MCP Protocol ↔ MCP Server ↔ 工具
  任何 LLM 都能复用相同的 MCP Server
```

## 二、MCP 架构

```
LLM / Agent 应用（Claude / GPT / 自建）
       ↓
MCP Client（管理连接、发现 Server、转换 Tool Call）
       ↓ JSON-RPC 2.0
MCP Server（暴露 Tools / Resources / Prompts，执行操作）
    ↓        ↓        ↓
 数据库    文件系统   外部 API
```

## 三、MCP 三大原语（Primitives）

| 原语 | 用途 | 类比 |
|------|------|------|
| **Tools（工具）** | LLM 可调用的函数，由模型主动发起 | Function Calling |
| **Resources（资源）** | 暴露数据/文件给模型读取，模型被动消费 | REST GET 端点 |
| **Prompts（提示模板）** | 预定义的 Prompt 模板 | 快捷指令 |

## 四、MCP 通信机制

### 4.1 传输层

| 传输方式 | 说明 | 适用场景 |
|----------|------|----------|
| **stdio** | 标准输入输出，MCP Server 为子进程 | 本地工具、命令行场景 |
| **HTTP + SSE** | HTTP POST 发请求，SSE 推事件 | 远程工具、微服务场景 |

### 4.2 JSON-RPC 2.0 消息格式

```json
// Request：LLM 想调用工具
{
  "jsonrpc": "2.0",
  "id": "req_001",
  "method": "tools/call",
  "params": {
    "name": "get_weather",
    "arguments": {"city": "北京"}
  }
}

// Response：工具返回结果
{
  "jsonrpc": "2.0",
  "id": "req_001",
  "result": {
    "content": [{"type": "text", "text": "北京今天 25°C，晴天"}]
  }
}

// Notification：服务器主动通知
{
  "jsonrpc": "2.0",
  "method": "notifications/resources/updated",
  "params": {"uri": "file:///data/report.md"}
}
```

## 五、MCP 生命周期

### 5.1 完整流程

```
1. 初始化（Initialize）：握手 → 协商能力 → 交换协议版本
2. 能力发现（Discovery）：Client 查询 Server 可用工具/资源列表
3. 运行（Runtime）：Client 调用 tools/call，Server 执行并返回
4. 关闭（Shutdown）：Client 发送 shutdown → Server 清理 → 断开
```

### 5.2 工具发现示例

```json
// Client 请求
{"jsonrpc": "2.0", "id": "1", "method": "tools/list"}

// Server 响应
{
  "jsonrpc": "2.0", "id": "1",
  "result": {
    "tools": [
      {
        "name": "get_weather",
        "description": "获取指定城市的天气信息",
        "inputSchema": {
          "type": "object",
          "properties": {"city": {"type": "string"}},
          "required": ["city"]
        }
      }
    ]
  }
}
```

## 六、Python 实现 MCP Server（最小示例）

```python
# server.py
from mcp.server import Server
from mcp.server.stdio import stdio_server
import asyncio

app = Server("weather-server")

@app.tool()
async def get_weather(city: str) -> str:
    """获取指定城市的天气"""
    weather_data = {
        "北京": "25°C 晴天 AQI 45",
        "上海": "28°C 小到中雨 AQI 85",
    }
    return weather_data.get(city, "暂无该城市数据")

@app.resource("file:///notes/{filename}")
async def read_note(filename: str) -> str:
    """读取笔记文件"""
    with open(f"/data/notes/{filename}") as f:
        return f.read()

if __name__ == "__main__":
    asyncio.run(stdio_server(app))
```

### 客户端调用

```python
from mcp.client.stdio import stdio_client
from mcp import ClientSession, StdioConnection

async def main():
    connection = StdioConnection(command="python", args=["server.py"])
    async with stdio_client(connection) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            tools = await session.list_tools()
            result = await session.call_tool("get_weather", {"city": "北京"})
            print(f"结果: {result}")

asyncio.run(main())
```

## 七、Java 实现 MCP Server（Spring Boot）

```java
@Configuration
public class McpServerConfig {
    @Bean
    public McpServer mcpServer() {
        return McpServer.create()
            .tool("get_weather", "获取指定城市的天气",
                (args) -> {
                    String city = args.get("city");
                    Map<String, String> weather = Map.of(
                        "北京", "25°C 晴天",
                        "上海", "28°C 有雨"
                    );
                    return weather.getOrDefault(city, "未知城市");
                })
            .tool("query_database", "查询业务数据库",
                (args) -> {
                    String sql = args.get("sql");
                    if (!sql.trim().toUpperCase().startsWith("SELECT")) {
                        throw new SecurityException("仅允许 SELECT 查询");
                    }
                    return jdbcTemplate.queryForList(sql);
                })
            .build();
    }
}
```

## 八、MCP vs 传统 Function Calling

| 维度 | 传统 Function Calling | MCP |
|------|----------------------|-----|
| 工具定义 | 每个平台自己的 JSON Schema | 统一 MCP Tool 定义 |
| 发现机制 | 代码中硬编码 | 自动 `tools/list` 发现 |
| 复用性 | 每个应用重新接入 | 一次开发到处使用 |
| 传输层 | HTTP API | stdio / HTTP+SSE 双模式 |
| 扩展性 | 厂商锁定 | 开放协议，任何 LLM 可用 |
| 生态系统 | 各自为战 | 统一社区，共享工具 |

## 九、MCP 生态与应用场景

| MCP Server 类型 | 场景 |
|-----------------|------|
| Filesystem Server | 读写本地/远程文件 |
| Database Server | 查询 MySQL/PostgreSQL/Redis |
| API Server | 调用第三方 API |
| Code Execution Server | 安全执行 Python/JS 代码 |
| Browser Server | Puppeteer 浏览器操作 |
| Git Server | 仓库操作代码审查 |
| Slack/Email Server | 企业办公集成 |
| Kubernetes Server | DevOps 运维操作 |

## 十、面试核心要点

| 问题 | 答案 |
|------|------|
| **MCP 是什么？** | Model Context Protocol，AI 模型与工具的标准连接协议 |
| **MCP 三大原语？** | Tools（调用）、Resources（读取）、Prompts（模板） |
| **通信协议？** | JSON-RPC 2.0 over stdio 或 HTTP+SSE |
| **和 Function Calling 区别？** | MCP 统一了工具定义和发现，跨平台复用 |
| **MCP Server 怎么被发现？** | Client 调用 `tools/list` 获取工具列表 |

---

## 核心要点回顾

- MCP = AI 的 USB-C，统一连接 LLM 和工具
- 三大原语：Tools（调）+ Resources（读）+ Prompts（模板）
- JSON-RPC 2.0 是标准通信协议，支持 stdio 和 HTTP+SSE
- 核心优势：一次开发到处使用，LLM 平台无关
- Java 端：Spring AI MCP 支持，微服务暴露为 MCP Tool

## 参考资料

1. [[MCP（Model Context Protocol，模型上下文协议）]]
2. [[基于大模型的RAG应用开发与优化——Data Agent开发核心知识点大全]]
3. [[LangChain4j 核心知识点]]
