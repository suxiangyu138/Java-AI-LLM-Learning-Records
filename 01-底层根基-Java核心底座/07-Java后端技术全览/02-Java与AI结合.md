# 02 — Java 与 AI 结合

> AI 是 2025-2026 年技术招聘的最大增量。懂 AI 集成的 Java 开发者，正在获得显著的薪资溢价。本文从实战角度出发，教你如何在 Java 项目中集成大语言模型（LLM）、构建 RAG 应用、使用向量数据库，以及如何将 AI 能力转化为面试竞争力。

---

## 目录

1. [为什么 Java 开发者需要学 AI](#一为什么-java-开发者需要学-ai)
2. [AI/ML 全景图：Java 开发者的视角](#二aiml-全景图java-开发者的视角)
3. [调用 LLM API 从 Java 开始](#三调用-llm-api-从-java-开始)
4. [LangChain4j 实战](#四langchain4j-实战)
5. [Spring AI 快速上手](#五spring-ai-快速上手)
6. [向量数据库与 RAG](#六向量数据库与-rag)
7. [Java 中的 ML 模型推理](#七java-中的-ml-模型推理)
8. [构建 AI 增强的简历项目](#八构建-ai-增强的简历项目)
9. [AI 编码助手与 Java 开发](#九ai-编码助手与-java-开发)
10. [职业定位与市场策略](#十职业定位与市场策略)

---

## 一、为什么 Java 开发者需要学 AI

### 1.1 市场趋势

```
传统 Java 开发竞争格局：
  ┌──────────────────────────────────────────────┐
  │  2024: 每个 Java 岗位平均收到 100+ 份简历    │
  │  2025: 需求从"会用 Spring Boot"转向          │
  │         "Spring Boot + 能集成 AI 能力"        │
  │  2026: AI 集成已从"加分项"变为"必备技能"      │
  └──────────────────────────────────────────────┘

AI 对 Java 开发的影响：
  1. 企业内部系统全面接入 AI（智能客服、知识库、代码助手）
  2. AI 应用后端首选 Java（稳定性、性能、生态）
  3. Python 做模型训练，Java 做服务部署 → 分工明确
  4. 云厂商提供大量 AI SDK，Java 是核心支持语言
```

### 1.2 Java 开发者做 AI 的独特优势

| 优势 | 说明 |
|------|------|
| **企业级能力** | Java 的并发、事务、安全、监控体系完善 |
| **生态成熟** | Maven 仓库有大量 AI 相关依赖 |
| **云原生支持** | Spring Cloud + AI 集成 = 企业级 AI 应用 |
| **性能稳定** | JVM GC 优化、长连接管理成熟 |
| **团队需求** | 现有 Java 团队需要 AI 能力，你的价值更高 |

### 1.3 Java + AI 的市场价值

```
薪资影响（2025-2026 市场数据）：
  纯 Java 开发者：                            ~15-25K/月
  Java + 基本 AI 集成（调 API）：               ~20-30K/月
  Java + AI 应用架构（RAG、微服务 AI）：        ~30-45K/月
  Java + AI Infra（推理部署、模型服务化）：      ~40-60K/月
```

---

## 二、AI/ML 全景图：Java 开发者的视角

### 2.1 AI 技术栈分层

```
┌─────────────────────────────────────────────────────────────┐
│                  应用层（你的战场）                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  AI Chatbot  │  │  RAG 知识库  │  │  AI 代码助手 │     │
│  │  (Spring)    │  │  (Spring)    │  │  (IntelliJ)  │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  智能搜索   │  │  内容生成   │  │  推荐系统   │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
├─────────────────────────────────────────────────────────────┤
│                 集成层（Java 主导）                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  LLM 客户端  │  │  向量数据库  │  │  提示词管理  │     │
│  │  LangChain4j │  │  pgvector/   │  │  Prompt      │     │
│  │  Spring AI   │  │  Milvus      │  │  Template    │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
├─────────────────────────────────────────────────────────────┤
│                 基础设施层（多语言）                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  LLM 服务    │  │  向量模型    │  │  ML 模型     │     │
│  │  OpenAI/     │  │  embedding   │  │  ONNX/       │     │
│  │  本地Ollama  │  │  模型        │  │  TensorFlow  │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
├─────────────────────────────────────────────────────────────┤
│                 模型训练层（Python 主导）                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  PyTorch     │  │  TensorFlow  │  │  HuggingFace │     │
│  │  模型训练    │  │  模型训练    │  │  模型库      │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Java 与 Python 的分工

| 场景 | 最佳语言 | 原因 |
|------|---------|------|
| **模型训练** | Python | PyTorch/TensorFlow 生态，Data Scientist 工具链 |
| **数据分析** | Python | Pandas, NumPy, Jupyter Notebook |
| **数据预处理** | Python/Java | 小数据 Python，大数据 Java (Spark) |
| **模型推理服务** | Java | 高并发、低延迟、企业级监控 |
| **AI 应用后端** | Java | Spring Boot 生态、事务管理、安全 |
| **API 聚合层** | Java | 微服务架构、负载均衡、熔断降级 |
| **向量数据库** | 各语言 SDK | Java 客户端成熟 |
| **RAG 流水线** | Java | LangChain4j / Spring AI |

### 2.3 Java + Python 混合架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Java 后端（Spring Boot）                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  API Gateway │  │  RAG Service │  │  Chat        │     │
│  │  (路由/鉴权) │  │  (Java)      │  │  Service     │     │
│  └──────────────┘  └──────┬───────┘  └──────┬───────┘     │
│                           │                  │             │
│                    ┌──────┴───────┐  ┌──────┴───────┐     │
│                    │  Python      │  │  LLM API     │     │
│                    │  Model       │  │  (HTTP)      │     │
│                    │  Server      │  └──────────────┘     │
│                    │  (Flask/Fast)│                        │
│                    └──────────────┘                        │
└─────────────────────────────────────────────────────────────┘

调用方式对比：
  1. 直接 HTTP 调用 LLM API（最简单）
  2. Java 通过 gRPC 调用 Python 模型服务（高性能）
  3. Java 通过 MQ 异步调用（高吞吐）
  4. Java 直接加载 ONNX 模型（低延迟，无 Python 依赖）
```

---

## 三、调用 LLM API 从 Java 开始

### 3.1 HTTP 客户端选择

在 Java 中调用 LLM API，本质上是发送 HTTP 请求。以下是选项对比：

| 客户端 | 优势 | 劣势 | 推荐场景 |
|--------|------|------|---------|
| **RestTemplate** | Spring Boot 内建，简单易用 | 阻塞式 IO | 简单调用，同步场景 |
| **WebClient** | 非阻塞，响应式 | 学习曲线 | 高并发、流式场景 |
| **OkHttp** | 连接池管理好，拦截器强大 | 外部依赖 | 性能敏感场景 |
| **HttpClient (JDK 11+)** | 无需外部依赖 | API 略复杂 | 无额外依赖的场景 |
| **Retrofit** | 声明式接口 | 需定义接口 | 多 API 管理的场景 |

### 3.2 OpenAI API 集成示例

#### 使用 JDK HttpClient（零外部依赖）

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;

public class OpenAIClient {

    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;

    public OpenAIClient(String apiKey) {
        this(apiKey, "https://api.openai.com");
    }

    public OpenAIClient(String apiKey, String baseUrl) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    /**
     * 非流式聊天完成
     */
    public String chat(String systemPrompt, String userMessage) {
        try {
            String requestBody = String.format("""
                {
                    "model": "gpt-4o-mini",
                    "messages": [
                        {"role": "system", "content": "%s"},
                        {"role": "user", "content": "%s"}
                    ],
                    "temperature": 0.7,
                    "max_tokens": 2048
                }
                """, escapeJson(systemPrompt), escapeJson(userMessage));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(
                request, BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                    "API 调用失败: " + response.statusCode()
                    + " - " + response.body());
            }

            // 解析响应
            return extractContent(response.body());

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("API 调用异常", e);
        }
    }

    /**
     * 流式聊天完成（SSE）
     */
    public void chatStream(
            String systemPrompt,
            String userMessage,
            Consumer<String> onToken,
            Runnable onComplete,
            Consumer<Throwable> onError) {

        String requestBody = String.format("""
            {
                "model": "gpt-4o-mini",
                "messages": [
                    {"role": "system", "content": "%s"},
                    {"role": "user", "content": "%s"}
                ],
                "temperature": 0.7,
                "stream": true
            }
            """, escapeJson(systemPrompt), escapeJson(userMessage));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(BodyPublishers.ofString(requestBody))
                .build();

        httpClient.sendAsync(request, BodyHandlers.ofLines())
            .thenAccept(response -> {
                try {
                    response.body().forEach(line -> {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            if ("[DONE]".equals(data)) {
                                onComplete.run();
                                return;
                            }
                            String content = extractStreamContent(data);
                            if (content != null) {
                                onToken.accept(content);
                            }
                        }
                    });
                } catch (Exception e) {
                    onError.accept(e);
                }
            })
            .exceptionally(e -> {
                onError.accept(e);
                return null;
            });
    }

    private String extractContent(String jsonResponse) {
        // 使用 JSON 解析库
        JsonObject json = JsonParser.parseString(jsonResponse)
            .getAsJsonObject();
        return json.getAsJsonArray("choices")
            .get(0).getAsJsonObject()
            .getAsJsonObject("message")
            .get("content").getAsString();
    }

    private String extractStreamContent(String jsonData) {
        try {
            JsonObject json = JsonParser.parseString(jsonData)
                .getAsJsonObject();
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) return null;

            JsonObject delta = choices.get(0).getAsJsonObject()
                .getAsJsonObject("delta");
            if (delta == null || delta.get("content") == null) {
                return null;
            }
            return delta.get("content").getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    // 使用示例
    public static void main(String[] args) {
        OpenAIClient client = new OpenAIClient("sk-xxx");

        // 同步调用
        String answer = client.chat(
            "你是一个 Java 专家",
            "Java 21 有什么新特性？"
        );
        System.out.println(answer);

        // 流式调用
        client.chatStream(
            "你是一个 Java 专家",
            "解释一下虚拟线程",
            System.out::print,       // onToken
            () -> System.out.println("\n\n---完成---"), // onComplete
            Throwable::printStackTrace // onError
        );

        // 等待流式完成
        try { Thread.sleep(10000); } catch (InterruptedException e) {}
    }
}
```

### 3.3 使用 WebClient（响应式 + 流式支持）

```java
@Service
public class LLMService {

    private final WebClient webClient;

    @Value("${openai.api-key}")
    private String apiKey;

    public LLMService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * 流式调用 - 返回 Flux
     * 前端可以用 SSE 接收
     */
    public Flux<String> streamChat(String message) {
        Map<String, Object> requestBody = Map.of(
            "model", "gpt-4o-mini",
            "messages", List.of(
                Map.of("role", "system", "content", "你是 Java 专家"),
                Map.of("role", "user", "content", message)
            ),
            "stream", true
        );

        return webClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(requestBody)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> line.startsWith("data: "))
                .map(line -> line.substring(6))
                .filter(data -> !"[DONE]".equals(data))
                .map(this::extractToken)
                .filter(Objects::nonNull);
    }

    private String extractToken(String jsonData) {
        try {
            JsonObject json = JsonParser.parseString(jsonData)
                .getAsJsonObject();
            return json.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("delta")
                .get("content").getAsString();
        } catch (Exception e) {
            return null;
        }
    }
}

// Controller - 转发 SSE 到前端
@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private LLMService llmService;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestBody ChatRequest request) {
        return llmService.streamChat(request.getMessage())
            .map(token -> ServerSentEvent.builder(token)
                .event("message")
                .build()
            );
    }
}
```

### 3.4 多模型适配器模式

```java
/**
 * LLM 服务接口 - 适配多种模型
 */
public interface LLMProvider {

    String chat(String systemPrompt, String userMessage);

    Flux<String> streamChat(String systemPrompt, String userMessage);

    String getProviderName();
}

// OpenAI 实现
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
public class OpenAIProvider implements LLMProvider {

    // ... 实现代码

    @Override
    public String getProviderName() {
        return "OpenAI";
    }
}

// Ollama 本地模型实现
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "ollama")
public class OllamaProvider implements LLMProvider {

    private final WebClient webClient;

    public OllamaProvider(
            @Value("${ollama.base-url:http://localhost:11434}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        Map<String, Object> request = Map.of(
            "model", "llama3.2",
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
            ),
            "stream", false
        );

        return webClient.post()
                .uri("/api/chat")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonObject.class)
                .map(json -> json.get("message").getAsJsonObject()
                    .get("content").getAsString())
                .block();
    }

    @Override
    public Flux<String> streamChat(String systemPrompt, String userMessage) {
        // Ollama 流式响应
        Map<String, Object> request = Map.of(
            "model", "llama3.2",
            "messages", ...,
            "stream", true
        );

        return webClient.post()
                .uri("/api/chat")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> line.contains("\"response\""))
                .map(this::extractResponse);
    }

    @Override
    public String getProviderName() {
        return "Ollama";
    }
}

// 模型路由服务
@Service
public class ModelRouter {

    private final Map<String, LLMProvider> providers;

    public ModelRouter(List<LLMProvider> providerList) {
        this.providers = providerList.stream()
            .collect(Collectors.toMap(
                LLMProvider::getProviderName,
                Function.identity()
            ));
    }

    public LLMProvider getProvider(String name) {
        LLMProvider provider = providers.get(name);
        if (provider == null) {
            // 默认使用第一个
            return providers.values().iterator().next();
        }
        return provider;
    }
}
```

### 3.5 重试与退避策略

```java
@Component
public class RetryableLLMClient {

    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 1000;

    private final OpenAIClient client;

    /**
     * 带重试的 API 调用
     * - 网络错误：重试
     * - 429 (Rate Limit)：等待后重试
     * - 5xx 错误：重试
     * - 4xx 其他错误：不重试
     */
    public String chatWithRetry(String systemPrompt, String userMessage) {
        Exception lastException = null;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                return client.chat(systemPrompt, userMessage);

            } catch (HttpStatusCodeException e) {
                int statusCode = e.getStatusCode().value();

                if (statusCode == 429) {
                    // Rate limit - 提取 Retry-After 头
                    String retryAfter = e.getResponseHeaders()
                        .getFirst("Retry-After");
                    long delayMs = retryAfter != null
                        ? Long.parseLong(retryAfter) * 1000
                        : calculateBackoff(attempt);

                    log.warn("Rate limited (attempt {}/{}), waiting {}ms",
                        attempt + 1, MAX_RETRIES, delayMs);
                    sleep(delayMs);
                    lastException = e;

                } else if (statusCode >= 500) {
                    // 服务端错误 - 指数退避
                    long delayMs = calculateBackoff(attempt);
                    log.warn("Server error {} (attempt {}/{}), waiting {}ms",
                        statusCode, attempt + 1, MAX_RETRIES, delayMs);
                    sleep(delayMs);
                    lastException = e;

                } else {
                    // 4xx 其他错误 - 不重试
                    throw e;
                }

            } catch (IOException e) {
                // 网络错误 - 重试
                long delayMs = calculateBackoff(attempt);
                log.warn("Network error (attempt {}/{}), waiting {}ms",
                    attempt + 1, MAX_RETRIES, delayMs);
                sleep(delayMs);
                lastException = e;
            }
        }

        throw new RuntimeException("API 调用重试耗尽", lastException);
    }

    /**
     * 指数退避 + 抖动
     */
    private long calculateBackoff(int attempt) {
        double delay = BASE_DELAY_MS * Math.pow(2, attempt);
        // 添加 ±25% 的随机抖动
        double jitter = delay * 0.25 * (Math.random() - 0.5);
        return (long) (delay + jitter);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("重试中断", e);
        }
    }
}
```

### 3.6 Prompt 管理

```java
/**
 * 提示词模板管理
 * 支持：模板 + 变量替换 + 版本管理
 */
@Component
public class PromptTemplateManager {

    private final Map<String, PromptTemplate> templates = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 从配置文件或数据库加载模板
        registerTemplate("rag_qa", PromptTemplate.builder()
            .name("rag_qa")
            .version("1.0")
            .systemPrompt("""
                你是一个专业的技术助手。
                基于以下已知信息，回答用户的问题。

                规则：
                1. 如果已知信息不足以回答问题，请说"抱歉，我没有足够的信息"
                2. 不要编造信息
                3. 使用中文回答
                4. 回答要简洁、准确

                已知信息：
                {context}
                """)
            .userPrompt("{question}")
            .build());

        registerTemplate("code_review", PromptTemplate.builder()
            .name("code_review")
            .version("1.0")
            .systemPrompt("""
                你是一个资深的 Java 代码审查专家。
                请审查以下代码，指出：
                1. 潜在的 BUG
                2. 性能问题
                3. 代码规范问题
                4. 改进建议

                请按严重程度排序输出。
                """)
            .userPrompt("```java\n{code}\n```")
            .build());

        registerTemplate("summarize", PromptTemplate.builder()
            .name("summarize")
            .version("1.1")
            .systemPrompt("""
                请用中文总结以下内容。
                要求：
                1. 提取核心观点
                2. 控制在 200 字以内
                3. 使用列表形式
                """)
            .userPrompt("{content}")
            .build());
    }

    public PromptTemplate getTemplate(String name) {
        PromptTemplate template = templates.get(name);
        if (template == null) {
            throw new IllegalArgumentException("模板不存在: " + name);
        }
        return template;
    }

    public String render(String templateName, Map<String, String> variables) {
        PromptTemplate template = getTemplate(templateName);
        String systemPrompt = replaceVariables(
            template.getSystemPrompt(), variables);
        String userPrompt = replaceVariables(
            template.getUserPrompt(), variables);
        return String.format("System: %s\n\nUser: %s",
            systemPrompt, userPrompt);
    }

    private String replaceVariables(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace(
                "{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    @Data
    @Builder
    public static class PromptTemplate {
        private String name;
        private String version;
        private String systemPrompt;
        private String userPrompt;
    }
}
```

---

## 四、LangChain4j 实战

### 4.1 LangChain4j 概述

LangChain4j 是 Java 生态的 LLM 集成框架，对标 Python 的 LangChain。

```
LangChain4j 核心优势：
  √ Java 原生：接口设计符合 Java 开发习惯
  √ Spring Boot 集成：自动配置、依赖注入
  √ 流式支持：完整 SSE/反应式支持
  √ 多种模型：OpenAI, Claude, Ollama, 通义千问等
  √ 向量数据库：pgvector, Milvus, Pinecone, Redis 等
  √ Function Calling：@Tool 注解
```

| 特性 | LangChain4j | Python LangChain |
|------|------------|------------------|
| 语言 | Java | Python |
| 模型支持 | 30+ | 100+ |
| 工具链 | Spring Boot 生态 | Python 生态 |
| 性能 | 高并发优秀 | 单线程受限 |
| 学习曲线 | 中等 | 中等 |

### 4.2 核心抽象

```java
// 1. ChatLanguageModel - 聊天模型
ChatLanguageModel model = OpenAiChatModel.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .modelName("gpt-4o-mini")
    .temperature(0.7)
    .build();

String answer = model.generate("什么是 Java 虚拟线程？");

// 2. EmbeddingModel - 嵌入模型
EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .modelName("text-embedding-3-small")
    .build();

Embedding embedding = embeddingModel.embed("Java is a programming language").content();

// 3. EmbeddingStore - 向量存储
EmbeddingStore<TextSegment> store = InMemoryEmbeddingStore.builder()
    .build();
store.add(embedding, TextSegment.from("Java is a programming language"));

// 4. Document - 文档
Document doc = Document.from(
    "Java 21 introduces virtual threads...",
    Metadata.from("source", "java-21-guide")
        .add("author", "Oracle")
);

// 5. TextSegment - 文本片段
DocumentSplitter splitter = DocumentSplitters.recursive(
    500,  // 每段最大字符数
    50    // 重叠字符数
);
List<TextSegment> segments = splitter.split(doc);
```

### 4.3 聊天记忆

```java
@Service
public class ChatService {

    private final ChatLanguageModel chatModel;

    /**
     * 固定窗口记忆 - 保留最近 N 条消息
     */
    public String chatWithWindowMemory(
            String sessionId, String message) {

        ChatMemory memory = ChatMemoryBuilder()
            .withWindowSize(10)  // 保留最近 10 条消息
            .build();

        ConversationalChain chain = ConversationalChain.builder()
            .chatLanguageModel(chatModel)
            .chatMemory(memory)
            .build();

        return chain.execute(message);
    }

    /**
     * Token 窗口记忆 - 按 token 数量截断
     */
    public String chatWithTokenMemory(
            String sessionId, String message) {

        ChatMemory memory = TokenWindowChatMemory.builder()
            .maxTokens(2000)        // 最多保留 2000 tokens
            .chatMemoryStore(new RedisChatMemoryStore())  // Redis 持久化
            .build();

        ConversationalChain chain = ConversationalChain.builder()
            .chatLanguageModel(chatModel)
            .chatMemory(memory)
            .build();

        return chain.execute(message);
    }
}

// Redis 实现的持久化记忆存储
@Component
public class RedisChatMemoryStore implements ChatMemoryStore {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String MEMORY_KEY_PREFIX = "chat:memory:";

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String key = MEMORY_KEY_PREFIX + memoryId;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) return new ArrayList<>();
        return JSON.parseArray(json, ChatMessage.class);
    }

    @Override
    public void updateMessages(
            Object memoryId, List<ChatMessage> messages) {
        String key = MEMORY_KEY_PREFIX + memoryId;
        String json = JSON.toJSONString(messages);
        redisTemplate.opsForValue().set(key, json,
            24, TimeUnit.HOURS);  // 24 小时过期
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String key = MEMORY_KEY_PREFIX + memoryId;
        redisTemplate.delete(key);
    }
}
```

### 4.4 Function Calling (@Tool)

```java
/**
 * 工具定义 - 让 LLM 能调用你的 Java 方法
 */
@Slf4j
@Component
public class WeatherTools {

    @Tool("根据城市名称获取当前天气")
    public String getWeather(@P("城市名称") String city) {
        log.info("查询天气: {}", city);
        // 调用天气 API
        String result = callWeatherApi(city);
        return result;
    }

    @Tool("获取当前日期和时间")
    public String getCurrentDateTime() {
        return LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Tool("运行 SQL 查询并返回结果")
    public String executeSqlQuery(@P("SQL 查询语句") String sql) {
        log.info("执行 SQL: {}", sql);
        // 安全检查：只允许 SELECT
        if (!sql.trim().toUpperCase().startsWith("SELECT")) {
            return "只允许 SELECT 查询";
        }
        // 执行查询...
        return "查询结果...";
    }
}

@Service
public class ToolEnabledChatService {

    private final ChatLanguageModel chatModel;

    public String chat(String message) {
        // 将工具注册给 LLM
        return chatModel.generate(
            UserMessage.from(message),
            Tools.from(
                new WeatherTools(),
                new DocumentTools()
            )
        );
    }
}
```

### 4.5 完整 RAG 流水线

```java
/**
 * 完整的 RAG 实现
 * 文档加载 → 分割 → 嵌入 → 存储 → 检索 → 生成
 */
@Service
public class CompleteRAGPipeline {

    private final ChatLanguageModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    // 1. 文档加载和索引
    public void indexDocument(String content, String source) {
        // 1.1 创建文档
        Document document = Document.from(content,
            Metadata.from("source", source)
                .add("indexed_at", Instant.now().toString())
        );

        // 1.2 分割文档
        DocumentSplitter splitter = DocumentSplitters
            .recursive(500, 50);
        List<TextSegment> segments = splitter.split(document);

        // 1.3 批量生成嵌入向量
        List<Embedding> embeddings =
            embeddingModel.embedAll(segments).content();

        // 1.4 存储到向量数据库
        embeddingStore.addAll(embeddings, segments);

        log.info("文档索引完成: {} → {} 个片段", source, segments.size());
    }

    // 2. 检索相关信息
    public List<RelevantInfo> search(String query, int topK) {
        // 2.1 查询向量化
        Embedding queryEmbedding =
            embeddingModel.embed(query).content();

        // 2.2 向量相似度搜索
        List<EmbeddingMatch<TextSegment>> matches =
            embeddingStore.findRelevant(
                queryEmbedding, topK, 0.7);  // 相似度阈值

        return matches.stream()
            .map(match -> new RelevantInfo(
                match.score(),
                match.embedded().text(),
                match.embedded().metadata().getString("source")
            ))
            .collect(Collectors.toList());
    }

    // 3. 生成回答（RAG）
    public String generateAnswer(String query) {
        // 3.1 检索
        List<RelevantInfo> relevantDocs = search(query, 3);

        if (relevantDocs.isEmpty()) {
            return chatModel.generate(
                "请回答：" + query + "\n如果不知道就说不知道。");
        }

        // 3.2 构建上下文
        String context = relevantDocs.stream()
            .map(doc ->
                String.format("[来源: %s]\n%s",
                    doc.getSource(), doc.getContent()))
            .collect(Collectors.joining("\n\n---\n\n"));

        // 3.3 构建 Prompt
        String systemPrompt = String.format("""
            你是一个基于知识库的智能问答助手。
            根据提供的参考信息回答用户的问题。

            规则：
            - 基于参考信息回答，不要编造
            - 如果参考信息不足以回答，明确说明
            - 引用来源
            - 使用中文

            参考信息：
            %s
            """, context);

        // 3.4 调用 LLM
        return chatModel.generate(
            SystemMessage.from(systemPrompt),
            UserMessage.from(query)
        );
    }

    // 4. 流式 RAG
    public TokenStream streamAnswer(String query) {
        List<RelevantInfo> relevantDocs = search(query, 3);
        String context = buildContext(relevantDocs);

        // 创建带上下文的 Prompt
        Prompt prompt = buildPrompt(query, context);

        // 返回流式 TokenStream
        return chatModel.generate(prompt);
    }

    /**
     * 批量文档处理（PDF/TXT 等）
     */
    public void batchIndexDocuments(
            List<MultipartFile> files) throws IOException {
        for (MultipartFile file : files) {
            String content = extractText(file);
            indexDocument(content, file.getOriginalFilename());
        }
    }

    private String extractText(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName.endsWith(".pdf")) {
            return extractPDFText(file);
        } else if (fileName.endsWith(".txt")) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } else {
            throw new UnsupportedOperationException(
                "不支持的文件格式: " + fileName);
        }
    }

    @Data
    @AllArgsConstructor
    public static class RelevantInfo {
        private double score;
        private String content;
        private String source;
    }
}
```

---

## 五、Spring AI 快速上手

### 5.1 Spring AI 概述

Spring AI 是 Spring 官方推出的 AI 集成框架，与 Spring Boot 深度集成。

```
Spring AI vs LangChain4j 对比：

Spring AI 优势：
  √ 官方支持：Spring 生态原生集成
  √ AutoConfiguration：零配置使用
  √ ETL 框架：内置文档处理流水线
  √ 与 Spring Boot 深度绑定

LangChain4j 优势：
  √ 更丰富的模型支持
  √ 社区更活跃
  √ 更接近 Python LangChain 的设计
  √ Function Calling 更方便

选择建议：
  如果主要用 Spring Boot → Spring AI
  如果需要更多灵活性 → LangChain4j
  最佳实践：两者都了解，根据场景选择
```

### 5.2 Spring AI 基础配置

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0-M2</version>
</dependency>

<!-- 使用 BOM 管理版本 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.0.0-M2</version>
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
      model: gpt-4o-mini
      embedding:
        model: text-embedding-3-small

    # 向量存储配置
    vectorstore:
      pgvector:
        host: localhost
        port: 5432
        database: aivector
        table-name: vector_store
```

### 5.3 ChatClient 使用

```java
@Service
public class SpringAIService {

    private final ChatClient chatClient;

    public SpringAIService(ChatClient.Builder builder) {
        this.chatClient = builder
            .defaultSystem("你是一个 Java 技术专家，请用中文回答")
            .build();
    }

    /**
     * 简单对话
     */
    public String chat(String message) {
        return chatClient.prompt()
            .user(message)
            .call()
            .content();
    }

    /**
     * 流式对话
     */
    public Flux<String> streamChat(String message) {
        return chatClient.prompt()
            .user(message)
            .stream()
            .content();
    }

    /**
     * 带参数的 Prompt
     */
    public String structuredChat(String question, String context) {
        return chatClient.prompt()
            .system(sp -> sp
                .text("基于以下上下文回答问题：{context}")
                .param("context", context))
            .user(question)
            .call()
            .content();
    }

    /**
     * 结构化输出
     */
    public JavaConcept extractConcept(String text) {
        return chatClient.prompt()
            .user("从以下文本中提取 Java 概念信息：" + text)
            .call()
            .entity(JavaConcept.class);  // 自动映射到 POJO
    }

    @Data
    public static class JavaConcept {
        private String name;
        private String category;
        private String briefDescription;
        private String javaVersion;
    }
}
```

### 5.4 ETL 流水线

```java
@Service
public class DocumentETLService {

    private final VectorStore vectorStore;

    /**
     * Spring AI 内置的 ETL 流水线
     * Extract → Transform → Load
     */
    public void processDocument(MultipartFile file) {
        // 1. Extract - 文档加载
        DocumentReader reader = new PagePdfDocumentReader(
            new InputStreamResource(file.getInputStream())
        );
        List<Document> documents = reader.get();

        // 2. Transform - 文本分割和增强
        TokenTextSplitter splitter = TokenTextSplitter.builder()
            .defaultChunkSize(500)
            .minChunkSize(100)
            .minChunkSizeChars(50)
            .build();
        List<Document> splitDocs = splitter.apply(documents);

        // 3. 元数据增强
        splitDocs.forEach(doc -> {
            doc.getMetadata().put("source", file.getOriginalFilename());
            doc.getMetadata().put("processed_at",
                LocalDateTime.now().toString());
            doc.getMetadata().put("type",
                file.getContentType());
        });

        // 4. Load - 写入向量数据库
        vectorStore.add(splitDocs);

        log.info("文档处理完成: {} → {} 个片段",
            file.getOriginalFilename(), splitDocs.size());
    }

    /**
     * 检索
     */
    public List<Document> search(String query, int topK) {
        return vectorStore.similaritySearch(
            SearchRequest.query(query)
                .withTopK(topK)
                .withSimilarityThreshold(0.7)
        );
    }
}
```

---

## 六、向量数据库与 RAG

### 6.1 Embedding 概念

```
文本向量化过程：

"Java 是一种编程语言"
        ↓
    Embedding 模型 (如 text-embedding-3-small)
        ↓
    [0.012, -0.034, 0.087, ..., 0.056]  ← 1536 维向量
        ↓
    语义相似的文本 → 向量空间距离近

向量相似度计算：
  Cosine Similarity = cos(θ) = (A·B) / (|A| * |B|)
  - 范围：[-1, 1]
  - 值越接近 1，语义越相似
```

### 6.2 向量数据库对比

| 数据库 | 部署方式 | 性能 | 特性 | Java 客户端 |
|--------|---------|------|------|------------|
| **pgvector** | PostgreSQL 插件 | 中等 | 与业务数据共存 | JDBC + Spring Data |
| **Milvus** | 独立部署 | 高 | 分布式，十亿级 | Milvus SDK |
| **Pinecone** | 云托管 | 高 | 免运维，付费 | Pinecone Java SDK |
| **Redis Stack** | Redis 模块 | 高 | 缓存 + 向量 | Redisson / Jedis |
| **Weaviate** | 独立/云 | 高 | 自动 schema | Weaviate Java |
| **Elasticsearch** | ES 插件 | 中等 | 搜索 + 向量 | ES Java Client |

### 6.3 pgvector 集成（最推荐）

pgvector 最适合 Java 开发者 —— 不需要引入新的基础设施，直接在 PostgreSQL 中使用。

```sql
-- 安装扩展
CREATE EXTENSION vector;

-- 创建向量表
CREATE TABLE knowledge_embeddings (
    id          BIGSERIAL PRIMARY KEY,
    content     TEXT NOT NULL,
    metadata    JSONB DEFAULT '{}',
    embedding   vector(1536),  -- text-embedding-3-small 维度
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引（IVFFlat 索引）
CREATE INDEX idx_embedding ON knowledge_embeddings
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- 或使用 HNSW 索引（更精确但更费内存）
CREATE INDEX idx_embedding_hnsw ON knowledge_embeddings
    USING hnsw (embedding vector_cosine_ops);

-- 相似度查询
SELECT
    id,
    content,
    1 - (embedding <=> '[0.012, -0.034, ...]'::vector) AS similarity
FROM knowledge_embeddings
ORDER BY embedding <=> '[0.012, -0.034, ...]'::vector
LIMIT 5;
```

```java
// Spring Data JPA + pgvector
@Entity
@Table(name = "knowledge_embeddings")
public class KnowledgeEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "JSONB")
    private String metadata;

    @Column(columnDefinition = "vector(1536)")
    private float[] embedding;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

@Repository
public interface KnowledgeEmbeddingRepository
    extends JpaRepository<KnowledgeEmbedding, Long> {

    // 自定义向量相似度查询
    @Query(value = """
        SELECT id, content, metadata,
               1 - (embedding <=> :embedding::vector) AS similarity
        FROM knowledge_embeddings
        ORDER BY embedding <=> :embedding::vector
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilar(
        @Param("embedding") String embedding,
        @Param("limit") int limit
    );
}
```

### 6.4 Redis 向量搜索

```java
@Service
public class RedisVectorService {

    private final StringRedisTemplate redisTemplate;
    private static final String INDEX_NAME = "idx:knowledge";

    @PostConstruct
    public void init() {
        // 创建向量索引
        try {
            redisTemplate.execute((RedisCallback<Object>) connection -> {
                connection.executeCommand(
                    "FT.CREATE", INDEX_NAME,
                    "ON", "HASH",
                    "PREFIX", "1", "doc:",
                    "SCHEMA",
                    "content", "TEXT",
                    "embedding", "VECTOR", "FLAT",
                    "6", "TYPE", "FLOAT32",
                    "DIM", "1536",
                    "DISTANCE_METRIC", "COSINE"
                );
                return null;
            });
        } catch (Exception e) {
            // 索引可能已存在
            log.info("Redis vector index may already exist");
        }
    }

    /**
     * 存储向量
     */
    public void store(String id, String content, float[] embedding) {
        Map<String, String> fields = new HashMap<>();
        fields.put("content", content);
        fields.put("embedding", floatArrayToByteString(embedding));

        redisTemplate.opsForHash().putAll("doc:" + id, fields);
    }

    /**
     * 向量搜索
     */
    public List<String> search(float[] queryVector, int topK) {
        String queryBytes = floatArrayToByteString(queryVector);

        String query = String.format(
            "@embedding:[VECTOR_RANGE 0.5 $vec]=>{$YIELD_DISTANCE_AS: dist}",
            queryBytes
        );

        // 执行 FT.SEARCH
        return redisTemplate.execute(
            (RedisCallback<List<String>>) connection -> {
                connection.executeCommand(
                    "FT.SEARCH", INDEX_NAME, query,
                    "PARAMS", "2", "vec", queryBytes,
                    "SORTBY", "dist",
                    "RETURN", "3", "content", "dist",
                    "LIMIT", "0", String.valueOf(topK)
                );
                return null;
            }
        );
    }
}
```

### 6.5 流式 RAG 完整示例

```java
/**
 * 完整的 RAG 对话服务
 * 支持流式输出、会话记忆、来源引用
 */
@Service
public class RAGChatService {

    private final ChatLanguageModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final Map<String, ChatMemory> sessions = new ConcurrentHashMap<>();

    public RAGChatService(
            ChatLanguageModel chatModel,
            EmbeddingModel embeddingModel,
            @Qualifier("pgvectorEmbeddingStore")
            EmbeddingStore<TextSegment> embeddingStore) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    /**
     * 流式 RAG 问答
     */
    public TokenStream streamAnswer(String sessionId, String query) {
        // 1. 检索相关文档
        Embedding queryEmbedding =
            embeddingModel.embed(query).content();
        List<EmbeddingMatch<TextSegment>> matches =
            embeddingStore.findRelevant(queryEmbedding, 5);

        // 2. 构建上下文
        String context = matches.stream()
            .map(m -> String.format(
                "[相关性: %.2f]\n%s",
                m.score(), m.embedded().text()))
            .collect(Collectors.joining("\n\n"));

        // 3. 获取会话记忆
        ChatMemory memory = sessions
            .computeIfAbsent(sessionId,
                k -> MessageWindowChatMemory.builder()
                    .maxMessages(20)
                    .build());

        // 4. 构建 Prompt
        Prompt prompt = Prompt.from(
            SystemMessage.from(String.format("""
                你是一个基于知识库的助手。
                已知信息：
                %s

                请基于上述信息回答。
                """, context)),
            UserMessage.from(query)
        );

        // 5. 流式回复 + 记忆
        return chatModel.generate(prompt)
            .onComplete(response -> {
                // 保存到记忆
                memory.add(UserMessage.from(query));
                memory.add(response.aiMessage());
            });
    }

    /**
     * 获取来源文档
     */
    public List<SourceDocument> getSources(String query) {
        Embedding queryEmbedding =
            embeddingModel.embed(query).content();
        return embeddingStore.findRelevant(queryEmbedding, 3)
            .stream()
            .map(m -> new SourceDocument(
                m.embedded().text(),
                m.score()))
            .collect(Collectors.toList());
    }

    @Data
    @AllArgsConstructor
    public static class SourceDocument {
        private String content;
        private double relevance;
    }
}
```

---

## 七、Java 中的 ML 模型推理

### 7.1 ONNX Runtime Java

```xml
<dependency>
    <groupId>com.microsoft.onnxruntime</groupId>
    <artifactId>onnxruntime</artifactId>
    <version>1.18.0</version>
</dependency>
```

```java
/**
 * Java 加载 ONNX 模型进行推理
 * 适用于：Python 训练 → 导出 ONNX → Java 推理
 */
@Service
public class ONNXInferenceService {

    private final OrtSession session;
    private final OrtEnvironment env;

    public ONNXInferenceService(
            @Value("${model.onnx.path}") String modelPath) throws IOException {
        this.env = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL);
        this.session = env.createSession(modelPath, options);
    }

    /**
     * 文本分类推理
     */
    public float[] classify(String text) {
        try {
            // 1. 预处理：文本转张量
            long[] inputShape = {1, sequenceLength};
            OnnxTensor inputTensor = OnnxTensor.createTensor(
                env, tokenize(text), inputShape);

            // 2. 运行推理
            OrtSession.Result result = session.run(
                Map.of("input_ids", inputTensor));

            // 3. 提取结果
            OnnxTensor output = (OnnxTensor)
                result.get("output").get();
            float[][] scores = (float[][]) output.getValue();

            return scores[0];

        } catch (OrtException e) {
            throw new RuntimeException("ONNX 推理失败", e);
        }
    }

    private long[] tokenize(String text) {
        // 简化的 tokenizer
        long[] tokens = new long[sequenceLength];
        tokens[0] = 101;  // [CLS]
        // ... 实际使用 Tokenizer
        tokens[1] = 102;  // [SEP]
        return tokens;
    }
}
```

### 7.2 DJL (Deep Java Library)

```xml
<dependency>
    <groupId>ai.djl</groupId>
    <artifactId>api</artifactId>
    <version>0.27.0</version>
</dependency>
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-engine</artifactId>
    <version>0.27.0</version>
</dependency>
```

```java
import ai.djl.*;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.nlp.*;
import ai.djl.repository.zoo.*;

/**
 * DJL 推理示例
 */
@Service
public class DJLInferenceService {

    private Predictor<String, Classifications> predictor;

    @PostConstruct
    public void init() throws Exception {
        // 从 Model Zoo 加载预训练模型
        Model model = ModelZoo.loadModel(
            Criteria.builder()
                .optApplication("nlp/text_classification")
                .setTypes(String.class, Classifications.class)
                .build()
        );
        this.predictor = model.newPredictor();
    }

    public Classifications predict(String text) {
        try {
            return predictor.predict(text);
        } catch (TranslateException e) {
            throw new RuntimeException("DJL 推理失败", e);
        }
    }
}
```

---

## 八、构建 AI 增强的简历项目

### 8.1 项目：AI-Powered Developer Assistant

**项目定位**：
> 一个基于 RAG 的智能开发者助手，支持上传技术文档、代码片段，通过自然语言交互获取技术问答、代码解释和错误排查建议。

**技术架构**：

```
┌─────────────────────────────────────────────────────────────┐
│  Vue 3 前端                                                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                 │
│  │ 聊天窗口 │  │ 文档上传 │  │ 设置面板 │                 │
│  │  (SSE)   │  │ (拖拽)   │  │ (模型选择)│                 │
│  └──────────┘  └──────────┘  └──────────┘                 │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────┴──────────────────────────────────┐
│  Spring Boot 后端                                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │  Chat    │  │ Document │  │ Vector   │  │  User    │  │
│  │  Service │  │ Service  │  │ Service  │  │  Service │  │
│  ├──────────┤  ├──────────┤  ├──────────┤  ├──────────┤  │
│  │ LangChain4j  │  │ PDF解析 │  │ pgvector│  │ JWT Auth│  │
│  │ OpenAI/Ollama│  │ Tika   │  │ 检索    │  │         │  │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │
└─────────────────────────────────────────────────────────────┘
```

**核心功能**：

```
1. 智能对话
   - 支持 GPT-4o-mini / Claude / Ollama 多模型切换
   - 流式 SSE 输出
   - 上下文记忆（Redis 持久化）
   - 支持代码高亮

2. 文档知识库
   - 上传 PDF / TXT / Markdown
   - 自动分割 + 向量化
   - 语义检索 + RAG 问答
   - 来源引用展示

3. 代码助手
   - 代码解释
   - Bug 排查
   - 代码优化建议
   - 单元测试生成

4. 系统管理
   - 对话历史查看/删除
   - 文档管理
   - Prompt 模板管理
   - API 用量统计
```

**面试亮点**：

```
这个项目可以在面试中展示：

1. 技术广度：
   - Spring Boot + AI 集成
   - 向量数据库 + 语义搜索
   - 流式响应实现

2. 架构能力：
   - 多模型切换（策略模式）
   - RAG 流水线设计
   - 缓存和会话管理

3. 工程能力：
   - Docker 部署
   - 环境变量管理
   - API 版本控制

4. 差异化：
   - Java 开发者 + AI 集成技能
   - 紧跟技术趋势
   - 全栈能力
```

### 8.2 项目 README 亮点示例

```markdown
# AI-Powered Developer Assistant

> 基于 RAG 的智能开发者助手 —— 让 AI 帮你调代码、查文档、学技术

## 技术栈

| 后端 | AI | 数据 | 部署 |
|------|-----|------|------|
| Java 21 | OpenAI API | PostgreSQL (pgvector) | Docker |
| Spring Boot 3.2 | LangChain4j | Redis | Railway |
| Spring Security | Ollama (本地) | MinIO (文件) | GitHub Actions |

## 核心特性

- **多模型支持**：GPT-4o-mini / Claude / DeepSeek / Ollama 一键切换
- **知识库 RAG**：上传技术文档，基于语义检索的智能问答
- **流式对话**：SSE 实时输出，打字机效果
- **代码智能**：代码解释、Bug 排查、优化建议、测试生成
- **会话管理**：多轮对话，Redis 持久化，支持历史回溯

## 技术亮点

1. **RAG 流水线优化**：文档分块策略 + 向量检索 + Prompt 模板
2. **多模型架构**：策略模式 + 工厂模式，支持 4 种以上 LLM
3. **流式响应**：基于 Spring WebFlux + SSE，前端打字机效果
4. **缓存策略**：Redis 缓存对话历史，减少 API 调用成本

## 在线演示

🔗 [https://ai-assistant.your-domain.com](https://ai-assistant.your-domain.com)

## 快速开始

```bash
git clone https://github.com/your-username/ai-assistant.git
cd ai-assistant
docker-compose up -d
```

## 截图

[聊天界面截图] [文档管理截图] [设置面板截图]

## 后续计划

- [ ] 支持图片理解和多模态输入
- [ ] 集成 Langfuse 进行 LLM 可观测性
- [ ] 添加 Agent 能力（自动执行代码/查询）
```

### 8.3 部署架构

```yaml
# docker-compose.yml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      OPENAI_API_KEY: ${OPENAI_API_KEY}
      DB_URL: jdbc:postgresql://db:5432/aiknowledge
      REDIS_HOST: redis
    depends_on:
      - db
      - redis

  db:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_DB: aiknowledge
      POSTGRES_USER: app
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
      - ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "5432:5432"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  # 可选：本地 LLM
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
              count: all
              capabilities: [gpu]

volumes:
  pgdata:
  ollama_data:
```

---

## 九、AI 编码助手与 Java 开发

### 9.1 GitHub Copilot

```
Copilot 在 Java 开发中的最佳实践：

1. 写注释生成代码
   // 使用 ThreadPoolExecutor 创建线程池，核心线程 5，最大 10
   // 队列容量 100，拒绝策略为 CallerRunsPolicy

2. 生成测试
   // 测试 ArticleService.createArticle 方法
   // - 正常创建
   // - 标题为空时抛异常
   // - 用户不存在时抛异常

3. 重构建议
   // 重构此方法，使用策略模式替代 if-else

4. 生成文档注释
   /**
    * 生成 JWT Token
    * @param userId 用户 ID
    * @param role 用户角色
    * @return 签发的 Token
    */
```

### 9.2 Claude Code

```
Claude Code 在 Java 项目中的使用：

1. 代码审查
   /review 检查此方法的并发安全性

2. 理解代码
   /explain 这个方法的作用和调用链

3. 生成代码
   "创建一个 Redis 分布式锁工具类"

4. 调试
   "这段代码为什么会触发 ConcurrentModificationException？"

5. 编写测试
   "为这个 Service 类生成单元测试，使用 Mockito"
```

### 9.3 有效 Prompt 技巧

```
不好的 Prompt：
"帮我写一个 Java 类"

好的 Prompt：
"创建一个 Spring Boot 的 Redis 缓存工具类，
包含以下方法：
1. get(key) - 获取缓存
2. set(key, value, ttl) - 设置缓存
3. delete(key) - 删除缓存
4. 支持泛型
5. 使用 StringRedisTemplate
6. 处理序列化异常"

高效使用 AI 编码助手的三个原则：
1. 明确上下文
2. 描述输入输出
3. 指定约束条件
```

---

## 十、职业定位与市场策略

### 10.1 简历定位

```
简历标题方案：

❌ "Java 后端开发工程师"
✅ "Java 后端开发工程师 | AI 应用集成经验"

❌ "熟练掌握 Spring Boot"
✅ "使用 Spring Boot + LangChain4j 构建 AI RAG 应用"

技能描述优化：

❌ "了解 AI 技术"
✅ "集成 OpenAI API 实现智能对话（SSE 流式输出）"
✅ "使用 LangChain4j 构建 RAG 知识库问答系统"
✅ "部署 pgvector 向量数据库实现语义搜索"
```

### 10.2 学习路径

```
第 1 周：基础了解
- 理解 LLM 基本原理
- 使用 Java HttpClient 调用 OpenAI API
- 实现简单的聊天功能

第 2 周：深入集成
- 学习 LangChain4j 核心 API
- 实现多模型切换
- 添加对话记忆

第 3 周：RAG 实现
- 学习 Embedding 和向量搜索
- 部署 pgvector
- 实现完整的 RAG 流水线

第 4 周：项目构建
- 搭建 AI 助手项目
- 前端 SSE 对接
- Docker 部署

持续：深入优化
- Function Calling / 工具调用
- 性能优化和成本控制
- Agent 架构探索
```

### 10.3 面试中展示 AI 能力

```
面试官："你用过 AI 吗？"

回答参考：
"是的，我在最近的个人项目中深度使用了 AI 技术。
我基于 Spring Boot + LangChain4j 构建了一个知识库问答系统，
走通了完整的 RAG 流程：
1. 用户上传技术文档（PDF/文本）
2. 使用 embedding 模型向量化
3. 存储到 pgvector 向量数据库
4. 用户提问时检索相关文档片段
5. 结合上下文调用 LLM 生成回答
6. 通过 SSE 流式返回给前端

这个项目让我理解了
- 大模型在实际业务中的集成方式
- 向量数据库和语义搜索的原理
- 流式响应的实现方案
- Prompt 工程的最佳实践"

关键点：
1. 提到具体的技术：LangChain4j, pgvector, SSE
2. 讲清楚流程，展示系统性思考
3. 展示学习能力和技术视野
```

### 10.4 持续的 AI 学习资源

| 资源 | 类型 | 说明 |
|------|------|------|
| LangChain4j 官方文档 | 文档 | 核心 API 参考 |
| Spring AI 官方文档 | 文档 | Spring 生态的 AI 集成 |
| 吴恩达 ChatGPT Prompt Engineering | 课程 | Prompt 工程入门 |
| Andrej Karpathy 的 LLM 介绍 | 视频 | LLM 原理入门 |
| GitHub Trending Java AI 项目 | 开源 | 学习最佳实践 |
| Anthropic / OpenAI Cookbook | 教程 | API 使用示例 |
| O'Reilly "AI Engineering" | 书籍 | 系统学习 AI 工程 |

---

## 附录

### A. Maven 依赖汇总

```xml
<!-- LangChain4j -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>0.33.0</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>0.33.0</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-pgvector</artifactId>
    <version>0.33.0</version>
</dependency>

<!-- Spring AI -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0-M2</version>
</dependency>

<!-- ONNX Runtime -->
<dependency>
    <groupId>com.microsoft.onnxruntime</groupId>
    <artifactId>onnxruntime</artifactId>
    <version>1.18.0</version>
</dependency>

<!-- DJL -->
<dependency>
    <groupId>ai.djl</groupId>
    <artifactId>api</artifactId>
    <version>0.27.0</version>
</dependency>
```

### B. 面试可能问到的 AI 问题

```
1. 什么是 RAG？和微调有什么区别？
2. Embedding 是什么？如何计算相似度？
3. 为什么要用向量数据库？和传统数据库有什么区别？
4. Token 是什么？如何估算 token 数量？
5. 如何处理大模型的幻觉问题？
6. 如何控制 API 调用成本？
7. 流式输出和普通输出有什么区别？
8. 如何设计多轮对话的记忆管理？
9. Function Calling 的原理是什么？
10. Prompt Engineering 的最佳实践有哪些？
```

### C. 常见陷阱

```
1. 不要试图训练自己的大模型
   → 正确做法：API 调用 + RAG

2. 不要忽略成本控制
   → 加缓存、合并请求、选择合适模型

3. 不要忽略响应延迟
   → 流式输出、异步处理、前端加载态

4. 不要忽略安全性
   → API Key 管理、Prompt 注入防护、数据隐私

5. 不要过度依赖 AI
   → AI 是工具，基础能力才是根本
   → 理解原理比调 API 更重要
```

---

*"AI 不会取代 Java 开发者，但会用 AI 的 Java 开发者会取代不会的。这句话在未来 2-3 年依然成立。"*
