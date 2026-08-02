# 07 - LangChain4j 深入

> 定位：@AiService 声明式编程、RAG 模块、工具调用、记忆、与 Spring AI 对比——LangChain4j 完整实践

## 📚 目录

1. [LangChain4j 定位](#1-langchain4j-定位)
2. [@AiService 声明式接口](#2-aiservice-声明式接口)
3. [RAG 模块](#3-rag-模块)
4. [工具与记忆](#4-工具与记忆)
5. [与 Spring AI 对比](#5-与-spring-ai-对比)

---

## 1. LangChain4j 定位

```
LangChain4j = LangChain 的 Java 版（框架中立）

特点：
  框架中立（Spring/Quarkus/Micronaut 皆可）
  30+ 模型 Provider（覆盖最广）
  30+ 向量存储（生态最广）
  @AiService 声明式接口（类 MyBatis Mapper 风格）

⚠️ 面试必答：
"LangChain4j = LangChain Java 移植——
 框架中立 + 生态最广；
 亮点是 @AiService 声明式接口
 （像 MyBatis：写接口即可用）。"
```

---

## 2. @AiService 声明式接口

### 2.1 核心用法

```java
// ⚠️ @AiService：声明式接口（AI 版 Mapper）
@AiService
public interface Assistant {

    // 简单对话：返回 String
    String chat(String userMessage);

    // 结构化返回：DTO 自动映射
    SentimentResult analyze(String text);

    // 多参数 + 系统提示
    @SystemMessage("你是订单客服，回答简洁专业")
    String answerOrderQuestion(String question, @MemoryId String userId);
}

// 装配与使用
@Configuration
public class AiConfig {

    @Bean
    Assistant assistant() {
        return AiServices.builder(Assistant.class)
                .chatLanguageModel(openAiModel())
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory
                        .builder().maxMessages(10).build())
                .build();
    }
}

@Service
public class OrderAssistantService {
    private final Assistant assistant;
    // assistant.chat("我的订单多久到？");  ← 直接调用接口
}
```

> 🎯 **要点**：@AiService = 声明式 AI 接口——写接口（方法签名 = 调用约定）、@SystemMessage 定义角色、@MemoryId 隔离会话、AiServices 装配。**MyBatis 风格的 AI 编程**。

---

## 3. RAG 模块

### 3.1 RAG 装配

```java
// ① 检索组件（EmbeddingStore + ContentRetriever）
@Bean
ContentRetriever contentRetriever(EmbeddingStore<TextSegment> store,
                                  EmbeddingModel embeddingModel) {
    return EmbeddingStoreContentRetriever.builder()
            .embeddingStore(store)
            .embeddingModel(embeddingModel)
            .maxResults(5)                   // TopK
            .minScore(0.7)                   // 相似度阈值
            .build();
}

// ② 注入 AiService（RAG 自动生效）
Assistant assistant = AiServices.builder(Assistant.class)
        .chatLanguageModel(model)
        .contentRetriever(contentRetriever)   // ⚠️ RAG 一行注入
        .build();
```

### 3.2 ETL 管线

```java
// 文档导入（解析 → 分块 → 向量化 → 入库）
public void importDocuments(Path path) {
    // ① 解析（支持 PDF/Word/HTML）
    DocumentParser parser = new ApacheTikaDocumentParser();
    Document document = parser.parse(path);

    // ② 分块
    DocumentSplitter splitter = DocumentSplitters
            .recursive(600, 50);            // 块 600、重叠 50
    List<TextSegment> segments = splitter.split(document);

    // ③ 向量化 + 入库
    List<Embedding> embeddings = embeddingModel
            .embedAll(segments).content();
    embeddingStore.addAll(embeddings, segments);
}
```

> 🎯 **要点**：LangChain4j RAG = ContentRetriever（检索注入）+ EmbeddingStore（向量库）——装配到 AiService 后自动检索增强。

---

## 4. 工具与记忆

### 4.1 工具调用

```java
// ⚠️ @Tool 注解（与 Spring AI 同风格）
public class OrderTools {

    @Tool("查询订单状态")
    public String getOrderStatus(String orderId) {
        return orderService.getStatus(orderId);
    }
}

// 装配
Assistant assistant = AiServices.builder(Assistant.class)
        .chatLanguageModel(model)
        .tools(new OrderTools())            // ⚠️ 工具注入
        .build();
```

### 4.2 记忆体系

```java
// 记忆 Provider（按会话隔离）
@Bean
ChatMemoryProvider chatMemoryProvider() {
    return memoryId -> MessageWindowChatMemory
            .builder()
            .maxMessages(20)
            .build();
}

// 多轮对话：@MemoryId 自动路由到对应记忆
@AiService
public interface Assistant {
    @SystemMessage("你是有帮助的助手")
    String chat(@MemoryId String conversationId, @UserMessage String message);
}
```

> 🎯 **要点**：LangChain4j 工具（@Tool + .tools()）与记忆（@MemoryId + ChatMemoryProvider）与 Spring AI 同理念、更简洁——**声明式是它的统一风格**。

---

## 5. 与 Spring AI 对比

| 维度 | Spring AI | LangChain4j |
|------|:---:|:---:|
| 框架绑定 | 强（Spring Boot） | 中立 |
| 编程风格 | Fluent（ChatClient.prompt） | 声明式（@AiService） |
| 模型覆盖 | 20+ | 30+（最广） |
| 向量存储 | 15+ | 30+（最广） |
| MCP | ✅ 最完整 | ✅ 模块 |
| Spring 集成 | 自动配置/Actuator | 需手动装配 |
| 企业背书 | VMware/Broadcom | Red Hat/Microsoft |

```
选型决策：
  Spring Boot 团队（零摩擦 + MCP） → Spring AI
  多框架/非 Spring → LangChain4j
  声明式偏好 → LangChain4j（@AiService）
  Fluent 偏好 → Spring AI（ChatClient）

⚠️ 面试必答：
"Spring AI 赢在 Spring 集成（自动配置、
 MCP 完整）；LangChain4j 赢在生态广
 （30+ 模型/向量）+ 声明式接口。
 Spring 团队选 Spring AI 是主流答案。"
```

---

> 🎯 **核心要点**：LangChain4j = **@AiService 声明式**（接口即 AI 服务）+ **RAG**（ContentRetriever 注入）+ **工具/记忆**（@Tool + @MemoryId）+ **生态最广**（30+ 模型/向量）。与 Spring AI 对比：Spring 集成 vs 生态广度——**Spring 团队选 Spring AI**。

---

**返回总览**：[00-JavaAI技术栈总览与全景架构](00-JavaAI技术栈总览与全景架构.md) | **上一篇**：[06-SpringAI深入](06-SpringAI深入.md) | **下一篇**：[08-生产级工程化](08-生产级工程化.md)
