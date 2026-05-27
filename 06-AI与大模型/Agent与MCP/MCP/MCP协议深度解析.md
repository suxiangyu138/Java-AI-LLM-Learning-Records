# MCP 协议深度解析（Java 后端 + AI 全栈实战版）

> **文档定位**：AI Agent 核心技术文档 | MCP 协议从原理到实战
> **版本**：MCP 2024-11-05 规范
> **核心问题**：MCP 是什么？如何统一 LLM 与外部工具的连接方式？

---

## 一、MCP 是什么

**MCP（Model Context Protocol）** 是 Anthropic 提出的开放协议，为 AI 模型与外部工具、数据源之间提供**标准化连接方式**，相当于"AI 领域的 USB-C 接口"。

```
之前：每个 LLM 应用独立接入工具
  LLM App → 自定义连接 → 工具 A
  LLM App → 自定义连接 → 工具 B
  LLM App → 自定义连接 → 工具 C
  每个都是定制代码，不可复用

MCP 后：标准协议统一连接
  LLM App ↔ MCP Client ↔ MCP Protocol ↔ MCP Server ↔ 工具
                    ↓
              任何 LLM 都能复用相同的 MCP Server
```

---

## 二、MCP 架构

```
┌──────────────────────────────────────┐
│          LLM / Agent 应用              │
│        (Claude / GPT / 自建)           │
└──────────────┬───────────────────────┘
               ↓
┌──────────────────────────────────────┐
│          MCP Client（客户端）           │
│  - 管理连接                             │
│  - 发现 MCP Server                      │
│  - 转换 Tool Call → MCP Request         │
└──────────────┬───────────────────────┘
               ↓ JSON-RPC 2.0
┌──────────────────────────────────────┐
│          MCP Server（服务端）           │
│  - 暴露 Tools / Resources / Prompts    │
│  - 执行实际操作                         │
└──────┬──────────┬──────────┬──────────┘
       ↓          ↓          ↓
   ┌───────┐ ┌───────┐ ┌──────────┐
   │数据库  │ │文件系统│ │外部 API  │
   └───────┘ └───────┘ └──────────┘
```

---

## 三、MCP 三大原语（Primitives）

| 原语 | 用途 | 类比 |
|---|---|---|
| **Tools（工具）** | LLM 可调用的函数，由模型主动发起 | Function Calling |
| **Resources（资源）** | 暴露数据/文件给模型读取，模型被动消费 | REST GET 端点 |
| **Prompts（提示模板）** | 预定义的 Prompt 模板，方便用户快速使用 | 快捷指令 |

---

## 四、MCP 通信机制

### 4.1 传输层

```
支持两种传输方式：

1. stdio（标准输入输出）
   - MCP Client 启动 MCP Server 为子进程
   - 通过 stdin/stdout 通信
   - 适用：本地工具、命令行场景
   
2. HTTP + SSE（Server-Sent Events）
   - MCP Server 作为独立 HTTP 服务
   - HTTP POST 发请求，SSE 推事件
   - 适用：远程工具、微服务场景
```

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

---

## 五、MCP 生命周期

### 5.1 完整流程

```
1. 初始化（Initialize）
   握手 → 协商能力 → 交换协议版本

2. 能力发现（Discovery）
   Client: "tools/list" → Server 返回可用工具列表
   Client: "resources/list" → Server 返回可用资源列表

3. 运行（Runtime）
   Client 调用 "tools/call"
   Client 读取 "resources/read"
   服务器可能发送通知

4. 关闭（Shutdown）
   Client 发送 "shutdown" → Server 清理 → 断开
```

### 5.2 工具发现示例

```json
// Client 请求：你能做什么？
{"jsonrpc": "2.0", "id": "1", "method": "tools/list"}

// Server 响应：
{
  "jsonrpc": "2.0",
  "id": "1",
  "result": {
    "tools": [
      {
        "name": "get_weather",
        "description": "获取指定城市的天气信息",
        "inputSchema": {
          "type": "object",
          "properties": {
            "city": {"type": "string", "description": "城市名"}
          },
          "required": ["city"]
        }
      },
      {
        "name": "send_email",
        "description": "发送邮件",
        "inputSchema": {
          "type": "object",
          "properties": {
            "to": {"type": "string"},
            "subject": {"type": "string"},
            "body": {"type": "string"}
          },
          "required": ["to", "subject", "body"]
        }
      }
    ]
  }
}
```

---

## 六、Python 实现 MCP Server（最小示例）

```python
# server.py
from mcp.server import Server
from mcp.server.stdio import stdio_server

app = Server("weather-server")

@app.tool()
async def get_weather(city: str) -> str:
    """获取指定城市的天气"""
    # 实际项目中调用天气 API
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
    import asyncio
    asyncio.run(stdio_server(app))
```

### 客户端调用

```python
# client.py
from mcp.client.stdio import stdio_client
from mcp import ClientSession, StdioConnection

async def main():
    connection = StdioConnection(
        command="python",
        args=["server.py"]
    )
    
    async with stdio_client(connection) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            
            # 发现工具
            tools = await session.list_tools()
            print(f"可用工具: {tools}")
            
            # 调用工具
            result = await session.call_tool("get_weather", {"city": "北京"})
            print(f"结果: {result}")

asyncio.run(main())
```

---

## 七、Java 实现 MCP Server（SpringBoot）

```java
// 基于 Spring AI MCP 支持
@Configuration
public class McpServerConfig {
    
    @Bean
    public McpServer mcpServer() {
        return McpServer.create()
            .tool("get_weather",
                "获取指定城市的天气",
                (args) -> {
                    String city = args.get("city");
                    Map<String, String> weather = Map.of(
                        "北京", "25°C 晴天",
                        "上海", "28°C 有雨"
                    );
                    return weather.getOrDefault(city, "未知城市");
                })
            .tool("query_database",
                "查询业务数据库",
                (args) -> {
                    String sql = args.get("sql");
                    // 安全检查：只能 SELECT，禁止 DDL/DML
                    if (!sql.trim().toUpperCase().startsWith("SELECT")) {
                        throw new SecurityException("仅允许 SELECT 查询");
                    }
                    return jdbcTemplate.queryForList(sql);
                })
            .build();
    }
}
```

---

## 八、MCP vs 传统 Function Calling

| 维度 | 传统 Function Calling | MCP |
|---|---|---|
| **工具定义** | 每个 LLM 平台自己的 JSON Schema | 统一 MCP Tool 定义 |
| **发现机制** | 代码中硬编码 | 自动 `tools/list` 发现 |
| **复用性** | 每个应用重新接入 | MCP Server 一次开发到处使用 |
| **传输层** | HTTP API | stdio / HTTP+SSE 双模式 |
| **扩展性** | 厂商锁定 | 开放协议，任何 LLM 可用 |
| **生态系统** | 各自为战 | 统一社区，共享工具 |

---

## 九、MCP 生态与应用场景

```
MCP Server 类型        | 场景
─────────────────────┼────────────────
Filesystem Server     │ 读写本地/远程文件
Database Server       │ 查询 MySQL/PostgreSQL/Redis  
API Server            │ 调用第三方 API（天气/新闻/支付）
Code Execution Server │ 安全执行 Python/JS 代码
Browser Server        │ Puppeteer 浏览器操作
Git Server            │ 仓库操作代码审查
Slack/Email Server    │ 企业办公集成
Kubernetes Server     │ DevOps 运维操作
```

---

## 十、面试核心要点

1. **MCP 是什么？** Model Context Protocol，AI 模型与工具的标准连接协议
2. **MCP 三大原语？** Tools（调用）、Resources（读取）、Prompts（模板）
3. **通信协议？** JSON-RPC 2.0 over stdio 或 HTTP+SSE
4. **和 Function Calling 区别？** MCP 统一了工具定义和发现，跨平台复用
5. **MCP Server 怎么被发现？** Client 调用 `tools/list` 获取工具列表

---

## 十一、极简总结

```
MCP = AI 的 USB-C，统一连接 LLM 和工具
三大原语 = Tools（调）+ Resources（读）+ Prompts（模板）
JSON-RPC 2.0 = 标准通信协议（stdio / HTTP+SSE）
优势 = 一次开发到处使用，LLM 平台无关
Java 端 = Spring AI MCP 支持，把微服务暴露为 MCP Tool
```
