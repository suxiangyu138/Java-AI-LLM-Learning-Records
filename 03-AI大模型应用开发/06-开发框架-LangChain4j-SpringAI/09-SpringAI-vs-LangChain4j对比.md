# 09 - Spring AI vs LangChain4j 对比

> 🎯 两个框架都能做 Chat/RAG/Agent/Function Calling — 核心差异在于设计哲学和生态整合。本文给出 15 维度深度对比 + 代码级差异 + 迁移指南

---

## 目录

1. [15 维度深度对比](#1-15-维度深度对比)
2. [设计哲学差异](#2-设计哲学差异)
3. [代码级对比：同一功能的两种写法](#3-代码级对比同一功能的两种写法)
4. [生态与社区对比](#4-生态与社区对比)
5. [选型决策树](#5-选型决策树)
6. [框架迁移指南](#6-框架迁移指南)

---

## 1. 15 维度深度对比

| 维度 | Spring AI | LangChain4j | 优势 |
|------|-----------|-------------|:---:|
| **开发者** | Broadcom（原 VMware/Spring） | 社区驱动 | — |
| **设计哲学** | Spring Boot 原生集成 | LangChain 的 Java 移植 | — |
| **版本（2026.08）** | 2.0.0 GA（稳定） | 1.15.0-beta25（快速迭代） | Spring AI |
| **Java 最低版本** | 17（Boot 4 推荐 21） | 8+ | LangChain4j |
| **Chat API** | `ChatClient`（Builder 模式） | `ChatLanguageModel.generate()` + `AiServices` | 各有特色 |
| **声明式接口** | ❌ 无动态代理 | ✅ **AiServices**（核心特色） | LangChain4j |
| **RAG 管道** | `QuestionAnswerAdvisor` + `RetrievalAugmentationAdvisor` | `EmbeddingStoreContentRetriever` + `AiServices` | 各有特色 |
| **Function Calling** | `@Tool` + 反射扫描 | `@Tool` + `AiServices` 自动集成 | 各有特色 |
| **多轮对话记忆** | `MessageChatMemoryAdvisor` + `InMemoryChatMemory` | `ChatMemory` + `MessageWindowChatMemory` | 各有特色 |
| **流式输出** | `Flux<String>` (Reactor) + SSE | `StreamingResponseHandler` + `TokenStream` | Spring AI |
| **Spring Boot 集成** | ⭐⭐⭐⭐⭐ **原生**（自动配置/starter） | ⭐⭐⭐ 需手动 Bean 配置 | Spring AI |
| **文档加载器** | Tika/PDF/JSON/Text 等 10+ | **30+**（含 GitHub/Notion/数据库等） | LangChain4j |
| **多模型切换** | 统一的 `ChatModel` 接口 → 改配置 | 各自独立 Builder → 改代码 | Spring AI |
| **可观测性** | 内置 Micrometer + OpenTelemetry | 需手动集成 | Spring AI |
| **MCP 支持** | ✅ 官方 Java SDK 提供方 | ✅ 社区支持 | Spring AI |

---

## 2. 设计哲学差异

```text
Spring AI 的哲学：
  让 AI 成为 Spring 生态的"一等公民"
  → ChatModel 像 JdbcTemplate 一样注入
  → Advisor 像 Interceptor 一样编写
  → VectorStore 像 Spring Data Repository 一样抽象
  → @Tool 像 @Service 一样被扫描
  核心思想：如果你会 Spring Boot，就会 Spring AI

LangChain4j 的哲学：
  把 Python LangChain 的设计思想带到 Java
  → AiServices = 声明式编程（JDK 动态代理）
  → Chain/Agent = 显式组件组合
  → 尽可能兼容 Python LangChain 的 API 命名
  核心思想：如果你用过 Python LangChain，这里就是 Java 的"家"
```

---

## 3. 代码级对比：同一功能的两种写法

### 3.1 简单对话

```java
// ===== Spring AI =====
@Autowired private ChatClient chatClient;
String reply = chatClient.prompt().user("Hello").call().content();

// ===== LangChain4j =====
ChatLanguageModel model = OpenAiChatModel.builder()...build();
String reply = model.generate("Hello");
```

### 3.2 带 System Prompt 的多轮对话

```java
// ===== Spring AI =====
List<Message> history = new ArrayList<>();
history.add(new SystemMessage("你是 Java 专家"));
history.add(new UserMessage("解释 IoC"));
String reply = chatClient.call(new Prompt(history)).getResult()...;

// ===== LangChain4j（AiServices 声明式）=====
interface Expert {
    @SystemMessage("你是 Java 专家")
    String chat(@MemoryId String userId, @UserMessage String msg);
}
Expert expert = AiServices.builder(Expert.class)
    .chatLanguageModel(model)
    .chatMemoryProvider(id -> MessageWindowChatMemory.withMaxMessages(20))
    .build();
expert.chat("user1", "解释 IoC");  // AiServices 自动管理记忆
```

### 3.3 RAG 问答

```java
// ===== Spring AI =====
var advisor = QuestionAnswerAdvisor.builder(vectorStore)
    .topK(4).similarityThreshold(0.7).build();
ChatClient ragClient = ChatClient.builder(model)
    .defaultAdvisors(advisor).build();
String answer = ragClient.prompt().user("年假怎么算？").call().content();

// ===== LangChain4j =====
ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
    .embeddingStore(store).embeddingModel(embeddingModel)
    .maxResults(4).minScore(0.7).build();
Assistant rag = AiServices.builder(Assistant.class)
    .chatLanguageModel(model)
    .contentRetriever(retriever).build();
String answer = rag.answer("年假怎么算？");
```

### 3.4 Function Calling（Agent）

```java
// ===== Spring AI =====
@Tool(description = "查询天气") String getWeather(@ToolParam String city) { ... }
@Bean ChatClient agentClient(ChatModel model, WeatherTool tool) {
    return ChatClient.builder(model)
        .defaultTools(ToolCallbacks.from(tool)).build();
}
String reply = agentClient.prompt().user("北京天气？").call().content();

// ===== LangChain4j（AiServices 更简洁）=====
interface WeatherAgent {
    String ask(@UserMessage String question);
}
WeatherAgent agent = AiServices.builder(WeatherAgent.class)
    .chatLanguageModel(model)
    .tools(new WeatherTool()).build();
String reply = agent.ask("北京天气？");
```

---

## 4. 生态与社区对比

| 维度 | Spring AI | LangChain4j |
|------|-----------|-------------|
| GitHub Stars | ~10k+ | ~5k+ |
| 维护方 | Broadcom（商业公司） | 社区志愿者 |
| 发布节奏 | 跟随 Spring Boot 大版本 | 独立迭代（更频繁） |
| 文档质量 | 官方文档 + Spring 风格指南 | 官方文档 + 大量社区教程 |
| 商业支持 | ✅（通过 Broadcom） | ❌（纯社区） |
| 与 Spring 全家桶集成 | ✅ 安全/Session/Actuator | 需手动配置 |
| 向量库支持 | 10+（pgvector/Redis/Milvus/Pinecone/...） | 15+ |
| 嵌入模型 | 10+ | 10+ |

---

## 5. 选型决策树

```text
Q1: 项目是否使用 Spring Boot？
  ├── 是 → Q2
  │   ├── 需要快速原型 + 声明式接口 → LangChain4j（AiServices 最快）
  │   ├── 重视稳定 + 企业支持 + 长期维护 → Spring AI
  │   └── 已有 Spring 全家桶（Security/Session/Actuator）→ Spring AI（零摩擦）
  │
  └── 否（非 Spring 项目 / Quarkus / 纯 Java）→ LangChain4j

Q2: 团队背景？
  ├── 熟悉 Python LangChain → LangChain4j（API 风格接近）
  ├── 熟悉 Spring 生态 → Spring AI（零学习成本）
  └── 纯 Java 新手（没有框架偏好）→ Spring AI（Spring 社区更大）

Q3: 功能侧重？
  ├── 复杂 Agent 编排 + Chain 组合 → LangChain4j（Chain/Agent 更成熟）
  ├── 需要 MCP 协议支持 → Spring AI（官方 Java SDK 提供方）
  ├── 多供应商 AI 网关 → Spring AI（统一 ChatModel 抽象）
  └── 流式 SSE + Reactor 生态 → Spring AI（Flux 原生流式）
```

| 场景 | 推荐 | 原因 |
|------|:---:|------|
| Spring Boot + AI Chat | Spring AI | 自动配置 + 一行注入 |
| Spring Boot + RAG | Spring AI | Advisor 链原生集成 |
| 非 Spring 项目 + AI | LangChain4j | 无框架依赖、Java 8+ |
| 复杂 Agent 编排 | LangChain4j | Chain/Agent/State 更成熟 |
| 多供应商 AI 网关 | Spring AI | 统一 Model 抽象 |
| 快速原型/声明式开发 | **LangChain4j** | AiServices 是独有特色 |
| 企业级稳定+长期支持 | **Spring AI** | Broadcom 商业维护 |
| 大量文档加载需求 | LangChain4j | 30+ 加载器 vs 10+ |
| 已有 Python LangChain 团队 | LangChain4j | 概念迁移成本低 |
| 可观测性/监控有要求 | Spring AI | 内置 Micrometer+OTel |

---

## 6. 框架迁移指南

### 6.1 LangChain4j → Spring AI

| LangChain4j 概念 | Spring AI 等价物 |
|-------------------|------------------|
| `ChatLanguageModel.generate()` | `ChatClient.prompt().call().content()` |
| `AiServices.create()` | 手动 `ChatClient` + 显式工具注册 |
| `ChatMemory` + `@MemoryId` | `MessageChatMemoryAdvisor` + `conversationId` |
| `ContentRetriever` | `QuestionAnswerAdvisor(vectorStore)` |
| `@Tool` on class methods | `@Tool` on `@Service` methods |
| `StreamingResponseHandler` | `Flux<String>` via `.stream().content()` |
| `EmbeddingStore<TextSegment>` | `VectorStore` + `Document` |

### 6.2 Spring AI → LangChain4j

| Spring AI 概念 | LangChain4j 等价物 |
|----------------|-------------------|
| `ChatClient` | `ChatLanguageModel` + `AiServices` |
| `Advisor` 链 | `AiServices` 的 `chatMemory/tools/contentRetriever` 自动注入 |
| `VectorStore` | `EmbeddingStore<TextSegment>` |
| `Document` + `DocumentReader` | `Document` + `DocumentLoader` |
| `@Tool` | `@Tool`（语法几乎相同） |
| `QuestionAnswerAdvisor` | `EmbeddingStoreContentRetriever` |

---

> 🎯 **核心要点**：没有绝对的好坏 — **Spring Boot 团队 → Spring AI（零摩擦原生集成 + 商业支持）**；**复杂 Agent/Chain 编排 → LangChain4j（声明式 AiServices + 更丰富的加载器/编排能力）**。2026 年的趋势是两者互补共存：Spring AI 做基础设施层（模型网关/可观测性/MCP），LangChain4j 做 Agent 编排层。项目里同时引入两个框架也是合理选择。

**下一模块**：[10-其他框架速览](10-其他框架速览.md) / **返回总览**：[00-总览](00-Java-AI开发框架体系总览.md)
