# Java 生态集成实践

> Spring AI 和 LangChain4j 都内置 OpenAI 兼容客户端——改一个 base_url，就能接入任何兼容接口。本文提供完整可运行的 Java 代码示例。

---

## 📚 目录

1. [Java AI 框架的兼容策略](#1-java-ai-框架的兼容策略)
2. [Spring AI 集成 OpenAI 兼容接口](#2-spring-ai-集成-openai-兼容接口)
3. [LangChain4j 集成 OpenAI 兼容接口](#3-langchain4j-集成-openai-兼容接口)
4. [直接 HTTP 调用（无框架）](#4-直接-http-调用无框架)
5. [声明式客户端（Retrofit / Feign）](#5-声明式客户端retrofit--feign)
6. [多模型动态切换实现](#6-多模型动态切换实现)
7. [跨模型可移植代码最佳实践](#7-跨模型可移植代码最佳实践)

---

## 1. Java AI 框架的兼容策略

### 1.1 两大框架的兼容方式

```text
Spring AI
  └── spring-ai-openai-spring-boot-starter
      └── 内置 OpenAI 兼容客户端
          └── 修改 spring.ai.openai.base-url 即切换后端

LangChain4j
  └── langchain4j-open-ai
      └── OpenAiChatModel / OpenAiStreamingChatModel
          └── .baseUrl("http://...") 即切换后端
```

### 1.2 配置对照表

| 模型后端 | base_url | 说明 |
|------|------|------|
| OpenAI 官方 | `https://api.openai.com` | 默认 |
| DeepSeek | `https://api.deepseek.com` | 完全兼容 |
| 智谱 GLM | `https://open.bigmodel.cn/api/paas/v4` | 基本兼容 |
| 通义千问 | `https://dashscope.aliyuncs.com/compatible-mode/v1` | 需加 compatible-mode |
| Ollama 本地 | `http://localhost:11434` | api-key 随便填 |
| vLLM 本地 | `http://localhost:8000` | 需设置 --api-key |
| One-API 网关 | `http://your-gateway:3000` | 统一入口 |
| LiteLLM Proxy | `http://localhost:4000` | 统一入口 |

---

## 2. Spring AI 集成 OpenAI 兼容接口

### 2.1 依赖配置

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0-M6</version>
</dependency>
```

### 2.2 基础配置（application.yml）

```yaml
spring:
  ai:
    openai:
      # ===== 核心配置：改这两个就能切换模型 =====
      base-url: https://api.deepseek.com       # 目标端点
      api-key: ${DEEPSEEK_API_KEY}             # API Key

      # ===== 模型配置 =====
      chat:
        enabled: true
        options:
          model: deepseek-chat                  # 模型名
          temperature: 0.7
          max-tokens: 4096

      # ===== 嵌入模型配置 =====
      embedding:
        enabled: true
        options:
          model: text-embedding-3-small

      # ===== 多模态配置 =====
      image:
        enabled: false                          # DeepSeek 不支持图片
```

### 2.3 Chat Service 示例

```java
@Service
public class ChatService {

    private final OpenAiChatModel chatModel;

    // 构造器注入
    public ChatService(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 基础对话
     */
    public String chat(String userMessage) {
        return chatModel.call(userMessage);
    }

    /**
     * 带 System Prompt 的对话
     */
    public String chatWithSystem(String userMessage) {
        return chatModel.call(
            new Prompt(
                userMessage,
                OpenAiChatOptions.builder()
                    .model("deepseek-chat")
                    .temperature(0.3)
                    .build()
            )
        ).getResult().getOutput().getText();
    }

    /**
     * 多轮对话
     */
    public String multiTurnChat(List<Message> history, String newMessage) {
        List<Message> messages = new ArrayList<>(history);
        messages.add(new UserMessage(newMessage));

        Prompt prompt = new Prompt(messages);
        return chatModel.call(prompt).getResult().getOutput().getText();
    }

    /**
     * 流式输出（SSE）
     */
    public Flux<ChatResponse> streamChat(String userMessage) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .model("deepseek-chat")
            .build();

        Prompt prompt = new Prompt(userMessage, options);
        return chatModel.stream(prompt);
    }
}
```

### 2.4 流式输出的 Controller

```java
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * SSE 流式接口
     * 前端用 EventSource 或 fetch + ReadableStream 消费
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(
            @RequestParam String message) {

        return chatService.streamChat(message)
            .map(response -> {
                String content = response.getResult()
                    .getOutput().getText();
                return ServerSentEvent.<String>builder()
                    .data(content)
                    .build();
            });
    }
}
```

### 2.5 多环境配置

```yaml
# application-dev.yml — 开发环境用本地 Ollama
spring:
  ai:
    openai:
      base-url: http://localhost:11434
      api-key: ollama
      chat:
        options:
          model: qwen2.5:7b

---
# application-prod.yml — 生产环境用 DeepSeek
spring:
  ai:
    openai:
      base-url: https://api.deepseek.com
      api-key: ${DEEPSEEK_API_KEY}
      chat:
        options:
          model: deepseek-chat

---
# application-fallback.yml — 兜底用 GPT-4o
spring:
  ai:
    openai:
      base-url: https://api.openai.com
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o
```

### 2.6 Tool Calling 实现

```java
@Component
public class WeatherTools {

    @Autowired
    private WeatherService weatherService;

    /**
     * 定义为 Tool，Spring AI 自动生成 OpenAI 格式的 tool 定义
     */
    @Tool(description = "获取指定城市的天气信息")
    public String getWeather(
            @ToolParam(description = "城市名称") String city) {
        return weatherService.getWeather(city);
    }

    @Tool(description = "计算两个数字的和")
    public double add(
            @ToolParam(description = "第一个数字") double a,
            @ToolParam(description = "第二个数字") double b) {
        return a + b;
    }
}

// 使用
@Service
public class ToolCallingService {

    private final OpenAiChatModel chatModel;
    private final WeatherTools weatherTools;

    public String chatWithTools(String userMessage) {
        // Spring AI 自动处理 Tool Calling 的完整循环：
        // 1. 发送请求（带 tool 定义）
        // 2. 收到 tool_calls → 自动执行 → 发回结果
        // 3. 收到最终回复 → 返回
        return chatModel.call(
            new Prompt(
                userMessage,
                OpenAiChatOptions.builder()
                    .tools(List.of(
                        ToolCallback.from(weatherTools)
                    ))
                    .build()
            )
        ).getResult().getOutput().getText();
    }
}
```

---

## 3. LangChain4j 集成 OpenAI 兼容接口

### 3.1 依赖配置

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>1.0.0-beta1</version>
</dependency>
```

### 3.2 基础使用

```java
public class LangChain4jExample {

    public static void main(String[] args) {
        // ===== 方式 1：Builder 模式 =====
        OpenAiChatModel model = OpenAiChatModel.builder()
            .baseUrl("https://api.deepseek.com/v1")
            .apiKey(System.getenv("DEEPSEEK_API_KEY"))
            .modelName("deepseek-chat")
            .temperature(0.7)
            .maxTokens(4096)
            .timeout(Duration.ofSeconds(120))
            .build();

        String answer = model.chat("用 Java 解释面向对象的三大特性");
        System.out.println(answer);

        // ===== 方式 2：流式对话 =====
        OpenAiStreamingChatModel streamingModel = 
            OpenAiStreamingChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .build();

        streamingModel.chat("用 Java 写一个单例模式",
            new StreamingChatModelHandler() {
                @Override
                public void onNext(String token) {
                    System.out.print(token);  // 逐 token 打印
                }

                @Override
                public void onComplete(ChatCompletionResponse response) {
                    System.out.println("\n[流式结束]");
                }

                @Override
                public void onError(Throwable error) {
                    System.err.println("流式错误: " + error.getMessage());
                }
            });
    }
}
```

### 3.3 Spring Boot 集成

```yaml
# application.yml
langchain4j:
  open-ai:
    chat-model:
      base-url: https://api.deepseek.com/v1
      api-key: ${DEEPSEEK_API_KEY}
      model-name: deepseek-chat
      temperature: 0.7
      max-tokens: 4096
      timeout: 120s
```

```java
@RestController
public class ChatController {

    @Autowired
    private OpenAiChatModel chatModel;

    @Autowired
    private OpenAiStreamingChatModel streamingChatModel;

    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody ChatRequest request) {
        String response = chatModel.chat(
            SystemMessage.from("你是 Java 专家"),
            UserMessage.from(request.getMessage())
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/chat/stream", produces = TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam String message) {
        // 将 LangChain4j 的流式回调适配为 Flux
        return Flux.create(sink -> {
            streamingChatModel.chat(message, new StreamingChatModelHandler() {
                @Override
                public void onNext(String token) {
                    sink.next(token);
                }
                @Override
                public void onComplete(ChatCompletionResponse r) {
                    sink.complete();
                }
                @Override
                public void onError(Throwable error) {
                    sink.error(error);
                }
            });
        });
    }
}
```

### 3.4 LangChain4j Tool Calling

```java
// 定义工具
public class Calculator {
    @Tool("计算两个数的和")
    public double add(
            @P("第一个数") double a,
            @P("第二个数") double b) {
        return a + b;
    }

    @Tool("计算两个数的乘积")
    public double multiply(
            @P("第一个数") double a,
            @P("第二个数") double b) {
        return a * b;
    }
}

// 使用
OpenAiChatModel model = OpenAiChatModel.builder()
    .baseUrl("https://api.deepseek.com/v1")
    .apiKey("sk-xxx")
    .modelName("deepseek-chat")
    .build();

// 创建带工具的 Agent
AiServices<Assistant> aiService = AiServices.builder(Assistant.class)
    .chatLanguageModel(model)
    .tools(new Calculator())  // 注册工具
    .build();

String result = aiService.chat("123 + 456 等于多少？");
// LangChain4j 自动：发请求 → 收到 tool_calls → 执行 add(123,456)
// → 发回结果 → 收到 "123 + 456 = 579"
```

---

## 4. 直接 HTTP 调用（无框架）

### 4.1 使用 OkHttp

```java
public class OpenAiDirectClient {

    private final OkHttpClient client;
    private final String baseUrl;
    private final String apiKey;
    private final ObjectMapper mapper;

    public OpenAiDirectClient(String baseUrl, String apiKey) {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.mapper = new ObjectMapper();
    }

    /**
     * 非流式对话
     */
    public ChatCompletionResponse chat(
            String model, List<Message> messages,
            double temperature, int maxTokens) throws IOException {

        Map<String, Object> body = Map.of(
            "model", model,
            "messages", messages,
            "temperature", temperature,
            "max_tokens", maxTokens,
            "stream", false
        );

        Request request = new Request.Builder()
            .url(baseUrl + (baseUrl.endsWith("/") ? "" : "/") 
                 + "chat/completions")
            .post(RequestBody.create(
                mapper.writeValueAsString(body),
                MediaType.parse("application/json")))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API Error: " + response.code()
                    + " " + response.body().string());
            }
            return mapper.readValue(
                response.body().string(), ChatCompletionResponse.class);
        }
    }

    /**
     * 流式对话 — 返回每个 token
     */
    public void chatStream(
            String model, List<Message> messages,
            Consumer<String> onToken,
            Consumer<Throwable> onError) {

        Map<String, Object> body = Map.of(
            "model", model,
            "messages", messages,
            "stream", true
        );

        // 使用 OkHttp SSE 或 WebClient 实现
        // 核心：解析 "data: {...}" 行
        // 跳过 "data: [DONE]" 行
        // 从 choices[0].delta.content 获取增量文本
    }
}
```

### 4.2 使用 WebClient（响应式）

```java
@Service
public class ReactiveChatService {

    private final WebClient webClient;

    public ReactiveChatService(
            @Value("${openai.base-url}") String baseUrl,
            @Value("${openai.api-key}") String apiKey) {
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .build();
    }

    /**
     * 流式输出：返回 Flux<String>，每个元素是一个 token
     */
    public Flux<String> chatStream(String model, String userMessage) {
        Map<String, Object> body = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "user", "content", userMessage)
            ),
            "stream", true
        );

        return webClient.post()
            .uri("/chat/completions")
            .bodyValue(body)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .retrieve()
            .bodyToFlux(String.class)          // 原始 SSE 行
            .filter(line -> !line.equals("[DONE]"))
            .filter(line -> line.startsWith("data: "))
            .map(line -> line.substring(6))    // 去掉 "data: " 前缀
            .map(this::extractToken)            // 提取 content
            .filter(Objects::nonNull);
    }

    private String extractToken(String json) {
        try {
            JsonNode node = new ObjectMapper().readTree(json);
            JsonNode delta = node.path("choices").get(0).path("delta");
            JsonNode content = delta.path("content");
            return content.isNull() ? null : content.asText();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 更优雅的方式：使用 Spring 6 的 RestClient
     */
    public String chatSync(String model, String userMessage) {
        Map<String, Object> body = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "user", "content", userMessage)
            ),
            "stream", false
        );

        return webClient.post()
            .uri("/chat/completions")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(ChatCompletionResponse.class)
            .map(resp -> resp.getChoices().get(0)
                .getMessage().getContent())
            .block();
    }
}
```

---

## 5. 声明式客户端（Retrofit / Feign）

### 5.1 Retrofit 示例

```java
// API 接口定义
public interface OpenAiApi {

    @POST("chat/completions")
    Call<CompletionResponse> chat(@Body CompletionRequest request);

    @Streaming
    @POST("chat/completions")
    @Headers("Accept: text/event-stream")
    Call<ResponseBody> chatStream(@Body CompletionRequest request);
}

// Request/Response DTO
@Data
public class CompletionRequest {
    private String model;
    private List<Message> messages;
    private double temperature = 0.7;
    @JsonProperty("max_tokens")
    private int maxTokens = 4096;
    private boolean stream = false;
}

// 客户端工厂
public class OpenAiClientFactory {

    public static OpenAiApi create(String baseUrl, String apiKey) {
        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                Request request = chain.request().newBuilder()
                    .header("Authorization", "Bearer " + apiKey)
                    .build();
                return chain.proceed(request);
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();

        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/")
            .client(client)
            .addConverterFactory(JacksonConverterFactory.create())
            .build();

        return retrofit.create(OpenAiApi.class);
    }
}
```

### 5.2 Feign 示例

```java
@FeignClient(
    name = "openai",
    url = "${openai.base-url}",
    configuration = OpenAiFeignConfig.class
)
public interface OpenAiFeignClient {

    @PostMapping("/chat/completions")
    CompletionResponse chat(@RequestBody CompletionRequest request);
}

@Configuration
public class OpenAiFeignConfig {
    @Bean
    public RequestInterceptor apiKeyInterceptor(
            @Value("${openai.api-key}") String apiKey) {
        return template -> template.header(
            "Authorization", "Bearer " + apiKey);
    }
}
```

---

## 6. 多模型动态切换实现

### 6.1 核心思路

```java
/**
 * 多模型路由器：运行时根据配置切换模型后端
 */
@Service
public class ModelRouterService {

    private final Map<String, OpenAiChatModel> modelCache = 
        new ConcurrentHashMap<>();

    @Autowired
    private ModelConfigRepository configRepository;

    /**
     * 根据业务场景获取对应的模型
     */
    public OpenAiChatModel getModel(String scene) {
        ModelConfig config = configRepository.findByScene(scene);
        return modelCache.computeIfAbsent(scene, k -> buildModel(config));
    }

    private OpenAiChatModel buildModel(ModelConfig config) {
        return new OpenAiChatModel(
            OpenAiApi.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .build(),
            OpenAiChatOptions.builder()
                .model(config.getModelName())
                .temperature(config.getTemperature())
                .maxTokens(config.getMaxTokens())
                .build()
        );
    }

    /**
     * 智能路由：根据消息内容选择模型
     */
    public OpenAiChatModel route(String userMessage) {
        if (userMessage.length() < 100) {
            return getModel("simple");      // 简单问题用便宜模型
        }
        if (userMessage.contains("review") || userMessage.contains("审查")) {
            return getModel("code-review"); // 代码审查用高质量模型
        }
        return getModel("default");         // 默认模型
    }
}
```

### 6.2 配置驱动的路由表

```yaml
app:
  models:
    - scene: simple
      base-url: http://localhost:11434
      api-key: ollama
      model: qwen2.5:7b
      temperature: 0.0
      priority: 1

    - scene: default
      base-url: https://api.deepseek.com
      api-key: ${DEEPSEEK_KEY}
      model: deepseek-chat
      temperature: 0.7
      priority: 2

    - scene: code-review
      base-url: https://api.openai.com
      api-key: ${OPENAI_KEY}
      model: gpt-4o
      temperature: 0.1
      priority: 3
```

---

## 7. 跨模型可移植代码最佳实践

### 7.1 安全区原则

```text
✅ 安全区（所有兼容接口都支持）：
   ├── /v1/chat/completions 基础调用
   ├── user + assistant 消息
   ├── Streaming (SSE)
   ├── temperature, max_tokens, top_p
   └── stop 序列

⚠️ 条件区（大部分支持，需测试）：
   ├── system message
   ├── JSON Mode (response_format: json_object)
   ├── Tool Calling
   └── seed 参数

❌ 风险区（少数支持，建议避免）：
   ├── json_schema (Structured Output) — 仅 OpenAI
   ├── n > 1 — 大部分不支持
   ├── logprobs — vLLM 部分支持
   └── Vision — 仅多模态模型
```

### 7.2 可移植代码模板

```java
/**
 * 可移植的 LLM 调用 — 使用安全区特性
 */
public class PortableChatService {

    private final OpenAiChatModel model;

    /**
     * 仅使用安全区 API：
     * - messages (user + assistant)
     * - temperature, max_tokens
     * - stream: false
     *
     * 这段代码在所有兼容接口上都能跑。
     */
    public String portableChat(String userMessage) {
        return model.call(
            new Prompt(
                new UserMessage(userMessage),
                OpenAiChatOptions.builder()
                    .model("deepseek-chat")     // ← 唯一需要改的地方
                    .temperature(0.7)
                    .maxTokens(4096)
                    .build()
            )
        ).getResult().getOutput().getText();
    }
}
```

### 7.3 能力检测模式

```java
/**
 * 在运行时检测模型是否支持某个特性，优雅降级
 */
public class CapabilityAwareService {

    /**
     * 尝试使用 Tool Calling，不支持时降级为纯文本模式
     */
    public String smartChat(String userMessage, List<ToolCallback> tools) {
        try {
            // 先尝试 Tool Calling
            return callWithTools(userMessage, tools);
        } catch (Exception e) {
            if (isNotSupportedError(e)) {
                // 降级：用 prompt 描述工具，让模型输出 JSON
                log.warn("Tool Calling 不支持，降级到 prompt 模式");
                return callWithPromptEmbeddedTools(userMessage, tools);
            }
            throw e;
        }
    }

    private boolean isNotSupportedError(Exception e) {
        String msg = e.getMessage().toLowerCase();
        return msg.contains("tool") && 
               (msg.contains("not supported") || msg.contains("unsupported"));
    }

    /**
     * 降级方案：将工具描述嵌入 system prompt，让模型输出 JSON
     */
    private String callWithPromptEmbeddedTools(
            String userMessage, List<ToolCallback> tools) {
        String toolDesc = tools.stream()
            .map(t -> String.format("- %s: %s", 
                t.getToolDefinition().name(),
                t.getToolDefinition().description()))
            .collect(Collectors.joining("\n"));

        String systemPrompt = String.format(
            "你可以使用以下工具。如果需要使用工具，" +
            "请返回 JSON 格式：{\"tool\": \"工具名\", \"args\": {...}}\n\n%s",
            toolDesc
        );

        return model.call(new Prompt(
            List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userMessage)
            ),
            OpenAiChatOptions.builder()
                .responseFormat(
                    new ResponseFormat(ResponseFormat.Type.JSON_OBJECT))
                .build()
        )).getResult().getOutput().getText();
    }
}
```

---

> 🎯 **核心要点**：Java 生态中，Spring AI 和 LangChain4j 都提供了成熟的 OpenAI 兼容客户端。核心技巧是**多用配置少写代码**——切换模型只需改 yaml/base_url，不碰业务代码。生产环境建议配合 One-API 网关做统一管理和灰度切换。

---

**下一模块**：[06 - 流式输出与生产最佳实践](./06-流式输出与生产最佳实践.md)  
**返回总览**：[00 - OpenAI 兼容接口总览](./00-OpenAI兼容接口总览.md)
