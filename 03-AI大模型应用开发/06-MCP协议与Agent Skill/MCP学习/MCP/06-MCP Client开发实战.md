# 06 - MCP Client 开发实战

> 🎯 MCP Client 是连接 LLM 和 Server 的桥梁 — 管理多 Server 连接、Session 生命周期、工具发现与路由、重连与容错

---

## 目录

1. [Client 架构设计](#1-client-架构设计)
2. [多 Server 连接管理](#2-多-server-连接管理)
3. [工具发现与聚合](#3-工具发现与聚合)
4. [Python Client 实现](#4-python-client-实现)
5. [TypeScript Client 实现](#5-typescript-client-实现)
6. [Java Client 实现（Spring AI）](#6-java-client-实现spring-ai)

---

## 1. Client 架构设计

### 1.1 Client 分层架构

```text
┌──────────────────────────────────────────────────────────┐
│                    MCP Client 架构                         │
├──────────────────────────────────────────────────────────┤
│                                                           │
│  ┌──────────────────────────────────────────────────┐    │
│  │  LLM 集成层                                        │    │
│  │  ├── Function Calling Adapter   → OpenAI 格式     │    │
│  │  ├── Tool Schema Converter     → 框架特定格式      │    │
│  │  └── Result Formatter          → LLM 可理解格式    │    │
│  └──────────────────────┬───────────────────────────┘    │
│                         │                                 │
│  ┌──────────────────────┴───────────────────────────┐    │
│  │  连接与路由层                                       │    │
│  │  ├── Connection Pool   — 多 Server 连接池         │    │
│  │  ├── Tool Router       — 按 Server 路由工具调用    │    │
│  │  ├── Session Manager   — Session 生命周期管理      │    │
│  │  └── Health Checker    — 连接健康检查 + 自动重连   │    │
│  └──────────────────────┬───────────────────────────┘    │
│                         │                                 │
│  ┌──────────────────────┴───────────────────────────┐    │
│  │  传输适配层                                         │    │
│  │  ├── StdioClientTransport    — 本地进程管理        │    │
│  │  ├── SSEClientTransport      — SSE 事件监听        │    │
│  │  └── StreamableHTTPTransport — HTTP 流式连接       │    │
│  └──────────────────────────────────────────────────┘    │
│                                                           │
└──────────────────────────────────────────────────────────┘
```

### 1.2 单 Server vs 多 Server

```text
单 Server 模式：
  Claude Desktop 连接 weather-server
  └── Client 只需维护一个连接

多 Server 模式（企业级）：
  Claude Code 连接:
  ├── weather-server      (天气预报工具)
  ├── github-server       (GitHub 操作)
  ├── database-server     (数据库查询)
  ├── filesystem-server   (文件系统操作)
  └── slack-server        (消息通知)

挑战：
  ① 同名工具冲突：两个 Server 都有 search 工具
  ② 连接故障隔离：一个 Server 挂了不影响其他
  ③ 工具聚合：4 个 Server 的 tools/list 需要合并
  ④ 路由：tools/call 需要知道发给哪个 Server
```

---

## 2. 多 Server 连接管理

### 2.1 连接池设计

```text
┌────────────────────────────────────────────────────────┐
│                 MCP Connection Pool                     │
├────────────────────────────────────────────────────────┤
│                                                         │
│  connections: Map<ServerName, Connection>              │
│  ┌──────────────┬──────────────┬──────────────────┐   │
│  │ weather      │ github       │ database          │   │
│  │ ● connected  │ ● connected  │ ○ disconnected    │   │
│  │ stdio://     │ streamable://│ streamable://     │   │
│  └──────────────┴──────────────┴──────────────────┘   │
│                                                         │
│  toolRoutes: Map<ToolName, ServerName>                 │
│  ┌──────────────────┬─────────────────┐               │
│  │ get_weather      │ → weather       │               │
│  │ create_issue     │ → github        │               │
│  │ query_database   │ → database      │               │
│  └──────────────────┴─────────────────┘               │
│                                                         │
└────────────────────────────────────────────────────────┘
```

### 2.2 生命周期管理

```java
/**
 * MCP Client 连接生命周期
 */
public class McpClientManager {

    private final Map<String, McpClientConnection> connections = new ConcurrentHashMap<>();
    private final Map<String, String> toolRoutes = new ConcurrentHashMap<>();  // toolName → serverName

    /**
     * 初始化所有 Server 连接
     */
    public void initialize(List<ServerConfig> configs) {
        for (ServerConfig config : configs) {
            try {
                // ① 建立传输连接
                McpTransport transport = createTransport(config);

                // ② 创建 Client 连接
                McpClientConnection conn = McpClientConnection.create(transport);

                // ③ 执行 Initialize 握手
                InitializeResult init = conn.initialize(
                    new InitializeRequest(
                        "2024-11-05",
                        new ClientCapabilities(...),
                        new Implementation("my-client", "1.0.0")
                    )
                );

                // ④ 发送 initialized 通知
                conn.sendInitialized();

                // ⑤ 注册到连接池
                connections.put(config.getName(), conn);

                // ⑥ 获取工具列表，建立路由表
                List<Tool> tools = conn.listTools();
                for (Tool tool : tools) {
                    // 工具名冲突检测
                    String existing = toolRoutes.putIfAbsent(
                        tool.name(), config.getName()
                    );
                    if (existing != null) {
                        log.warn("工具名冲突: {} 同时存在于 {} 和 {}，使用 {}",
                            tool.name(), existing, config.getName(), existing);
                    }
                }

                log.info("Server {} 连接成功，注册 {} 个工具",
                    config.getName(), tools.size());

            } catch (Exception e) {
                log.error("Server {} 连接失败: {}", config.getName(), e.getMessage());
                // 不影响其他 Server 的初始化
            }
        }
    }

    /**
     * 路由工具调用到正确的 Server
     */
    public CallToolResult callTool(String toolName, Map<String, Object> args) {
        String serverName = toolRoutes.get(toolName);
        if (serverName == null) {
            throw new ToolNotFoundException("未知工具: " + toolName);
        }

        McpClientConnection conn = connections.get(serverName);
        if (conn == null || !conn.isConnected()) {
            // 尝试重连
            conn = reconnect(serverName);
        }

        return conn.callTool(toolName, args);
    }

    /**
     * 健康检查 + 自动重连
     */
    @Scheduled(fixedDelay = 30000)  // 每 30 秒
    public void healthCheck() {
        for (Map.Entry<String, McpClientConnection> entry : connections.entrySet()) {
            if (!entry.getValue().isConnected()) {
                log.warn("Server {} 连接断开，尝试重连...", entry.getKey());
                reconnect(entry.getKey());
            } else {
                // Ping 检查
                try {
                    entry.getValue().ping();
                } catch (Exception e) {
                    log.warn("Server {} Ping 失败，标记为断开", entry.getKey());
                    entry.getValue().markDisconnected();
                }
            }
        }
    }
}
```

---

## 3. 工具发现与聚合

### 3.1 工具聚合流程

```text
多 Server 工具聚合流程：

  Step 1: 并行获取各 Server 的 tools/list
    weather-server  → [get_weather, get_forecast, list_cities]
    github-server   → [create_issue, search_code, create_pr]
    database-server → [query_db, list_tables]

  Step 2: 合并去重
    同名工具 → Server 前缀命名 = "weather:get_weather"

  Step 3: 转换为 LLM 原生格式
    OpenAI: Function Calling Schema
    Anthropic: Tool Use Schema

  Step 4: 注入 System Prompt
    将聚合后的工具列表 + 描述发送给 LLM
```

### 3.2 格式转换器

```java
/**
 * MCP Tool Schema → OpenAI Function Calling Schema
 */
public class ToolSchemaConverter {

    /**
     * MCP Tool → OpenAI Function Definition
     */
    public static FunctionDef toOpenAI(McpSchema.Tool mcpTool, String serverPrefix) {
        return FunctionDef.builder()
            .name(serverPrefix + "__" + mcpTool.name())  // 前缀避免冲突
            .description(buildDescription(mcpTool))
            .parameters(buildParameters(mcpTool.inputSchema()))
            .build();
    }

    private static String buildDescription(McpSchema.Tool tool) {
        StringBuilder desc = new StringBuilder(tool.description());

        // 附加 Server 来源信息
        desc.append("\n[来源: MCP Server]");

        // 附加 Annotations 信息
        if (tool.annotations() != null) {
            if (tool.annotations().readOnlyHint()) {
                desc.append("\n⚠️ 只读操作，无副作用");
            }
            if (tool.annotations().destructiveHint()) {
                desc.append("\n🔴 破坏性操作！执行前需用户确认");
            }
        }

        return desc.toString();
    }

    private static JsonObject buildParameters(JsonSchema schema) {
        // 递归转换 JSON Schema → OpenAI Function parameters
        // Schema 格式兼容，基本可直接转换
        return schema.toJsonObject();
    }
}
```

---

## 4. Python Client 实现

```python
"""
多 Server MCP Client — Python 实现
"""
import asyncio
from typing import Dict, List, Optional
from dataclasses import dataclass
from fastmcp import Client
from openai import AsyncOpenAI


@dataclass
class ServerConfig:
    name: str
    transport: str  # "stdio" | "sse" | "streamable-http"
    command: Optional[str] = None      # stdio 模式
    args: Optional[List[str]] = None   # stdio 模式
    url: Optional[str] = None          # HTTP 模式
    headers: Optional[Dict] = None


class MultiServerMCPClient:
    """多 Server MCP Client — 连接池 + 工具路由 + LLM 集成"""

    def __init__(self, llm: AsyncOpenAI):
        self.llm = llm
        self.clients: Dict[str, Client] = {}
        self.tool_routes: Dict[str, str] = {}   # tool_name → server_name
        self.all_tools: List[dict] = []          # OpenAI 格式的工具列表

    async def connect_all(self, configs: List[ServerConfig]):
        """并行初始化所有 Server 连接"""
        tasks = [self._connect_one(cfg) for cfg in configs]
        results = await asyncio.gather(*tasks, return_exceptions=True)

        for cfg, result in zip(configs, results):
            if isinstance(result, Exception):
                print(f"❌ Server '{cfg.name}' 连接失败: {result}")
            else:
                print(f"✅ Server '{cfg.name}' 连接成功，{result} 个工具")

    async def _connect_one(self, config: ServerConfig) -> int:
        """连接单个 Server 并注册工具"""
        client = Client(
            name=config.name,
            transport=config.transport,
            # stdio
            command=config.command,
            args=config.args,
            # HTTP
            url=config.url,
            headers=config.headers,
        )

        # 建立连接
        await client.connect()

        # 获取工具列表
        tools = await client.list_tools()

        # 注册路由（处理冲突：server_prefix__tool_name）
        prefix = config.name
        for tool in tools:
            route_name = f"{prefix}__{tool.name}"

            # 转换为 OpenAI Function Calling 格式
            openai_tool = {
                "type": "function",
                "function": {
                    "name": route_name,
                    "description": f"{tool.description}\n[来源: MCP Server '{config.name}']",
                    "parameters": tool.inputSchema,
                }
            }
            self.all_tools.append(openai_tool)
            self.tool_routes[route_name] = config.name

        self.clients[config.name] = client
        return len(tools)

    async def chat(self, user_message: str) -> str:
        """完整的 Agent 循环：LLM 决策 → 工具调用 → 结果返回"""

        messages = [{"role": "user", "content": user_message}]

        for _ in range(10):  # 最多 10 轮工具调用
            # LLM 决策
            response = await self.llm.chat.completions.create(
                model="gpt-4o",
                messages=messages,
                tools=self.all_tools,
                tool_choice="auto",
            )

            msg = response.choices[0].message

            # 不需要工具 → 返回回复
            if not msg.tool_calls:
                return msg.content

            # 处理工具调用
            messages.append(msg)

            for tool_call in msg.tool_calls:
                # 路由：从 tool_name 中提取 server
                route_name = tool_call.function.name
                server_name = self.tool_routes.get(route_name)

                if not server_name:
                    result = f"错误: 工具 {route_name} 未注册"
                else:
                    client = self.clients[server_name]
                    # 移除前缀，调用原始工具名
                    original_name = route_name[len(server_name) + 2:]
                    args = json.loads(tool_call.function.arguments)
                    result = await client.call_tool(original_name, args)

                messages.append({
                    "role": "tool",
                    "tool_call_id": tool_call.id,
                    "content": str(result),
                })

        return "达到最大工具调用轮数"

    async def close(self):
        """关闭所有连接"""
        for client in self.clients.values():
            await client.disconnect()
```

---

## 5. TypeScript Client 实现

```typescript
/**
 * MCP Client Manager — TypeScript 实现
 */
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";
import { StreamableHTTPClientTransport } from "@modelcontextprotocol/sdk/client/streamableHttp.js";
import type { Tool } from "@modelcontextprotocol/sdk/types.js";
import OpenAI from "openai";

interface ServerConfig {
  name: string;
  transport: "stdio" | "streamable-http";
  command?: string;
  args?: string[];
  url?: string;
  headers?: Record<string, string>;
}

class McpClientManager {
  private clients = new Map<string, Client>();
  private toolRoutes = new Map<string, string>();
  private allTools: OpenAI.Chat.Completions.ChatCompletionTool[] = [];
  private openai: OpenAI;

  constructor() {
    this.openai = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });
  }

  async connectAll(configs: ServerConfig[]): Promise<void> {
    const results = await Promise.allSettled(
      configs.map((cfg) => this.connectOne(cfg))
    );

    results.forEach((result, i) => {
      const name = configs[i].name;
      if (result.status === "fulfilled") {
        console.log(`✅ ${name}: ${result.value} tools`);
      } else {
        console.error(`❌ ${name}: ${result.reason}`);
      }
    });
  }

  private async connectOne(config: ServerConfig): Promise<number> {
    // 创建传输
    const transport =
      config.transport === "stdio"
        ? new StdioClientTransport({
            command: config.command!,
            args: config.args,
          })
        : new StreamableHTTPClientTransport({
            url: config.url!,
            headers: config.headers,
          });

    // 创建 Client
    const client = new Client(
      { name: "my-client", version: "1.0.0" },
      { capabilities: { sampling: {} } }
    );

    // 连接 + 初始化
    await client.connect(transport);

    // 获取工具
    const { tools } = await client.listTools();
    const prefix = config.name;

    for (const tool of tools) {
      const routeName = `${prefix}__${tool.name}`;

      // 注册路由
      this.toolRoutes.set(routeName, prefix);

      // 转换为 OpenAI 格式
      this.allTools.push({
        type: "function",
        function: {
          name: routeName,
          description: `${tool.description}\n[来源: ${config.name}]`,
          parameters: tool.inputSchema as Record<string, unknown>,
        },
      });
    }

    this.clients.set(config.name, client);
    return tools.length;
  }

  async callTool(
    routeName: string,
    args: Record<string, unknown>
  ): Promise<string> {
    const serverName = this.toolRoutes.get(routeName);
    if (!serverName) throw new Error(`Unknown tool: ${routeName}`);

    const client = this.clients.get(serverName)!;
    const originalName = routeName.slice(serverName.length + 2);

    const result = await client.callTool({
      name: originalName,
      arguments: args,
    });

    return result.content
      .map((c) => ("text" in c ? c.text : "[非文本内容]"))
      .join("\n");
  }

  async chat(message: string): Promise<string> {
    const messages: OpenAI.Chat.Completions.ChatCompletionMessageParam[] = [
      { role: "user", content: message },
    ];

    for (let round = 0; round < 10; round++) {
      const response = await this.openai.chat.completions.create({
        model: "gpt-4o",
        messages,
        tools: this.allTools,
        tool_choice: "auto",
      });

      const msg = response.choices[0].message;
      if (!msg.tool_calls) return msg.content ?? "";

      messages.push(msg);

      for (const tc of msg.tool_calls) {
        const result = await this.callTool(
          tc.function.name,
          JSON.parse(tc.function.arguments)
        );
        messages.push({
          role: "tool",
          tool_call_id: tc.id,
          content: result,
        });
      }
    }

    return "Reached maximum tool call rounds";
  }

  async disconnectAll(): Promise<void> {
    for (const client of this.clients.values()) {
      await client.close();
    }
  }
}
```

---

## 6. Java Client 实现（Spring AI）

```java
/**
 * Spring AI MCP Client — 多 Server 管理
 */
@Configuration
@Slf4j
public class McpClientConfig {

    /**
     * 多 Server Client 配置
     */
    @Bean
    public List<McpClient> mcpClients() {
        return List.of(
            // Weather Server — 远程 Streamable HTTP
            McpClient.builder()
                .clientInfo(new Implementation("my-client", "1.0"))
                .transport(StreamableHttpClientTransport.builder()
                    .url("https://weather-api.example.com/mcp")
                    .build())
                .build(),

            // GitHub Server — 本地 stdio
            McpClient.builder()
                .clientInfo(new Implementation("my-client", "1.0"))
                .transport(StdioClientTransport.builder()
                    .command("npx")
                    .args(List.of("-y", "@modelcontextprotocol/server-github"))
                    .build())
                .build()
        );
    }

    /**
     * 工具聚合 Bean：将所有 Server 的工具聚合为统一的 ToolCallback 列表
     */
    @Bean
    public List<ToolCallback> aggregatedTools(List<McpClient> clients) {
        List<ToolCallback> allTools = new ArrayList<>();

        for (McpClient client : clients) {
            // 获取此 Client 的所有工具
            List<Tool> tools = client.listTools();

            for (Tool tool : tools) {
                // 包装为 Spring AI ToolCallback
                ToolCallback callback = new McpToolCallback(client, tool);
                allTools.add(callback);
            }
        }

        log.info("聚合了 {} 个工具，来自 {} 个 MCP Server",
            allTools.size(), clients.size());
        return allTools;
    }

    /**
     * Spring AI ChatClient — 自动使用聚合的工具
     */
    @Bean
    public ChatClient chatClient(
            ChatClient.Builder builder,
            List<ToolCallback> aggregatedTools) {
        return builder
            .defaultTools(aggregatedTools.toArray(new ToolCallback[0]))
            .build();
    }
}

/**
 * MCP Tool → Spring AI ToolCallback 适配器
 */
public class McpToolCallback implements ToolCallback {

    private final McpClient client;
    private final Tool tool;

    @Override
    public String getName() {
        return tool.name();
    }

    @Override
    public String getDescription() {
        return tool.description();
    }

    @Override
    public String getInputSchema() {
        return tool.inputSchema().toString();
    }

    @Override
    public String call(String input) {
        Map<String, Object> args = JsonUtils.parse(input);
        CallToolResult result = client.callTool(tool.name(), args);

        return result.content().stream()
            .filter(c -> c instanceof TextContent)
            .map(c -> ((TextContent) c).text())
            .collect(Collectors.joining("\n"));
    }
}
```

> 🎯 **核心要点**：Client 的核心职责 = 多 Server 连接池 + 工具发现聚合 + 名称路由 + LLM 格式适配。关键是工具名冲突处理和连接故障隔离

---

**上一模块**：[05 - MCP Server 开发实战](./05-MCP%20Server开发实战.md)  
**下一模块**：[07 - MCP 认证与安全](./07-MCP认证与安全.md)  
**返回总览**：[00 - MCP 知识体系总览](./00-MCP知识体系总览.md)
