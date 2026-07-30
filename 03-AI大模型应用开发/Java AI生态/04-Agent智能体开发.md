# Agent 智能体开发（Java）

> Java Agent 开发已从概念验证进入生产就绪——LangChain4j 的 AiServices + A2A 协议、LangGraph4j 的有状态图编排、Spring AI 的 FunctionCallback，三大路径覆盖从简单 Agent 到复杂多 Agent 系统的完整谱系

---

## 📚 目录

1. [Agent 开发路径总览](#1-agent-开发路径总览)
2. [LangChain4j AiServices：声明式 Agent](#2-langchain4j-aiservices声明式-agent)
3. [Spring AI FunctionCallback：工具驱动 Agent](#3-spring-ai-functioncallback工具驱动-agent)
4. [LangGraph4j：有状态图编排](#4-langgraph4j有状态图编排)
5. [A2A 协议：Agent 间通信](#5-a2a-协议agent-间通信)
6. [多 Agent 编排模式](#6-多-agent-编排模式)
7. [AgentScope Java：阿里企业级方案](#7-agentscope-java阿里企业级方案)
8. [Agent 可观测性](#8-agent-可观测性)

---

## 1. Agent 开发路径总览

```text
Java Agent 三大开发路径（2026）

┌──────────────────────────────────────────────────────────┐
│                                                          │
│  简单 Agent                    复杂多 Agent 系统           │
│  ─────────                    ─────────────────           │
│                                                          │
│  LangChain4j AiServices    →  LangGraph4j 图编排          │
│  @AiService + @Tool           状态机 + 循环图 + 检查点     │
│  声明式、快速上手             复杂逻辑、可控流程            │
│                                                          │
│  Spring AI FunctionCallback → A2A 跨 Agent 通信           │
│  @Tool + ChatClient           Agent 间协作与互委派         │
│  Spring 原生、自动装配         跨语言、跨平台               │
│                                                          │
│  共同基础：                                                │
│  ├── MCP 协议（工具调用标准化）                              │
│  ├── A2A 协议（Agent 间通信标准化）                          │
│  ├── AgentListener/AgentMonitor（可观测性）                  │
│  └── Function Calling（模型驱动工具调用）                    │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

## 2. LangChain4j AiServices：声明式 Agent

### 2.1 基础 Agent

```java
// Step 1: 定义工具
class CodeTools {

    @Tool("在项目中搜索代码，返回匹配文件和行号")
    List<SearchResult> searchCode(
            @P("搜索关键词或正则表达式") String pattern) {
        return codeSearchService.search(pattern);
    }

    @Tool("读取指定文件的代码内容")
    String readFile(
            @P("文件路径") String filePath,
            @P("起始行号，从 1 开始") int startLine,
            @P("读取行数") int lineCount) {
        return fileService.read(filePath, startLine, lineCount);
    }
}

// Step 2: 定义 Agent 接口
@AiService
interface CodeAssistantAgent {

    @SystemMessage("""
        你是一个代码助手，可以帮助用户：
        1. 理解代码逻辑
        2. 查找 Bug
        3. 建议优化方案
        
        规则：
        - 先搜索相关代码，再分析
        - 每个问题给出具体代码示例
        - 不确定时明确告知
        """)
    String assist(@UserMessage String task);
}

// Step 3: 组装并运行
ChatLanguageModel model = OpenAiChatModel.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .modelName("gpt-4o")
    .build();

CodeAssistantAgent agent = AiServices.builder(CodeAssistantAgent.class)
    .chatLanguageModel(model)
    .tools(new CodeTools())
    .build();

String response = agent.assist("帮我分析 UserService.java 的登录逻辑");
```

### 2.2 带记忆的 Agent

```java
// ChatMemory：为 Agent 添加对话记忆
@AiService
interface CustomerServiceAgent {

    @SystemMessage("""
        你是客服助手。可以查询订单、处理退款、解答常见问题。
        始终保持礼貌专业。
        
        用户信息：{{userInfo}}
        """)
    String handle(@UserMessage String message,
                  @V("userInfo") String userInfo);
}

// 使用
ChatMemory memory = MessageWindowChatMemory.withMaxMessages(20);

CustomerServiceAgent agent = AiServices.builder(CustomerServiceAgent.class)
    .chatLanguageModel(model)
    .chatMemory(memory)           // ← 对话记忆
    .tools(new OrderTools())
    .build();
```

### 2.3 Agent 监听器（可观测性）

```java
// LangChain4j 1.10+ 的 AgentListener
AgentListener listener = new AgentListener() {

    @Override
    public void onToolCallStarted(ToolCall toolCall) {
        log.info("🔧 工具调用开始: {} 参数: {}",
            toolCall.name(), toolCall.arguments());
    }

    @Override
    public void onToolCallCompleted(ToolCall toolCall, String result) {
        log.info("✅ 工具调用完成: {} 耗时: {}ms",
            toolCall.name(), toolCall.elapsed());
    }

    @Override
    public void onError(Throwable error) {
        log.error("❌ Agent 错误: {}", error.getMessage());
        // 触发告警
    }
};

Agent agent = AiServices.builder(Agent.class)
    .chatLanguageModel(model)
    .tools(new MyTools())
    .listener(listener)            // ← 监听器
    .build();
```

---

## 3. Spring AI FunctionCallback：工具驱动 Agent

### 3.1 自动工具注册

```java
// Spring AI 中，@Service + @Tool 自动暴露为 Agent 工具
@Service
public class WeatherTools {

    @Tool(name = "get_weather",
          description = "获取指定城市的实时天气信息")
    public WeatherInfo getWeather(
            @ToolParam(description = "城市名称") String city) {
        return weatherApi.query(city);
    }

    @Tool(name = "compare_weather",
          description = "比较两个城市的天气")
    public String compareWeather(
            @ToolParam(description = "城市A") String cityA,
            @ToolParam(description = "城市B") String cityB) {
        WeatherInfo a = weatherApi.query(cityA);
        WeatherInfo b = weatherApi.query(cityB);
        return String.format("%s: %s vs %s: %s",
            cityA, a.summary(), cityB, b.summary());
    }
}

// ChatClient 自动发现所有 @Tool 并注册
@RestController
class AgentController {

    private final ChatClient chatClient;

    public AgentController(ChatClient.Builder builder) {
        this.chatClient = builder
            .defaultSystem("你是天气助手，可以查询和比较天气")
            .build();
        // WeatherTools 被自动发现并注册为 FunctionCallback
    }

    @GetMapping("/ask")
    public String ask(@RequestParam String q) {
        return chatClient.prompt().user(q).call().content();
    }
}
```

### 3.2 多工具编排

```java
// Spring AI 支持多个工具组合
@Configuration
public class AgentConfig {

    @Bean
    public ChatClient agentClient(
            ChatClient.Builder builder,
            WeatherTools weatherTools,        // 自动注入
            OrderTools orderTools,            // 自动注入
            KnowledgeBaseTools kbTools) {      // 自动注入

        return builder
            .defaultSystem("你是全能助手，可以处理天气、订单和知识查询")
            .defaultFunctions(
                "get_weather",       // 指定可用工具名称
                "compare_weather",
                "query_order",
                "search_knowledge"
            )
            .build();
    }
}
```

---

## 4. LangGraph4j：有状态图编排

### 4.1 核心概念

```text
LangGraph4j（v1.8.20，2026.06）
├── 定位：Java 有状态多 Agent 图编排框架
├── 兼容：LangChain4j 和 Spring AI
│
├── 核心能力：
│   ├── 有向图（DAG）→ 流程编排
│   ├── 循环图 → 迭代式 Agent（如 Self-Refine）
│   ├── 条件边 → 动态路由
│   ├── 检查点（Checkpoint）→ 暂停/恢复/回滚
│   ├── 人机交互（Human-in-the-Loop）
│   ├── 并行执行 → 多分支同时进行
│   └── 可视化调试器 → 图结构可视化
│
└── 典型应用：
    ├── Self-Refine Agent（生成 → 评估 → 改进 → 循环）
    ├── Multi-Agent 协作（各 Agent 作为图节点）
    └── 复杂工作流（审批、分支、合并）
```

### 4.2 Self-Refine Agent 示例

```java
// LangGraph4j：自我改进 Agent
public class SelfRefineAgent {

    public static void main(String[] args) {
        // 创建图
        StateGraph<AgentState> graph = new StateGraph<>(AgentState.class);

        // 节点定义
        graph.addNode("generate", state -> {
            // 生成初稿
            String draft = llm.generate(
                "根据以下要求写代码：%s".formatted(state.getTask())
            );
            state.setOutput(draft);
            return state;
        });

        graph.addNode("evaluate", state -> {
            // 评估质量（代码审查 Agent）
            String review = reviewAgent.review(state.getOutput());
            state.setReview(review);
            return state;
        });

        graph.addNode("refine", state -> {
            // 根据审查意见改进
            String refined = llm.generate(
                "改进以下代码：\n%s\n\n审查意见：%s"
                    .formatted(state.getOutput(), state.getReview())
            );
            state.setOutput(refined);
            state.incrementIteration();
            return state;
        });

        // 边定义
        graph.setEntryPoint("generate");
        graph.addEdge("generate", "evaluate");

        // 条件边：如果审查不通过且未超过最大迭代，继续改进
        graph.addConditionalEdges("evaluate", state -> {
            if (state.isApproved()) {
                return "done";   // 通过 → 结束
            } else if (state.getIteration() >= 3) {
                return "done";   // 超过 3 轮 → 强制结束
            } else {
                return "refine"; // 继续改进
            }
        });

        graph.addEdge("refine", "evaluate");  // 改进后重新评估（循环）

        // 编译并运行
        CompiledGraph<AgentState> compiled = graph.compile();
        AgentState result = compiled.invoke(
            new AgentState("实现一个线程安全的 LRU 缓存")
        );

        System.out.println("最终输出：\n" + result.getOutput());
        System.out.println("迭代次数：" + result.getIteration());
    }
}
```

### 4.3 多 Agent 图编排

```java
// LangGraph4j 多 Agent 协作图
StateGraph<MultiAgentState> graph = new StateGraph<>(MultiAgentState.class);

// 各 Agent 作为图节点
graph.addNode("planner", new PlannerAgent());
graph.addNode("coder", new CoderAgent());
graph.addNode("reviewer", new ReviewerAgent());
graph.addNode("tester", new TesterAgent());
graph.addNode("merge", new MergeAgent());

// 流程编排
graph.setEntryPoint("planner");
graph.addEdge("planner", "coder");
graph.addEdge("coder", "reviewer");

// 条件分支：审查不通过 → 回到 coder 修改
graph.addConditionalEdges("reviewer", state ->
    state.isReviewPassed() ? "tester" : "coder");

// 条件分支：测试不通过 → 回到 coder 修复
graph.addConditionalEdges("tester", state ->
    state.isTestPassed() ? "merge" : "coder");

graph.addEdge("merge", "__end__");

CompiledGraph<MultiAgentState> pipeline = graph.compile();
MultiAgentState result = pipeline.invoke(
    new MultiAgentState("实现用户反馈收集模块"));
```

---

## 5. A2A 协议：Agent 间通信

### 5.1 A2A vs MCP

| 维度 | MCP | A2A |
|------|-----|-----|
| **发起方** | Google（Google DeepMind） | Google |
| **定位** | Agent ↔ 工具 | Agent ↔ Agent |
| **Java 支持** | Spring AI ✅ / LangChain4j Client ✅ | LangChain4j ✅ / Spring AI ❌ |
| **通信模式** | JSON-RPC | 任务导向 |
| **典型场景** | 查询数据库、调用 API | Agent A 委托任务给 Agent B |

### 5.2 LangChain4j A2A 示例

```java
// Agent A：项目经理 Agent —— 委托任务给开发 Agent
@AiService
interface ProjectManagerAgent {

    @SystemMessage("""
        你是项目经理 Agent。分析用户需求，将实现任务委托给开发 Agent。
        如果开发 Agent 返回了需要澄清的问题，整理后回复用户。
        """)
    String manage(@UserMessage String requirement);
}

// Agent B：开发 Agent —— 接收来自 PM Agent 的任务
@AiService
interface DeveloperAgent {

    @SystemMessage("""
        你是开发 Agent。接收编码任务，完成实现。
        
        规则：
        - 严格按照需求实现
        - 不确定的地方标注为 TODO 并说明
        - 完成后说明实现了什么、没有实现什么
        """)
    String implement(@UserMessage String task);
}

// A2A 通信设置
// LangChain4j 的 A2A 协议让 PM Agent 可以直接调用 Developer Agent
// 就像调用一个工具一样
class DevAgentTool {
    private final DeveloperAgent devAgent;

    @Tool("将编码任务委托给开发 Agent")
    String delegateTask(@P("编码任务描述") String task) {
        return devAgent.implement(task);
    }
}

ProjectManagerAgent pmAgent = AiServices.builder(ProjectManagerAgent.class)
    .chatLanguageModel(model)
    .tools(new DevAgentTool(devAgent))  // ← A2A：Agent 作为工具
    .build();

// 使用
String result = pmAgent.manage("开发一个用户积分系统");
```

---

## 6. 多 Agent 编排模式

### 6.1 常用模式对比

| 模式 | 结构 | 适用场景 | 实现方式 |
|------|------|---------|---------|
| **顺序链** | A → B → C | 阶段分明的任务 | LangGraph4j 顺序边 |
| **并行+汇总** | A1/A2/A3 → Merge | 多维度独立研究 | CompletableFuture |
| **辩论** | A1/A2/A3 → Judge | 方案选型 | LangGraph4j |
| **层级委派** | Manager → Worker₁, Worker₂ | 复杂项目 | A2A 协议 |
| **Self-Refine** | Generate → Evaluate → Refine ↩ | 质量要求高 | LangGraph4j 循环图 |

### 6.2 模式选择指南

```text
选择哪种多 Agent 模式？

1. 任务有明确的顺序依赖？
   → 顺序链（LangGraph4j DAG）

2. 需要从多个角度独立分析？
   → 并行+汇总（CompletableFuture + Merge Agent）

3. 需要在多个方案中做决策？
   → 辩论模式（多方案 Agent + 仲裁 Agent）

4. 质量要求高，需要反复改进？
   → Self-Refine（LangGraph4j 循环图 + 检查点）

5. 大型项目，需要分工协作？
   → 层级委派（A2A 协议 + Manager-Worker）
```

---

## 7. AgentScope Java：阿里企业级方案

### 7.1 特点

| 特性 | AgentScope Java | LangChain4j |
|------|:---:|:---:|
| **架构** | 响应式（Project Reactor） | 阻塞/回调 |
| **Hook 系统** | 前置/后置/环绕 Hook | AgentListener |
| **GraalVM** | 原生支持，<200ms 冷启动 | 通过 Quarkus |
| **A2A** | 原生支持 | 原生支持 |
| **可观测性** | OpenTelemetry 原生集成 | 手动集成 |
| **安全审计** | Hook 系统拦截审查 | 需自建 |

### 7.2 基础用法

```java
// AgentScope Java 示例
@Agent
public class AuditAgent {

    @Tool(name = "audit_log",
          description = "审计操作日志")
    @PreHook(AuthHook.class)      // 前置 Hook：权限检查
    @PostHook(LoggingHook.class)  // 后置 Hook：日志记录
    public Mono<AuditResult> auditLog(
            @ToolParam String operation,
            @ToolParam String operator) {

        return Mono.fromCallable(() -> {
            // 审计逻辑
            return new AuditResult(operation, operator, "PASSED");
        });
    }
}
```

---

## 8. Agent 可观测性

### 8.1 监控指标

```java
// 自定义 Micrometer 指标
@Component
public class AgentMetrics {

    private final MeterRegistry registry;

    // Agent 调用计数
    public void recordAgentCall(String agentName, String status) {
        Counter.builder("agent.calls")
            .tag("agent", agentName)
            .tag("status", status)
            .register(registry)
            .increment();
    }

    // 工具调用耗时
    public void recordToolDuration(String toolName, long millis) {
        Timer.builder("agent.tool.duration")
            .tag("tool", toolName)
            .register(registry)
            .record(millis, TimeUnit.MILLISECONDS);
    }

    // Token 消耗
    public void recordTokenUsage(String agentName,
                                  int inputTokens, int outputTokens) {
        Counter.builder("agent.tokens.input")
            .tag("agent", agentName)
            .register(registry)
            .increment(inputTokens);

        Counter.builder("agent.tokens.output")
            .tag("agent", agentName)
            .register(registry)
            .increment(outputTokens);
    }
}
```

### 8.2 关键告警规则

```yaml
alerts:
  - name: "Agent 调用失败率过高"
    condition: "失败率 > 5%"
    action: "检查模型可用性"

  - name: "Agent 循环检测"
    condition: "同一 Agent 同一操作连续 > 5 次"
    action: "人工介入确认"

  - name: "Token 消耗异常"
    condition: "单次调用 > 10K tokens"
    action: "检查是否需要优化 Prompt 或分片策略"

  - name: "工具调用超时"
    condition: "tool.duration > 30s"
    action: "检查目标服务状态"
```

---

> 🎯 **核心要点**：Java Agent 开发路线清晰——简单场景用 AiServices/FallbackCallback，复杂工作流用 LangGraph4j 图编排，跨 Agent 通信走 A2A 协议。2026 年的关键能力是**可观测性**（AgentListener/Micrometer）和**状态管理**（LangGraph4j Checkpoint）——这两项决定 Agent 系统能否生产落地。

---

**上一模块**：[03-RAG 检索增强生成](03-RAG检索增强生成.md) ｜ **下一模块**：[05-向量数据库与嵌入](05-向量数据库与嵌入.md) ｜ **返回总览**：[00-Java AI 生态总览](00-Java AI生态总览.md)
