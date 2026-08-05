# 05 - Agent Skills 框架实现对比

> 🎯 六大主流框架的 Skill 实现方式全景对比 — 从 OpenAI Function Calling 到 Spring AI，掌握各框架的 Skill 抽象和最佳实践

---

## 目录

1. [OpenAI Function Calling / Tools](#1-openai-function-calling--tools)
2. [LangChain Tool & Agent](#2-langchain-tool--agent)
3. [Spring AI Tool Calling](#3-spring-ai-tool-calling)
4. [Claude Code Skill System](#4-claude-code-skill-system)
5. [Semantic Kernel Plugin](#5-semantic-kernel-plugin)
6. [MCP (Model Context Protocol)](#6-mcp-model-context-protocol)
7. [框架全景对比与选型](#7-框架全景对比与选型)

---

## 1. OpenAI Function Calling / Tools

### 1.1 核心机制

```text
OpenAI 的 Tool 是 Skill 的最基础形态：定义函数签名 → LLM 决定调用 → 应用执行 → 结果返回

流程：
  User Message → LLM 判断需要 Tool → 返回 tool_calls → 应用执行函数
  → 将结果作为 tool role 消息发回 → LLM 生成最终回复
```

### 1.2 代码实现

```java
// OpenAI Tool 定义（Java SDK）
ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
    .model("gpt-4o")
    .messages(List.of(
        ChatMessage.ofUser("北京今天天气怎么样？")
    ))
    .tools(List.of(
        Tool.of(FunctionDef.builder()
            .name("get_weather")
            .description("获取指定城市的天气信息。适用：查询实时天气、温度、湿度、风力等")
            .parameters(JsonObject.of(
                "type", "object",
                "properties", JsonObject.of(
                    "city", JsonObject.of(
                        "type", "string",
                        "description", "城市名称，中文或英文，如'北京'或'Beijing'"
                    ),
                    "unit", JsonObject.of(
                        "type", "string",
                        "enum", List.of("celsius", "fahrenheit"),
                        "description", "温度单位"
                    )
                ),
                "required", List.of("city")
            ))
            .build()
        )
    ))
    .build();
```

### 1.3 并行 Tool 调用

```text
OpenAI 2023.11 起支持 parallel_tool_calls：

  User: "北京和上海的天气怎么样？"
  → LLM 返回两个 tool_calls:
    tool_calls[0]: get_weather(city="北京")
    tool_calls[1]: get_weather(city="上海")
  → 应用并行执行 → 合并结果返回 LLM
```

> 💡 **OpenAI 模式特点**：最简洁的 Skill 抽象，没有框架层 — 应用完全负责任务编排、状态管理、错误重试

---

## 2. LangChain Tool & Agent

### 2.1 Tool 抽象层次

```text
LangChain 的 Tool 体系（从低到高）：

  Runnable（基础单元）
    └── BaseTool（Tool 基类）
         ├── Tool（简单 Tool：name + description + func）
         ├── StructuredTool（结构化 Tool：带 args_schema）
         ├── @tool 装饰器（函数 → Tool）
         └── BaseRetriever（检索类 Tool）
    └── AgentExecutor（Skill 编排器）
         ├── ReAct Agent
         ├── OpenAI Tools Agent
         └── Structured Chat Agent
```

### 2.2 代码实现

```java
// LangChain4j 中的 Tool/Skill 定义
public class CodeAnalysisSkills {

    // 方式一：@Tool 注解（最简单）
    @Tool("在代码库中搜索匹配的代码片段。适用：查找函数、类、特定模式")
    public List<SearchResult> searchCode(
        @P("搜索关键词或正则表达式") String query,
        @P("文件类型过滤，如 .java,.xml") String fileTypes
    ) {
        return searchService.search(query, fileTypes);
    }

    // 方式二：@Tool 复杂参数（结构化 Tool）
    @Tool("对代码变更进行系统化审查，发现Bug、安全、性能问题")
    public ReviewReport reviewCode(
        @P("要审查的文件路径列表") List<String> filePaths,
        @P("审查类型：bug/security/performance/style") List<String> checkTypes
    ) {
        return reviewService.review(filePaths, checkTypes);
    }
}

// Agent 绑定 Skills
AiServices.builder(Assistant.class)
    .chatLanguageModel(model)
    .tools(new CodeAnalysisSkills())  // 自动注册所有 @Tool 方法
    .build();
```

### 2.3 AgentExecutor — Skill 编排

```java
// LangChain4j AgentExecutor：自动编排 Skill 调用
public class SkillOrchestrator {
    public static void main(String[] args) {
        // 注册多个 Skill
        List<Object> skills = List.of(
            new CodeSearchSkill(),
            new CodeReviewSkill(),
            new TestRunnerSkill(),
            new DeploySkill()
        );

        // Agent 自动选择并编排 Skill
        Agent agent = AiServices.builder(Agent.class)
            .chatLanguageModel(OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o")
                .build())
            .tools(skills)
            .build();

        String result = agent.chat(
            "审查 src/main/java 下昨天修改的代码，如果有严重问题，尝试修复并运行测试"
        );
        // Agent 自动编排：code_review → bug_fix → test_runner
    }
}
```

---

## 3. Spring AI Tool Calling

### 3.1 Spring AI Tool 机制

```text
Spring AI 的 Tool Calling 深度集成 Spring 生态：

  ① @Tool 注解标记方法
  ② 自动扫描 @Component 中的 @Tool 方法
  ③ 自动生成 OpenAI Function 格式的 Schema
  ④ 通过 ToolCallback 执行
```

### 3.2 代码实现

```java
// Spring AI Skill 定义
@Component
public class WeatherSkill {

    private final WeatherService weatherService;

    public WeatherSkill(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @Tool(description = "获取指定城市的天气信息")
    public WeatherResponse getWeather(
        @ToolParam(description = "城市名称，如'北京'、'Shanghai'") String city,
        @ToolParam(description = "温度单位：celsius或fahrenheit") String unit
    ) {
        return weatherService.query(city, unit);
    }
}

// Spring AI + OpenAI 自动集成
@Configuration
public class AIConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                  List<ToolCallback> toolCallbacks) {
        return builder
            .defaultTools(toolCallbacks.toArray(new ToolCallback[0]))
            .build();
    }
}

// 使用
@RestController
public class AgentController {

    @Autowired
    private ChatClient chatClient;

    @GetMapping("/agent/chat")
    public String chat(@RequestParam String message) {
        return chatClient.prompt()
            .user(message)
            .call()
            .content();  // Spring AI 自动处理 Tool Calling 循环
    }
}
```

### 3.3 Spring AI vs LangChain4j

| 维度 | Spring AI | LangChain4j |
|------|-----------|-------------|
| **Spring 集成** | 原生一等公民 | 需要适配 |
| **Tool 定义** | `@Tool` + `@ToolParam` | `@Tool` + `@P` |
| **自动配置** | Spring Boot AutoConfiguration | 手动配置 |
| **MCP 支持** | ✅ 内置 MCP Client | ✅ 社区支持 |
| **Advisors 链** | ✅ 过滤器链模式 | ❌ |
| **多模态** | ✅ OpenAI/Vertex 原生 | ✅ |
| **模型切换** | `spring.ai.openai` → 改依赖即可 | 修改 Builder |

---

## 4. Claude Code Skill System

### 4.1 Claude Code 的 Skill 设计

```text
Claude Code 的 Skill 是最高级的抽象层次：

  Skill = 结构化指令文件（Markdown + YAML Frontmatter）

特点：
  ① 文件即 Skill：一个 .md 文件 = 一个 Skill
  ② 渐进式加载：Skill 描述先加载，内容按需加载
  ③ 自然语言定义：没有代码 Schema，全凭 LLM 理解
  ④ 工具完全可用：Skill 内可使用所有 Claude Code 工具
```

### 4.2 Skill 定义

```markdown
---
name: code-review
description: >
  对代码变更进行系统化审查。
  适用：PR Review、代码质量检查、安全审查。
  不适用：仅运行测试（用 test-runner skill）
triggers:
  - "review"
  - "code review"
  - "检查代码"
  - "审查"
---

# Code Review Skill

When the user asks for a code review, follow this workflow:

1. **Get Changes**: Use `git diff` to see what changed
2. **Read Files**: Read each changed file in full
3. **Analyze** across dimensions:
   - Correctness: logic errors, edge cases, null handling
   - Security: injection, auth, secrets exposure
   - Performance: N+1 queries, memory leaks, blocking IO
   - Style: naming, consistency, documentation
4. **Report**: Structured findings with severity (critical/high/medium/low)
5. **Suggest Fixes**: For each finding, suggest concrete fix
```

### 4.3 与其他框架的本质区别

```text
Claude Code Skill vs 传统 Tool/Skill：

  传统 Tool：
    定义 = 代码（函数签名 + Schema）
    执行 = 框架调用代码
    选择 = LLM 看 Schema 匹配

  Claude Code Skill：
    定义 = 自然语言（Markdown 指令）
    执行 = LLM 阅读指令 + 自主调用工具
    选择 = LLM 看描述匹配 + 按需加载全文

  本质区别：Claude Code Skill 是指令（告诉 LLM 怎么做），
          传统 Tool 是工具（让 LLM 调用做好的功能）
```

---

## 5. Semantic Kernel Plugin

### 5.1 核心概念

```text
Microsoft Semantic Kernel 的 Plugin = Skill 的 .NET 实现

  Kernel（内核）
    └── Plugin（插件 = Skill 集合）
         ├── NativeFunction（原生函数 = 代码 Skill）
         │   └── [KernelFunction] 特性标注
         └── SemanticFunction（语义函数 = Prompt Skill）
             └── skprompt.txt + config.json
```

### 5.2 代码实现

```csharp
// Semantic Kernel Native Plugin（代码 Skill）
public class CodeAnalysisPlugin
{
    [KernelFunction]
    [Description("在代码库中搜索匹配的代码片段")]
    [return: Description("匹配的代码搜索结果列表")]
    public async Task<List<SearchResult>> SearchCodeAsync(
        [Description("搜索关键词或正则表达式")] string query,
        [Description("文件类型过滤，如 .cs,.csproj")] string fileTypes = ".cs"
    )
    {
        return await _searchService.SearchAsync(query, fileTypes);
    }

    [KernelFunction]
    [Description("对代码变更进行系统化审查")]
    public async Task<ReviewReport> ReviewCodeAsync(
        [Description("审查的文件路径")] List<string> files,
        [Description("审查类型")] List<string> checkTypes
    )
    {
        return await _reviewService.ReviewAsync(files, checkTypes);
    }
}

// 注册 Plugin
var builder = Kernel.CreateBuilder();
builder.Plugins.AddFromType<CodeAnalysisPlugin>("code_analysis");
var kernel = builder.Build();

// Agent 自动选择 Function
var result = await kernel.InvokePromptAsync(
    "审查最近修改的代码，找出安全问题"
);
```

---

## 6. MCP (Model Context Protocol)

### 6.1 MCP 作为 Skill 传输协议

```text
MCP 不定义 Skill 的实现方式，而是定义 Skill 的暴露和调用协议：

  ┌──────────────┐         MCP 协议          ┌──────────────┐
  │   AI Client  │ ◄──────────────────────► │  MCP Server   │
  │  (Claude等)  │   tools/list              │  (Skill宿主)  │
  │              │   tools/call              │               │
  └──────────────┘   resources/read          └──────────────┘
                     prompts/get
```

### 6.2 MCP Tool = 远程 Skill

```json
// MCP Server 暴露的 tools/list 响应
{
  "tools": [
    {
      "name": "browser_navigate",
      "description": "导航到指定URL。适用：打开网页、查看API文档",
      "inputSchema": {
        "type": "object",
        "properties": {
          "url": {
            "type": "string",
            "description": "要导航到的URL"
          }
        },
        "required": ["url"]
      }
    },
    {
      "name": "browser_snapshot",
      "description": "获取当前页面的可访问性快照。适用：读取页面内容、检查UI状态",
      "inputSchema": {
        "type": "object",
        "properties": {}
      }
    }
  ]
}
```

### 6.3 Java MCP Server 实现

```java
// 使用 Spring AI MCP Server 暴露 Skill
@Configuration
public class McpServerConfig {

    @Bean
    public McpServer mcpServer() {
        return McpServer.builder()
            .tool(new BrowserNavigateTool())
            .tool(new BrowserSnapshotTool())
            .tool(new BrowserClickTool())
            .resource(new PageSnapshotResource())
            .prompt(new DebuggingPrompt())
            .build();
    }
}

// 每个 MCP Tool 就是一个远程可调用的 Skill
@Component
public class BrowserNavigateTool implements McpTool {

    @Override
    public String name() { return "browser_navigate"; }

    @Override
    public String description() {
        return "导航到指定URL。适用：打开网页、访问API文档、查看部署结果";
    }

    @Override
    public JsonObject inputSchema() {
        return JsonObject.of(
            "type", "object",
            "properties", JsonObject.of(
                "url", JsonObject.of(
                    "type", "string",
                    "description", "要导航到的URL，必须以 http:// 或 https:// 开头"
                )
            ),
            "required", List.of("url")
        );
    }

    @Override
    public McpToolResult call(Map<String, Object> arguments) {
        String url = (String) arguments.get("url");
        // 执行浏览器导航...
        return McpToolResult.success("Navigated to: " + url);
    }
}
```

---

## 7. 框架全景对比与选型

### 7.1 六维对比

| 维度 | OpenAI Func | LangChain | Spring AI | Claude Code | Semantic Kernel | MCP |
|------|:---:|:---:|:---:|:---:|:---:|:---:|
| **Skill 抽象** | Function | Tool/Agent | @Tool Bean | .md 指令文件 | Plugin/Function | Tools |
| **定义方式** | JSON Schema | 注解+代码 | 注解+Bean | Markdown+YAML | 特性+配置 | JSON Schema |
| **编排能力** | ❌ 手动 | ✅ AgentExecutor | ⚠️ 基础 | ✅ LLM自治 | ✅ Planner | ❌ 纯传输 |
| **模型支持** | OpenAI | 多模型 | 多模型 | Anthropic | OpenAI/Azure | 无关 |
| **语言生态** | 多语言 SDK | Python/JS/Java | Java/Kotlin | 自然语言 | .NET/Python | 协议层 |
| **生产就绪** | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **学习曲线** | 低 | 中高 | 低（Spring系） | 低（自然语言） | 中 | 中 |

### 7.2 选型决策树

```text
你的技术栈是什么？

├── Java / Spring 项目
│   ├── 需要简单 Tool Calling  → Spring AI @Tool
│   ├── 需要复杂 Agent 编排    → LangChain4j
│   └── 需要暴露 Skill 为服务  → MCP + Spring AI
│
├── Python 项目
│   ├── 实验/原型              → OpenAI Function Calling
│   ├── 复杂 Agent 系统        → LangChain / LangGraph
│   └── 企业级 Skill 平台      → MCP Server
│
├── .NET 项目
│   └── → Semantic Kernel
│
├── Claude Code 生态
│   └── → Claude Code Skill (.md)
│
└── 跨平台 / 跨模型
    └── → MCP 协议（通用 Skill 传输层）
```

> 🎯 **核心要点**：框架是手段不是目的。选择最契合技术栈的框架，用 MCP 做跨框架桥接，让 Skill 定义与执行解耦才是长久之计

---

**上一模块**：[04 - Agent Skills 组合与编排](./04-Agent%20Skills组合与编排.md)  
**下一模块**：[06 - Agent Skills 实战与最佳实践](./06-Agent%20Skills实战与最佳实践.md)  
**返回总览**：[00 - Agent Skills 知识体系总览](./00-Agent%20Skills知识体系总览.md)
