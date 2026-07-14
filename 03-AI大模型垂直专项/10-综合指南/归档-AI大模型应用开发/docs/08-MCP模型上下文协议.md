# 08 — MCP 模型上下文协议

> **目标**：深入理解 MCP 协议，学会开发 MCP Server 和 Client，掌握企业级 MCP 实践。

---

## 1. MCP 概述

### 1.1 什么是 MCP

> **MCP (Model Context Protocol)** 是 Anthropic 提出的开放协议，标准化了 AI 应用与外部工具/数据源之间的交互。类比为"AI 应用的 USB-C 接口"——统一的物理层（协议），让任何 LLM 都能接入任何工具。

```
传统方式（N×M 集成问题）：
  Claude ──→ 工具A, 工具B, 工具C  (各自集成)
  GPT-4  ──→ 工具A, 工具B, 工具C  (重复集成)
  文心   ──→ 工具A, 工具B, 工具C  (再重复)

MCP 方式（协议统一）：

  ┌───────┐    ┌────────────┐    ┌──────────────┐    ┌─────────┐
  │ Claude│    │            │    │  MCP Server  │    │ Weather │
  ├───────┤    │  MCP Client│────│  (天气服务)   │────│ API     │
  │ GPT-4 │────│  (Host)    │    └──────────────┘    └─────────┘
  ├───────┤    │            │
  │ Qwen  │    │            │    ┌──────────────┐    ┌─────────┐
  └───────┘    │            │────│  MCP Server  │────│Database │
               └────────────┘    │  (数据库服务) │    └─────────┘
                                 └──────────────┘
```

### 1.2 MCP vs Function Calling

| 维度 | MCP | Function Calling |
|------|-----|------------------|
| **标准化** | ✅ 开放标准，跨厂商 | ❌ 各厂商自定义格式 |
| **发现机制** | 自动发现 Server 的全部能力 | 手动注册每个工具 |
| **工具管理** | Server 端统一管理 | 应用端分散管理 |
| **多客户端** | 一套 Server 多 Client 共用 | 每个应用独立集成 |
| **资源暴露** | ✅ Tool + Resource + Prompt | ❌ 只有 Tool |
| **双向通信** | ✅ (Streamable HTTP) | ❌ 单向请求-响应 |
| **生态** | 快速成长的 MCP 市场 | 各厂商独立生态 |
| **复杂度** | 较高（需运行 Server） | 较低（代码内注册） |

---

## 2. MCP 协议架构

### 2.1 核心概念

```
┌─────────────────────────────────────────────────┐
│                  MCP Host                        │
│  (Claude Desktop / VS Code / 你的应用)            │
│                                                   │
│   ┌─────────────────────────────────────────┐    │
│   │           MCP Client                     │    │
│   │  - 管理多个 MCP Server 连接              │    │
│   │  - 路由 LLM 的 Tool Call 到 Server       │    │
│   │  - 将 Server 能力列表传给 LLM            │    │
│   └─────────────┬───────────────────────────┘    │
└─────────────────┼────────────────────────────────┘
                  │
        ┌─────────┼─────────┐
        │         │         │
        ▼         ▼         ▼
   ┌─────────┐┌─────────┐┌─────────┐
   │ MCP     ││ MCP     ││ MCP     │
   │ Server A││ Server B││ Server C│
   │         ││         ││         │
   │ Tools:  ││ Tools:  ││ Tools:  │
   │ - search││ - query ││ - send  │
   │ - read  ││ - insert││ - draft │
   │         ││         ││         │
   │Resources││Resources││Prompts: │
   │ - docs  ││ - schema││ - review│
   └─────────┘└─────────┘└─────────┘
```

### 2.2 三大原语

| 原语 | 用途 | 谁发起 | 示例 |
|------|------|--------|------|
| **Tool** | 可执行的函数（LLM 控制） | AI → Server | 搜索、查询、发送 |
| **Resource** | 可读取的数据（应用控制） | Client → Server | 文件内容、数据库记录、API 响应 |
| **Prompt** | 预定义的提示词模板（用户控制） | 用户 → Server | 代码审查模板、报告模板 |

---

## 3. 传输协议 (Transport)

### 3.1 三种传输方式

| Transport | 全称 | 通信方式 | 适用场景 |
|------|------|------|------|
| **stdio** | 标准输入输出 | 进程间管道 | 本地工具、CLI 工具 |
| **SSE (2024)** | Server-Sent Events | HTTP + SSE 长连接 | 远程服务、Web 服务 |
| **Streamable HTTP (2025)** | 可流式 HTTP | HTTP + 可选流式 | **推荐**：统一远程和本地 |

### 3.2 传输方式对比

```
stdio:
  ┌────────┐  stdin/stdout  ┌────────┐
  │ Client │◄══════════════►│ Server │
  └────────┘   (管道通信)    └────────┘
  用 JSON-RPC over stdio
  低延迟，零网络开销
  缺点：只能本地

Streamable HTTP (推荐):
  ┌────────┐   HTTP/SSE    ┌────────┐
  │ Client │◄══════════════►│ Server │
  └────────┘                └────────┘
  用 JSON-RPC over HTTP
  支持远程，也可本地
  支持流式响应
```

### 3.3 配置示例

```json
// .mcp.json (项目级 MCP 配置)
{
  "mcpServers": {
    "weather": {
      "type": "stdio",
      "command": "java",
      "args": ["-jar", "mcp-weather-server.jar"],
      "env": {
        "API_KEY": "${WEATHER_API_KEY}"
      }
    },
    "database": {
      "type": "streamable-http",
      "url": "http://localhost:3001/mcp",
      "headers": {
        "Authorization": "Bearer ${DB_TOKEN}"
      }
    },
    "filesystem": {
      "type": "stdio",
      "command": "npx",
      "args": ["-y", "@anthropic/mcp-server-filesystem", "/data/docs"]
    }
  }
}
```

---

## 4. Java MCP Server 开发

### 4.1 Spring AI MCP Server

```java
// Maven 依赖
// <dependency>
//     <groupId>org.springframework.ai</groupId>
//     <artifactId>spring-ai-starter-mcp-server-webflux</artifactId>
// </dependency>

@SpringBootApplication
@Configuration
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

    // ===== 定义 Tool =====
    @Tool(description = "获取指定城市的天气信息")
    public WeatherInfo getWeather(
        @ToolParam(description = "城市名称") String city) {
        return new WeatherInfo(city, 25.0, "晴", 60.0);
    }

    @Tool(description = "在知识库中搜索文档")
    public List<SearchResult> searchDocuments(
        @ToolParam(description = "搜索关键词") String query,
        @ToolParam(description = "返回结果数量，默认5") int topK) {
        return vectorStore.similaritySearch(SearchRequest.query(query).withTopK(topK))
            .stream()
            .map(doc -> new SearchResult(doc.getContent(), doc.getMetadata()))
            .toList();
    }

    // ===== 定义 Resource =====
    @Resource(uri = "config://app-settings", name = "应用配置")
    public String getAppConfig() {
        return """
            {
              "app_name": "AI Assistant",
              "version": "1.0.0",
              "features": ["chat", "rag", "agent"]
            }
            """;
    }

    @Resource(uri = "db://schema/{table}", name = "数据库表结构")
    public String getTableSchema(@PathVariable String table) {
        return jdbcTemplate.queryForObject(
            "SHOW CREATE TABLE " + table, String.class);
    }

    // ===== 定义 Prompt =====
    @Prompt(name = "code-review",
            description = "代码审查提示词模板")
    public String codeReviewPrompt(
        @PromptParam(description = "编程语言") String language,
        @PromptParam(description = "审查重点") String focus) {
        return String.format("""
            请审查以下 %s 代码，重点关注 %s。
            审查要点：
            1. 安全漏洞
            2. 性能问题
            3. 代码规范
            4. 潜在 bug
            请以结构化格式输出审查结果。
            """, language, focus);
    }
}
```

### 4.2 手动构建 MCP Server（无 Spring）

```java
// 使用官方 MCP SDK
import io.modelcontextprotocol.server.*;
import io.modelcontextprotocol.server.transport.*;

public class ManualMcpServer {

    public static void main(String[] args) {
        // 创建 Server
        McpServer server = McpServer.builder()
            .name("weather-server")
            .version("1.0.0")
            .build();

        // 注册 Tool
        server.addTool(
            McpTool.builder()
                .name("get_weather")
                .description("获取指定城市的天气信息")
                .inputSchema(Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "city", Map.of(
                            "type", "string",
                            "description", "城市名称"
                        )
                    ),
                    "required", List.of("city")
                ))
                .handler(params -> {
                    String city = (String) params.get("city");
                    String weather = fetchWeatherFromAPI(city);
                    return McpToolResult.success(weather);
                })
                .build()
        );

        // 注册 Resource
        server.addResource(
            McpResource.builder()
                .uri("weather://cities")
                .name("支持的城市列表")
                .mimeType("application/json")
                .handler(request -> fetchSupportedCities())
                .build()
        );

        // 启动（HTTP 模式）
        StreamableHttpTransport transport = new StreamableHttpTransport(8080);
        server.start(transport);
    }
}
```

---

## 5. Java MCP Client 开发

### 5.1 Spring AI MCP Client

```java
// application.yml
spring:
  ai:
    mcp:
      client:
        servers:
          weather:
            type: STREAMABLE_HTTP
            url: http://localhost:8080/mcp
          database:
            type: STDIO
            command: java
            args: ["-jar", "db-server.jar"]

// 自动使用 MCP Tools
@RestController
public class ChatController {

    private final ChatClient chatClient;

    // MCP Server 的 Tool 自动注册到 ChatClient
    // 不需要手动注册！

    @PostMapping("/chat")
    public String chat(@RequestBody String message) {
        return chatClient.prompt()
            .user(message)
            .call()
            .content();
    }
}
```

### 5.2 手动 MCP Client

```java
public class ManualMcpClient {

    public static void main(String[] args) {
        // 连接 MCP Server
        McpClient client = McpClient.builder()
            .transport(new StreamableHttpTransport("http://localhost:8080/mcp"))
            .build();

        client.connect();

        // 列出可用 Tools
        List<McpTool> tools = client.listTools();
        System.out.println("Available tools: " + tools.size());

        // 调用 Tool
        McpToolResult result = client.callTool("get_weather",
            Map.of("city", "北京"));

        System.out.println("Weather: " + result.content());

        // 读取 Resource
        McpResourceContent cities = client.readResource("weather://cities");
        System.out.println("Cities: " + cities.text());

        // 关闭连接
        client.close();
    }
}
```

---

## 6. MCP 生态与市场

### 6.1 官方与社区 Server

```
常用开源 MCP Server：

数据与搜索类：
  ├── @anthropic/mcp-server-filesystem    → 文件系统操作
  ├── @anthropic/mcp-server-puppeteer     → 浏览器自动化
  ├── @anthropic/mcp-server-github        → GitHub API
  ├── mcp-server-postgres                 → PostgreSQL 查询
  ├── mcp-server-elasticsearch            → ES 查询
  └── mcp-server-brave-search             → Web 搜索

开发工具类：
  ├── mcp-server-docker                   → Docker 管理
  ├── mcp-server-git                      → Git 操作
  ├── mcp-server-k8s                      → Kubernetes 管理
  └── mcp-server-sentry                   → 错误追踪

信息类：
  ├── mcp-server-slack                    → Slack 集成
  ├── mcp-server-jira                     → Jira 集成
  └── mcp-server-notion                   → Notion 集成
```

### 6.2 企业 MCP 架构

```
企业级 MCP 架构：

┌─────────────────────────────────────────────────────┐
│                   API Gateway (统一入口)              │
│              认证 / 限流 / 路由 / 审计              │
├─────────────────────────────────────────────────────┤
│                                                       │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────┐ │
│  │ MCP Server   │  │ MCP Server   │  │ MCP Server │  │
│  │ (天气/搜索)   │  │ (数据库查询) │  │ (业务API)   │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬─────┘  │
│         │                 │                  │         │
├─────────┼─────────────────┼──────────────────┼─────────┤
│         │                 │                  │         │
│    ┌────▼────┐      ┌─────▼─────┐     ┌─────▼─────┐  │
│    │ 天气API │      │  MySQL    │     │ 业务微服务 │  │
│    └─────────┘      └───────────┘     └───────────┘  │
│                                                       │
├─────────────────────────────────────────────────────┤
│  基础设施：Prometheus监控 / ELK日志 / Vault密钥管理  │
└─────────────────────────────────────────────────────┘
```

---

## 7. MCP 安全

```java
@Component
public class McpSecurityFilter {

    // 1. 认证：验证 Client 身份
    public boolean authenticate(McpRequest request) {
        String token = request.getHeader("Authorization");
        return tokenService.validate(token);
    }

    // 2. 授权：检查是否有权限调用该 Tool
    public boolean authorize(String clientId, String toolName) {
        return permissionService.hasPermission(clientId, toolName);
    }

    // 3. 输入校验：防止恶意参数
    public Map<String, Object> sanitizeParams(String toolName,
                                               Map<String, Object> params) {
        // SQL 注入检查（针对数据库 Tool）
        if (toolName.startsWith("db_")) {
            params.forEach((k, v) -> {
                if (v instanceof String s && containsSqlInjection(s)) {
                    throw new SecurityException("Potential SQL injection");
                }
            });
        }
        return params;
    }
}
```

---

## 8. 快速复习

```
□ MCP 三大原语：Tool (LLM控制) / Resource (Client控制) / Prompt (用户控制)
□ 三种 Transport：stdio (本地) / SSE (远程) / Streamable HTTP (推荐)
□ MCP vs Function Calling：标准化、自动发现、多 Client 共享
□ .mcp.json 配置 MCP Server 连接
□ Spring AI MCP Server: @Tool + @Resource + @Prompt 注解
□ Spring AI MCP Client: 自动发现和注册 Tool
□ MCP 安全：认证 / 授权 / 输入校验 / 审计
□ MCP 生态：官方 Server + 社区 Server + 企业自建
```

---

> **下一步**：[09 — 模型部署与服务化](./09-模型部署与服务化.md)
