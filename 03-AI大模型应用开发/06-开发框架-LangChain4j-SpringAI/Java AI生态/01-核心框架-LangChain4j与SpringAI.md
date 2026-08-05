# 核心框架对决：LangChain4j vs Spring AI

> Java AI 生态的两大支柱——LangChain4j 以灵活性和多 Agent 能力领先（采用率 68%），Spring AI 以深度 Spring 集成和企业可观测性见长（52%）。选型的关键不是"谁更好"，而是"谁更适合你的场景"

---

## 📚 目录

1. [框架概览](#1-框架概览)
2. [核心哲学对比](#2-核心哲学对比)
3. [API 风格对比](#3-api-风格对比)
4. [能力矩阵深度对比](#4-能力矩阵深度对比)
5. [选型决策框架](#5-选型决策框架)
6. [迁移与共存策略](#6-迁移与共存策略)
7. [直接 API 调用：第三种选择](#7-直接-api-调用第三种选择)

---

## 1. 框架概览

### 1.1 版本现状（2026 年 7 月）

| 维度 | LangChain4j | Spring AI |
|------|-------------|-----------|
| **当前版本** | 1.12.x（快速迭代中） | 1.1.x（维护）/ 2.0-M4（里程碑） |
| **首次正式版** | 2025 年 5 月（1.0） | 2025 年 5 月（1.0） |
| **采用率** | 68%（JetBrains 2025 Q1） | 52% |
| **社区贡献者** | 200+（Google, Red Hat, JetBrains） | Spring 官方团队主导 |
| **迭代节奏** | 快速（月度小版本） | 稳健（季度） |
| **GitHub Stars** | 12K+ | 8K+ |

### 1.2 关键风险

| 框架 | 风险 | 等级 | 应对 |
|------|------|:---:|------|
| **Spring AI 1.x** | Spring Boot 3.5 将于 **2026 年 6 月 EOL** | 🔴 高 | 规划 2.0 迁移，新项目直接用 2.0 M4+ |
| **LangChain4j** | 版本间 Breaking Change 较多 | 🟡 中 | 锁定版本，升级前阅读 Release Notes |
| **Spring AI 2.0** | 正式版发布窗口仅 1 个月（5 月→6 月 EOL） | 🟡 中 | 提前在 M4 上验证兼容性 |

---

## 2. 核心哲学对比

```text
┌─────────────────────────────────────────────────────────┐
│                   核心哲学差异                             │
│                                                         │
│  LangChain4j：乐高积木                                   │
│  ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐                         │
│  │LLM│ │RAG│ │MCP│ │A2A│ │AG │  ← 自由选择、自由组装      │
│  └───┘ └───┘ └───┘ └───┘ └───┘                         │
│  任何框架、任何模型、任何场景                               │
│                                                         │
│  Spring AI：全屋定制                                     │
│  ┌──────────────────────────────────────┐               │
│  │  Spring Boot → Spring AI → Security  │               │
│  │       → Actuator → Cloud → ...       │  ← 一体化方案  │
│  └──────────────────────────────────────┘               │
│  开箱即用、约定优于配置、Spring 生态无缝衔接                 │
└─────────────────────────────────────────────────────────┘
```

| 哲学维度 | LangChain4j | Spring AI |
|---------|-------------|-----------|
| **定位** | 框架无关的 LLM 集成工具箱 | Spring 生态的 AI 子模块 |
| **设计理念** | 模块化、可拆卸、DIY | 约定优于配置、自动装配 |
| **框架绑定** | 无（Spring / Quarkus / Micronaut / 纯 Java SE） | Spring Boot（强绑定 ApplicationContext） |
| **模型集成方式** | 统一抽象层，手动装配 | 自动配置 + Starter 依赖 |
| **典型用户** | 需要灵活性、多框架、快速试错的团队 | Spring 全家桶用户，求稳不求新 |

---

## 3. API 风格对比

### 3.1 基础对话

```java
// ========== LangChain4j：声明式接口 ==========
@AiService
interface Assistant {
    @SystemMessage("你是一个 Java 技术专家，回答简洁专业")
    String chat(@UserMessage String message);
}

// 使用
Assistant assistant = AiServices.create(Assistant.class, model);
String answer = assistant.chat("如何优化 Spring Boot 启动速度？");


// ========== Spring AI：Fluent API ==========
@RestController
class ChatController {
    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder
            .defaultSystem("你是一个 Java 技术专家，回答简洁专业")
            .build();
    }

    String chat(String message) {
        return chatClient.prompt()
            .user(message)
            .call()
            .content();
    }
}
```

### 3.2 工具调用

```java
// ========== LangChain4j：@Tool 注解 ==========
class CalculatorTools {
    @Tool("计算两个数的和")
    double add(@P("第一个数") double a, @P("第二个数") double b) {
        return a + b;
    }
}

@AiService
interface MathAssistant {
    String solve(@UserMessage String problem);
}

MathAssistant assistant = AiServices.builder(MathAssistant.class)
    .chatLanguageModel(model)
    .tools(new CalculatorTools())  // 手动注册工具
    .build();


// ========== Spring AI：自动暴露为 FunctionCallback ==========
@SpringAiTool(name = "add", description = "计算两个数的和")
public class CalculatorTools {
    public double add(double a, double b) {
        return a + b;
    }
}
// Spring AI 自动扫描 @SpringAiTool 并注册为 FunctionCallback
// ChatClient 自动可用 —— 无需手动注册
```

### 3.3 流式响应

```java
// ========== LangChain4j：回调模式 ==========
assistant.chatStreaming("介绍一下 Java 21 虚拟线程", 
    new StreamingResponseHandler<>() {
        @Override
        public void onNext(String token) {
            System.out.print(token);
        }
        @Override
        public void onComplete(Response response) {
            System.out.println("\n[完成]");
        }
    });


// ========== Spring AI：响应式 Flux ==========
Flux<String> stream = chatClient.prompt()
    .user("介绍一下 Java 21 虚拟线程")
    .stream()
    .content();

stream.subscribe(
    token -> System.out.print(token),
    error -> System.err.println("错误: " + error),
    () -> System.out.println("\n[完成]")
);
```

---

## 4. 能力矩阵深度对比

### 4.1 全面对比

| 能力维度 | LangChain4j | Spring AI | 差异评价 |
|---------|:---:|:---:|------|
| **LLM 提供商** | 20+（国产模型支持好） | 20+（国际模型为主） | LangChain4j 国产模型更丰富 |
| **向量存储** | 30+ | 15+ | LangChain4j 选择更多 |
| **MCP Client** | ✅ 支持 | ✅ 支持 | 都有 |
| **MCP Server** | ❌ 不支持 | ✅ 原生 Starters | **Spring AI 胜出** |
| **A2A 协议** | ✅ 原生支持 | ❌ 不支持 | **LangChain4j 胜出** |
| **多 Agent 编排** | ✅ 一级支持（agentic 模块） | ⚠️ 需自建 | **LangChain4j 胜出** |
| **RAG 管道** | 细粒度组装 | Advisor 模式（开箱即用） | 各有优势 |
| **可观测性** | 手动 Micrometer/OTel | Actuator + Micrometer 原生 | **Spring AI 胜出** |
| **结构化输出** | @StructuredPrompt / AiServices | 直接映射 Java Record | 都成熟 |
| **多模态** | ✅ 图片/音频/视频 | ✅ 基础多模态 | LangChain4j 更全面 |
| **GraalVM 支持** | ✅（Quarkus） | ⚠️ 部分支持 | **LangChain4j 胜出** |
| **启动速度** | <100ms（Quarkus+GraalVM） | 200-400ms（Spring Boot） | **LangChain4j 胜出** |
| **内存占用** | 50-100MB | 150-300MB | **LangChain4j 胜出** |
| **企业安全集成** | 需手动 | Spring Security 原生 | **Spring AI 胜出** |

### 4.2 RAG 实现风格对比

```text
LangChain4j RAG：乐高式组装（精细控制）
┌──────────┐  ┌───────────┐  ┌──────────┐  ┌───────────┐
│Document  │→│Splitter    │→│Embedding │→│VectorStore│
│Reader    │  │(自定义策略)│  │(选模型)  │  │(30+选择)  │
└──────────┘  └───────────┘  └──────────┘  └─────┬─────┘
                                                  │
┌──────────┐  ┌───────────┐  ┌──────────┐        │
│LLM 生成  │←│Context     │←│Retriever │←────────┘
│          │  │Augmenter   │  │(混合检索)│
└──────────┘  └───────────┘  └──────────┘

Spring AI RAG：Advisor 模式（开箱即用）
┌────────────────────────────────────────────────┐
│ ChatClient.prompt()                             │
│   .advisors(                                    │
│     new QuestionAnswerAdvisor(vectorStore),     │  ← 一行搞定 RAG
│     new SimpleLoggerAdvisor()                   │
│   )                                             │
│   .user("question")                             │
│   .call();                                      │
└────────────────────────────────────────────────┘
```

---

## 5. 选型决策框架

### 5.1 决策矩阵

```text
                         你的团队用什么框架？
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
         Spring Boot       Quarkus /        纯 Java SE
                          Micronaut         / 多框架混合
              │                │                │
              ▼                ▼                ▼
    ┌─────────────────┐  ┌─────────────┐  ┌─────────────┐
    │ 需要 MCP Server？│  │  LangChain4j│  │  LangChain4j│
    │                 │  │   (首选)     │  │   (唯一选)   │
    │ 是 → Spring AI  │  └─────────────┘  └─────────────┘
    │ 否 → 都可以      │
    └────────┬────────┘
             │
    ┌────────┴────────┐
    │ 需要多Agent/A2A？│
    │                 │
    │ 是 → LangChain4j│
    │ 否 → 都可以      │
    └────────┬────────┘
             │
    ┌────────┴────────┐
    │ 需要企业级       │
    │ 监控和审计？     │
    │                 │
    │ 是 → Spring AI  │
    │ 否 → 都可以      │
    └─────────────────┘
```

### 5.2 一句话选型

| 场景 | 推荐 | 理由 |
|------|------|------|
| Spring Boot 团队，求稳 | **Spring AI 2.0** | 全家桶集成，开箱即用 |
| 需要多 Agent 协作 | **LangChain4j** | A2A 协议 + agentic 模块 |
| 云原生/Serverless | **LangChain4j + Quarkus** | <100ms 启动，50MB 内存 |
| 需要 MCP Server | **Spring AI** | LangChain4j 不支持 MCP Server |
| 国产模型为主的团队 | **LangChain4j** | 通义千问、百度千帆原生支持 |
| 多框架混合环境 | **LangChain4j** | 框架无关 |

### 5.3 混合策略（大厂实践）

```text
┌─────────────────────────────────────────────────────────┐
│                    企业级混合架构                          │
│                                                         │
│  ┌─────────────────────┐    ┌─────────────────────┐     │
│  │  核心交易链路         │    │  边缘 AI 服务         │     │
│  │                     │    │                     │     │
│  │  Spring AI 2.0      │    │  LangChain4j        │     │
│  │  + Spring Security  │    │  + Quarkus          │     │
│  │  + Actuator         │    │  + GraalVM          │     │
│  │  + Micrometer       │    │  + 多模型切换        │     │
│  │                     │    │                     │     │
│  │  职责：稳定、可审计   │    │  职责：快速迭代、创新   │     │
│  └──────────┬──────────┘    └──────────┬──────────┘     │
│             │                          │                │
│             └──────────┬───────────────┘                │
│                        │                                │
│              ┌─────────┴─────────┐                      │
│              │   API Gateway     │  ← 统一入口           │
│              │   消息队列（Kafka）│  ← 异步解耦           │
│              └───────────────────┘                      │
└─────────────────────────────────────────────────────────┘
```

---

## 6. 迁移与共存策略

### 6.1 Spring AI 1.x → 2.0 迁移

```text
时间线：
  2026.05    Spring AI 2.0 GA 发布
  2026.06    Spring Boot 3.5 EOL ← 🔴 仅 1 个月窗口！
  2026.??    Spring Boot 4.0 GA

迁移清单：
  ├── 1. 升级 Spring Boot 3.5 → 4.0
  ├── 2. 升级 Spring AI 1.1.x → 2.0
  ├── 3. API 变更适配（ChatClient API 重大调整）
  ├── 4. 配置项迁移（2.0 配置结构变化）
  ├── 5. 向量存储连接器兼容性检查
  └── 6. 全量回归测试
```

### 6.2 直接 API → 框架迁移

> ⚠️ **严重警告**：从直接 API 调用迁移到框架比从头用框架困难得多

```text
为什么迁移成本高：
├── 没有统一的模型抽象 → 每个模型调用要逐一替换
├── 没有 RAG 管道 → 检索逻辑要重写
├── 没有会话管理 → 对话历史要重构
├── 没有 Tool 注册机制 → Function Calling 要重新对接
└── 没有可观测性 → 监控要从头搭建

结论：除非 100% 确定永远不会需要 RAG/Agent/多模型，否则一开始就用框架。
```

---

## 7. 直接 API 调用：第三种选择

### 7.1 适用场景

```text
✅ 适合直接 API 调用的场景（且仅在以下场景）：
├── 单次文本补全（无对话、无工具、无 RAG）
├── 批处理（一次性处理大量文本后不再需要）
├── 嵌入式脚本（不想引入框架依赖）
└── 学习和理解底层原理

❌ 不适合的场景：
├── 需要对话记忆
├── 需要工具调用（Function Calling）
├── 需要 RAG 检索
├── 需要多模型切换
└── 未来可能需要上述任何功能（因为你将来会后悔）
```

### 7.2 最小化示例

```java
// 直接 OpenAI API 调用（Java 11+ HttpClient）
HttpClient client = HttpClient.newHttpClient();
String apiKey = System.getenv("OPENAI_API_KEY");

String requestBody = """
    {
        "model": "gpt-4o-mini",
        "messages": [
            {"role": "system", "content": "你是一个助手"},
            {"role": "user", "content": "Hello"}
        ]
    }
    """;

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
    .header("Authorization", "Bearer " + apiKey)
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
    .build();

HttpResponse<String> response = client.send(request, 
    HttpResponse.BodyHandlers.ofString());
// 需要手动解析 JSON...然后加上对话记忆...然后加上错误处理...
// 然后你会想念 @AiService 的
```

---

> 🎯 **核心要点**：LangChain4j 和 Spring AI 不是零和博弈。**LangChain4j 胜在灵活性和 Agent 能力**（A2A、多 Agent 编排、国产模型），**Spring AI 胜在企业集成**（MCP Server、Security、Actuator）。企业级最佳实践是混合使用——核心链路用 Spring AI，创新服务用 LangChain4j。

---

**下一模块**：[02-MCP 协议 Java 实现](02-MCP协议Java实现.md) ｜ **返回总览**：[00-Java AI 生态总览](00-Java AI生态总览.md)
