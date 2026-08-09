# 框架与生态：从注解到 MCP

> 落地层 = 框架把"注册、声明、执行"工程化的程度。Java 双雄：Spring AI `@Tool` 与 LangChain4j `@Tool` 各成体系；MCP 是跨语言跨宿主的工具分发标准（10 个官方 SDK）。2026 现状：**注解是主流、Provider 是动态化手段、MCP 是生态收敛方向**。

## 1. Java 双框架：注解注册对照

| 维度 | Spring AI | LangChain4j |
|---|---|---|
| 工具注解 | `@org.springframework.ai.tool.annotation.Tool` | `@dev.langchain4j.agent.tool.Tool` |
| 参数注解 | `@ToolParam` | `@P` |
| 注册入口 | `ToolCallbacks.from(obj)` | `AiServices.builder().tools(obj)` |
| 结果消息 | `ToolResponseMessage` | `ToolExecutionResultMessage` |
| 调用请求 | `ToolCall` | `ToolExecutionRequest` |
| 序列化 | Jackson JSON | Java 序列化 |

```java
// LangChain4j 最小工具
public class CalculatorTools {
    @Tool("Add two numbers")
    public double add(@P("first number") double a, @P("second number") double b) {
        return a + b;
    }
}
// 注册
MathAssistant assistant = AiServices.builder(MathAssistant.class)
    .chatModel(chatModel)
    .tools(new CalculatorTools())
    .build();
```

```java
// Spring AI 最小工具
@Component
public class WeatherTools {
    @Tool(description = "查询城市天气")
    public String getWeather(@ToolParam(description = "城市名") String city) {
        return weatherService.query(city);
    }
}
// 注册：ToolCallbacks.from(new WeatherTools())
```

> 🎯 核心要点：两框架注解语义一致（方法 = 工具、注解值 = 描述、参数注解 = 参数描述）——**契约生成自动完成**：注解即 schema。手写 JSON Schema 的时代只存在于无框架场景。

## 2. 注册机制：从静态到动态

| 机制 | 框架 | 说明 |
|---|---|---|
| 静态列表 | 两者皆可 | `.tools(new Calculator(), new WeatherService())` |
| Bean 自动扫描 | Spring AI | `AiServicesAutoConfig` 扫描容器内 `@Tool` Bean 自动生成 ToolSpecification |
| 动态 Provider | LangChain4j | `.toolProvider(new DynamicToolProvider())`——运行时决定工具集（对应 05 篇动态发现） |
| 批量注册 | LangGraph4j | `tool(ToolCallback)` / `tools(List)` / `tools(ToolCallbackProvider)` / `toolsFromObject(Object)` |
| 执行控制 | LangGraph4j | `internalToolExecutionEnabled(false)`——关掉框架自动执行，由图路由控制（Spring AI ToolCallingChatOptions） |

> 💡 2026 Java 生产模式：**注解注册 + 动态 Provider + 外部工具执行**三件套——注解负责契约生成，Provider 负责按需注入，执行控制交给编排层（如 LangGraph4j 的图流程）。

## 3. 执行与容错能力

| 能力 | Spring AI | LangChain4j |
|---|---|---|
| 并发执行 | ToolCallbacks 支持 | `.executeToolsConcurrently()` |
| 错误处理器 | `ToolExecutionErrorHandler` | `toolExecutionErrorHandler(...)` |
| 工具上下文 | `ToolContext`（元数据透传） | — |
| 返回类型 | 基本类型（Gemini 需字符串——`Objects.toString()` 兜底） | 可序列化类型（字符串/List 最佳） |
| 方法约束 | public 方法 | public、可序列化返回 |

> ⚠️ 注意差异：Gemini 要求字符串返回（Spring AI 官方提示 `Objects.toString()` 兜底）；LangChain4j 强调"返回字符串/List 最好"——**结果字符串化原则（06 篇）在框架层同样成立**。

## 4. MCP：工具的分发生态

| 维度 | 现状（2026） |
|---|---|
| 官方 SDK | 10 个：TS/Python/Go/C#/Java/Kotlin/Swift/Rust/Ruby/PHP |
| 调试工具 | MCP Inspector（官方）：检查工具定义、发调用、验证行为 |
| 传输 | stdio（本地/开发）/ HTTP+SSE（生产托管） |
| Java 集成 | `spring-ai-starter-mcp-client` / `langchain4j-mcp` |
| 注册中心 | 官方注册中心 + 动态服务器发现（演进中） |
| 生态工具 | API→MCP 生成器（Postman/Stainless/Zuplo）、mcpadapt（解锁 650+ MCP 工具跨框架） |

> 🎯 核心要点：MCP 与框架 `@Tool` 互补——**注解封装"自己进程里的方法"；MCP 封装"别的进程/服务的工具"**。同一 Agent 里两者可共存：本地工具走注解、外部能力走 MCP。

## 5. MCP 服务器架构五模式（2026 研究）

| 模式 | 特征 | 适用 |
|---|---|---|
| Resource Gateway | 以资源为中心暴露数据 | RAG、知识库 |
| Tool Orchestrator | 编排多个后端动作 | 业务流程 |
| Stateful Session Server | 带会话状态 | 对话型工具 |
| Proxy Aggregator | 聚合多个上游服务 | API 网关化工具 |
| Domain-Specific Adapter | 领域专用适配 | 单一业务域 |

> ⚠️ 关键数据：**10-15 个工具/上下文是 Haiku 级模型的准确率边界**——服务器设计要少而精，别堆工具（05 篇数量治理在 MCP 侧同样成立）。

## 6. MCP 服务器构建规范

| 步骤 | 内容 |
|---|---|
| 1. 脚手架 | 安装 MCP SDK（`npm i @modelcontextprotocol/sdk`） |
| 2. 定义 | name + description + inputSchema（JSON Schema，带参数级描述与约束） |
| 3. 实现 | 处理器绑定 `CallToolRequestSchema` |
| 4. 传输 | stdio（本地）/ HTTP+SSE（生产） |
| 5. 测试 | MCP Inspector 先行，再接真实客户端 |

| 设计规范 | 规则 |
|---|---|
| 命名 | 动作导向：create_issue、list_repositories；禁 do_github/handle_request |
| schema | 全参数描述 + min/max 约束 + required |
| 返回 | 结构化可预测，含下一步建议；搜索工具避免过早"未找到" |
| 搜索工具 | 让 LLM 从返回数据判断相关性，不轻易判负（敏感数据除外） |
| 安全 | 凭据环境变量（启动校验）、Zod 输入校验、限流、RBAC、审计 |
| 服务器指令 | 说明服务器用途、工具与工作流映射、环境配置要求 |

> 💡 MCP vs 脚本的选型：**MCP 用于探索性/一次性/跨工具工作流**（自然语言、自适应）；**传统脚本用于可重复/确定性/性能敏感/离线操作**。混合模式常见：MCP 原型 → 验证后固化为脚本。

## 7. 框架选择建议

| 场景 | 选择 |
|---|---|
| Java Spring 生态 | Spring AI（与 Boot 集成、Bean 自动扫描） |
| 纯 Java/独立服务 | LangChain4j（AiServices 更轻、动态 Provider 成熟） |
| 图式编排 Agent | LangGraph4j（双框架集成 + 执行路由控制） |
| 跨语言共享工具 | MCP（一次定义多宿主） |
| 多模型切换 | 框架抽象层（避免手写协议，04 篇） |

> 🎯 核心要点：选框架 = 选**注册与执行的生命周期管理方式**——注解省了 schema 手写，Provider 补了动态发现，MCP 补了跨应用分发。三层可以组合，不必二选一。

## 8. 常见误区

| 误区 | 真相 |
|---|---|
| "@Tool 注解就是全部" | 注解只生成契约；权限、过滤、执行控制还要自己接 |
| "框架自动执行工具很省事" | 自动执行跳过了你的授权逻辑——生产要接管执行（internalToolExecutionEnabled） |
| "MCP 只给前端用" | MCP 是分发协议，Java 客户端（spring-ai-starter-mcp-client）完全可用 |
| "MCP 与注解互斥" | 互补：本地方法走注解、外部能力走 MCP，可共存 |
| "MCP 服务器越多越好" | 单服务器 10-15 工具准确率边界——少而精 |
| "stdio 够生产用" | stdio 适合本地/开发；生产要 HTTP/SSE + 限流 + 鉴权 |
| "框架处理了安全" | 框架不防参数注入——工具参数不可信，白名单/参数化自己管 |

## 9. 面试速记

| 问题 | 一句话答案 |
|---|---|
| Spring AI 工具注解？ | @Tool + @ToolParam，ToolCallbacks.from() 注册，Bean 自动扫描 |
| LangChain4j 工具注解？ | @Tool + @P，AiServices.builder().tools() 注册，toolProvider 动态化 |
| 两框架返回类型注意？ | Gemini 要字符串（Objects.toString 兜底）；可序列化类型最佳 |
| 并发执行？ | LangChain4j executeToolsConcurrently() |
| 动态工具怎么做？ | LangChain4j DynamicToolProvider；Spring AI 自实现 Provider |
| 接管执行控制？ | LangGraph4j internalToolExecutionEnabled(false) |
| MCP 是什么层？ | 分发协议——一次定义、10 SDK、多宿主调用 |
| MCP 服务器五模式？ | Resource Gateway/Tool Orchestrator/Stateful Session/Proxy Aggregator/Domain Adapter |
| MCP 工具命名？ | 动作导向 create_issue；禁 do_github 泛名 |
| 怎么测 MCP 服务器？ | MCP Inspector：查定义、发调用、验行为 |
| MCP vs 脚本？ | MCP 探索性/跨工具；脚本确定性/性能——原型后固化 |
| 框架与 MCP 关系？ | 注解封进程内方法、MCP 封跨进程服务——互补共存 |

---

**下一模块**：[10-生产实践与面试冲刺](10-生产实践与面试冲刺.md)　**返回总览**：[00-Tool 工具开发与注册总览](00-Tool工具开发与注册总览.md)

## 参考来源

- [Tool Management and Execution（LangGraph4j DeepWiki）](https://deepwiki.com/langgraph4j/langgraph4j/4.2-tool-management-and-execution)
- [Agent and Tool Examples（LangGraph4j DeepWiki）](https://deepwiki.com/langgraph4j/langgraph4j/8.3-agent-and-tool-examples)
- [dev.langchain4j.agent.tool 包文档（LangChain4j）](https://docs.langchain4j.dev/apidocs/dev/langchain4j/agent/tool/package-summary.html)
- [MCP Server Complete Guide for Developers (2026)（Cosmic JS）](https://www.cosmicjs.com/blog/mcp-server-complete-guide)
- [The MCP Ecosystem in 2026（StraySpark）](https://www.strayspark.studio/blog/mcp-ecosystem-2026-strayspark-landscape)
- [MCP Server Architecture Patterns（arXiv 2606.30317）](https://huggingface.co/buckets/huggingchat/papers-content/tree/2606/2606.30317.md)
- [Tool Calling & Programmatic Prompts（Vaadin）](https://vaadin.com/docs/latest/flow/ai-support/tool-calling)
