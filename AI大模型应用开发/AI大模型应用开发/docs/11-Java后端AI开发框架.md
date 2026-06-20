# 11 — Java 后端 AI 开发框架

> **目标**：对比 Java 生态三大 AI 框架（Spring AI / LangChain4j / Semantic Kernel），掌握选型和项目搭建。

---

## 1. 三大框架全景对比

### 1.1 框架概览

| 维度 | Spring AI | LangChain4j | Semantic Kernel (Java) |
|------|-----------|-------------|------------------------|
| **开发者** | VMware/Spring | 社区驱动 | 微软 |
| **定位** | Spring 原生 AI 框架 | Java 版 LangChain | 微软 AI 编排框架 Java 版 |
| **最新版本** | 1.0.0-M6 (2025) | 0.36.2 (2025) | 1.x (preview) |
| **Spring 集成** | ⭐⭐⭐⭐⭐ 原生 | ⭐⭐⭐ Boot Starter | ⭐⭐ |
| **多模型支持** | 15+ | 20+ | 3 (OpenAI/Azure/Gemini) |
| **RAG 支持** | ⭐⭐⭐⭐ ETL 流水线 | ⭐⭐⭐⭐⭐ 最完整 | ⭐⭐⭐ |
| **Agent 支持** | ⭐⭐⭐ @Tool注解 | ⭐⭐⭐⭐⭐ AI Services | ⭐⭐⭐⭐ Planner |
| **MCP 支持** | ⭐⭐⭐⭐⭐ 原生 Server/Client | ⭐⭐⭐ | ❌ |
| **中文文档** | ⭐⭐⭐ 中等 | ⭐⭐⭐⭐ 较好 | ⭐⭐ 较少 |
| **社区活跃度** | ⭐⭐⭐⭐ 快速增长 | ⭐⭐⭐⭐ 活跃 | ⭐⭐ |
| **学习曲线** | 低 (Spring 开发者) | 中 | 中 |

### 1.2 一句话选型

```
Spring Boot 项目 → Spring AI (原生集成，零学习成本)
复杂 RAG / Agent → LangChain4j (功能最全，社区成熟)
微软技术栈      → Semantic Kernel (Azure/Teams 集成)
简单 HTTP 调用  → 直接 OkHttp/WebClient + JSON (最轻量)
```

---

## 2. Spring AI 深入

### 2.1 架构图

```
┌────────────────────────────────────────────────────┐
│                  Spring AI                           │
│                                                      │
│  ┌───────────┐ ┌──────────┐ ┌──────────┐ ┌───────┐ │
│  │ ChatClient│ │Embedding │ │ Image    │ │ Audio │ │
│  │           │ │ Model    │ │ Model    │ │ Model │ │
│  └─────┬─────┘ └────┬─────┘ └────┬─────┘ └───┬───┘ │
│        │             │            │            │      │
│  ┌─────┴─────────────┴────────────┴────────────┴──┐ │
│  │             Model Adapter Layer                 │ │
│  │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐  │ │
│  │  │ OpenAI │ │Azure OA│ │Ollama  │ │Gemini  │  │ │
│  │  └────────┘ └────────┘ └────────┘ └────────┘  │ │
│  │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐  │ │
│  │  │Anthrop│ │QianFan │ │ZhiPu   │ │DeepSeek│  │ │
│  │  └────────┘ └────────┘ └────────┘ └────────┘  │ │
│  └────────────────────────────────────────────────┘ │
│                                                      │
│  ┌────────────────────────────────────────────────┐  │
│  │            Vector Store Adapter                 │  │
│  │  PGVector│Milvus│Redis│Qdrant│Weaviate│Chroma  │  │
│  └────────────────────────────────────────────────┘  │
│                                                      │
│  ┌────────────────────────────────────────────────┐  │
│  │          ETL Pipeline (Document Processing)      │  │
│  │  Reader → Transformer → Writer                 │  │
│  └────────────────────────────────────────────────┘  │
│                                                      │
│  ┌────────────────────────────────────────────────┐  │
│  │          MCP Support (Server + Client)          │  │
│  └────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────┘
```

### 2.2 项目依赖配置

```xml
<!-- pom.xml -->
<dependencyManagement>
    <dependencies>
        <!-- Spring AI BOM -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.0.0-M6</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- 核心 -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter</artifactId>
    </dependency>

    <!-- OpenAI (包括 DeepSeek 等兼容接口) -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    </dependency>

    <!-- Ollama (本地模型) -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
    </dependency>

    <!-- Anthropic Claude -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-anthropic-spring-boot-starter</artifactId>
    </dependency>

    <!-- PGVector -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>
    </dependency>

    <!-- Redis Stack Vector -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-redis-store-spring-boot-starter</artifactId>
    </dependency>

    <!-- MCP Server -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-mcp-server-webflux</artifactId>
    </dependency>

    <!-- MCP Client -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-mcp-client</artifactId>
    </dependency>
</dependencies>
```

---

## 3. LangChain4j 深入

### 3.1 核心模块

```
LangChain4j 模块架构：

┌────────────────────────────────────────────┐
│              langchain4j-core               │
│          (核心接口：ChatModel 等)           │
├────────────────────────────────────────────┤
│                                             │
│  ┌───────────────┐  ┌──────────────────┐   │
│  │langchain4j    │  │langchain4j-      │   │
│  │  (基本集成)    │  │embedding         │   │
│  └───────────────┘  └──────────────────┘   │
│  ┌───────────────┐  ┌──────────────────┐   │
│  │langchain4j-   │  │langchain4j-      │   │
│  │open-ai        │  │elasticsearch     │   │
│  └───────────────┘  └──────────────────┘   │
│  ┌───────────────┐  ┌──────────────────┐   │
│  │langchain4j-   │  │langchain4j-      │   │
│  │ollama         │  │pgvector          │   │
│  └───────────────┘  └──────────────────┘   │
│  ... (30+ 集成模块)                         │
└────────────────────────────────────────────┘
```

### 3.2 AI Services（核心特性）

```java
// LangChain4j 的 AI Services：声明式 AI 接口

// 1. 定义接口
interface Assistant {
    String chat(String message);
}

interface SentimentAnalyzer {
    @SystemMessage("你是一个情感分析专家，返回 POSITIVE/NEGATIVE/NEUTRAL")
    String analyze(String text);
}

interface CodeReviewer {
    @SystemMessage("你是资深 Java 代码审查者")
    @UserMessage("审查以下 {{language}} 代码：\n{{code}}")
    ReviewResult review(@V("language") String language,
                        @V("code") String code);
}

// 2. 创建代理实例
Assistant assistant = AiServices.create(Assistant.class, chatModel);
SentimentAnalyzer analyzer = AiServices.create(SentimentAnalyzer.class, chatModel);

// 3. 调用
String reply = assistant.chat("Hello!");
String sentiment = analyzer.analyze("这个产品太棒了！");

// 4. 带 Tool 的 Agent
interface SupportAgent {
    @SystemMessage("""
        你是一个技术支持助手。
        可用的工具：查询知识库、查询工单状态。
        """)
    String support(String userMessage);
}

SupportAgent agent = AiServices.builder(SupportAgent.class)
    .chatLanguageModel(chatModel)
    .tools(new KnowledgeBaseTool(), new TicketTool())
    .build();
```

### 3.3 Maven 依赖

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>0.36.2</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>0.36.2</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-ollama</artifactId>
    <version>0.36.2</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-pgvector</artifactId>
    <version>0.36.2</version>
</dependency>
```

---

## 4. Semantic Kernel (Java)

### 4.1 核心概念

```java
// Semantic Kernel 核心：Kernel + Plugins

// 1. 创建 Kernel
Kernel kernel = Kernel.builder()
    .withAIService(ChatCompletionService.builder()
        .withModelId("gpt-4o")
        .withOpenAIKey(System.getenv("OPENAI_API_KEY"))
        .build())
    .build();

// 2. 定义 Plugin
public class TimePlugin {
    @KernelFunction(description = "获取当前时间")
    public String getCurrentTime() {
        return LocalDateTime.now().toString();
    }

    @KernelFunction(description = "获取指定时区的当前时间")
    public String getTimeInZone(
        @KernelFunctionParameter(description = "时区，如 Asia/Shanghai")
        String timezone) {
        return ZonedDateTime.now(ZoneId.of(timezone)).toString();
    }
}

// 3. 注册 Plugin
kernel.importPluginFromObject(new TimePlugin(), "time");

// 4. Planner：自动编排
var planner = new SequentialPlanner(kernel);
var plan = planner.createPlanAsync("查一下北京时间，然后告诉我是否是工作时间");
var result = plan.invokeAsync();
```

---

## 5. 框架选型决策

### 5.1 决策矩阵

```
                    Spring AI    LangChain4j   直接用 HTTP
Spring 项目         ✅✅✅       ✅✅           ✅
非 Spring 项目      ❌           ✅✅✅         ✅✅✅
简单 API 调用       ✅✅✅       ✅✅✅         ✅ (最轻)
复杂 RAG            ✅✅✅       ✅✅✅✅       ❌
复杂 Agent          ✅✅         ✅✅✅✅       ❌
MCP 集成            ✅✅✅✅     ✅✅           ❌
最小依赖            ❌           ✅✅           ✅✅✅
中文社区支持        ✅✅         ✅✅✅         ✅✅✅
文档与培训成本      ✅✅✅       ✅✅           ✅✅✅
```

### 5.2 推荐组合

```
方案 A：纯 Spring 技术栈（推荐）
  Spring Boot + Spring AI + PGVector/Redis
  适合：大部分 Spring 团队

方案 B：最佳 RAG/Agent
  Spring Boot + LangChain4j + Milvus
  适合：RAG/Agent 为核心的项目

方案 C：最小依赖
  Spring Boot + OkHttp/WebClient + 自封装
  适合：只需简单调用 API 的项目

方案 D：微软生态
  .NET/Java + Semantic Kernel + Azure
  适合：深度使用微软产品
```

---

## 6. 项目搭建模板

### 6.1 Spring AI 完整项目结构

```
ai-platform/
├── pom.xml
├── application.yml
├── src/main/java/com/example/ai/
│   ├── AiApplication.java
│   ├── config/
│   │   ├── ChatClientConfig.java        # ChatClient Bean
│   │   ├── VectorStoreConfig.java       # 向量库配置
│   │   ├── RetryConfig.java             # 重试熔断
│   │   └── McpConfig.java               # MCP Client 配置
│   ├── controller/
│   │   ├── ChatController.java          # 对话接口
│   │   └── RagController.java           # RAG 接口
│   ├── service/
│   │   ├── ChatService.java
│   │   ├── RagService.java
│   │   ├── EmbeddingService.java
│   │   └── AgentService.java
│   ├── tools/
│   │   ├── WeatherTool.java
│   │   ├── SearchTool.java
│   │   └── DatabaseTool.java
│   ├── rag/
│   │   ├── DocumentLoader.java
│   │   ├── TextSplitter.java
│   │   └── QueryRewriter.java
│   ├── model/
│   │   ├── ChatRequest.java
│   │   ├── ChatResponse.java
│   │   └── RagDocument.java
│   ├── repository/
│   │   └── DocumentRepository.java
│   ├── gateway/
│   │   └── ModelGateway.java            # 模型路由网关
│   └── util/
│       ├── PromptTemplates.java
│       └── TokenEstimator.java
└── src/main/resources/
    ├── application.yml
    ├── application-dev.yml
    ├── application-prod.yml
    └── prompts/
        ├── code-review.st
        ├── doc-writer.st
        └── support-agent.st
```

### 6.2 核心配置

```yaml
# application.yml
spring:
  ai:
    # 主模型
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o
          temperature: 0.7
      embedding:
        options:
          model: text-embedding-3-small
          dimensions: 512

    # 本地模型（开发用）
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: qwen2.5:7b
          temperature: 0.7

    # 向量数据库
    vectorstore:
      pgvector:
        host: ${PG_HOST:localhost}
        database: ai_platform
        dimensions: 512
        index-type: HNSW

  # 数据库
  datasource:
    url: jdbc:postgresql://localhost:5432/ai_platform
    username: postgres
    password: ${DB_PASSWORD}

# 应用配置
app:
  ai:
    retry:
      max-attempts: 3
      backoff: 1000
    rate-limit:
      tokens-per-minute: 100000
    cache:
      enabled: true
      ttl: 3600
```

---

## 7. 快速复习

```
□ Java 三大框架：Spring AI (Spring原生) / LangChain4j (功能最全) / Semantic Kernel (微软)
□ Spring AI 核心：ChatClient + EmbeddingModel + VectorStore + Image/Audio
□ LangChain4j AI Services：声明式接口，自动代理
□ Spring Boot 项目首选 Spring AI
□ 复杂 RAG/Agent 可考虑 LangChain4j
□ 非 Spring 项目推荐 LangChain4j 或直接 HTTP
□ MCP 支持 Spring AI 最完整
□ 项目结构模板：config / controller / service / tools / rag / model
```

---

> **下一步**：[12 — Spring AI 实战详解](./12-Spring-AI实战详解.md)
