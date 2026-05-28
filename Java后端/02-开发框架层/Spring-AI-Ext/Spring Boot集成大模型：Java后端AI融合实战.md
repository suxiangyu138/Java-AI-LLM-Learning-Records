# Spring Boot 集成大模型：Java 后端 AI 融合实战

> **所属阶段**：阶段六 — 企业级工程实践
> **适用场景**：将 AI 能力嵌入现有 Spring Boot 项目
> **前置知识**：Spring Boot 基础、大模型 API 调用概念

---

## 1. 整体架构

```
┌─────────────────────────────────────────────────┐
│                 Spring Boot Application          │
│                                                  │
│  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │ Controller│  │ Service  │  │ AI Service    │  │
│  │ (REST)   │→ │ (业务)    │→ │ (LLM 调用封装) │  │
│  └──────────┘  └──────────┘  └───────┬───────┘  │
│                                      │          │
│                          ┌───────────┼───────┐  │
│                          │ Cache     │ LLM   │  │
│                          │ (Redis)   │ API   │  │
│                          └───────────┴───────┘  │
│                                                  │
│  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │ MySQL    │  │ RabbitMQ │  │ Monitoring    │  │
│  │ (反馈存储)│  │ (异步任务)│  │ (Actuator)    │  │
│  └──────────┘  └──────────┘  └───────────────┘  │
└─────────────────────────────────────────────────┘
```

---

## 2. AI 服务封装

### 2.1 配置

```yaml
# application.yml
ai:
  llm:
    provider: deepseek
    api-key: ${AI_API_KEY}
    base-url: https://api.deepseek.com
    default-model: deepseek-chat
    timeout: 30s
  embedding:
    provider: openai
    model: text-embedding-3-small
  conversation:
    max-history: 20
    cache-ttl: 3600
```

```java
// AiProperties.java
@ConfigurationProperties(prefix = "ai.llm")
public record AiProperties(
    String provider,
    String apiKey,
    String baseUrl,
    String defaultModel,
    Duration timeout
) {}
```

### 2.2 AI 调用客户端

```java
// AiClient.java
@Slf4j
@Service
public class AiClient {

    private final RestClient restClient;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    public AiClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
            .baseUrl(properties.baseUrl())
            .defaultHeader("Authorization", "Bearer " + properties.apiKey())
            .requestInterceptor((req, body, exec) -> {
                log.debug("AI API call: {} tokens in/out",
                    estimateTokens(new String(body, StandardCharsets.UTF_8)));
                return exec.execute(req, body);
            })
            .build();
    }

    public ChatResponse chat(ChatRequest request) {
        return restClient.post()
            .uri("/v1/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(ChatResponse.class);
    }

    public Flux<String> chatStream(ChatRequest request) {
        // 流式响应 — WebFlux
        return WebClient.create(properties.baseUrl())
            .post()
            .uri("/v1/chat/completions")
            .header("Authorization", "Bearer " + properties.apiKey())
            .bodyValue(request)
            .retrieve()
            .bodyToFlux(String.class)
            .filter(line -> line.startsWith("data: ") && !line.equals("data: [DONE]"))
            .map(line -> {
                try {
                    var node = objectMapper.readTree(line.substring(6));
                    return node.path("choices").get(0)
                        .path("delta").path("content").asText("");
                } catch (Exception e) {
                    return "";
                }
            });
    }

    public List<Float> embed(String text) {
        // Embedding 调用
        // ...
    }
}
```

### 2.3 对话服务

```java
// ConversationService.java
@Service
public class ConversationService {

    private final AiClient aiClient;
    private final RedisTemplate<String, ChatMessage> redisTemplate;
    private final AiProperties properties;

    private static final String HISTORY_PREFIX = "conv:history:";

    public String chat(String sessionId, String userMessage) {
        // 加载对话历史
        List<ChatMessage> history = loadHistory(sessionId);
        history.add(new ChatMessage("user", userMessage));

        // 构建请求
        var request = ChatRequest.builder()
            .model(properties.defaultModel())
            .messages(history)
            .temperature(0.7)
            .build();

        // 调用 LLM
        var response = aiClient.chat(request);
        String reply = response.choices().get(0).message().content();

        // 保存历史
        history.add(new ChatMessage("assistant", reply));
        saveHistory(sessionId, history);

        return reply;
    }

    public SseEmitter chatStream(String sessionId, String message) {
        SseEmitter emitter = new SseEmitter(60_000L);

        CompletableFuture.runAsync(() -> {
            try {
                var history = loadHistory(sessionId);
                history.add(new ChatMessage("user", message));
                var request = ChatRequest.builder()
                    .model(properties.defaultModel())
                    .messages(history)
                    .stream(true)
                    .build();

                StringBuilder fullReply = new StringBuilder();

                aiClient.chatStream(request)
                    .doOnNext(token -> {
                        try {
                            emitter.send(SseEmitter.event().data(token));
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                        fullReply.append(token);
                    })
                    .doOnComplete(() -> {
                        history.add(new ChatMessage("assistant", fullReply.toString()));
                        saveHistory(sessionId, history);
                        emitter.complete();
                    })
                    .doOnError(emitter::completeWithError)
                    .subscribe();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private List<ChatMessage> loadHistory(String sessionId) {
        var key = HISTORY_PREFIX + sessionId;
        var cached = redisTemplate.opsForList().range(key, 0, -1);
        return cached != null ? new ArrayList<>(cached) : new ArrayList<>();
    }

    private void saveHistory(String sessionId, List<ChatMessage> history) {
        var key = HISTORY_PREFIX + sessionId;
        // 保留最近 N 条
        int maxHistory = properties.conversation().maxHistory();
        if (history.size() > maxHistory) {
            history = history.subList(history.size() - maxHistory, history.size());
        }
        redisTemplate.delete(key);
        redisTemplate.opsForList().rightPushAll(key, history);
        redisTemplate.expire(key, Duration.ofSeconds(properties.conversation().cacheTtl()));
    }
}
```

### 2.4 Controller 层

```java
// ChatController.java
@RestController
@RequestMapping("/api/ai")
public class ChatController {

    private final ConversationService conversationService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequestDto request,
            @RequestHeader("X-Session-Id") String sessionId) {
        String reply = conversationService.chat(sessionId, request.message());
        return ResponseEntity.ok(new ChatResponse(reply));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @RequestBody ChatRequestDto request,
            @RequestHeader("X-Session-Id") String sessionId) {
        return conversationService.chatStream(sessionId, request.message());
    }
}

// DTO
public record ChatRequestDto(String message) {}
public record ChatResponse(String reply, List<String> sources) {
    public ChatResponse(String reply) {
        this(reply, List.of());
    }
}
```

---

## 3. Spring AI 方案

Spring AI 提供了更原生的集成方式（类似 Spring Data 的设计）：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
```

```java
@RestController
public class SpringAiController {

    private final ChatClient chatClient;

    public SpringAiController(ChatClient.Builder builder) {
        this.chatClient = builder
            .defaultSystem("你是 Java 技术专家")
            .build();
    }

    @GetMapping("/ai/generate")
    public String generate(@RequestParam String message) {
        return chatClient.prompt()
            .user(message)
            .call()
            .content();
    }

    @GetMapping(value = "/ai/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String message) {
        return chatClient.prompt()
            .user(message)
            .stream()
            .content();
    }
}
```

---

## 4. 数据库设计

```sql
-- AI 对话记录表
CREATE TABLE ai_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL,
    role VARCHAR(16) NOT NULL,       -- 'user' | 'assistant'
    content TEXT NOT NULL,
    model VARCHAR(64),
    tokens INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id)
);

-- 用户反馈表
CREATE TABLE ai_feedback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT,
    rating TINYINT,                   -- 1-5 评分
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES ai_conversation(id)
);

-- AI 调用日志（成本追踪）
CREATE TABLE ai_call_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    model VARCHAR(64),
    input_tokens INT,
    output_tokens INT,
    latency_ms INT,
    cost_micro_yuan BIGINT,          -- 成本（微元，1元=1000000）
    status VARCHAR(16),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 5. 异步任务处理

```java
// AiTaskService.java
@Service
@Slf4j
public class AiTaskService {

    private final AiClient aiClient;
    private final RabbitTemplate rabbitTemplate;

    public String submitAsyncTask(String prompt) {
        var taskId = UUID.randomUUID().toString();

        rabbitTemplate.convertAndSend("ai.tasks", "task.new", new AiTask(taskId, prompt));
        log.info("AI task submitted: taskId={}", taskId);

        return taskId;
    }

    @RabbitListener(queues = "ai.tasks")
    public void processTask(AiTask task) {
        log.info("Processing task: {}", task.taskId());
        try {
            var result = aiClient.chat(ChatRequest.simple(task.prompt()));
            rabbitTemplate.convertAndSend("ai.results", "task.completed",
                new AiTaskResult(task.taskId(), result.content(), "success"));
        } catch (Exception e) {
            log.error("Task failed: {}", task.taskId(), e);
            rabbitTemplate.convertAndSend("ai.results", "task.failed",
                new AiTaskResult(task.taskId(), null, "failed: " + e.getMessage()));
        }
    }
}
```

---

## 6. 部署配置

```dockerfile
# Dockerfile
FROM eclipse-temurin:21-jre-alpine
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx512m", "-jar", "/app.jar"]
```

```yaml
# docker-compose.yml
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - AI_API_KEY=${AI_API_KEY}
      - SPRING_REDIS_HOST=redis
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/aiapp
    depends_on:
      - redis
      - mysql

  redis:
    image: redis:7-alpine
    volumes:
      - redis_data:/data

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: aiapp
    volumes:
      - mysql_data:/var/lib/mysql

volumes:
  redis_data:
  mysql_data:
```

---

## 7. 关键决策检查清单

| 决策点 | 选择 | 原因 |
|--------|------|------|
| HTTP 客户端 | RestClient (Spring 6.1+) | 原生支持，无需第三方依赖 |
| 流式响应 | WebFlux / SseEmitter | WebFlux 适合全响应式栈，SseEmitter 适合 Servlet |
| 对话缓存 | Redis List | 支持 TTL，原子操作 |
| 异步任务 | RabbitMQ | 持久化、可靠、支持延迟队列 |
| 配置管理 | 环境变量 + application.yml | API Key 不入库、不提交 |
