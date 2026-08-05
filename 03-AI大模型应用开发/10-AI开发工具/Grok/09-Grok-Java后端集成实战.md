# 09 - Grok Java 后端集成实战

> 🎯 Java 开发者如何将 Grok 的能力嵌入到后端系统中？本章从 Spring AI 集成、自建 API 客户端、Agent 开发到生产级最佳实践，提供完整的 Java + Grok 集成方案

---

## 📚 目录

1. [集成方案选型](#1-集成方案选型)
2. [Spring AI 集成 Grok](#2-spring-ai-集成-grok)
3. [自建 Grok 客户端](#3-自建-grok-客户端)
4. [RAG 检索增强生成实战](#4-rag-检索增强生成实战)
5. [基于 Grok 的 AI Agent 开发](#5-基于-grok-的-ai-agent-开发)
6. [生产级部署最佳实践](#6-生产级部署最佳实践)

---

## 1. 集成方案选型

```text
方案对比（2026.07）
│
├── 🥇 Cursor + Grok 4.5 ⭐⭐⭐⭐⭐ (2026 新增！最推荐)
│   ├── IDE 内原生 Grok 4.5 — 免 API Key、免配置
│   ├── Agent 模式：多文件编辑 + 终端 + Git 一键操作
│   ├── Token 效率 4.2× → Agent 任务极省
│   └── 适合：Java 开发者日常编码首选
│
├── Spring AI ⭐⭐⭐⭐⭐ (API 集成推荐)
│   ├── 自动配置、连接池、重试
│   ├── 统一抽象（可随时切换底层模型）
│   └── 适合：Spring Boot 项目需要 API 可控调用
│
├── LangChain4j ⭐⭐⭐⭐
│   ├── 更灵活的 API
│   ├── 内置 RAG / Agent 原语
│   └── 适合：需要复杂 LLM 编排
│
├── 直接 HTTP ⭐⭐⭐
│   ├── 零依赖、完全可控
│   ├── 不依赖 Spring 生态
│   └── 适合：轻量级项目 / 非 Spring 项目
│
└── OpenAI SDK 兼容 ⭐⭐⭐⭐
    ├── 直接用 OpenAI Java SDK
    ├── 仅改 base URL 和 Key
    └── 适合：已有 OpenAI 集成的项目迁移
```

---

## 2. Spring AI 集成 Grok

### 2.1 依赖配置

```xml
<!-- pom.xml -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- Spring AI OpenAI Starter（兼容 xAI API） -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    </dependency>

    <!-- 可选：向量数据库支持 -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

### 2.2 配置文件

```yaml
# application.yml
spring:
  ai:
    openai:
      api-key: ${XAI_API_KEY}             # 从环境变量读取
      base-url: https://api.x.ai           # xAI API 地址
      chat:
        enabled: true
        options:
          model: grok-3-mini               # 默认模型
          temperature: 0.7
          max-tokens: 4096

# 多模型配置（用于不同场景）
grok:
  models:
    fast:
      model: grok-3-mini
      temperature: 0.7
    powerful:
      model: grok-3
      temperature: 0.3

# 连接池配置
  retry:
    max-attempts: 3
    backoff:
      initial-interval: 1000
      multiplier: 2
      max-interval: 10000
```

### 2.3 多模型 Bean 配置

```java
@Configuration
@Slf4j
public class GrokMultiModelConfig {

    @Bean
    public OpenAiApi grokApi() {
        return new OpenAiApi(
            "https://api.x.ai",
            System.getenv("XAI_API_KEY")
        );
    }

    /**
     * 快速模型：适合简单问答、内容生成
     */
    @Bean
    @Primary
    public OpenAiChatModel grokFastModel(OpenAiApi api) {
        return OpenAiChatModel.builder()
            .openAiApi(api)
            .defaultOptions(OpenAiChatOptions.builder()
                .model("grok-3-mini")
                .temperature(0.7)
                .maxTokens(4096)
                .build())
            .build();
    }

    /**
     * 强力模型：适合复杂推理、代码分析
     */
    @Bean
    public OpenAiChatModel grokPowerfulModel(OpenAiApi api) {
        return OpenAiChatModel.builder()
            .openAiApi(api)
            .defaultOptions(OpenAiChatOptions.builder()
                .model("grok-3")
                .temperature(0.3)
                .maxTokens(8192)
                .build())
            .build();
    }
}
```

### 2.4 核心 Service 封装

```java
@Service
@Slf4j
public class GrokService {

    private final OpenAiChatModel fastModel;
    private final OpenAiChatModel powerfulModel;

    public GrokService(
            @Qualifier("grokFastModel") OpenAiChatModel fastModel,
            @Qualifier("grokPowerfulModel") OpenAiChatModel powerfulModel) {
        this.fastModel = fastModel;
        this.powerfulModel = powerfulModel;
    }

    /**
     * 简单对话
     */
    public String chat(String userMessage) {
        return fastModel.call(userMessage);
    }

    /**
     * 带系统提示词的对话
     */
    public String chatWithSystem(String systemPrompt, String userMessage) {
        Prompt prompt = new Prompt(
            List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userMessage)
            )
        );
        return fastModel.call(prompt).getResult().getOutput().getContent();
    }

    /**
     * 代码审查（使用强力模型）
     */
    public CodeReviewResult reviewCode(String sourceCode, String language) {
        String systemPrompt = """
            你是一名资深 %s 代码审查专家。请审查以下代码：
            1. 潜在 Bug 和空指针问题
            2. 并发安全性
            3. 性能瓶颈
            4. 可维护性问题
            5. 最佳实践偏离

            请以 JSON 格式返回结果：
            {"issues": [{"severity":"HIGH/MEDIUM/LOW",
              "line": number, "description": "...",
              "suggestion": "..."}],
             "overall_score": "A/B/C/D"}
            """.formatted(language);

        String result = powerfulModel.call(
            new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage("```%s\n%s\n```".formatted(language, sourceCode))
            ))
        ).getResult().getOutput().getContent();

        return parseReviewResult(result);
    }

    /**
     * 流式对话（SSE → Flux）
     */
    public Flux<String> chatStream(String userMessage) {
        Prompt prompt = new Prompt(new UserMessage(userMessage),
            OpenAiChatOptions.builder()
                .model("grok-3-mini")
                .temperature(0.7)
                .build());

        return fastModel.stream(prompt)
            .map(response -> response.getResult().getOutput().getContent());
    }
}
```

---

## 3. 自建 Grok 客户端

### 3.1 轻量级 HTTP 客户端

适合非 Spring 项目或需要完全控制的场景。

```java
/**
 * 零框架依赖的 Grok HTTP 客户端
 * 仅依赖: java.net.http (Java 11+) + Jackson
 */
public class GrokHttpClient implements AutoCloseable {

    private static final String BASE_URL = "https://api.x.ai/v1";
    private static final String CHAT_ENDPOINT = "/chat/completions";

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String apiKey;

    public GrokHttpClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 同步对话
     */
    public ChatResponse chat(ChatRequest request)
            throws IOException, InterruptedException {
        String body = mapper.writeValueAsString(request);
        var httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + CHAT_ENDPOINT))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .timeout(Duration.ofSeconds(60))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        var response = httpClient.send(httpRequest,
            HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new GrokApiException(
                "API 错误: " + response.statusCode() + " " + response.body());
        }

        return mapper.readValue(response.body(), ChatResponse.class);
    }

    /**
     * 流式对话
     */
    public Flow.Publisher<String> chatStream(ChatRequest request) {
        request = request.withStream(true);
        // 实现 SSE 解析（省略详细实现）
        return subscriber -> {
            // ... SSE 订阅逻辑
        };
    }

    @Override
    public void close() {
        // HttpClient 无需显式关闭
    }
}

// 数据模型
@Data
@Builder
public class ChatRequest {
    private String model;
    private List<Message> messages;
    @Builder.Default
    private double temperature = 0.7;
    @Builder.Default
    private int maxTokens = 4096;
    @Builder.Default
    private boolean stream = false;
}

@Data
public class ChatResponse {
    private String id;
    private List<Choice> choices;
    private Usage usage;

    public String getContent() {
        return choices.get(0).message.content;
    }
}
```

### 3.2 使用示例

```java
// 初始化
var client = new GrokHttpClient(System.getenv("XAI_API_KEY"));

// 简单对话
var response = client.chat(ChatRequest.builder()
    .model("grok-3-mini")
    .messages(List.of(
        new Message("system", "You are a Java expert."),
        new Message("user", "Explain virtual threads in Java 21")
    ))
    .temperature(0.7)
    .build());

System.out.println(response.getContent());

// 关闭
client.close();
```

---

## 4. RAG 检索增强生成实战

### 4.1 架构设计

```text
           ┌──────────────┐
           │   用户提问     │
           └──────┬───────┘
                  │
                  ▼
           ┌──────────────┐
           │ ① 向量检索     │  ← 从向量数据库检索相关文档
           │   (PGVector)  │
           └──────┬───────┘
                  │ Top-K 相关文档片段
                  ▼
           ┌──────────────┐
           │ ② 构建 Prompt  │  ← 将检索结果注入 System Prompt
           │               │      "基于以下资料回答..."
           └──────┬───────┘
                  │ 增强后的 Prompt
                  ▼
           ┌──────────────┐
           │ ③ Grok 生成   │  ← 基于私有知识 + LLM 能力
           │   (grok-3)   │      生成准确回答
           └──────┬───────┘
                  │
                  ▼
           ┌──────────────┐
           │ ④ 返回答案    │  ← 带引用来源
           └──────────────┘
```

### 4.2 RAG Service 实现

```java
@Service
@Slf4j
public class RagService {

    private final VectorStore vectorStore;       // PGVector
    private final OpenAiChatModel chatModel;

    public RagService(VectorStore vectorStore,
                      @Qualifier("grokPowerfulModel") OpenAiChatModel chatModel) {
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
    }

    /**
     * RAG 问答
     */
    public RagResponse ask(String question) {
        // 1. 向量检索
        List<Document> relevantDocs = vectorStore.similaritySearch(
            SearchRequest.query(question).withTopK(5)
        );

        if (relevantDocs.isEmpty()) {
            return new RagResponse("未找到相关文档，请尝试其他问题",
                List.of());
        }

        // 2. 构建上下文
        String context = relevantDocs.stream()
            .map(doc -> "【来源: %s】%s".formatted(
                doc.getMetadata().getOrDefault("source", "未知"),
                doc.getContent()))
            .collect(Collectors.joining("\n---\n"));

        // 3. 组装 Prompt
        String systemPrompt = """
            你是一个基于知识库的问答助手。请严格根据以下资料回答问题。
            如果资料中没有相关信息，请明确说"根据现有资料无法回答"，
            不要编造信息。

            ## 参考资料
            %s

            ## 回答要求
            - 引用具体来源
            - 保持客观准确
            - 如资料有矛盾，请指出
            """.formatted(context);

        // 4. 调用 Grok
        String answer = chatModel.call(new Prompt(List.of(
            new SystemMessage(systemPrompt),
            new UserMessage(question)
        ))).getResult().getOutput().getContent();

        // 5. 返回带引用的答案
        return new RagResponse(answer, relevantDocs.stream()
            .map(d -> d.getMetadata().getOrDefault("source", "").toString())
            .distinct()
            .toList());
    }

    /**
     * 文档入库
     */
    public void ingestDocument(String content, Map<String, Object> metadata) {
        Document doc = new Document(content, metadata);
        vectorStore.add(List.of(doc));
        log.info("文档已入库: {}", metadata.get("source"));
    }
}
```

---

## 5. 基于 Grok 的 AI Agent 开发

### 5.1 Agent 架构

```text
┌─────────────────────────────────────────┐
│              GrokAgent                   │
│                                          │
│  ┌──────────┐  ┌──────────────────────┐ │
│  │ Planner   │  │ Tool Executor         │ │
│  │ - 任务分解 │  │ - 工具注册/发现       │ │
│  │ - 步骤规划 │  │ - 安全沙箱执行        │ │
│  └─────┬────┘  └──────────┬───────────┘ │
│        │                  │              │
│  ┌─────┴──────────────────┴───────────┐ │
│  │        Grok Thinking Engine        │ │
│  │     (grok-3 + Function Calling)    │ │
│  └──────────────────┬────────────────┘ │
│                     │                   │
│  ┌──────────────────┴────────────────┐  │
│  │        Memory / Context            │  │
│  │  - 短期：对话上下文                 │  │
│  │  - 中期：对话摘要                   │  │
│  │  - 长期：向量数据库                  │  │
│  └────────────────────────────────────┘  │
└─────────────────────────────────────────┘

可用工具：
├── 📁 FileTool — 读写文件
├── 🔍 SearchTool — 搜索 Web/X 平台
├── 🗄️ DatabaseTool — SQL 查询
├── 🌐 HttpTool — HTTP API 调用
├── 📊 CodeInterpreter — Python 执行
└── 🔧 ShellTool — Shell 命令 (沙箱)
```

### 5.2 Agent 核心实现

```java
@Component
public class GrokAgent {

    private final OpenAiChatModel chatModel;
    private final Map<String, AgentTool> tools = new ConcurrentHashMap<>();
    private final List<Message> conversationHistory = new ArrayList<>();

    public GrokAgent(
            @Qualifier("grokPowerfulModel") OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 注册工具
     */
    public void registerTool(AgentTool tool) {
        tools.put(tool.getName(), tool);
        log.info("工具已注册: {} - {}", tool.getName(), tool.getDescription());
    }

    /**
     * Agent 主循环
     */
    public String execute(String task) throws AgentException {
        conversationHistory.add(new UserMessage(task));

        for (int iteration = 0; iteration < 10; iteration++) {
            log.info("Agent 迭代 #{}, 思考中...", iteration + 1);

            // 1. 构建工具定义
            List<String> toolDefinitions = tools.values().stream()
                .map(AgentTool::toFunctionDefinition)
                .toList();

            // 2. 调用 Grok（带 Function Calling）
            var response = chatModel.call(new Prompt(
                conversationHistory,
                OpenAiChatOptions.builder()
                    .model("grok-3")
                    .functions(toolDefinitions)
                    .build()
            ));

            var output = response.getResult().getOutput();

            // 3. 判断是否调用了工具
            if (output.hasToolCalls()) {
                // 执行工具调用
                for (var toolCall : output.getToolCalls()) {
                    String toolName = toolCall.getName();
                    String arguments = toolCall.getArguments();

                    AgentTool tool = tools.get(toolName);
                    if (tool == null) {
                        throw new AgentException("未注册的工具: " + toolName);
                    }

                    log.info("执行工具: {}({})", toolName, arguments);
                    String result = tool.execute(arguments);

                    // 将工具结果加入对话
                    conversationHistory.add(
                        new AssistantMessage(toolCall.toString()));
                    conversationHistory.add(
                        new FunctionMessage(toolName, result));
                }
                // 继续循环，Grok 会基于工具结果生成下一步
                continue;
            }

            // 4. 没有工具调用 → 最终回答
            String finalAnswer = output.getContent();
            conversationHistory.add(new AssistantMessage(finalAnswer));
            return finalAnswer;
        }

        throw new AgentException("Agent 达到最大迭代次数");
    }

    /**
     * 清空对话历史
     */
    public void reset() {
        conversationHistory.clear();
    }
}

/**
 * 工具接口
 */
public interface AgentTool {
    String getName();
    String getDescription();
    String getParametersSchema();     // JSON Schema
    String execute(String arguments); // 执行工具

    default String toFunctionDefinition() {
        return """
        {
            "type": "function",
            "function": {
                "name": "%s",
                "description": "%s",
                "parameters": %s
            }
        }
        """.formatted(getName(), getDescription(), getParametersSchema());
    }
}
```

### 5.3 示例工具实现

```java
/**
 * 数据库查询工具
 */
@Component
public class DatabaseQueryTool implements AgentTool {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseQueryTool(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String getName() { return "query_database"; }

    @Override
    public String getDescription() {
        return "执行 SQL 查询（仅支持 SELECT），返回 JSON 格式结果";
    }

    @Override
    public String getParametersSchema() {
        return """
        {
            "type": "object",
            "properties": {
                "sql": {
                    "type": "string",
                    "description": "要执行的 SELECT SQL 查询语句"
                }
            },
            "required": ["sql"]
        }
        """;
    }

    @Override
    public String execute(String arguments) {
        try {
            String sql = new ObjectMapper()
                .readTree(arguments).get("sql").asText();

            // 🛡️ 安全检查：只允许 SELECT
            if (!sql.trim().toUpperCase().startsWith("SELECT")) {
                return "错误：仅允许 SELECT 查询";
            }

            List<Map<String, Object>> results = jdbcTemplate
                .queryForList(sql);

            return new ObjectMapper().writeValueAsString(results);

        } catch (Exception e) {
            return "查询失败: " + e.getMessage();
        }
    }
}
```

---

## 6. 生产级部署最佳实践

### 6.1 关键实践清单

| 实践 | 说明 | 优先级 |
|------|------|:---:|
| **API Key 安全管理** | 环境变量/密钥管理服务，绝不硬编码 | 🔴 必须 |
| **连接池 + 超时** | 配置 HTTP 连接池，防止连接泄漏 | 🔴 必须 |
| **熔断降级** | 使用 Resilience4j/Sentinel 防止雪崩 | 🔴 必须 |
| **缓存策略** | 相同问题缓存结果，减少 API 调用 | 🟡 推荐 |
| **Token 预算管理** | 监控和限制每用户/每会话 Token 消耗 | 🟡 推荐 |
| **内容审核** | 对用户输入和模型输出做内容安全检查 | 🟡 推荐 |
| **请求日志** | 记录所有 API 调用（脱敏后），便于审计 | 🟢 建议 |
| **灰度发布** | 模型切换时用灰度策略验证 | 🟢 建议 |
| **多模型 Fallback** | Grok 不可用时自动切换 GPT-4o 等 | 🟢 建议 |

### 6.2 熔断配置示例

```java
@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreaker grokCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)           // 50% 失败率触发熔断
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)              // 滑动窗口大小
            .permittedNumberOfCallsInHalfOpenState(3)
            .recordExceptions(GrokApiException.class,
                HttpServerErrorException.class)
            .build();

        return CircuitBreaker.of("grok-api", config);
    }

    @Bean
    public Retry grokRetry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofSeconds(1))
            .retryExceptions(HttpServerErrorException.class)
            .build();

        return Retry.of("grok-retry", config);
    }

    @Bean
    public RateLimiter grokRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(50)                  // 每秒 50 次
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .timeoutDuration(Duration.ofSeconds(5))
            .build();

        return RateLimiter.of("grok-rate-limiter", config);
    }
}

@Service
public class ResilientGrokService {

    private final OpenAiChatModel chatModel;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final RateLimiter rateLimiter;

    public String chatWithResilience(String message) {
        return Decorators.ofSupplier(() ->
                chatModel.call(message))  // ← Grok 调用
            .withCircuitBreaker(circuitBreaker)
            .withRetry(retry)
            .withRateLimiter(rateLimiter)
            .withFallback(List.of(GrokApiException.class),
                e -> "Grok 服务暂时不可用，请稍后重试。错误: " + e.getMessage())
            .decorate()
            .get();
    }
}
```

### 6.3 监控指标

```java
@Component
public class GrokMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter requestCounter;
    private final Timer responseTimer;
    private final Counter tokenCounter;

    public GrokMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.requestCounter = Counter.builder("grok.requests.total")
            .description("Grok API 总请求数")
            .tag("provider", "xai")
            .register(meterRegistry);

        this.responseTimer = Timer.builder("grok.response.time")
            .description("Grok 响应时间")
            .register(meterRegistry);

        this.tokenCounter = Counter.builder("grok.tokens.total")
            .description("Grok 消耗的 Token 总数")
            .register(meterRegistry);
    }

    public <T> T recordCall(String model, Supplier<T> call) {
        requestCounter.increment();
        long start = System.nanoTime();
        try {
            T result = call.get();
            responseTimer.record(System.nanoTime() - start,
                TimeUnit.NANOSECONDS);
            return result;
        } catch (Exception e) {
            Counter.builder("grok.errors.total")
                .tag("error", e.getClass().getSimpleName())
                .register(meterRegistry)
                .increment();
            throw e;
        }
    }

    public void recordTokens(int count) {
        tokenCounter.increment(count);
    }
}
```

> 🎯 **核心要点**：Java 集成 Grok 最推荐 Spring AI 方案——开箱即用的连接管理、多模型配置和工具调用支持。生产环境务必加上熔断、限流、重试三板斧，配合指标监控和成本追踪，才能让 Grok 从"Demo"变成"生产级"。

---

**上一模块**：[08-Grok订阅与付费生态](08-Grok订阅与付费生态.md) | **下一模块**：[10-Grok与主流模型深度对比](10-Grok与主流模型深度对比.md) | **返回总览**：[00-Grok知识体系总览](00-Grok知识体系总览.md)
