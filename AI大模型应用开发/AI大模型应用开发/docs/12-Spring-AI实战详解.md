# 12 — Spring AI 实战详解

> **目标**：从零搭建 Spring AI 项目，掌握所有核心 API 的生产级用法。

---

## 1. 快速起步

### 1.1 最小可用项目

```xml
<!-- pom.xml -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.0</version>
</parent>

<properties>
    <spring-ai.version>1.0.0-M6</spring-ai.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

```yaml
# application.yml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com    # 可改为 DeepSeek/Ollama 等
```

```java
// 第一个 API
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@RestController
class ChatController {
    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String q) {
        return chatClient.prompt().user(q).call().content();
    }
}
```

---

## 2. ChatClient 深度用法

### 2.1 对话模式

```java
@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder builder) {
        this.chatClient = builder
            .defaultSystem("你是一个有用的 AI 助手，用中文回答")
            .build();
    }

    // ===== 1. 简单对话 =====
    public String simpleChat(String message) {
        return chatClient.prompt()
            .user(message)
            .call()
            .content();
    }

    // ===== 2. 带 System Prompt =====
    public String chatWithRole(String message) {
        return chatClient.prompt()
            .system("你是 Java 性能优化专家，回答简洁，给出代码示例")
            .user(message)
            .call()
            .content();
    }

    // ===== 3. 多轮对话（手动管理历史） =====
    public String multiTurnChat(List<Message> history, String newMessage) {
        return chatClient.prompt()
            .messages(history)     // 历史消息
            .user(newMessage)      // 新消息
            .call()
            .content();
    }

    // ===== 4. 带模型参数 =====
    public String chatWithOptions(String message) {
        return chatClient.prompt()
            .user(message)
            .options(OpenAiChatOptions.builder()
                .withModel("gpt-4o")
                .withTemperature(0.3)
                .withMaxTokens(2048)
                .withTopP(0.9)
                .withFrequencyPenalty(0.0)
                .withPresencePenalty(0.0)
                .build())
            .call()
            .content();
    }

    // ===== 5. 结构化输出 =====
    record CodeIssue(String severity, int line, String description) {}

    public List<CodeIssue> structuredOutput(String code) {
        return chatClient.prompt()
            .system("分析代码问题，返回 JSON 数组，每项包含 severity(high/medium/low)、line(行号)、description(描述)")
            .user(code)
            .call()
            .entity(new ParameterizedTypeReference<List<CodeIssue>>() {});
    }
}
```

### 2.2 Advisor 拦截器链

```java
@Configuration
public class AdvisorConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
            // 按顺序执行 Advisor 链
            .defaultAdvisors(
                new LoggingAdvisor(),           // 1. 记录日志
                new RetryAdvisor(),              // 2. 失败重试
                new QuestionAnswerAdvisor(vectorStore) // 3. RAG 增强
            )
            .build();
    }
}

// 自定义 Advisor：日志记录
@Component
public class LoggingAdvisor implements CallAroundAdvisor {

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request,
                                       CallAroundAdvisorChain chain) {
        log.info(">> Request: {}", request.userText());
        long start = System.currentTimeMillis();

        AdvisedResponse response = chain.nextAroundCall(request);

        long elapsed = System.currentTimeMillis() - start;
        log.info("<< Response: {} chars, {}ms",
            response.response().length(), elapsed);

        return response;
    }
}

// RAG Advisor
@Component
public class RagAdvisor implements CallAroundAdvisor {

    private final VectorStore vectorStore;

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request,
                                       CallAroundAdvisorChain chain) {
        // 检索相关文档
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.query(request.userText()).withTopK(3));

        // 注入到 System Prompt
        String context = docs.stream()
            .map(Document::getContent)
            .collect(Collectors.joining("\n\n"));

        AdvisedRequest enriched = AdvisedRequest.from(request)
            .withSystemParam("context", context)
            .build();

        return chain.nextAroundCall(enriched);
    }
}
```

---

## 3. 流式输出

### 3.1 基础流式

```java
@RestController
public class StreamingController {

    private final ChatClient chatClient;

    // ===== 方式 1：SSE 流式 =====
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam String message) {
        return chatClient.prompt()
            .user(message)
            .stream()
            .content();
    }

    // ===== 方式 2：完整 ChatResponse 流 =====
    @GetMapping(value = "/chat/stream/full", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStreamFull(@RequestParam String message) {
        return chatClient.prompt()
            .user(message)
            .stream()
            .chatResponse()
            .map(response -> ServerSentEvent.<String>builder()
                .data(response.getResult().getOutput().getContent())
                .build());
    }

    // ===== 方式 3：手动控制流式 =====
    @GetMapping(value = "/chat/stream/manual", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStreamManual(@RequestParam String message) {
        return chatClient.prompt()
            .user(message)
            .stream()
            .content()
            .doOnSubscribe(s -> log.info("Stream started"))
            .doOnNext(token -> log.debug("Token: {}", token))
            .doOnComplete(() -> log.info("Stream completed"))
            .doOnError(e -> log.error("Stream error", e))
            .timeout(Duration.ofSeconds(60))
            .retry(2);
    }
}
```

### 3.2 Redis 多轮对话管理

```java
@Service
public class ConversationService {

    private final RedisTemplate<String, Message> redisTemplate;
    private final ChatClient chatClient;
    private static final int MAX_HISTORY = 20; // 最多保留 20 轮

    public Flux<String> chatWithHistory(String sessionId, String userMessage) {
        // 1. 从 Redis 加载历史
        List<Message> history = loadHistory(sessionId);

        // 2. 添加新消息
        history.add(new UserMessage(userMessage));

        // 3. 流式调用
        StringBuilder fullResponse = new StringBuilder();

        return chatClient.prompt()
            .messages(history)
            .stream()
            .content()
            .doOnNext(fullResponse::append)
            .doOnComplete(() -> {
                // 4. 保存完整回复到 Redis
                history.add(new AssistantMessage(fullResponse.toString()));
                saveHistory(sessionId, history);
            });
    }

    private List<Message> loadHistory(String sessionId) {
        List<Message> history = (List<Message>) redisTemplate
            .opsForValue().get("chat:" + sessionId);
        if (history == null) return new ArrayList<>();

        // 截断到 MAX_HISTORY
        if (history.size() > MAX_HISTORY) {
            history = history.subList(history.size() - MAX_HISTORY, history.size());
        }
        return history;
    }

    private void saveHistory(String sessionId, List<Message> history) {
        redisTemplate.opsForValue()
            .set("chat:" + sessionId, history, Duration.ofHours(24));
    }
}
```

---

## 4. RAG 完整实现

### 4.1 文档摄入 ETL

```java
@Service
public class DocumentIngestionService {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;

    /**
     * 完整 ETL 流水线：Load → Transform → Embed → Store
     */
    public void ingestFile(MultipartFile file) throws IOException {
        // 1. Load：读取文档
        String text = readDocument(file);

        // 2. Transform：文本分割
        List<String> chunks = splitText(text);

        // 3. Transform → Document
        List<Document> documents = chunks.stream()
            .map(chunk -> new Document(chunk,
                Map.of(
                    "source", file.getOriginalFilename(),
                    "chunk_size", String.valueOf(chunk.length()),
                    "ingested_at", Instant.now().toString()
                )))
            .toList();

        // 4. Embed + Store
        vectorStore.add(documents);

        log.info("Ingested {} chunks from {}", chunks.size(), file.getOriginalFilename());
    }

    private String readDocument(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();

        if (filename.endsWith(".pdf")) {
            return extractPdfText(file.getInputStream());
        } else if (filename.endsWith(".docx")) {
            return extractDocxText(file.getInputStream());
        } else if (filename.endsWith(".txt") || filename.endsWith(".md")) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } else {
            // 用 Apache Tika 通用提取
            return new Tika().parseToString(file.getInputStream());
        }
    }

    private List<String> splitText(String text) {
        TokenTextSplitter splitter = TokenTextSplitter.builder()
            .withChunkSize(512)
            .withMinChunkSizeChars(100)
            .withMinChunkLengthToEmbed(20)
            .withKeepSeparator(true)
            .build();

        return splitter.apply(List.of(new Document(text)))
            .stream()
            .map(Document::getContent)
            .toList();
    }
}
```

### 4.2 批量摄入 + 进度追踪

```java
@Service
public class BatchIngestionService {

    private final VectorStore vectorStore;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    /**
     * 批量摄入 + 进度追踪
     */
    public IngestionResult ingestBatch(List<MultipartFile> files) {
        IngestionResult result = new IngestionResult();
        result.totalFiles = files.size();

        // 并行处理
        List<CompletableFuture<Void>> futures = files.stream()
            .map(file -> CompletableFuture.runAsync(() -> {
                try {
                    ingestSingleFile(file);
                    result.successCount.incrementAndGet();
                } catch (Exception e) {
                    log.error("Failed to ingest: {}", file.getOriginalFilename(), e);
                    result.failures.add(file.getOriginalFilename() + ": " + e.getMessage());
                }
                result.processedCount.incrementAndGet();
            }, executor))
            .toList();

        // 等待全部完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return result;
    }

    @Data
    public static class IngestionResult {
        int totalFiles;
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        List<String> failures = new CopyOnWriteArrayList<>();

        public double getProgress() {
            return (double) processedCount.get() / totalFiles;
        }
    }
}
```

---

## 5. Function Calling 实战

### 5.1 企业级 Tool 开发

```java
@Component
public class EnterpriseTools {

    private final JdbcTemplate jdbc;
    private final RestTemplate rest;
    private final RedisTemplate<String, String> redis;

    // ===== Tool 1: 数据库查询 =====
    @Tool(description = """
        查询业务数据库。支持以下查询类型：
        - 用户订单查询：根据用户ID查询订单
        - 销售统计：按日期范围统计销售额
        - 库存查询：查询商品库存
        """)
    public List<Map<String, Object>> queryDatabase(
        @ToolParam(description = "SQL SELECT 查询语句，仅支持 SELECT") String sql) {

        // 安全校验
        if (!sql.trim().toUpperCase().startsWith("SELECT")) {
            return List.of(Map.of("error", "Only SELECT allowed"));
        }

        try {
            return jdbc.queryForList(sql);
        } catch (Exception e) {
            return List.of(Map.of("error", e.getMessage()));
        }
    }

    // ===== Tool 2: 发送通知 =====
    @Tool(description = "发送企业微信/钉钉通知消息给指定用户")
    public String sendNotification(
        @ToolParam(description = "接收人用户名") String username,
        @ToolParam(description = "通知内容") String message,
        @ToolParam(description = "紧急程度：normal 或 urgent") String priority) {

        // 实际调用企业微信 API
        NotificationResult result = notificationService.send(username, message, priority);
        return result.isSuccess() ? "发送成功" : "发送失败: " + result.getError();
    }

    // ===== Tool 3: 带缓存的 API 调用 =====
    @Tool(description = "查询指定城市的实时天气")
    public WeatherInfo getWeather(
        @ToolParam(description = "城市名称，如 北京") String city) {

        // 先查缓存
        String cached = redis.opsForValue().get("weather:" + city);
        if (cached != null) {
            return objectMapper.readValue(cached, WeatherInfo.class);
        }

        // 调用天气 API
        WeatherInfo weather = weatherApi.query(city);

        // 缓存 30 分钟
        redis.opsForValue().set("weather:" + city,
            objectMapper.writeValueAsString(weather), Duration.ofMinutes(30));

        return weather;
    }
}
```

### 5.2 智能路由 Agent

```java
@Service
public class IntelligentAgent {

    private final ChatClient chatClient;
    private final EnterpriseTools tools;

    /**
     * 智能 Agent：自动选择合适的工具
     */
    public String execute(String task) {
        return chatClient.prompt()
            .system("""
                你是一个智能助手，可以：
                1. 查询数据库（用户订单、销售数据、库存）
                2. 查询天气
                3. 发送通知
                4. Web 搜索

                遵循以下规则：
                - 数据库相关操作前，先确认权限
                - 发送通知前，和用户确认内容
                - 如果不确定答案，如实说明
                """)
            .user(task)
            .tools(tools)      // 注册所有工具
            .call()
            .content();
    }
}
```

---

## 6. 图片与音频

### 6.1 图片生成

```java
@Service
public class ImageGenerationService {

    private final ImageModel imageModel;

    public String generateImage(String prompt, String style, String size) {
        ImageOptions options = OpenAiImageOptions.builder()
            .withModel("dall-e-3")
            .withQuality("hd")
            .withStyle(style)     // vivid / natural
            .withHeight(parseHeight(size))
            .withWidth(parseWidth(size))
            .withN(1)
            .build();

        ImageResponse response = imageModel.call(
            new ImagePrompt(prompt, options));

        return response.getResult().getOutput().getUrl();
    }
}
```

### 6.2 语音转文字

```java
@RestController
public class AudioController {

    private final AudioTranscriptionModel transcriptionModel;

    @PostMapping("/transcribe")
    public String transcribe(@RequestParam("file") MultipartFile audioFile) {
        Resource audioResource = new InputStreamResource(audioFile.getInputStream());

        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioResource,
            OpenAiAudioTranscriptionOptions.builder()
                .withLanguage("zh")
                .withResponseFormat(OpenAiAudioApi.TranscriptResponseFormat.TEXT)
                .build());

        return transcriptionModel.call(prompt).getResult().getOutput();
    }
}
```

---

## 7. 生产配置

### 7.1 多模型配置

```yaml
spring:
  ai:
    # 默认模型：OpenAI GPT-4o
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o
          temperature: 0.7

    # 备用模型：本地 Ollama
    ollama:
      base-url: http://gpu-server-01:11434
      chat:
        options:
          model: qwen2.5:7b
          temperature: 0.7

    # Embedding
    embedding:
      options:
        model: text-embedding-3-small
        dimensions: 512

    # 向量库
    vectorstore:
      pgvector:
        host: ${PG_HOST}
        database: ai_platform
        dimensions: 512
        index-type: HNSW

# 重试配置
spring:
  ai:
    retry:
      max-attempts: 3
      backoff:
        initial-interval: 1000
        multiplier: 2

# 连接池
spring:
  ai:
    openai:
      connect-timeout: 30s
      read-timeout: 120s

# HTTP 客户端连接池
http:
  client:
    max-connections: 100
    max-connections-per-route: 20
    connect-timeout: 10s
    read-timeout: 120s
```

---

## 8. 快速复习

```
□ ChatClient: prompt() → user() → call() → content()
□ 结构化输出: .entity(Class.class) / .entity(ParameterizedTypeReference)
□ Advisor 链: 日志 → 重试 → RAG → 缓存 → ...
□ 流式输出: .stream().content() 返回 Flux<String>
□ 多轮对话: Redis 存储消息历史 + 上下文截断
□ RAG ETL: Load → Split → Embed → Store
□ @Tool 注解: 声明式定义 + 自动注册
□ 多模型配置: OpenAI + Ollama + Embedding 各自配置
□ 生产配置: 重试 / 超时 / 连接池 / 熔断
```

---

> **下一步**：[13 — 流式输出与实时通信](./13-流式输出与实时通信.md)
