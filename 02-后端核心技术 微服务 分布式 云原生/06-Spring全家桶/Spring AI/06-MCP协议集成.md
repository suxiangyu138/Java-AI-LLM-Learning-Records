# 06-MCP 协议集成
> Model Context Protocol 在 Spring AI 2.0 的原生实现：@McpTool 编程模型、Client/Server 双角色、传输层与安全

## 📚 目录
1. [MCP 是什么：协议三要素](#1-mcp-是什么协议三要素)
2. [Spring AI 的 MCP 实现方式](#2-spring-ai-的-mcp-实现方式)
3. [作为 MCP Client：接入外部工具](#3-作为-mcp-client接入外部工具)
4. [作为 MCP Server：@McpTool 暴露能力](#4-作为-mcp-servermcptool-暴露能力)
5. [传输层：Streamable HTTP 与 STDIO](#5-传输层streamable-http-与-stdio)
6. [工具命名、过滤与安全](#6-工具命名过滤与安全)
7. [MCP 在 Agent 架构中的位置](#7-mcp-在-agent-架构中的位置)
8. [核心要点](#8-核心要点)
9. [参考来源](#9-参考来源)

## 1. MCP 是什么：协议三要素

MCP（Model Context Protocol，Anthropic 提出、开源协议）让 LLM 应用统一、安全地调用外部能力：

| 要素 | 语义 | Spring AI 对应 |
|------|------|----------------|
| **Tools（工具）** | 可执行的函数（读文件、查库、搜网页） | `ToolCallback` / `@McpTool` |
| **Resources（资源）** | 可读取的数据（文档、配置、DB schema） | Resource 包 |
| **Prompts（提示词模板）** | 预定义提示模板 | Prompt 包 |

```text
┌─────────────┐   MCP 协议   ┌─────────────┐
│   LLM 应用   │ ◀─────────▶ │   MCP Server │
│ (MCP Client) │  工具/资源   │  (能力提供方) │
└─────────────┘             └─────────────┘
```

> 💡 **定位**：MCP 是"LLM 的 USB-C 接口"——一次接入，任何支持 MCP 的客户端都能用你的工具；Spring AI 把 MCP 做成一等公民，本地 `@Tool` 与 MCP 远程工具共用同一个 `ToolCallback` 抽象。

## 2. Spring AI 的 MCP 实现方式

| 维度 | 实现 |
|------|------|
| 编程模型 | 注解驱动：`@McpTool` 扫描注册 |
| 传输层 | **移入 Spring AI 自身**（不再依赖 MCP Java SDK 的传输实现），WebMVC/WebFlux 统一处理 |
| 默认传输 | **Streamable HTTP**（SSE 已弃用）；STDIO 用于本地进程 |
| 双角色 | 同一应用可同时是 Client 和 Server |
| 自动配置 | `spring-ai-starter-mcp-client` / `spring-ai-starter-mcp-server` |

## 3. 作为 MCP Client：接入外部工具

### 3.1 依赖与配置

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-client</artifactId>
</dependency>
```

```yaml
spring:
  ai:
    mcp:
      client:
        # servers-configuration: classpath:mcp-servers.json
        toolcallback:
          enabled: true          # 工具回调自动注册（默认开启）
```

```json
// mcp-servers.json：声明外部 MCP 服务器
{
  "mcpServers": {
    "github": {
      "url": "https://api.githubcopilot.com/mcp/",
      "headers": { "Authorization": "Bearer ${GITHUB_TOKEN}" }
    },
    "search": {
      "url": "http://localhost:3001/mcp"
    }
  }
}
```

### 3.2 使用外部工具

```java
// 外部 MCP 工具自动成为 ToolCallback，与本地工具混用
@GetMapping("/assist")
public String assist(@RequestParam String task) {
    return chatClient.prompt()
            .user(task)                                // 例："查一下这个仓库的 issue"
            .tools("github")                           // 引用 MCP 服务器名（或直接传 ToolCallback）
            .call()
            .content();
}
```

| 能力 | 说明 |
|------|------|
| 自动 schema | 外部工具的 JSON Schema 由 MCP 服务器提供，框架自动转为 `ToolCallback` |
| 鉴权 | `MCPClientCustomization` 可给 HTTP 请求加授权头（OAuth 等） |
| 过滤 | `McpToolFilter` Bean 限制哪些外部工具进入工具空间 |

## 4. 作为 MCP Server：@McpTool 暴露能力

```java
@Configuration
public class McpToolConfiguration {

    @McpTool(name = "queryOrder", description = "查询订单状态")
    public String queryOrder(@ToolParam(description = "订单号") String orderId) {
        return orderService.queryStatus(orderId);
    }
}
```

| 机制 | 说明 |
|------|------|
| 扫描 | `@McpTool` 注解 Bean 自动发现，生成 JSON Schema |
| 注册 | 自动注册到内嵌 MCP Server（`spring-ai-starter-mcp-server`） |
| 双角色 | 该应用既可为自己的 ChatClient 服务，也可被其他应用当作外部 MCP 调用 |
| 集成 | 与 Spring Batch、Cloud Config 等组合，构建企业 MCP 服务 |

> 🎯 **核心要点**：`@McpTool` 与 `@Tool` 的差别是**暴露范围**——`@Tool` 只服务本应用 ChatClient；`@McpTool` 把能力发布为协议服务，任何 MCP 客户端（其他 Spring AI 应用、Claude 桌面端、自研 Agent）都能调用。

## 5. 传输层：Streamable HTTP 与 STDIO

| 传输 | 适用 | 特点 |
|------|------|------|
| **Streamable HTTP** | 网络远程（默认） | 2.0 默认；流式响应（SSE over HTTP）；替代已弃用的 SSE-only 传输 |
| STDIO | 本地进程（同机） | 通过标准输入/输出通信，零网络开销 |

```yaml
spring:
  ai:
    mcp:
      server:
        transport: http    # http | stdio（默认 http）
```

> ⚠️ **迁移警告**：1.x 配置的 SSE 传输端点（`/mcp/sse`）已弃用，2.0 客户端默认连 Streamable HTTP 端点——老 MCP Server 若只支持 SSE，客户端需显式配置或升级 Server。

## 6. 工具命名、过滤与安全

| 机制 | 说明 |
|------|------|
| 名字前缀 | `DefaultMcpToolNamePrefixGenerator` 给每个 MCP 服务器工具加前缀（如 `github_queryIssue`），防跨服务器冲突 |
| `McpToolFilter` | 白名单/黑名单过滤外部工具（例如：接入多服务器时只暴露 `github` 的读工具） |
| OAuth | MCP HTTP 支持 OAuth 授权（`MCPClientCustomization` 注入令牌） |
| 最小权限 | 生产建议按"工具级"收敛：只注册业务需要的服务器与工具 |

## 7. MCP 在 Agent 架构中的位置

```text
企业 Agent
 ├── 本地 @Tool：数据库操作、业务服务（进程内，低延迟）
 ├── MCP Client → 内部 MCP Server：订单系统、库存系统（协议化，跨语言/跨团队）
 ├── MCP Client → 外部 MCP Server：GitHub、搜索、天气（第三方能力即插即用）
 └── 自己的能力用 @McpTool 发布 → 供其他团队/客户端复用
```

| 架构决策 | 建议 |
|----------|------|
| 进程内高频工具 | 本地 `@Tool`（无网络开销） |
| 跨团队/跨语言能力 | MCP Server 发布（协议边界清晰） |
| 第三方能力 | MCP Client 接入（生态即插即用） |
| 治理 | 统一 MCP 服务器注册表 + 工具命名规范 + 权限审计 |

## 8. 核心要点

> 🎯 **核心要点**：
> - MCP 三要素：Tools（执行）、Resources（数据）、Prompts（模板），Spring AI 全支持；
> - `@McpTool` 发布能力（Server），`mcp-servers.json` 接入能力（Client），双角色可并存；
> - 2.0 传输层内移：Streamable HTTP 默认，SSE 弃用，STDIO 留给本地；
> - 本地 `@Tool` 与 MCP 工具统一为 `ToolCallback`，在 `.tools(...)` 里混用；
> - 命名前缀防冲突 + `McpToolFilter` 收敛权限是生产必配。

## 9. 参考来源

- [Spring AI Reference：MCP](https://docs.spring.io/spring-ai/reference/api/mcp.html)
- [Spring AI 2.0.0 GA 发布博客（MCP 原生集成）](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now)
- [Model Context Protocol 规范（modelcontextprotocol.io）](https://modelcontextprotocol.io/)
- [Tool Calling in Spring AI 2.0（本地/MCP 工具混用）](https://spring.io/blog/2026/06/15/spring-ai-composable-tool-calling)

---

**下一模块**：[07-RAG与向量数据库](07-RAG与向量数据库.md)　/　**返回总览**：[00-总览](00-Spring%20AI总览.md)
