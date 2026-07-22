# MCP 实战指南 面试宝典
> 基于课程大纲全面覆盖面试高频考点 — MCP 协议原理、架构与实战全解析

## 目录

1. [一、基础概念速答](#一基础概念速答)
2. [二、深度原理剖析](#二深度原理剖析)
3. [三、实战场景题](#三实战场景题)
4. [四、手写代码题](#四手写代码题)
5. [五、系统设计题](#五系统设计题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板)
8. [八、快速查漏补缺 Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答

> 面试官快速考察基础认知，每题控制在 30 秒内回答完毕。

### Q1: MCP 是什么？为什么要用 MCP？

**MCP (Model Context Protocol)** 是由 Anthropic 于 2024 年 11 月开源的一个开放协议，旨在为大语言模型提供**标准化的工具/数据交互接口**。

- **Why**: 传统 LLM 无法直接访问外部数据源和工具。MCP 统一了 LLM 与外部系统的通信方式，类似 "USB-C for AI"。
- 解决了 M 个 LLM × N 个工具的集成爆炸问题（M×N → M+N）。

> 💡 MCP = 大模型的 USB-C 接口，统一所有工具和数据的接入标准。

### Q2: MCP 的三种 Transport（传输层）是什么？

| Transport | 特点 | 适用场景 |
|-----------|------|----------|
| **stdio** | 通过标准输入/输出通信，子进程方式运行 | 本地开发、CLI 工具、单机部署 |
| **SSE** (Server-Sent Events) | 服务端推送事件，HTTP 长连接 | 远程服务、实时通知场景 |
| **Streamable HTTP** (2025.05 新标准化) | 基于 HTTP POST，支持流式和非流式统一 | 生产级远程部署，取代纯 SSE |

> ⚠️ Streamable HTTP 是 2025 年 5 月后推荐的生产环境传输方式，兼容性和可观测性优于纯 SSE。

### Q3: MCP 的三大核心原语（Primitives）是什么？

| 原语 | 作用 | 类比 |
|------|------|------|
| **Resources** (资源) | 暴露外部数据给 LLM 读取（文件、数据库、API 响应） | GET 请求 / 文件读取 |
| **Tools** (工具) | 允许 LLM 执行动作（写文件、发邮件、调 API） | POST 请求 / 函数调用 |
| **Prompts** (提示模板) | 预定义的提示模板，引导 LLM 行为 | 路由 / 预设指令 |

### Q4: MCP 与传统 REST API 有什么本质区别？

| 维度 | MCP | 传统 REST API |
|------|-----|---------------|
| 调用者 | LLM Agent 自主决策 | 人类开发者编码调用 |
| 协议模型 | 功能声明式（声明 Tools/Resources） | 端点定义式（URL + Method） |
| 参数描述 | JSON Schema 描述，供 LLM 理解 | 文档描述，供人类阅读 |
| 运行时绑定 | 动态发现（ListTools → CallTool） | 静态编码（HTTP 请求硬编码） |
| 连接方式 | 长连接 / 流式 | 短连接 Request-Response |

### Q5: MCP Host、Client、Server 分别指什么？

- **Host**: 运行 LLM 的应用程序（如 Claude Desktop、Cursor、Cline），发起连接请求
- **Client**: Host 内部与 MCP Server 建立一对一连接的通信代理
- **Server**: 提供 Tools、Resources、Prompts 的轻量级服务进程

> 一个 Host 可以同时连接多个 MCP Server，每个连接对应一个 Client 实例。

### Q6: uvx 和 npx 在 MCP 中起什么作用？

- **uvx**: Python 生态的工具运行器（由 uv 提供），无需安装即可运行 MCP Server 脚本
- **npx**: Node.js 生态的工具运行器，运行 npm 包中的 MCP Server
- 两者都是 **零安装执行** 工具，适合在 MCP 配置中直接引用 Server 入口

```json
{
  "mcpServers": {
    "my-server": {
      "command": "uvx",
      "args": ["mcp-server-fetch"]
    }
  }
}
```

### Q7: Tool 定义中的 JSON Schema 起什么作用？

JSON Schema 描述了工具的**输入参数结构**，LLM 通过它自动理解如何调用工具。

```json
{
  "name": "get_weather",
  "description": "获取指定城市的天气信息",
  "inputSchema": {
    "type": "object",
    "properties": {
      "city": { "type": "string", "description": "城市名称" },
      "unit": { "type": "string", "enum": ["celsius", "fahrenheit"] }
    },
    "required": ["city"]
  }
}
```

> 💡 LLM 通过 `description` 字段理解工具用途，通过 `inputSchema` 生成正确的调用参数。

### Q8: MCP Server 有哪几种实现方式？

1. **Python SDK**（官方推荐）：`pip install mcp`，快速构建 Server
2. **TypeScript SDK**（官方支持）：`npm install @modelcontextprotocol/sdk`
3. **Java SDK**（社区）：`mcp-java-sdk`，Spring AI 集成
4. **Kotlin SDK**（社区）
5. **手动实现**：直接解析 JSON-RPC 协议，不依赖 SDK

### Q9: MCP 协议基于什么通信格式？

MCP 基于 **JSON-RPC 2.0** 协议进行消息通信。所有请求、响应、通知都封装为 JSON-RPC 消息帧。

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "get_weather",
    "arguments": { "city": "北京" }
  }
}
```

### Q10: 什么是 MCP 的 Tool Permission 机制？

Host 层面的安全控制，决定 LLM 能否调用特定工具：

- **Auto-approve**: 自动批准（白名单工具）
- **Prompt-approve**: 每次调用询问用户
- **Deny**: 完全禁止

> ⚠️ 生产环境中应对文件写入、网络请求等敏感工具设置 prompt-approve 级别。

### Q11: Cursor 如何集成 MCP？

Cursor 通过 `~/.cursor/mcp.json` 或项目级 `.cursor/mcp.json` 配置 MCP Server：

```json
{
  "mcpServers": {
    "my-server": {
      "type": "stdio",
      "command": "python",
      "args": ["server.py"]
    }
  }
}
```

### Q12: Cline 如何集成 MCP？

Cline (VS Code 插件) 通过 `~/cline_mcp_settings.json` 配置：

```json
{
  "mcpServers": {
    "my-server": {
      "command": "uvx",
      "args": ["mcp-server-fetch"]
    }
  }
}
```

### Q13: CherryStudio 如何集成 MCP？

CherryStudio 在设置页面中提供 MCP 配置入口，支持 stdio 和 SSE 两种方式，直接在 UI 中添加命令或 URL。

### Q14: MCP 协议的通信流程是什么？

```
Host (LLM App) → Client → [JSON-RPC over Transport] → MCP Server
  1. initialize        → 建立连接时进行版本协商
  2. listTools         → LLM 动态发现可用工具列表
  3. listResources     → 发现可用资源列表
  4. callTool          → LLM 决定调用某个工具执行动作
  5. readResource      → LLM 读取某个资源获取数据
  6. shutdown          → 关闭连接
```

### Q15: 什么是 MCP 的 Roots 概念？

Roots 是 MCP 协议中的一个概念，指 MCP Server 可以建议 Host 应该暴露哪些文件系统路径给 LLM，形成 Server → Host 的反向建议机制。

### Q16: MCP 中的 Sampling 是什么？

Sampling 允许 MCP Server 反过来请求 Host 调用 LLM 生成文本。这是一种 **Server→LLM** 的反向调用机制，用于需要 AI 推理的场景（如智能搜索代理）。

### Q17: 什么是 Streamable HTTP Transport？

2025 年 5 月引入的新传输标准，在单个 HTTP POST 请求中统一处理流式和非流式响应：

- **非流式**: 请求 + 响应体一次完成
- **流式**: 请求 + SSE 分块响应
- 替代了纯 SSE 传输，简化了网络层的兼容性

### Q18: MCP Server 的安全边界在哪里？

1. **Host 层面**：Tool Permission 控制（auto-approve / prompt-approve / deny）
2. **Server 层面**：输入参数校验（JSON Schema validation）
3. **传输层面**：TLS 加密（远程传输时必需）
4. **数据层面**：Resources 只读隔离，Tools 沙箱执行

---

## 二、深度原理剖析

> 考察对 MCP 协议的设计思想、架构演进和核心机制的深入理解。

### Q19: 为什么 MCP 选择 JSON-RPC 而不是 REST 或 gRPC？

| 维度 | JSON-RPC | REST | gRPC |
|------|----------|------|------|
| 消息模型 | 方法调用式，天然匹配 LLM 的函数调用语义 | 资源 CRUD，需要额外抽象 | 强类型 Service 定义，灵活性有限 |
| 动态发现 | 内置 ListXxx 方法，天然支持 | 需额外构建 Discovery Service | 需 Proto 编译，运行时修改困难 |
| 流式支持 | 通知(Notification)机制 | 需 WebSocket 配合 | 原生支持，但复杂度高 |
| 实现成本 | 极低，任何语言 50 行可实现 | 中等 | 高（Protobuf 编译链） |
| LLM 适配度 | JSON 结构天然可被 LLM 解析和生成 | 需要开发者编码组装 | 对 LLM 不友好 |

> 💡 MCP 选 JSON-RPC 的核心原因：LLM 本身就是 JSON 理解和生成的专家，JSON-RPC 的消息格式让 LLM 零成本理解协议。

### Q20: MCP 的 initialize 握手过程发生了什么？

```
Client → Server: {
  "jsonrpc": "2.0", "id": 1, "method": "initialize",
  "params": {
    "protocolVersion": "2025-03-26",
    "capabilities": { "tools": {}, "resources": {} },
    "clientInfo": { "name": "my-host", "version": "1.0.0" }
  }
}

Server → Client: {
  "jsonrpc": "2.0", "id": 1,
  "result": {
    "protocolVersion": "2025-03-26",
    "capabilities": { "tools": {}, "resources": {}, "prompts": {} },
    "serverInfo": { "name": "my-server", "version": "1.0.0" }
  }
}

Client → Server: { "method": "notifications/initialized" }
```

**能力协商（Capability Negotiation）**：双方声明支持哪些原语（Tools/Resources/Prompts），Host 根据 Server 的能力决定后续交互方式。

### Q21: MCP 的 Tool Calling 完整生命周期是什么？

```
1. Discovery:  Host 调用 listTools → Server 返回工具列表（含 JSON Schema）
2. Decision:   LLM 根据用户请求 + 工具描述，决定调用哪个工具
3. Invocation: Host 调用 callTool(name, arguments) → Server 执行
4. Response:   Server 返回 toolResult（含 content 和 isError 字段）
5. Feedback:   LLM 根据结果生成最终回复给用户
```

> 🎯 关键设计点：LLM 通过自然语言理解工具描述，通过 Schema 生成参数，不需要预编码调用逻辑。

### Q22: 如何实现 MCP Server 的 Tool 结果的流式返回？

使用 `CallToolResult` 的 `content` 数组，配合 `progress` 通知机制：

```python
from mcp.server import Server

server = Server("streaming-server")

@server.call_tool()
async def handle_call_tool(name: str, arguments: dict):
    # 发送进度通知
    await server.request_context.session.send_progress_notification(
        progress_token="...",
        progress=0,
        total=100
    )
    # 分块返回结果
    result_parts = []
    for chunk in process_long_task(arguments):
        result_parts.append(TextContent(type="text", text=chunk))
        # 更新进度
        await server.request_context.session.send_progress_notification(
            progress_token="...",
            progress=len(result_parts),
            total=100
        )
    return CallToolResult(content=result_parts)
```

### Q23: MCP Server 的连接生命周期是怎样的？

```
IDLE (等待连接)
  │
  ├─ initialize 请求到达
  │
  ▼
INITIALIZED (握手完成，能力协商)
  │
  ├─ ListTools      → 工具发现
  ├─ CallTool       → 工具调用
  ├─ ListResources  → 资源发现
  ├─ ReadResource   → 资源读取
  ├─ ListPrompts    → 提示发现
  ├─ GetPrompt      → 提示获取
  │
  ├─ notifications/cancelled → 取消请求
  ├─ notifications/progress  → 进度更新
  │
  ▼
SHUTDOWN (连接关闭)
```

### Q24: MCP 如何处理并发请求？

MCP Server 默认是单线程异步模型：

- **请求 ID 匹配机制**：每个 JSON-RPC 请求有唯一 `id`，响应通过 `id` 匹配请求
- **异步 non-blocking**：Python SDK 基于 `asyncio`，支持并发处理多个请求
- **注意事项**：如果 Tool 实现中有共享状态需要加锁，或使用无状态设计

```python
async def handle_call_tool(name: str, arguments: dict) -> CallToolResult:
    # 异步执行，不会阻塞其他请求
    result = await some_async_operation(arguments)
    return CallToolResult(content=[TextContent(type="text", text=result)])
```

### Q25: MCP 的 Resource 模板和 URI 是如何工作的？

Resources 使用类似 REST 的 URI 方案，支持参数化模板：

```python
from mcp.server import Server
from mcp.server.resources import ResourceTemplate

server = Server("db-server")

# 静态资源
@server.list_resources()
async def handle_list_resources():
    return [
        Resource(
            uri="file:///logs/app.log",
            name="Application Log",
            mimeType="text/plain",
            description="当前应用日志"
        )
    ]

# 资源模板（动态参数）
@server.read_resource()
async def handle_read_resource(uri: str):
    # 解析 URI 参数
    # file:///logs/{date}/{level}.log
    match = re.match(r"file:///logs/(\d{4}-\d{2}-\d{2})/(\w+)\.log", uri)
    if match:
        date, level = match.groups()
        return read_log_file(date, level)
```

### Q26: 什么是 MCP 的 Pagination 机制？

MCP 支持分页返回大型列表（如大量 Tools 或 Resources）：

```json
{
  "jsonrpc": "2.0", "id": 1, "method": "tools/list",
  "params": { "cursor": "next_page_token_abc" }
}
```

- Server 在响应中返回 `nextCursor` 字段
- Client 携带 cursor 继续请求下一页
- 适用于工具数量超过 100 的场景

### Q27: MCP Server 如何实现热更新（动态注册/注销工具）？

MCP Server 可以发送 `notifications/tools/list_changed` 通知给 Client，告知工具列表已变化，Client 收到后会重新调用 `listTools` 更新缓存：

```python
async def register_new_tool(server: Server, tool: Tool):
    tools_cache.append(tool)
    # 通知 Client 工具列表已变更
    await server.request_context.session.send_notification(
        "notifications/tools/list_changed"
    )
```

### Q28: Server-Sent Events (SSE) 与 Streamable HTTP 在 MCP 中有什么区别？

| 维度 | SSE (传统) | Streamable HTTP (新标准) |
|------|------------|------------------------|
| 连接模型 | 长连接 + endpoint 分离 | 标准 HTTP POST |
| 流式 | 原生 SSE 事件流 | 响应体分块（chunked） |
| 非流式 | 需额外处理 | 单次响应完成 |
| 防火墙友好 | 较差（长连接） | 优秀（标准 HTTP） |
| 负载均衡 | 困难（粘性会话） | 自然支持 |
| 推荐程度 | 生产不推荐 | 生产推荐 |

### Q29: MCP 的 Tool Permission 在 Host 层面是如何实现的？

大致流程：

```
LLM 决定调用 Tool
  → Host 检查此 Tool 的权限级别
    ├─ auto-approve: 直接放行，调用 Server
    ├─ prompt-approve: 弹出 UI 让用户确认
    │   └─ 用户同意 → 调用 Server
    │   └─ 用户拒绝 → 返回错误给 LLM
    └─ deny: 直接返回权限错误给 LLM
```

### Q30: MCP 中的 Error Handling 机制是怎样的？

MCP 遵循 JSON-RPC 2.0 的错误标准，定义了标准错误码：

| 错误码 | 含义 | 场景 |
|--------|------|------|
| -32700 | Parse Error | JSON 解析失败 |
| -32600 | Invalid Request | 请求格式错误 |
| -32601 | Method Not Found | 调用了不存在的方法 |
| -32602 | Invalid Params | 参数校验失败 |
| -32603 | Internal Error | 服务端内部异常 |
| -32000+ | 自定义错误 | 业务逻辑错误自定义 |

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "error": {
    "code": -32602,
    "message": "Invalid tool parameters",
    "data": {
      "tool": "get_weather",
      "validation": {
        "city": "required field is missing"
      }
    }
  }
}
```

---

## 三、实战场景题

> 考察在实际项目中如何正确使用 MCP 解决问题。

### Q31: 你的项目需要让 LLM 查询公司内部数据库，如何用 MCP 实现？

**方案**：构建一个 Database MCP Server

1. 使用 Python SDK 创建 MCP Server
2. 注册 `query_database` 工具，接受 SQL 参数（或预定义查询名称）
3. 工具内部连接数据库执行查询
4. 返回结果给 LLM

```python
@server.tool()
async def query_database(sql: str) -> str:
    """执行数据库查询并返回结果"""
    # ⚠️ 安全防护：只允许 SELECT 语句
    if not sql.strip().upper().startswith("SELECT"):
        return "Error: Only SELECT queries are allowed"
    async with db_pool.acquire() as conn:
        result = await conn.fetch(sql)
        return json.dumps([dict(row) for row in result], ensure_ascii=False, default=str)
```

> ⚠️ 生产环境必须限制 SQL 权限，防止注入和写操作。

### Q32: 如果需要 MCP Server 调用第三方 REST API，如何设计？

在 Tool 内部通过 HTTP 调用第三方 API：

```python
@server.tool()
async def github_get_repo(owner: str, repo: str) -> str:
    """获取 GitHub 仓库信息"""
    async with httpx.AsyncClient() as client:
        resp = await client.get(
            f"https://api.github.com/repos/{owner}/{repo}",
            headers={"Accept": "application/vnd.github.v3+json"}
        )
        resp.raise_for_status()
        return json.dumps(resp.json(), ensure_ascii=False)
```

**最佳实践**：
- 使用 `httpx` 或 `aiohttp` 异步库
- 设置超时（timeout）防止阻塞
- 错误处理：捕获 HTTP 异常并返回友好错误信息
- 限流（Rate Limiting）：防止 API 被封

### Q33: Cursor 中配置 MCP Server 后无法连接，如何排查？

**排查步骤**：

1. **检查配置格式**：确认 `cursor/mcp.json` 的 JSON 格式正确
2. **测试命令独立性**：在终端直接运行 command + args，看能否启动：
   ```bash
   python server.py
   ```
3. **检查 stdio**：确认 Server 通过 stdio 输出 JSON-RPC 消息
4. **查看 Cursor 日志**：命令面板 → "Developer: Toggle Developer Tools" → Console
5. **版本兼容性**：确认 MCP SDK 版本 >= 1.0.0
6. **环境变量**：某些 Server 需要 API Key 等环境变量

**常见错误**：
- 路径中使用 `~` 未展开 → 使用绝对路径
- Python 虚拟环境未激活 → 直接指定 `python` 路径
- Node.js Server 缺少 `node_modules` → 先 `npm install`

### Q34: 如何让多个 MCP Server 协同工作（比如一个负责搜索，一个负责存储）？

MCP 协议支持 Host 同时连接多个 Server：

```json
{
  "mcpServers": {
    "search-server": { "command": "python", "args": ["search_server.py"] },
    "storage-server": { "command": "python", "args": ["storage_server.py"] }
  }
}
```

**工作流程**：
1. User: "搜索 MCP 相关内容并保存到文件"
2. LLM 调用 `search_server` 的 `web_search` 工具获取结果
3. LLM 调用 `storage_server` 的 `save_to_file` 工具保存结果

> 💡 LLM 自动跨 Server 编排工具调用，不需要开发者编写编排代码。

### Q35: 生产环境中 MCP Server 应该以什么方式部署？

| 部署方式 | 传输类型 | 优点 | 缺点 |
|---------|---------|------|------|
| 子进程 | stdio | 零配置，自动管理生命周期 | 只能本地 |
| Docker 容器 | stdio / SSE | 环境隔离，便于分发 | 需要 Docker 环境 |
| Kubernetes | Streamable HTTP | 弹性伸缩，高可用 | 运维复杂 |
| Serverless (AWS Lambda) | Streamable HTTP | 按需付费，自动扩缩 | 冷启动延迟 |
| sidecar 进程 | stdio | 与主应用同生命周期 | 资源占用 |

### Q36: 如何为 MCP Tool 设计测试？

```python
import pytest
from mcp import Tool, CallToolResult

class TestWeatherTool:
    async def test_tool_definition(self):
        """验证工具定义正确"""
        tools = await server.list_tools()
        weather_tool = next(t for t in tools if t.name == "get_weather")
        assert weather_tool.name == "get_weather"
        assert "city" in weather_tool.inputSchema["properties"]

    async def test_call_tool_success(self):
        """验证工具调用成功"""
        result = await server.call_tool("get_weather", {"city": "北京"})
        assert not result.isError
        assert "北京" in result.content[0].text

    async def test_call_tool_missing_params(self):
        """验证缺少必要参数时的错误处理"""
        result = await server.call_tool("get_weather", {})
        assert result.isError
        assert "required" in result.content[0].text.lower()
```

### Q37: 如何监控和观测 MCP Server 的运行状态？

**关键指标**：
- **请求延迟**: 每个 toolCall 的响应时间
- **错误率**: toolCall 返回 isError 的比例
- **活跃连接数**: 当前连接的 Host 数量
- **工具调用频率**: 各工具被调用的次数统计

**实现方式**：
```python
import structlog

logger = structlog.get_logger()
server = Server("observable-server")

@server.tool()
async def monitored_tool(param: str) -> str:
    start = time.time()
    try:
        result = await actual_implementation(param)
        elapsed = time.time() - start
        logger.info("tool_call", tool="monitored_tool", duration=elapsed, success=True)
        return result
    except Exception as e:
        elapsed = time.time() - start
        logger.error("tool_call", tool="monitored_tool", duration=elapsed, error=str(e))
        return CallToolResult(content=[TextContent(type="text", text=str(e))], isError=True)
```

### Q38: MCP Server 如何优雅处理超时和取消？

```python
import asyncio
from mcp.server import Server

server = Server("timeout-server")

@server.call_tool()
async def handle_call_tool(name: str, arguments: dict) -> CallToolResult:
    try:
        # 设置超时
        result = await asyncio.wait_for(
            long_running_task(arguments),
            timeout=30.0  # 30 秒超时
        )
        return CallToolResult(content=[TextContent(type="text", text=result)])
    except asyncio.TimeoutError:
        return CallToolResult(
            content=[TextContent(type="text", text="Task timed out after 30s")],
            isError=True
        )
    except asyncio.CancelledError:
        # 处理取消通知
        return CallToolResult(
            content=[TextContent(type="text", text="Task was cancelled")],
            isError=True
        )
```

---

## 四、手写代码题

> 面试中可能会要求在白板或在线 IDE 中手写以下代码。

### Q39: 手写一个最简 MCP Server

```python
"""最小 MCP Server 实现 — 使用 Python SDK"""
from mcp.server import Server
from mcp.server.stdio import stdio_server
from mcp.types import Tool, TextContent, CallToolResult

# 1. 创建 Server 实例
server = Server("minimal-server")

# 2. 注册 Tool：获取当前时间
@server.list_tools()
async def list_tools() -> list[Tool]:
    return [
        Tool(
            name="current_time",
            description="获取当前时间",
            inputSchema={
                "type": "object",
                "properties": {},
                "required": []
            }
        )
    ]

@server.call_tool()
async def call_tool(name: str, arguments: dict) -> CallToolResult:
    if name == "current_time":
        from datetime import datetime
        return CallToolResult(
            content=[TextContent(type="text", text=f"当前时间: {datetime.now()}")]
        )
    raise ValueError(f"Unknown tool: {name}")

# 3. 启动（stdio 传输）
async def main():
    async with stdio_server() as (read_stream, write_stream):
        await server.run(read_stream, write_stream, server.create_initialization_options())

if __name__ == "__main__":
    import asyncio
    asyncio.run(main())
```

### Q40: 手写一个带复杂参数校验的 Tool

```python
"""带 JSON Schema 校验的 MCP Tool"""
from mcp.server import Server, stdio_server
from mcp.types import Tool, TextContent, CallToolResult
import re

server = Server("validator-server")

EMAIL_PATTERN = re.compile(r"^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$")

@server.list_tools()
async def list_tools() -> list[Tool]:
    return [
        Tool(
            name="send_email",
            description="发送电子邮件",
            inputSchema={
                "type": "object",
                "properties": {
                    "to": {
                        "type": "array",
                        "items": {"type": "string"},
                        "description": "收件人邮箱列表",
                        "minItems": 1
                    },
                    "subject": {
                        "type": "string",
                        "description": "邮件主题",
                        "maxLength": 200
                    },
                    "body": {
                        "type": "string",
                        "description": "邮件正文（支持 Markdown）"
                    },
                    "priority": {
                        "type": "string",
                        "enum": ["low", "normal", "high"],
                        "description": "优先级",
                        "default": "normal"
                    }
                },
                "required": ["to", "subject", "body"]
            }
        )
    ]

@server.call_tool()
async def call_tool(name: str, arguments: dict) -> CallToolResult:
    if name == "send_email":
        # 额外业务校验
        for addr in arguments.get("to", []):
            if not EMAIL_PATTERN.match(addr):
                return CallToolResult(
                    content=[TextContent(type="text", text=f"Invalid email: {addr}")],
                    isError=True
                )
        # 模拟发送
        return CallToolResult(
            content=[TextContent(type="text",
                text=f"Email sent to {len(arguments['to'])} recipients")]
        )
    raise ValueError(f"Unknown tool: {name}")
```

### Q41: 手写 MCP Client（不使用 SDK）

```python
"""纯 JSON-RPC 实现的 MCP Client（不使用 SDK）"""
import json
import asyncio
from typing import AsyncIterator

class MCPClient:
    """手动实现的最小 MCP Client"""

    def __init__(self, reader: asyncio.StreamReader, writer: asyncio.StreamWriter):
        self._reader = reader
        self._writer = writer
        self._request_id = 0
        self._pending = {}  # id -> asyncio.Future

    async def _send_request(self, method: str, params: dict = None) -> dict:
        self._request_id += 1
        request = {
            "jsonrpc": "2.0",
            "id": self._request_id,
            "method": method
        }
        if params:
            request["params"] = params

        # 创建 Future 等待响应
        future = asyncio.get_event_loop().create_future()
        self._pending[self._request_id] = future

        # 发送请求
        data = json.dumps(request, ensure_ascii=False) + "\n"
        self._writer.write(data.encode())
        await self._writer.drain()

        # 等待响应
        response = await future
        if "error" in response:
            raise Exception(f"RPC Error: {response['error']}")
        return response["result"]

    async def _read_loop(self):
        """后台读取响应并匹配请求"""
        buffer = ""
        while True:
            chunk = await self._reader.read(4096)
            if not chunk:
                break
            buffer += chunk.decode()
            while "\n" in buffer:
                line, buffer = buffer.split("\n", 1)
                if line.strip():
                    response = json.loads(line)
                    req_id = response.get("id")
                    if req_id in self._pending:
                        self._pending[req_id].set_result(response)
                        del self._pending[req_id]

    async def initialize(self):
        return await self._send_request("initialize", {
            "protocolVersion": "2025-03-26",
            "capabilities": {},
            "clientInfo": {"name": "manual-client", "version": "1.0.0"}
        })

    async def list_tools(self) -> list:
        result = await self._send_request("tools/list")
        return result.get("tools", [])

    async def call_tool(self, name: str, args: dict) -> dict:
        return await self._send_request("tools/call", {
            "name": name,
            "arguments": args
        })

    async def close(self):
        self._writer.close()
        await self._writer.wait_closed()

# 使用示例
async def main():
    reader, writer = await asyncio.open_unix_connection("/tmp/mcp-server.sock")
    # 或使用 stdio 方式：
    # process = await asyncio.create_subprocess_exec(...)
    # reader, writer = process.stdout, process.stdin

    client = MCPClient(reader, writer)
    read_task = asyncio.create_task(client._read_loop())

    await client.initialize()
    tools = await client.list_tools()
    print(f"Available tools: {tools}")
    result = await client.call_tool("get_weather", {"city": "北京"})
    print(f"Tool result: {result}")

    await client.close()
    read_task.cancel()

if __name__ == "__main__":
    asyncio.run(main())
```

### Q42: 手写一个文件系统 MCP Server

```python
"""文件系统 MCP Server — 安全地暴露文件操作给 LLM"""
import os
from pathlib import Path
from mcp.server import Server, stdio_server
from mcp.types import Tool, TextContent, CallToolResult

server = Server("safe-fs-server")
ALLOWED_BASE = Path(os.path.expanduser("~/mcp_sandbox"))
ALLOWED_BASE.mkdir(exist_ok=True)

def _safe_path(user_path: str) -> Path:
    """防止 Path Traversal 攻击"""
    target = (ALLOWED_BASE / user_path).resolve()
    if not str(target).startswith(str(ALLOWED_BASE.resolve())):
        raise ValueError(f"Access denied: path outside sandbox")
    return target

@server.list_tools()
async def list_tools() -> list[Tool]:
    return [
        Tool(
            name="read_file",
            description="读取沙箱目录下的文件",
            inputSchema={
                "type": "object",
                "properties": {
                    "path": {"type": "string", "description": "相对路径"}
                },
                "required": ["path"]
            }
        ),
        Tool(
            name="write_file",
            description="写入沙箱目录下的文件（需用户确认）",
            inputSchema={
                "type": "object",
                "properties": {
                    "path": {"type": "string", "description": "相对路径"},
                    "content": {"type": "string", "description": "文件内容"}
                },
                "required": ["path", "content"]
            }
        )
    ]

@server.call_tool()
async def call_tool(name: str, arguments: dict) -> CallToolResult:
    try:
        if name == "read_file":
            path = _safe_path(arguments["path"])
            if not path.exists():
                return CallToolResult(content=[TextContent(
                    type="text", text=f"File not found: {arguments['path']}"
                )], isError=True)
            content = path.read_text(encoding="utf-8")
            return CallToolResult(content=[TextContent(type="text", text=content)])

        elif name == "write_file":
            path = _safe_path(arguments["path"])
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(arguments["content"], encoding="utf-8")
            return CallToolResult(content=[TextContent(
                type="text", text=f"Written {len(arguments['content'])} bytes to {arguments['path']}"
            )])

        raise ValueError(f"Unknown tool: {name}")
    except ValueError as e:
        return CallToolResult(content=[TextContent(type="text", text=str(e))], isError=True)
```

### Q43: 手写 HTTP SSE MCP Server

```python
"""基于 SSE 的远程 MCP Server"""
from mcp.server import Server
from mcp.server.sse import SseServerTransport
from starlette.applications import Starlette
from starlette.routing import Route

server = Server("remote-server")

@server.list_tools()
async def list_tools() -> list[Tool]:
    return [
        Tool(
            name="remote_echo",
            description="Echo the input (remote test)",
            inputSchema={
                "type": "object",
                "properties": {
                    "message": {"type": "string", "description": "输入消息"}
                },
                "required": ["message"]
            }
        )
    ]

@server.call_tool()
async def call_tool(name: str, arguments: dict) -> CallToolResult:
    if name == "remote_echo":
        return CallToolResult(
            content=[TextContent(type="text", text=f"Echo: {arguments['message']}")]
        )

# Starlette app 包装
sse = SseServerTransport("/messages/")

async def handle_sse(request):
    async with sse.connect_sse(request.scope, request.receive, request._send) as streams:
        await server.run(streams[0], streams[1], server.create_initialization_options())

async def handle_messages(request):
    await sse.handle_post_message(request.scope, request.receive, request._send)

app = Starlette(routes=[
    Route("/sse", endpoint=handle_sse),
    Route("/messages/", endpoint=handle_messages, methods=["POST"]),
])

# 启动: uvicorn run:app --host 0.0.0.0 --port 8000
```

### Q44: 手写 MCP Server 的 Authentication 中间件

```python
"""带 API Key 认证的 MCP Server"""
import os
from mcp.server import Server
from mcp.types import Tool, TextContent, CallToolResult

API_KEY = os.environ.get("MCP_API_KEY", "default-dev-key")

class AuthenticatedServer(Server):
    def __init__(self, name: str):
        super().__init__(name)
        self._authenticated = False

    def authenticate(self, api_key: str) -> bool:
        """验证 API Key"""
        self._authenticated = (api_key == API_KEY)
        return self._authenticated

    async def call_tool(self, name: str, arguments: dict) -> CallToolResult:
        if not self._authenticated:
            return CallToolResult(
                content=[TextContent(type="text", text="Unauthorized: invalid API key")],
                isError=True
            )
        return await super().call_tool(name, arguments)

# 使用：Client 在 initialize 时传入 API Key
# 或在 requests 中添加自定义 header（SSE 模式下）
```

### Q45: 手写一个 MCP Tool 调用外部 API（天气查询）

```python
"""天气查询 MCP Tool"""
import httpx
from mcp.server import Server, stdio_server
from mcp.types import Tool, TextContent, CallToolResult

server = Server("weather-server")

@server.list_tools()
async def list_tools() -> list[Tool]:
    return [
        Tool(
            name="get_weather",
            description="查询指定城市当前天气",
            inputSchema={
                "type": "object",
                "properties": {
                    "city": {
                        "type": "string",
                        "description": "城市名称（中文，如：北京、上海）"
                    }
                },
                "required": ["city"]
            }
        ),
        Tool(
            name="get_forecast",
            description="查询指定城市未来天气预报",
            inputSchema={
                "type": "object",
                "properties": {
                    "city": {"type": "string", "description": "城市名称"},
                    "days": {
                        "type": "integer",
                        "description": "预报天数 (1-7)",
                        "minimum": 1,
                        "maximum": 7,
                        "default": 3
                    }
                },
                "required": ["city"]
            }
        )
    ]

@server.call_tool()
async def call_tool(name: str, arguments: dict) -> CallToolResult:
    try:
        city = arguments["city"]
        async with httpx.AsyncClient() as client:
            if name == "get_weather":
                resp = await client.get(
                    f"https://api.openweathermap.org/data/2.5/weather",
                    params={"q": city, "appid": "YOUR_API_KEY", "lang": "zh_cn"}
                )
                resp.raise_for_status()
                data = resp.json()
                return CallToolResult(content=[TextContent(
                    type="text",
                    text=f"{city}天气: {data['weather'][0]['description']}, "
                         f"温度: {data['main']['temp']}K"
                )])

            elif name == "get_forecast":
                days = arguments.get("days", 3)
                resp = await client.get(
                    f"https://api.openweathermap.org/data/2.5/forecast",
                    params={"q": city, "appid": "YOUR_API_KEY", "lang": "zh_cn", "cnt": days * 8}
                )
                resp.raise_for_status()
                # 处理并返回预报数据
                data = resp.json()
                summaries = []
                for item in data["list"][:days]:
                    summaries.append(
                        f"{item['dt_txt']}: {item['weather'][0]['description']}"
                    )
                return CallToolResult(content=[TextContent(
                    type="text",
                    text=f"{city}未来{days}天预报:\n" + "\n".join(summaries)
                )])

    except httpx.HTTPError as e:
        return CallToolResult(
            content=[TextContent(type="text", text=f"API request failed: {str(e)}")],
            isError=True
        )

    raise ValueError(f"Unknown tool: {name}")
```

---

## 五、系统设计题

> 考察架构设计能力，通常结合业务场景出题。

### Q46: 设计一个 MCP 网关，统一管理多个 MCP Server

**需求**：公司有多个业务系统（CRM、ERP、WMS），每个系统暴露自己的 MCP Server，需要一个统一入口。

**设计方案**：

```
                     ┌─────────────┐
                     │   MCP 网关   │
                     │ (统一端点)    │
                     └──────┬──────┘
               ┌───────────┼───────────┐
               ▼           ▼           ▼
         ┌─────────┐ ┌─────────┐ ┌─────────┐
         │ CRM MCP │ │ ERP MCP │ │ WMS MCP │
         └─────────┘ └─────────┘ └─────────┘
```

**架构要点**：
1. **路由层**：根据 Tool 名称前缀或 metadata 路由到对应后端 Server
2. **统一认证**：网关层统一处理 API Key / OAuth 认证
3. **限流熔断**：防止单一 Server 过载影响其他服务
4. **日志聚合**：所有工具的调用日志统一收集
5. **健康检查**：定期检查后端 Server 存活状态

```python
class MCPGateway:
    def __init__(self):
        self.servers = {}  # name -> MCPClient

    async def route_call(self, tool_name: str, arguments: dict):
        """根据工具名称路由"""
        for server_name, client in self.servers.items():
            tools = await client.list_tools()
            if any(t["name"] == tool_name for t in tools):
                return await client.call_tool(tool_name, arguments)
        return CallToolResult(content=[TextContent(
            type="text", text=f"No server provides tool: {tool_name}"
        )], isError=True)
```

### Q47: 设计一个企业级 MCP Server 部署架构

**需求**：生产环境需要高可用、可观测、易运维。

```
                    ┌──────────────┐
                    │   LLM App    │ (Claude Desktop / Cursor / 自研)
                    └──────┬───────┘
                           │ Streamable HTTP
                    ┌──────▼───────┐
                    │  Nginx/ALB   │ (TLS termination + 负载均衡)
                    └──────┬───────┘
               ┌───────────┼───────────┐
               ▼           ▼           ▼
         ┌─────────┐ ┌─────────┐ ┌─────────┐
         │ MCP Svr1│ │ MCP Svr2│ │ MCP Svr3│ (K8s Pods / 容器)
         └────┬────┘ └────┬────┘ └────┬────┘
              │           │           │
              ▼           ▼           ▼
         ┌─────────────────────────────────┐
         │    Redis (缓存 / 限流 / Session) │
         └─────────────────────────────────┘
              │           │           │
              ▼           ▼           ▼
         ┌─────────────────────────────────┐
         │    内部服务 / 数据库 / API       │
         └─────────────────────────────────┘
```

**关键设计点**：
- **无状态设计**：MCP Server 不存储会话状态，方便水平扩展
- **缓存层**：Tools 列表等元数据可缓存，减少重复查询
- **健康检查端点**：`/health` 返回 Server 状态和依赖健康度
- **优雅关闭**：SIGTERM 时完成正在处理的请求再退出

### Q48: 设计一个 MCP Server 的安全审计方案

**需求**：记录所有工具调用，满足合规审计要求。

```python
import json
import datetime
from pathlib import Path

class AuditLogger:
    """MCP 审计日志记录器"""

    def __init__(self, log_dir: str = "/var/log/mcp-audit"):
        self.log_dir = Path(log_dir)
        self.log_dir.mkdir(parents=True, exist_ok=True)

    async def log_call(self, client_id: str, tool_name: str,
                       arguments: dict, result: dict, duration: float):
        entry = {
            "timestamp": datetime.datetime.utcnow().isoformat(),
            "client_id": client_id,
            "action": "tool_call",
            "tool_name": tool_name,
            "arguments": self._sanitize_sensitive(arguments),
            "result_status": "success" if not result.get("isError") else "error",
            "duration_ms": round(duration * 1000, 2)
        }
        # 按日期分片存储
        log_file = self.log_dir / f"audit-{datetime.date.today().isoformat()}.jsonl"
        with open(log_file, "a", encoding="utf-8") as f:
            f.write(json.dumps(entry, ensure_ascii=False) + "\n")

    def _sanitize_sensitive(self, data: dict) -> dict:
        """脱敏敏感字段"""
        SENSITIVE_KEYS = ["password", "api_key", "token", "secret"]
        sanitized = {}
        for k, v in data.items():
            if k.lower() in SENSITIVE_KEYS:
                sanitized[k] = "***REDACTED***"
            else:
                sanitized[k] = v
        return sanitized
```

### Q49: 设计一个高并发 MCP Server（支持大量并发请求）

```python
"""高并发 MCP Server 设计"""
import asyncio
from collections import deque
from mcp.server import Server

class ConcurrentMCPServer(Server):
    def __init__(self, name: str, max_concurrent: int = 100):
        super().__init__(name)
        self._semaphore = asyncio.Semaphore(max_concurrent)
        self._request_queue = deque()
        self._metrics = {
            "active_requests": 0,
            "queued_requests": 0,
            "total_requests": 0,
            "rejected_requests": 0
        }

    async def call_tool(self, name: str, arguments: dict) -> CallToolResult:
        # 限流控制
        if self._semaphore.locked() and len(self._request_queue) > 200:
            self._metrics["rejected_requests"] += 1
            return CallToolResult(
                content=[TextContent(type="text", text="Server busy, try again later")],
                isError=True
            )

        async with self._semaphore:
            self._metrics["active_requests"] += 1
            self._metrics["total_requests"] += 1
            try:
                return await super().call_tool(name, arguments)
            finally:
                self._metrics["active_requests"] -= 1
```

> 💡 核心原则：无状态 + 异步 + 限流 + 优雅降级。

---

## 六、常见坑点与最佳实践

| 类别 | 坑点 | 解决方案 |
|------|------|----------|
| **JSON Schema** | Schema 不完整导致 LLM 生成无效参数 | 严格定义 `required`、`enum`、`minLength` 等约束 |
| **错误处理** | Tool 内部异常未捕获，导致 JSON-RPC 错误 | 所有 Tool 实现包裹 try-except，返回 `isError: true` |
| **路径安全** | Path Traversal 攻击 | 使用 `resolve()` + 前缀校验，不要直接拼接路径 |
| **并发安全** | 共享状态未加锁导致数据竞争 | 无状态设计优先；必要状态使用 `asyncio.Lock` |
| **超时控制** | 长时间运行的 Tool 阻塞其他请求 | 使用 `asyncio.wait_for` 设置超时 |
| **编码处理** | 中文/特殊字符导致 JSON 解析失败 | `json.dumps(ensure_ascii=False)` |
| **环境变量** | API Key 硬编码在配置文件中 | 使用环境变量或密钥管理服务 |
| **日志污染** | 大量 Debug 日志影响性能 | 使用结构化日志，支持级别控制 |
| **版本兼容** | SDK 版本不一致导致协议握手失败 | 固定 SDK 版本，先查 Capability |
| **stdio 关闭** | Server 进程未正确清理 | 使用 context manager 或信号处理 |

---

## 七、面试回答模板

### 模板 1: "请解释一下 MCP 是什么"

> MCP（Model Context Protocol）是 Anthropic 于 2024 年 11 月开源的一个开放协议。它本质上是要解决 LLM 与外部系统和数据交互的标准化问题——在没有 MCP 之前，每个 LLM 应用都要为每个数据源单独写集成代码，导致 M×N 的集成爆炸。MCP 的核心价值在于三点：第一，它提供了统一的工具（Tools）、资源（Resources）和提示模板（Prompts）抽象；第二，它基于 JSON-RPC 2.0 通信，对大语言模型非常友好；第三，它通过能力协商（Capability Negotiation）机制实现了灵活的功能发现。可以理解为 MCP 就是大模型的 USB-C 接口。

### 模板 2: "MCP 相比传统 API 有什么优势？"

> 最大的区别在于调用者的变化。传统 REST API 需要开发者阅读文档、理解端点、编码调用——每一步都是人类驱动的。而 MCP 的调用者是 LLM，它通过动态发现（listTools）理解工具的能力，通过 JSON Schema 理解参数结构，然后自主决策调用时机和参数。这带来了几个关键优势：第一，零集成代码，LLM 自动理解和使用工具；第二，动态扩展，新增工具后 LLM 立即感知；第三，统一协议，无论后端是什么技术栈。用一个比喻：传统 API 是电话本，你要一个个号码自己拨；MCP 是智能语音助手，说"帮我联系"就够了。

### 模板 3: "MCP 存在哪些安全风险？如何防护？"

> MCP 主要面临四个维度的安全风险。第一是工具权限风险——如果 LLM 被诱导调用危险工具（如删除文件、发送邮件），需要通过 Host 层面的 Permission 机制分级控制：敏感工具需要用户确认。第二是参数注入——攻击者可能通过 Prompt Injection 让 LLM 生成恶意参数，Server 端必须做严格的输入校验和业务校验，比如文件路径防止 Path Traversal。第三是数据泄露——Resources 可能暴露敏感数据，应该在 Server 层面实现基于身份的访问控制。第四是传输安全——远程传输必须启用 TLS 加密。总的来说，安全遵循纵深防御原则，在 Host、Server、Transport 三层都要做防护。

### 模板 4: "你在项目中最难解决的一个 MCP 问题是什么？"

> 之前做一个数据库查询 MCP Server 时，遇到的最棘手问题是 Prompt Injection 导致 LLM 生成恶意 SQL。虽然我们的 Tool 只允许 SELECT，但攻击者可以通过对话诱导 LLM 生成 `SELECT * FROM users WHERE 1=1 UNION SELECT ...` 这种越权查询。解决方案是三层防护：第一层，在 Tool 内部使用 SQL 解析器（sqlparse）提取并检查查询范围；第二层，数据库层面限制用户权限为只读且只能访问指定表；第三层，对敏感查询（涉及 user、password 等表）要求用户二次确认。这个案例让我深刻理解到，MCP 的安全不能只依赖单一防线。

### 模板 5: "如何将一个现有的 REST API 改造成 MCP Server？"

> 基本上就是做一层协议适配。第一步，分析 REST API 的功能，每个端点映射为一个 MCP Tool——GET 映射为资源读取，POST/PUT/DELETE 映射为 Tool 执行动作。第二步，将 API 文档中的参数描述转换为 JSON Schema，把自然语言描述放在 `description` 字段供 LLM 理解。第三步，用 MCP SDK 实现 Server，在 Tool 内部调用原始 REST API。第四步，配置 Transport，本地用 stdio，远程用 Streamable HTTP。整个过程不用改后端代码，MCP 就是一个新的接入层。实际项目中，一个阅读 10 个端点的 REST API 大约半天就能完成适配。

---

## 八、快速查漏补缺 Checklist

> 面试前逐项核查，确保没有知识盲区。

### 基础（必会）
- [x] 能 30 秒内说清楚 MCP 是什么
- [x] 说出 MCP 三大原语（Resources / Tools / Prompts）
- [x] 说出三种 Transport 及其区别
- [x] 理解 MCP 与传统 API 的核心区别
- [x] 理解 Host / Client / Server 三层架构
- [x] 理解 Tool 定义中的 JSON Schema 作用

### 进阶（掌握）
- [x] 能手写最简 MCP Server（含 listTools + callTool）
- [x] 理解 JSON-RPC 2.0 消息格式
- [x] 理解 initialize 握手过程
- [x] 理解 Tool Permission 机制
- [x] 熟悉 Cursor / Cline / CherryStudio 配置
- [x] 理解 uvx 和 npx 的作用
- [x] 知道 MCP vs A2A 的区别

### 深度（加分）
- [x] 理解 Capability Negotiation
- [x] 能实现 Resource 模板和动态 URI
- [x] 理解 Pagination 和 Progress 通知
- [x] 能实现 Tool 热更新（list_changed 通知）
- [x] 理解 Sampling（Server → LLM 反向调用）
- [x] 理解 Streamable HTTP 的优势
- [x] 能设计 MCP 网关架构
- [x] 能设计安全审计方案
- [x] 能处理 Path Traversal、Prompt Injection 等安全问题

### 对比（关键）
- [x] MCP vs REST API (核心区别)
- [x] MCP vs A2A (Agent-to-Agent Protocol)
- [x] stdio vs SSE vs Streamable HTTP (传输方式对比)
- [x] JSON-RPC vs REST vs gRPC (协议选型对比)

---

## 附录：MCP vs A2A 核心对比

| 对比维度 | MCP (Model Context Protocol) | A2A (Agent-to-Agent Protocol) |
|----------|------------------------------|-------------------------------|
| **发起方** | Anthropic (2024.11) | Google (2025.04) |
| **核心目标** | LLM ↔ 工具/数据集成 | Agent ↔ Agent 协作通信 |
| **协议基础** | JSON-RPC 2.0 | HTTP + JSON (REST 风格) |
| **通信模式** | C/S 模式（Host ↔ Server） | P2P 模式（Agent ↔ Agent） |
| **核心原语** | Tools / Resources / Prompts | Skills / Tasks / Cards |
| **传输** | stdio / SSE / Streamable HTTP | HTTPS |
| **认证** | Host 层 Permission 控制 | OAuth 2.0 / OpenID Connect |
| **典型场景** | 让 LLM 使用数据库、API、文件 | 让 AI Agent 互相委托任务 |
| **互补关系** | MCP 处理"工具接入" | A2A 处理"Agent 协作" |

> 🎯 **一句话总结**: MCP 让 LLM 能用工具，A2A 让 Agent 能互相聊天。两者是互补关系，不是竞争关系。在实际架构中，一个 Agent 可以通过 MCP 接入工具，再通过 A2A 与其他 Agent 协作。

---

## 附录：MCP 协议方法速查表

| 方法 | 方向 | 描述 |
|------|------|------|
| `initialize` | C → S | 连接初始化，版本和能力协商 |
| `notifications/initialized` | C → S | 初始化完成通知 |
| `tools/list` | C → S | 获取工具列表 |
| `tools/call` | C → S | 调用指定工具 |
| `resources/list` | C → S | 获取资源列表 |
| `resources/read` | C → S | 读取指定资源 |
| `prompts/list` | C → S | 获取提示模板列表 |
| `prompts/get` | C → S | 获取指定提示模板 |
| `notifications/tools/list_changed` | S → C | 工具列表变化通知 |
| `notifications/resources/list_changed` | S → C | 资源列表变化通知 |
| `notifications/cancelled` | C → S | 取消请求 |
| `notifications/progress` | S → C | 进度通知 |
| `logging/setLevel` | C → S | 设置日志级别 |

---

> 📝 **最后提醒**：面试时注意结合自己的实际项目经验回答，不要只背概念。特别是手动实现过 MCP Server 的经验、遇到的安全问题、排查过的坑，这些才是面试官最看重的部分。祝面试顺利！
