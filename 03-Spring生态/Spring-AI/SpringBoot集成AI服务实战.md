# Spring Boot 集成 AI 服务实战

> **核心认知**：将 AI 能力作为基础设施嵌入后端，用 Spring Boot 封装 RESTful API，通过 Redis 缓存、MySQL 持久化、Docker 部署，构成企业级 AI 中间层。
> **前置条件**：熟悉 Spring Boot 基础开发、RESTful API 设计

---

## 1. 架构总览

```
┌─────────────────────────────────────────────────────┐
│                     前端 (Web/App)                    │
└────────────────────┬────────────────────────────────┘
                     │ HTTP / SSE / WebSocket
┌────────────────────▼────────────────────────────────┐
│               Spring Boot AI 中间层                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐            │
│  │Controller│ │ Service  │ │  Cache   │            │
│  │  REST    │ │  AI调用   │ │  Redis   │            │
│  │  SSE     │ │  Prompt   │ │  Caffeine│            │
│  └──────────┘ └──────────┘ └──────────┘            │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐            │
│  │  Auth    │ │  Rate    │ │  Monitor │            │
│  │  JWT     │ │  Limiter │ │  Metrics │            │
│  └──────────┘ └──────────┘ └──────────┘            │
└────────────────────┬────────────────────────────────┘
                     │
     ┌───────────────┼───────────────┐
     ▼               ▼               ▼
┌─────────┐  ┌──────────┐  ┌──────────┐
│ DeepSeek│  │  Ollama  │  │  Qwen    │
│  API    │  │  Local   │  │  API     │
└─────────┘  └──────────┘  └──────────┘
```

---

## 2. 项目结构

```
src/main/java/com/example/aiservice/
├── AiServiceApplication.java
├── config/
│   ├── AIConfig.java           # AI 客户端配置
│   ├── RedisConfig.java        # Redis 配置
│   └── RateLimitConfig.java    # 限流配置
├── controller/
│   ├── ChatController.java     # 对话接口
│   └── AdminController.java    # 管理接口
├── service/
│   ├── LLMService.java         # LLM 调用封装
│   ├── PromptService.java      # Prompt 模板管理
│   ├── ConversationService.java # 对话历史管理
│   └── CostService.java        # 成本统计
├── model/
│   ├── ChatRequest.java
│   ├── ChatResponse.java
│   ├── Conversation.java
│   └── CostRecord.java
├── repository/
│   └── ConversationRepository.java
└── filter/
    ├── RateLimitFilter.java
    └── SensitiveWordFilter.java
```

---

## 3. 核心代码实现

### 3.1 AI 客户端配置

```java
// config/AIConfig.java
@Configuration
public class AIConfig {

    @Bean
    public RestClient llmRestClient(
            @Value("${ai.deepseek.base-url}") String baseUrl,
            @Value("${ai.deepseek.api-key}") String apiKey) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .requestInterceptor(new MetricsInterceptor()) // 埋点
                .build();
    }

    @Bean
    public RestClient ollamaRestClient(
            @Value("${ai.ollama.base-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
```

### 3.2 LLM 服务封装

```java
// service/LLMService.java
@Service
@Slf4j
public class LLMService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;

    @Value("${ai.deepseek.model:deepseek-chat}")
    private String model;

    public LLMService(RestClient restClient, CacheManager cacheManager) {
        this.restClient = restClient;
        this.cacheManager = cacheManager;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 同步对话（带缓存）
     */
    public String chat(String prompt, String conversationId) {
        String cacheKey = "chat:" + DigestUtils.md5Hex(prompt);
        Cache cache = cacheManager.getCache("llm-responses");
        String cached = cache.get(cacheKey, String.class);
        if (cached != null) {
            return cached;
        }

        Map<String, Object> request = buildRequest(prompt);
        Map<String, Object> response = restClient.post()
                .uri("/v1/chat/completions")
                .body(request)
                .retrieve()
                .body(Map.class);

        String content = extractContent(response);
        cache.put(cacheKey, content);  // 缓存结果
        return content;
    }

    /**
     * 流式对话（SSE）
     */
    public Flux<String> chatStream(String prompt) {
        Map<String, Object> request = buildRequest(prompt);
        request.put("stream", true);

        WebClient webClient = WebClient.builder()
                .baseUrl(restClient.toString())
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();

        return webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> line.startsWith("data: ") && !line.contains("[DONE]"))
                .map(this::parseStreamChunk)
                .onErrorResume(e -> {
                    log.error("Stream error", e);
                    return Flux.just("[流式输出中断]");
                });
    }

    private Map<String, Object> buildRequest(String prompt) {
        List<Map<String, String>> messages = List.of(
                Map.of("role", "user", "content", prompt)
        );
        return Map.of(
                "model", model,
                "messages", messages,
                "temperature", 0.7,
                "max_tokens", 2048
        );
    }

    private String extractContent(Map<String, Object> response) {
        List<Map<String, Object>> choices = (List) response.get("choices");
        Map<String, Object> message = (Map) choices.get(0).get("message");
        return (String) message.get("content");
    }
}
```

### 3.3 REST Controller

```java
// controller/ChatController.java
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final LLMService llmService;
    private final ConversationService conversationService;
    private final RateLimiter rateLimiter;

    /**
     * 同步对话
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @RequestBody @Valid ChatRequest request,
            @RequestHeader("X-User-Id") String userId) {

        // 限流检查
        if (!rateLimiter.tryAcquire(userId)) {
            return ResponseEntity.status(429)
                    .body(ChatResponse.error("请求过于频繁，请稍后再试"));
        }

        // 获取/创建对话历史
        Conversation conv = conversationService.getOrCreate(
                request.getConversationId(), userId);

        // 组装带历史的 Prompt
        String fullPrompt = conversationService.buildPrompt(
                conv, request.getMessage());

        // 调用 LLM
        String response = llmService.chat(fullPrompt, conv.getId());

        // 保存对话历史
        conversationService.appendMessages(conv,
                request.getMessage(), response);

        return ResponseEntity.ok(
                ChatResponse.success(response, conv.getId()));
    }

    /**
     * 流式对话（SSE）
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(
            @RequestParam String message,
            @RequestParam(required = false) String conversationId) {

        return llmService.chatStream(message)
                .map(content -> ServerSentEvent.<String>builder()
                        .data(content)
                        .build())
                .concatWith(Flux.just(
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("[DONE]")
                                .build()));
    }
}
```

### 3.4 对话历史管理（Redis）

```java
// service/ConversationService.java
@Service
public class ConversationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final int MAX_HISTORY = 20; // 保留最近20轮

    public String buildPrompt(Conversation conv, String newMessage) {
        List<Map<String, String>> history = getHistory(conv.getId());

        StringBuilder prompt = new StringBuilder();
        prompt.append("系统: 你是一个Java技术助手\n");

        // 最近 N 轮对话作为上下文
        int start = Math.max(0, history.size() - MAX_HISTORY);
        for (Map<String, String> msg : history.subList(start, history.size())) {
            prompt.append(msg.get("role").equals("user") ? "用户: " : "助手: ");
            prompt.append(msg.get("content")).append("\n");
        }
        prompt.append("用户: ").append(newMessage);

        return prompt.toString();
    }

    private List<Map<String, String>> getHistory(String conversationId) {
        String key = "conv:" + conversationId + ":messages";
        List<Object> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null) return List.of();
        return raw.stream()
                .map(o -> (Map<String, String>) o)
                .toList();
    }

    public void appendMessages(Conversation conv, String userMsg, String aiMsg) {
        String key = "conv:" + conv.getId() + ":messages";
        redisTemplate.opsForList().rightPushAll(key,
                Map.of("role", "user", "content", userMsg),
                Map.of("role", "assistant", "content", aiMsg));

        // 过期时间：24小时
        redisTemplate.expire(key, Duration.ofHours(24));
    }
}
```

### 3.5 Prompt 模板管理（MySQL）

```java
// entity/PromptTemplate.java
@Entity
@Table(name = "prompt_templates")
public class PromptTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;          // 模板名称

    @Column(nullable = false, length = 5000)
    private String template;      // 模板内容（支持 {variable} 占位符）

    @Column(length = 500)
    private String description;

    private String category;      // 分类：code_review, sql_optimize, doc_gen

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

// repository/PromptTemplateRepository.java
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {
    Optional<PromptTemplate> findByName(String name);
    List<PromptTemplate> findByCategory(String category);
}

// service/PromptService.java
@Service
public class PromptService {

    private final PromptTemplateRepository repository;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String render(String templateName, Map<String, String> variables) {
        String template = cache.computeIfAbsent(templateName, name -> {
            PromptTemplate pt = repository.findByName(name)
                    .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + name));
            return pt.getTemplate();
        });

        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
```

---

## 4. 安全与限流

### 4.1 敏感词过滤

```java
// filter/SensitiveWordFilter.java
@Component
public class SensitiveWordFilter implements Filter {

    private final Set<String> blockedWords = Set.of(
        "忽略之前的指令", "ignore previous instructions",
        "你是GPT", "system prompt", "<|im_start|>"
    );

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;

        if ("POST".equals(request.getMethod())) {
            // 读取 body 检查敏感词
            CachedBodyHttpServletRequest cachedRequest =
                    new CachedBodyHttpServletRequest(request);
            String body = new String(cachedRequest.getInputStream().readAllBytes());

            for (String word : blockedWords) {
                if (body.toLowerCase().contains(word.toLowerCase())) {
                    HttpServletResponse response = (HttpServletResponse) res;
                    response.setStatus(400);
                    response.getWriter().write("{\"error\":\"输入包含不合法内容\"}");
                    return;
                }
            }

            chain.doFilter(cachedRequest, res);
            return;
        }

        chain.doFilter(req, res);
    }
}
```

### 4.2 访问频率限制

```java
// filter/RateLimitFilter.java 或使用 Spring Interceptor
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final LoadingCache<String, AtomicInteger> counterCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                @Override
                public AtomicInteger load(String key) {
                    return new AtomicInteger(0);
                }
            });

    private static final int MAX_REQUESTS_PER_MINUTE = 20;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String userId = request.getHeader("X-User-Id");
        if (userId == null) {
            userId = request.getRemoteAddr();
        }

        int count = counterCache.getUnchecked(userId).incrementAndGet();
        if (count > MAX_REQUESTS_PER_MINUTE) {
            response.setStatus(429);
            response.getWriter().write("{\"error\":\"请求过于频繁\"}");
            return false;
        }

        return true;
    }
}
```

---

## 5. 配置文件

```yaml
# application.yml
server:
  port: 8080

spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: 6379
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:3306/ai_service
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:}

ai:
  deepseek:
    api-key: ${DEEPSEEK_API_KEY:}
    base-url: https://api.deepseek.com
    model: deepseek-chat
  ollama:
    base-url: http://localhost:11434
    model: qwen2:7b-q4_K_M
  cache:
    ttl-minutes: 30

rate-limit:
  requests-per-minute: 20
  burst: 5
```

---

## 6. Docker 部署

```dockerfile
# Dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/ai-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
```

```yaml
# docker-compose.yml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY}
      - REDIS_HOST=redis
      - DB_HOST=mysql
    depends_on:
      - redis
      - mysql
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_DATABASE: ai_service
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  ollama:
    image: ollama/ollama:latest
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]

volumes:
  redis_data:
  mysql_data:
  ollama_data:
```

---

## 快速调试检查清单

- [ ] API Key 是否正确配置在环境变量中？
- [ ] Redis 连接是否正常？`redis-cli ping`
- [ ] MySQL 表是否自动创建？（`spring.jpa.hibernate.ddl-auto: update`）
- [ ] 敏感词过滤器是否正常工作？（测试用 Prompt 注入语句）
- [ ] 限流是否生效？（短时间内连续请求应触发 429）
- [ ] Docker 部署后网络是否互通？（app → ollama:11434）
