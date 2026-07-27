# 10 - Java 后端集成与 AI 开发实战

> 🎯 将 Hermes 集成到 Java 生态中是用好它的"最后一公里"。本文覆盖 Spring AI 集成、LangChain4j 适配、Ollama Java Client、以及 Agent 开发的完整实战套路

---

## 目录

1. [集成方案全景](#1-集成方案全景)
2. [Spring AI 集成 Hermes](#2-spring-ai-集成-hermes)
3. [LangChain4j 集成](#3-langchain4j-集成)
4. [Ollama Java 客户端](#4-ollama-java-客户端)
5. [Function Calling 实战](#5-function-calling-实战)
6. [Agent 开发完整示例](#6-agent-开发完整示例)

---

## 1. 集成方案全景

```text
Java 后端集成 Hermes 的四种路径
│
├── 🟢 Spring AI (推荐)
│   └── Spring 原生 + Ollama starter + 自动配置
│
├── 🔵 LangChain4j
│   └── 更灵活的 LLM 编排 + 丰富的工具生态
│
├── 🟡 原生 HTTP Client
│   └── Ollama OpenAI-compatible API + RestClient
│
└── 🟣 Hermes Java SDK (自建)
    └── 封装 ChatML + Function Calling 协议
```

## 2. Spring AI 集成 Hermes

### 2.1 依赖配置

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
    <version>1.0.0-M5</version>
</dependency>
```

```yaml
# application.yml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        enabled: true
        model: hermes3:8b
        options:
          temperature: 0.7
          top-p: 0.9
          num-ctx: 8192
```

### 2.2 基础对话

```java
@RestController
public class HermesChatController {

    private final OllamaChatModel chatModel;

    public HermesChatController(OllamaChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @PostMapping("/chat")
    public String chat(@RequestBody String question) {
        return chatModel.call(question);
    }

    // 带 System Prompt 的对话
    @PostMapping("/chat/java-expert")
    public String chatAsJavaExpert(@RequestBody String question) {
        Prompt prompt = new Prompt(
            List.of(
                new SystemMessage("""
                    You are a Java 17+ backend expert.
                    Always provide code examples.
                    Follow Spring Boot best practices.
                    """),
                new UserMessage(question)
            )
        );
        return chatModel.call(prompt).getResult().getOutput().getText();
    }
}
```

### 2.3 Function Calling

```java
@Configuration
public class HermesFunctionConfig {

    @Bean
    @Description("获取指定城市的天气信息")
    public Function<WeatherRequest, WeatherResponse> getWeather() {
        return request -> {
            // 实际的天气 API 调用
            double temp = fetchFromWeatherAPI(request.city());
            return new WeatherResponse(request.city(), temp);
        };
    }
}

// 请求/响应 Record
public record WeatherRequest(
    @JsonProperty(required = true)
    @JsonPropertyDescription("城市名称，如 Beijing")
    String city
) {}

public record WeatherResponse(String city, double temperature) {}

// 控制器中使用
@PostMapping("/agent/weather")
public String weatherAgent(@RequestBody String query) {
    return chatModel.call(
        new Prompt(
            List.of(
                new UserMessage(query)
            ),
            OllamaOptions.builder()
                .function("getWeather")      // Spring AI 自动发现 @Bean
                .build()
        )
    ).getResult().getOutput().getText();
}
```

> 💡 Spring AI 的 `@Description` 注解自动生成 Function Schema，无需手写 JSON——开发体验接近 OpenAI SDK

### 2.4 流式输出（SSE）

```java
@PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> chatStream(@RequestBody String question) {
    Prompt prompt = new Prompt(new UserMessage(question));
    return chatModel.stream(prompt)
        .map(chunk -> chunk.getResult().getOutput().getText());
}
```

## 3. LangChain4j 集成

### 3.1 依赖

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-ollama</artifactId>
    <version>0.36.2</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>0.36.2</version>
</dependency>
```

### 3.2 Agent + Tool 模式

```java
public class HermesAgent {

    // 定义工具
    @Tool("获取股票价格")
    public double getStockPrice(String symbol) {
        return fetchPrice(symbol);  // 实际 API 调用
    }

    @Tool("发送邮件")
    public String sendEmail(String to, String subject, String body) {
        // 邮件发送逻辑
        return "Email sent to " + to;
    }

    public String run(String userQuery) {
        // 模型配置
        ChatLanguageModel model = OllamaChatModel.builder()
            .baseUrl("http://localhost:11434")
            .modelName("hermes3:8b")
            .temperature(0.7)
            .build();

        // 注册工具
        HermesAgent agent = new HermesAgent();

        // 构建 Agent（AiServices 自动处理工具调用循环）
        Assistant assistant = AiServices.builder(Assistant.class)
            .chatLanguageModel(model)
            .tools(agent)
            .build();

        return assistant.chat(userQuery);
    }

    interface Assistant {
        String chat(String userMessage);
    }
}
```

> 🎯 LangChain4j 的 `AiServices` 会自动处理 Function Calling 的完整循环（用户提问 → 模型选工具 → 执行 → 回填 → 最终答案），无需手写递归逻辑

## 4. Ollama Java 客户端

### 4.1 纯 HTTP 方案（无框架依赖）

```java
import org.springframework.web.client.RestClient;

public class OllamaHermesClient {

    private final RestClient restClient;

    public OllamaHermesClient() {
        this.restClient = RestClient.builder()
            .baseUrl("http://localhost:11434")
            .build();
    }

    public String chat(String systemPrompt, String userMessage) {
        var request = Map.of(
            "model", "hermes3:8b",
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
            ),
            "stream", false,
            "options", Map.of("temperature", 0.7)
        );

        var response = restClient.post()
            .uri("/api/chat")
            .body(request)
            .retrieve()
            .body(Map.class);

        return (String) ((Map) response.get("message")).get("content");
    }

    // Function Calling 专用方法
    public String chatWithTools(String userMessage, String toolsJson) {
        var systemPrompt = """
            You are a function calling AI model. Available tools:
            <tools>
            %s
            </tools>
            Return function calls as: <tool_call>{"name":"...","arguments":{...}}</tool_call>
            """.formatted(toolsJson);

        String response = chat(systemPrompt, userMessage);

        // 检查是否包含工具调用
        if (response.contains("<tool_call>")) {
            String toolCallJson = extractToolCall(response);
            String result = executeTool(toolCallJson);
            // 回填结果，获取最终答案
            return chatWithToolResult(userMessage, toolCallJson, result);
        }
        return response;
    }

    private String extractToolCall(String response) {
        int start = response.indexOf("<tool_call>") + "<tool_call>".length();
        int end = response.indexOf("</tool_call>");
        return response.substring(start, end).trim();
    }
}
```

## 5. Function Calling 实战

### 5.1 完整的工具调用流程

```java
@Service
public class HermesToolCallService {

    private final OllamaHermesClient client;
    private final Map<String, Function<Map<String, Object>, Object>> toolRegistry = new HashMap<>();

    public HermesToolCallService() {
        this.client = new OllamaHermesClient();

        // 注册工具
        registerTool("get_weather", args -> {
            String city = (String) args.get("city");
            return Map.of("city", city, "temperature", 22, "condition", "Sunny");
        });

        registerTool("search_docs", args -> {
            String keyword = (String) args.get("keyword");
            return searchDocumentation(keyword);
        });
    }

    public void registerTool(String name, Function<Map<String, Object>, Object> handler) {
        toolRegistry.put(name, handler);
    }

    /**
     * 递归工具调用：直到模型不再请求工具
     */
    public String agentExecute(String userQuery, String toolsJson) {
        String response = client.chatWithTools(userQuery, toolsJson);
        int maxRounds = 5;

        for (int round = 0; round < maxRounds; round++) {
            if (!response.contains("<tool_call>")) {
                return response;  // 最终答案
            }

            // 解析并执行工具
            String toolCall = extractToolCall(response);
            JSONObject call = new JSONObject(toolCall);
            String toolName = call.getString("name");
            Map<String, Object> args = call.getJSONObject("arguments").toMap();

            // 执行
            Object result = toolRegistry.get(toolName).apply(args);

            // 回填结果
            response = client.chatWithToolResult(toolCall, result);
        }
        return "Max tool call rounds exceeded";
    }
}
```

## 6. Agent 开发完整示例

### 6.1 Spring Boot 智能客服 Agent

```java
@SpringBootApplication
public class HermesAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(HermesAgentApplication.class, args);
    }

    // ====== 工具定义 ======

    @Bean
    @Description("查询订单状态")
    public Function<OrderQuery, OrderInfo> queryOrder(OrderService orderService) {
        return query -> orderService.findByOrderId(query.orderId());
    }

    @Bean
    @Description("查询用户账户余额")
    public Function<UserQuery, AccountInfo> queryAccount(AccountService accountService) {
        return query -> accountService.getAccount(query.userId());
    }

    @Bean
    @Description("发起退款申请")
    public Function<RefundRequest, RefundResult> requestRefund(RefundService refundService) {
        return request -> refundService.submitRefund(request.orderId(), request.reason());
    }

    // ====== Agent 控制器 ======

    @RestController
    @RequestMapping("/agent")
    public class CustomerServiceAgent {

        private final OllamaChatModel chatModel;

        public CustomerServiceAgent(OllamaChatModel chatModel) {
            this.chatModel = chatModel;
        }

        @PostMapping("/handle")
        public AgentResponse handle(@RequestBody String customerMessage) {
            var systemPrompt = """
                You are a customer service agent for an e-commerce platform.
                You have access to tools for:
                - queryOrder: check order status
                - queryAccount: check account balance
                - requestRefund: submit refund requests

                Always be polite and helpful.
                If you can't help, suggest contacting human support.
                """;

            String response = chatModel.call(
                new Prompt(
                    List.of(
                        new SystemMessage(systemPrompt),
                        new UserMessage(customerMessage)
                    )
                )
            ).getResult().getOutput().getText();

            return new AgentResponse(response);
        }
    }
}

// Record 类型定义
record OrderQuery(@JsonProperty(required = true) String orderId) {}
record OrderInfo(String orderId, String status, String trackingNumber) {}
record UserQuery(@JsonProperty(required = true) String userId) {}
record AccountInfo(String userId, double balance, String level) {}
record RefundRequest(@JsonProperty(required = true) String orderId,
                     String reason) {}
record RefundResult(String refundId, String status) {}
record AgentResponse(String message) {}
```

## 核心要点回顾

- Java 集成四条路：Spring AI（最推荐 / Spring 原生） > LangChain4j（最灵活） > 原生 HTTP（最轻量） > 自建 SDK
- Spring AI 的 `@Description` 注解自动生成 Function Schema，开发体验最佳
- LangChain4j 的 `AiServices` 自动处理完整 Function Calling 循环，无需手写递归
- Ollama 的 API 完全兼容 OpenAI 格式，`http://localhost:11434/v1`
- Function Calling 的核心就是：工具注册 → 模型选工具 → 执行 → 回填 → 循环
- 生产建议：开发用 Ollama + Spring AI → 生产用 vLLM + 同样的 OpenAI 兼容 API

## 参考资料

1. Spring AI 官方文档 - Ollama Chat
2. LangChain4j 官方文档 - Ollama Integration
3. Ollama 官方 API 文档 - `/api/chat` 端点
4. Hermes-Function-Calling GitHub - Function Schema 格式
