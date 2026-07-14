# ⛓️ LangChain4j 核心知识点

> **核心摘要**：LangChain4j 是 LangChain 的 Java 生态移植版，专为 Java 开发者打造的 LLM 应用开发框架。它完整复刻了 LangChain 的核心概念（Chain、Agent、Memory、Tool），同时充分利用 Java 的类型安全和面向对象特性，核心优势在于 Agent 能力和框架无关性。

**前置阅读**：[[快速吃透 LangChain]] | [[Genkit Java 核心知识点]]

---

## 一、概述

**LangChain4j** 是 LangChain 的 Java 生态移植版，专为 Java 开发者打造的 LLM 应用开发框架。核心定位：Java 生态中构建复杂 LLM 应用的首选框架，擅长 Agent 和多步推理场景。

## 二、核心概念

### 2.1 架构层次

```
应用层：Agent | Chain | RAG Pipeline
核心抽象层：LLM Service | Memory | Tool
模型接入层：OpenAI | Ollama | HuggingFace | DeepSeek | 通义千问
```

### 2.2 核心组件

| 组件 | 说明 |
|------|------|
| **ChatLanguageModel** | LLM 的统一抽象 |
| **ChatMemory** | 对话记忆管理 |
| **Tool** | 工具定义（`@Tool` 注解） |
| **Chain** | 处理管道 |
| **Agent** | 自主决策循环 |
| **Retriever** | 检索器 |
| **AiServices** | 声明式 AI 服务（接口 + 注解） |

## 三、快速上手

### 3.1 依赖

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>0.35.0</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>0.35.0</version>
</dependency>
```

### 3.2 基础对话

```java
ChatLanguageModel model = OpenAiChatModel.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .modelName("gpt-4o")
    .build();

String answer = model.generate("用三句话介绍 Spring Boot");
```

### 3.3 声明式 AI 服务（AiServices）

```java
interface Assistant {
    @SystemMessage("你是专业的Java技术顾问")
    String chat(@UserMessage String message);
}

Assistant assistant = AiServices.create(Assistant.class, model);
String reply = assistant.chat("Spring AI 和 LangChain4j 怎么选？");
```

### 3.4 构建 Agent

```java
@Tool("查询指定城市的天气")
String getWeather(String city) {
    return weatherService.query(city);
}

Agent agent = AiServices.builder(Agent.class)
    .chatLanguageModel(model)
    .tools(new WeatherTool())
    .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
    .build();

String result = agent.chat("北京周末天气如何？");
```

## 四、Agent 详解

### 4.1 Agent 循环

```
用户输入 → LLM 分析意图 → 需要工具？→ 选择工具 → 执行 → 结果反馈 → 生成最终回复
```

### 4.2 工具定义方式

| 方式 | 示例 |
|------|------|
| **@Tool 注解** | `@Tool("查询数据库") String query(String sql)` |
| **ToolSpecification** | 编程式定义工具签名 |

### 4.3 Memory 策略

| 策略 | 适用场景 |
|------|----------|
| **MessageWindowChatMemory** | 保留最近 N 轮 |
| **TokenWindowChatMemory** | 按 Token 数限制 |
| **ChatMemory with Summarizer** | 长对话自动摘要 |

## 五、RAG 实现

```java
Document document = FileSystemDocumentLoader.loadDocument(Path.of("manual.pdf"));
List<TextSegment> segments = DocumentSplitter.split(document, 500, 50);

EmbeddingStore<TextSegment> store = PgVectorEmbeddingStore.builder()
    .host("localhost").database("rag_db").dimension(1536).build();

RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
    .retriever(EmbeddingStoreRetriever.from(store, embeddingModel))
    .build();
```

## 六、模型支持

| 提供商 | Maven 依赖 |
|--------|-----------|
| OpenAI / Azure | `langchain4j-open-ai` |
| Ollama | `langchain4j-ollama` |
| DeepSeek | `langchain4j-open-ai` |
| 通义千问 | `langchain4j-dashscope` |
| Google Gemini | `langchain4j-vertex-ai` |
| HuggingFace | `langchain4j-hugging-face` |

## 七、框架对比

| 维度 | LangChain4j | Spring AI |
|------|-------------|-----------|
| 框架绑定 | **框架无关** | 深度绑定 Spring Boot |
| Agent 能力 | **核心优势** | 基本支持 |
| 声明式服务 | AiServices | ChatClient |
| 学习曲线 | 中等 | 极低（Spring 用户） |
| 适合场景 | 复杂 Agent、多步推理 | Spring 项目快速集成 |

---

## 核心要点回顾

- LangChain4j 是 Java 生态中最成熟的 LLM 编排框架
- 核心优势：Agent 能力和框架无关性
- 特色功能：AiServices 声明式服务、@Tool 注解驱动 Agent
- RAG 实现：DocumentSplitter + EmbeddingStore + RetrievalAugmentor

## 参考资料

1. [[快速吃透 LangChain]]
2. [[Genkit Java 核心知识点]]
3. [[Semantic Kernel 核心知识点]]
