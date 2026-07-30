# MCP 协议 Java 实现

> Java 生态中 MCP 有三种实现方案：Spring AI MCP Server（Spring Boot 原生）、官方 Java MCP SDK（框架无关）、Quarkus MCP Server（LangChain4j 推荐）。关键区别：LangChain4j 只支持 MCP Client，不支持 Server

---

## 📚 目录

1. [MCP 协议回顾](#1-mcp-协议回顾)
2. [Java MCP 生态全景](#2-java-mcp-生态全景)
3. [方案一：Spring AI MCP Server](#3-方案一spring-ai-mcp-server)
4. [方案二：官方 Java MCP SDK](#4-方案二官方-java-mcp-sdk)
5. [方案三：Quarkus MCP Server](#5-方案三quarkus-mcp-server)
6. [MCP Client 实现对比](#6-mcp-client-实现对比)
7. [安全实践](#7-安全实践)
8. [选型指南](#8-选型指南)

---

## 1. MCP 协议回顾

```text
MCP（Model Context Protocol）
├── 定义：AI 模型与外部工具/数据源之间的标准化通信协议
├── 发布者：Anthropic（2024 年底）
├── 通信方式：JSON-RPC 2.0 over stdio / SSE / WebSocket
│
├── 三种原语：
│   ├── Resources —— Server → Model（只读数据暴露）
│   ├── Tools —— Model → Server（模型驱动的动作）
│   └── Prompts —— Server → User（用户驱动的模板）
│
└── 重要区分：
    ├── MCP Client = 消费 MCP 工具（LangChain4j ✅, Spring AI ✅）
    └── MCP Server = 暴露 MCP 工具（LangChain4j ❌, Spring AI ✅）
```

> ⚠️ **关键认知**：LangChain4j 团队明确表示不会实现 MCP Server 端。如果需要用 Java 构建 MCP Server，必须使用 Spring AI 或官方 Java MCP SDK。

---

## 2. Java MCP 生态全景

```text
                    Java MCP 实现方案（2026.07）

┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  ┌─────────────────────┐  ┌─────────────────────┐           │
│  │  Spring AI MCP       │  │  官方 Java MCP SDK   │           │
│  │  Server Starter      │  │  (io.modelcontext    │           │
│  │  - WebMvc transport  │  │   protocol.sdk:mcp)  │           │
│  │  - WebFlux transport │  │  - stdio transport   │           │
│  │  - OAuth2 支持       │  │  - SSE transport     │           │
│  │  - Actuator 监控     │  │  - 框架无关           │           │
│  └──────────┬──────────┘  └──────────┬──────────┘           │
│             │                        │                       │
│             │     ┌──────────────────┘                       │
│             │     │                                          │
│  ┌──────────┴─────┴──────────┐                              │
│  │       MCP Server           │                              │
│  │  暴露 Tools / Resources    │                              │
│  └──────────┬─────────────────┘                             │
│             │                                                │
│  ┌──────────┴─────────────────┐                             │
│  │       MCP Client            │                             │
│  │  - LangChain4j MCP Client   │                             │
│  │  - Spring AI MCP Client     │                             │
│  │  - Quarkus MCP Client       │                             │
│  └─────────────────────────────┘                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. 方案一：Spring AI MCP Server

### 3.1 依赖配置

```xml
<!-- Spring AI MCP Server WebMvc（SSE 传输） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
    <version>1.1.0</version>
</dependency>

<!-- 或 WebFlux 版本 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webflux</artifactId>
    <version>1.1.0</version>
</dependency>
```

### 3.2 完整 Server 示例

```java
// ========== 工具定义 ==========
@Component
public class WeatherTools {

    @Tool(name = "get_weather",
          description = "获取指定城市的实时天气信息")
    public String getWeather(
            @ToolParam(description = "城市名称，如 Beijing") String city) {

        // 实际项目调用天气 API
        return String.format("""
            {
                "city": "%s",
                "temperature": "22-30°C",
                "humidity": "45%",
                "condition": "晴"
            }
            """, city);
    }

    @Tool(name = "get_forecast",
          description = "获取指定城市未来 3 天天气预报")
    public String getForecast(
            @ToolParam(description = "城市名称") String city,
            @ToolParam(description = "预报天数，1-7") int days) {

        return String.format("{\"city\":\"%s\", \"forecast_days\": %d}", city, days);
    }
}

// ========== Spring Boot 配置 ==========
@SpringBootApplication
public class McpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }
}

// application.yml
spring:
  ai:
    mcp:
      server:
        name: "weather-service"
        version: "1.0.0"
        type: SYNC       # SYNC 或 ASYNC
        protocol: SSE    # SSE 或 STREAMABLE
        capabilities:
          tools: true
          resources: true
```

### 3.3 暴露 REST 资源

```java
@RestController
public class DocumentResource {

    @GetMapping("/mcp/resources/documents")
    public List<McpResource> listDocuments() {
        return List.of(
            new McpResource("doc://readme", "项目说明文档", "text/markdown"),
            new McpResource("doc://api-spec", "API 规范文档", "text/yaml")
        );
    }

    @GetMapping("/mcp/resources/documents/{name}")
    public String readDocument(@PathVariable String name) {
        // 根据 name 读取文档内容
        return Files.readString(Path.of("/docs/" + name + ".md"));
    }
}
```

---

## 4. 方案二：官方 Java MCP SDK

### 4.1 依赖

```xml
<dependency>
    <groupId>io.modelcontextprotocol.sdk</groupId>
    <artifactId>mcp</artifactId>
    <version>2.0.0</version>
</dependency>
```

### 4.2 STDIO 模式 Server（无需任何框架）

```java
import io.modelcontextprotocol.sdk.mcp.server.*;
import io.modelcontextprotocol.sdk.mcp.shared.*;
import io.modelcontextprotocol.sdk.mcp.spec.*;
import io.modelcontextprotocol.sdk.mcp.util.*;

public class StdioMcpServer {

    public static void main(String[] args) {
        // 1. 创建 STDIO 传输
        StdioServerTransportProvider transport =
            new StdioServerTransportProvider(McpJsonDefaults.getMapper());

        // 2. 构建 MCP Server
        McpSyncServer server = McpServer.sync(transport)
            .serverInfo("my-java-tools", "1.0.0")
            .capabilities(ServerCapabilities.builder()
                .tools(true)
                .resources(false)  // 不需要只读资源
                .build())
            .build();

        // 3. 注册工具
        server.addTool(new SyncToolSpecification(
            new Tool(
                "greet",
                "向指定用户打招呼",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "name", Map.of(
                            "type", "string",
                            "description", "用户名称"
                        )
                    ),
                    "required", List.of("name")
                )
            ),
            (exchange, request) -> {
                String name = (String) request.arguments().get("name");
                return new CallToolResult(
                    List.of(new TextContent("你好，" + name + "！👋")),
                    false  // isError = false
                );
            }
        ));

        // 4. 注册第二个工具
        server.addTool(new SyncToolSpecification(
            new Tool(
                "calculate",
                "执行数学计算",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "expression", Map.of(
                            "type", "string",
                            "description", "数学表达式，如 2+3*4"
                        )
                    ),
                    "required", List.of("expression")
                )
            ),
            (exchange, request) -> {
                String expr = (String) request.arguments().get("expression");
                // ⚠️ 安全警告：生产环境必须校验表达式，禁止 eval！
                double result = evaluateSafely(expr);
                return new CallToolResult(
                    List.of(new TextContent(expr + " = " + result)),
                    false
                );
            }
        ));

        // 5. 保持运行
        Runtime.getRuntime().addShutdownHook(
            new Thread(() -> server.closeGracefully()));
    }

    private static double evaluateSafely(String expr) {
        // 使用安全的表达式求值库（如 exp4j）
        // 绝不使用 ScriptEngine.eval() 或 Runtime.exec()！
        return new Expression(expr).evaluate();
    }
}
```

### 4.3 SSE 模式 Server（HTTP 传输，适合远程）

```java
// SSE Server —— 通过 HTTP 暴露 MCP 服务
@WebServlet("/mcp/sse")
public class SseMcpServlet extends HttpServlet {

    private McpSyncServer server;

    @Override
    public void init() throws ServletException {
        // 创建 SSE 传输
        SseServerTransportProvider transport =
            new SseServerTransportProvider(
                McpJsonDefaults.getMapper(),
                "/mcp/messages"  // 消息端点
            );

        this.server = McpServer.sync(transport)
            .serverInfo("remote-tools", "1.0.0")
            .capabilities(ServerCapabilities.builder()
                .tools(true).build())
            .build();

        // 注册工具（同 STDIO 示例）
        registerTools(server);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        // 建立 SSE 连接...
    }
}
```

---

## 5. 方案三：Quarkus MCP Server

### 5.1 依赖

```xml
<dependency>
    <groupId>io.quarkiverse.mcp</groupId>
    <artifactId>quarkus-mcp-server</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 5.2 Quarkus 风格实现

```java
@McpTool(name = "file-reader", description = "文件读取工具")
@ApplicationScoped
public class FileReaderTool {

    @ToolMethod(name = "read_file",
                description = "读取指定路径的文件内容")
    @ToolParam(name = "path", description = "文件路径")
    public String readFile(String path) {
        // 安全校验：路径必须在允许的目录内
        if (!path.startsWith("/workspace/")) {
            return "❌ 禁止访问该路径";
        }
        return Files.readString(Path.of(path));
    }

    @ToolMethod(name = "list_files",
                description = "列出目录下的文件")
    @ToolParam(name = "directory", description = "目录路径")
    public String listFiles(String directory) {
        try (var stream = Files.list(Path.of(directory))) {
            return stream
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            return "❌ 读取目录失败: " + e.getMessage();
        }
    }
}
```

```properties
# application.properties
quarkus.mcp.server.name=file-service
quarkus.mcp.server.version=1.0.0
quarkus.mcp.server.transport=stdio
```

---

## 6. MCP Client 实现对比

### 6.1 LangChain4j MCP Client

```java
// LangChain4j MCP Client —— 消费外部 MCP 工具
McpTransport transport = new StdioMcpTransport(
    new File("/path/to/mcp-server"));
McpClient mcpClient = new DefaultMcpClient.Builder()
    .transport(transport)
    .build();

// 获取 MCP Server 提供的工具
List<ToolSpecification> tools = mcpClient.listTools();

// 构建支持 MCP 工具的 AI Service
Assistant assistant = AiServices.builder(Assistant.class)
    .chatLanguageModel(model)
    .tools(mcpClient)  // 直接把 MCP 工具注入 Agent
    .build();
```

### 6.2 Spring AI MCP Client

```java
// Spring AI MCP Client —— 自动配置
@Configuration
public class McpClientConfig {

    @Bean
    public McpClient mcpClient() {
        return McpClient.builder()
            .transport(new StdioTransport("/path/to/mcp-server"))
            .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                  McpClient mcpClient) {
        return builder
            .defaultTools(mcpClient.getTools())  // 自动注册 MCP 工具
            .build();
    }
}
```

### 6.3 Client 端选型速查

| 场景 | 推荐 Client |
|------|-----------|
| Spring Boot + 自动配置 | Spring AI MCP Client |
| LangChain4j + AiServices | LangChain4j MCP Client |
| Quarkus 项目 | Quarkus MCP Client |
| 纯 Java SE 项目 | 官方 Java MCP SDK Client |

---

## 7. 安全实践

### 7.1 工具安全三原则

```text
┌─────────────────────────────────────────────┐
│ 原则 1：永远不信任模型的参数                    │
│ ✅ 校验类型、范围、格式                        │
│ ✅ 使用 Allowlist（白名单）而非 Denylist       │
│ ❌ 不要假设模型传的参数是"干净"的               │
├─────────────────────────────────────────────┤
│ 原则 2：敏感操作需要审批                        │
│ ✅ 文件删除、数据库写入、网络请求等要确认         │
│ ✅ 审批不能依赖模型判断                        │
│ ❌ 不要给模型"自动审批"的权限                   │
├─────────────────────────────────────────────┤
│ 原则 3：沙箱隔离                              │
│ ✅ 文件系统限制在指定目录                       │
│ ✅ 命令执行使用白名单命令集                     │
│ ❌ 不要让模型参数直接拼接到 shell/ SQL           │
└─────────────────────────────────────────────┘
```

### 7.2 参数安全校验示例

```java
@Tool(name = "execute_sql",
      description = "执行只读 SQL 查询")
public String executeQuery(
        @ToolParam(description = "SQL 查询语句") String sql) {

    // 1. 只允许 SELECT（拒绝 INSERT/UPDATE/DELETE/DROP）
    if (!sql.trim().toUpperCase().startsWith("SELECT")) {
        return "❌ 只允许 SELECT 查询";
    }

    // 2. 拒绝危险的 SQL 关键字
    List<String> dangerous = List.of(
        "DROP", "TRUNCATE", "ALTER", "CREATE",
        "INSERT", "UPDATE", "DELETE", "EXEC", "--", ";--"
    );
    String upperSql = sql.toUpperCase();
    for (String keyword : dangerous) {
        if (upperSql.contains(keyword)) {
            return "❌ 禁止的操作: " + keyword;
        }
    }

    // 3. 使用参数化查询执行（而非字符串拼接）
    return jdbcTemplate.queryForList(sql).toString();
}
```

---

## 8. 选型指南

```text
你要构建 MCP Server 还是 MCP Client？

如果是 Server（暴露工具给 AI）：
├── Spring Boot 项目 → Spring AI MCP Server Starter ✅
├── Quarkus 项目 → Quarkus MCP Server ✅
├── 框架无关/纯 Java → 官方 Java MCP SDK ✅
└── 用 LangChain4j → ❌ 不支持 MCP Server！

如果是 Client（消费外部 MCP 工具）：
├── Spring Boot + Spring AI → Spring AI MCP Client
├── LangChain4j → LangChain4j MCP Client
├── Quarkus → Quarkus MCP Client
└── 纯 Java SE → 官方 Java MCP SDK Client
```

---

> 🎯 **核心要点**：Java MCP 生态在 2026 年已完全成熟。Spring AI 是唯一同时提供 MCP Server 和 Client 完整方案的 Java 框架，LangChain4j 仅在 Client 端支持 MCP。构建 MCP Server 的首选是 Spring AI Starter，框架无关场景用官方 SDK。

---

**上一模块**：[01-核心框架对决](01-核心框架-LangChain4j与SpringAI.md) ｜ **下一模块**：[03-RAG 检索增强生成](03-RAG检索增强生成.md) ｜ **返回总览**：[00-Java AI 生态总览](00-Java AI生态总览.md)
