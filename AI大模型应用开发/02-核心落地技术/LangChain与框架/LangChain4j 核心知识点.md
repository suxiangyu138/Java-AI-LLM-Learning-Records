# LangChain4j 核心知识点

## 一、概述

LangChain4j 是 LangChain 的 Java 生态移植版，专为 Java 开发者打造的 LLM 应用开发框架。它完整复刻了 LangChain 的核心概念（Chain、Agent、Memory、Tool），同时充分利用 Java 的类型安全和面向对象特性。

**核心定位：** Java 生态中构建复杂 LLM 应用的首选框架，擅长 **智能体（Agent）** 和 **多步推理** 场景。

**官网：** https://docs.langchain4j.dev

## 二、核心概念

### 2.1 架构层次

```
┌──────────────────────────────────────────────┐
│              LangChain4j 应用层               │
│  ┌──────────┬──────────┬──────────────────┐  │
│  │ Agent    │  Chain   │  RAG Pipeline    │  │
│  └──────────┴──────────┴──────────────────┘  │
├──────────────────────────────────────────────┤
│              核心抽象层                       │
│  ┌──────────┬──────────┬──────────────────┐  │
│  │ LLM      │ Memory   │  Tool            │  │
│  │ Service  │ (对话记忆)│  (工具调用)       │  │
│  └──────────┴──────────┴──────────────────┘  │
├──────────────────────────────────────────────┤
│              模型接入层                       │
│  ┌──────────┬──────────┬──────────────────┐  │
│  │ OpenAI   │  Ollama  │  HuggingFace     │  │
│  │ DeepSeek │  通义千问 │  本地模型         │  │
│  └──────────┴──────────┴──────────────────┘  │
└──────────────────────────────────────────────┘
```

### 2.2 核心组件

| 组件 | 说明 |
|------|------|
| **ChatLanguageModel** | LLM 的统一抽象，支持 OpenAI / Ollama / DeepSeek 等 |
| **ChatMemory** | 对话记忆管理，支持自动截断、摘要压缩 |
| **Tool** | 工具定义（`@Tool` 注解），Agent 可调用 |
| **Chain** | 处理管道，串联 Prompt → LLM → 后处理 |
| **Agent** | 自主决策循环：理解目标 → 选择工具 → 执行 → 评估 |
| **Retriever** | 检索器，从向量数据库/搜索引擎检索相关文档 |
| **AiServices** | 声明式 AI 服务（接口 + 注解），自动生成实现 |

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
// 定义接口
interface Assistant {
    @SystemMessage("你是专业的Java技术顾问")
    String chat(@UserMessage String message);
}

// 自动生成实现
Assistant assistant = AiServices.create(Assistant.class, model);
String reply = assistant.chat("Spring AI 和 LangChain4j 怎么选？");
```

### 3.4 构建 Agent

```java
@Tool("查询指定城市的天气")
String getWeather(String city) {
    return weatherService.query(city);
}

@Tool("计算数学表达式")
double calculate(String expression) {
    return scriptEngine.eval(expression);
}

Agent agent = AiServices.builder(Agent.class)
    .chatLanguageModel(model)
    .tools(new WeatherTool(), new CalcTool())
    .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
    .build();

String result = agent.chat("北京周末天气如何？适合户外活动吗？");
```

## 四、Agent 详解

### 4.1 Agent 循环

```
用户输入 → LLM 分析意图
              ↓
         需要工具？──是──→ 选择工具 → 执行 → 结果反馈给 LLM
              ↓ 否                         ↓
         生成最终回复 ←─────────────────────┘
```

### 4.2 工具定义方式

| 方式 | 示例 |
|------|------|
| **@Tool 注解** | `@Tool("查询数据库") String query(String sql)` |
| **ToolSpecification** | 编程式定义工具签名 |

### 4.3 Memory 策略

| 策略 | 适用场景 |
|------|----------|
| **MessageWindowChatMemory** | 保留最近 N 轮对话，简单高效 |
| **TokenWindowChatMemory** | 按 Token 数限制上下文，更精准 |
| **ChatMemory with Summarizer** | 长对话自动摘要压缩 |

## 五、RAG 实现

```java
// 1. 文档加载
Document document = FileSystemDocumentLoader.loadDocument(
    Path.of("manual.pdf"));

// 2. 文档分割
List<TextSegment> segments = DocumentSplitter.split(
    document, 500, 50);  // 500字符，50重叠

// 3. 向量存储
EmbeddingStore<TextSegment> store = 
    PgVectorEmbeddingStore.builder()
        .host("localhost").database("rag_db")
        .dimension(1536).build();

// 4. 检索增强
RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
    .retriever(EmbeddingStoreRetriever.from(store, embeddingModel))
    .build();
```

## 六、模型支持

| 模型提供商 | Maven 依赖 | 说明 |
|------------|-----------|------|
| OpenAI / Azure | `langchain4j-open-ai` | GPT-4 / GPT-4o |
| Ollama | `langchain4j-ollama` | 本地开源模型 |
| DeepSeek | `langchain4j-open-ai` | 兼容 OpenAI 协议 |
| 通义千问 | `langchain4j-dashscope` | 阿里云大模型 |
| Google Gemini | `langchain4j-vertex-ai` | Google AI |
| HuggingFace | `langchain4j-hugging-face` | 开源模型 |

## 七、LangChain4j vs Spring AI

| 维度 | LangChain4j | Spring AI |
|------|-------------|-----------|
| 框架绑定 | **框架无关**，纯 Java | 深度绑定 Spring Boot |
| Agent 能力 | **核心优势**，多步推理、工具编排 | 基本 Agent 支持 |
| 声明式服务 | AiServices（接口注解） | ChatClient（Builder 模式） |
| 学习曲线 | 中等 | Spring 用户极低 |
| 适合场景 | 复杂 Agent、多步推理 | Spring 项目快速集成 |
| RAG 支持 | 完善 | 完善 |

## 八、总结

LangChain4j 是 Java 生态中最成熟的 LLM 编排框架，核心优势在于 **Agent 能力** 和 **框架无关性**。如果项目需要大模型进行多步推理、自主决策、操作外部工具，LangChain4j 是比 Spring AI 更合适的选择。
