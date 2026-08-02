# 13 - LangChain4j 项目实战：Agent 系统

> 🎯 从零搭建一个 LangChain4j Agent 系统 — 接口定义 → 工具注册 → RAG 检索 → 记忆管理 → 流式 SSE。完整代码可直接运行，关键是理解 AiServices 动态代理的编排逻辑

---

## 目录

1. [项目概述：Agent 能力全景](#1-项目概述agent-能力全景)
2. [Maven 依赖与 Spring Boot 配置](#2-maven-依赖与-spring-boot-配置)
3. [完整项目结构](#3-完整项目结构)
4. [核心代码：五层装配](#4-核心代码五层装配)
5. [流式输出与记忆持久化](#5-流式输出与记忆持久化)
6. [Agent 接口完整示例](#6-agent-接口完整示例)

---

## 1. 项目概述：Agent 能力全景

```text
项目名：smart-agent — 具备"手"和"脑"的智能 Agent 系统

Agent 四大能力：
├── 🗣️ 对话理解    → ChatLanguageModel + @SystemMessage 角色定义
├── 🧠 推理规划    → 自动判断是否需要调工具、检索知识库
├── 🔧 工具调用    → @Tool 注解 → 查天气/查订单/计算/发邮件
├── 📚 知识检索    → ContentRetriever → 自动 RAG（无需手动调）
└── 💾 对话记忆    → ChatMemoryProvider → Redis 持久化（按用户隔离）

技术栈：
├── Spring Boot 4.1 + Java 21 + LangChain4j 1.15+
├── DeepSeek-V3（OpenAI 兼容接口，支持 Function Calling）
├── Redis（会话记忆持久化）
├── Maven
```

---

## 2. Maven 依赖与 Spring Boot 配置

### 2.1 pom.xml

```xml
<dependencies>
    <!-- LangChain4j Spring Boot Starter -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-spring-boot-starter</artifactId>
        <version>1.15.0-beta25</version>
    </dependency>

    <!-- OpenAI 兼容客户端（DeepSeek/通义/豆包 均可用） -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-open-ai</artifactId>
        <version>1.15.0-beta25</version>
    </dependency>

    <!-- 可选：流式 Web 支持 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

    <!-- Redis 记忆持久化 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
</dependencies>
```

### 2.2 application.yml

```yaml
langchain4j:
  open-ai:
    chat-model:
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com/v1
      model-name: deepseek-chat              # 支持 Function Calling
      temperature: 0.3
      timeout: 60s
      log-requests: true                      # 调试时开启
      log-responses: true
    streaming-chat-model:                     # 流式模型（独立配置）
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com/v1
      model-name: deepseek-chat
      temperature: 0.3
  logging:
    level: debug                               # 打印 Agent 内部编排日志
```

---

## 3. 完整项目结构

```text
smart-agent/
├── pom.xml
├── src/main/java/com/example/agent/
│   ├── SmartAgentApplication.java
│   ├── config/
│   │   ├── ModelConfig.java              # 模型 Bean（对话/流式/嵌入）
│   │   └── AgentConfig.java              # ★ Agent 装配（核心文件）
│   ├── controller/
│   │   └── AgentController.java          # REST + 流式 SSE 接口
│   ├── agent/
│   │   └── AssistantAgent.java           # ★ Agent 接口定义（纯 Interface）
│   ├── tool/
│   │   ├── WeatherTool.java              # 查天气工具
│   │   └── OrderTool.java                # 查订单工具
│   ├── rag/
│   │   └── RagService.java               # 知识库加载 + 检索配置
│   └── memory/
│       └── RedisChatMemoryStore.java      # Redis 记忆持久化
└── src/main/resources/
    ├── application.yml
    └── prompts/
        └── system-agent.st                # System Prompt 模板
```

---

## 4. 核心代码：五层装配

### 4.1 第一层：模型配置

```java
@Configuration
public class ModelConfig {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;
    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String baseUrl;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
            .apiKey(apiKey).baseUrl(baseUrl)
            .modelName("deepseek-chat")
            .timeout(Duration.ofSeconds(60))
            .temperature(0.3)
            .build();
    }

    @Bean
    public StreamingChatLanguageModel streamingChatModel() {
        return OpenAiStreamingChatModel.builder()
            .apiKey(apiKey).baseUrl(baseUrl)
            .modelName("deepseek-chat")
            .build();
    }
}
```

### 4.2 第二层：工具定义

```java
@Component
public class WeatherTool {
    @Tool("查询指定城市的实时天气，返回温度、湿度、天气状况")
    public String getWeather(@P("城市名称，如 北京、上海") String city) {
        // 实际项目中调天气 API
        return city + "：晴，25°C，湿度 60%";
    }
}

@Component
public class OrderTool {
    @Tool("根据订单号查询订单状态")
    public String queryOrder(@P("订单号") String orderId) {
        return "订单 " + orderId + "：已发货，预计明天到达";
    }

    @Tool("四则运算")
    public double calculate(
        @P("第一个操作数") double a,
        @P("第二个操作数") double b,
        @P("运算：add/subtract/multiply/divide") String op) {
        return switch (op) {
            case "add" -> a + b;
            case "subtract" -> a - b;
            case "multiply" -> a * b;
            case "divide" -> a / b;
            default -> throw new IllegalArgumentException("不支持的运算: " + op);
        };
    }
}
```

### 4.3 第三层：RAG 检索配置

```java
@Configuration
public class RagService {

    @Bean
    public ContentRetriever contentRetriever(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .maxResults(3)                     // TopK = 3
            .minScore(0.5)                     // 相似度阈值
            .build();
    }
}
```

### 4.4 第四层：记忆配置（Redis 持久化）

```java
@Component
public class RedisChatMemoryStore implements ChatMemoryStore {

    private final StringRedisTemplate redis;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = redis.opsForValue().get("chat:memory:" + memoryId);
        if (json == null) return new ArrayList<>();
        return ChatMessageDeserializer.messagesFromJson(json);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String json = ChatMessageSerializer.messagesToJson(messages);
        redis.opsForValue().set(
            "chat:memory:" + memoryId,
            json,
            Duration.ofDays(1)               // 24 小时过期
        );
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redis.delete("chat:memory:" + memoryId);
    }
}
```

### 4.5 第五层：★ Agent 装配（核心）

```java
@Configuration
public class AgentConfig {

    @Bean
    public ChatMemoryProvider chatMemoryProvider(RedisChatMemoryStore store) {
        return memoryId -> MessageWindowChatMemory.builder()
            .id(memoryId)
            .maxMessages(20)                 // 滑动窗口 20 条
            .chatMemoryStore(store)          // Redis 持久化
            .build();
    }

    @Bean
    public AssistantAgent assistantAgent(
            ChatLanguageModel chatModel,
            StreamingChatLanguageModel streamingModel,
            WeatherTool weatherTool,
            OrderTool orderTool,
            ContentRetriever contentRetriever,
            ChatMemoryProvider memoryProvider) {

        return AiServices.builder(AssistantAgent.class)
            .chatLanguageModel(chatModel)
            .streamingChatLanguageModel(streamingModel)
            .tools(weatherTool, orderTool)   // ★ 工具注册（Agent 的"手"）
            .contentRetriever(contentRetriever) // ★ RAG 检索（Agent 的"参考书"）
            .chatMemoryProvider(memoryProvider) // ★ 会话记忆（按用户隔离）
            .build();
        // 返回的是 JDK 动态代理对象 — 框架自动编排
        // "思考 → 调工具 → 检索 → 再思考 → 回答" 的完整循环
    }
}
```

---

## 5. 流式输出与记忆持久化

```java
@RestController
public class AgentController {

    private final AssistantAgent agent;

    // 同步接口
    @PostMapping("/api/agent/chat")
    public String chat(@RequestBody ChatRequest request) {
        return agent.chat(request.sessionId(), request.message());
    }

    // ★ 流式 SSE 接口（打字机效果）
    @PostMapping(value = "/api/agent/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        return agent.chatStream(request.sessionId(), request.message());
    }
}
```

**追问：** `@MemoryId` 如何工作？→ AiServices 代理拦截方法调用时，将 `@MemoryId` 参数传给 `ChatMemoryProvider`，`memoryId` 对应的独立 `ChatMemory` 自动注入 LLM 请求中 — 不同用户互不干扰。

---

## 6. Agent 接口完整示例

```java
// ★ Agent 只是一个 Interface！零实现代码 — AiServices 动态代理全部接管
public interface AssistantAgent {

    // 基础对话
    @SystemMessage(fromResource = "prompts/system-agent.st")
    String chat(@MemoryId String sessionId, @UserMessage String message);

    // 流式对话
    @SystemMessage(fromResource = "prompts/system-agent.st")
    Flux<String> chatStream(@MemoryId String sessionId, @UserMessage String message);

    // 结构化输出：提取订单信息
    @SystemMessage("你是订单信息提取助手")
    OrderInfo extractOrder(@UserMessage String input);

    // 带模板的翻译
    @SystemMessage("你是专业翻译，翻译成{{language}}")
    @UserMessage("请翻译：{{text}}")
    String translate(@V("text") String text, @V("language") String language);
}

// Agent 接口不包含工具/RAG/记忆的声明 — 全部在 AgentConfig 装配层注入
// 框架自动编排循环：用户提问 → 检索知识库 → 判断是否需要调工具 → 生成回复
```

```text
# prompts/system-agent.st
你是一个智能企业助手。你有以下能力：
1. 查询天气（调用天气工具）
2. 查询订单（调用订单工具）
3. 回答企业知识库中的问题（自动检索）

## 规则
- 如果用户的问题需要查询实时数据，请使用对应的工具
- 如果用户的问题属于企业知识范围内的，请基于检索结果回答
- 如果都不适用，请用你的通用知识回答
- 回答简洁专业，不确定时请坦诚告知
```

---

> 🎯 **核心要点**：LangChain4j Agent 的标准装配 = **五层注入** — ① `ChatLanguageModel`（大脑）② `tools(...)`（手脚）③ `contentRetriever(...)`（参考书）④ `chatMemoryProvider(...)`（记忆）⑤ `streamingChatLanguageModel(...)`（流式嘴）。Agent 只是一个 `Interface`，AiServices 动态代理自动编排"思考→检索→调工具→再思考→回答"循环。生产三件套：**Redis 记忆持久化 + Function Calling 模型（如 DeepSeek-V3）+ @AiService 自动装配**。

**下一模块**：[14-生产级AI架构设计与运维](14-生产级AI架构设计与运维.md) / **返回总览**：[00-总览](00-Java-AI开发框架体系总览.md)
