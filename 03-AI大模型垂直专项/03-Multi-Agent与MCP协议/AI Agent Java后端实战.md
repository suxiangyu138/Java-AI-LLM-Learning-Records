# AI Agent Java 后端实战

> **核心摘要**：本文系统讲解 Java / SpringBoot 集成 Agent 的三种技术方案——Spring AI（官方推荐）、LangChain4j（社区版 Java LangChain）和 HTTP API 调用，涵盖最简 Agent 实现、Tool 定义、会话管理、数据库接入和 MCP Server 配置。

## 前置阅读

- [[AI Agent核心知识点]]
- [[AI Agent 工具系统设计]]
- [[AI Agent 主流框架深度对比]]

---

## 一、Java Agent 技术栈

```
Java Agent 开发的三条路：

1. Spring AI（Spring 官方，推荐）
   ├── 与 SpringBoot 深度集成
   ├── 支持多 LLM（OpenAI / Claude / Ollama）
   ├── 原生 Function Calling
   └── MCP Client/Server 支持

2. LangChain4j（社区版 Java LangChain）
   ├── Agent 模式支持
   ├── 30+ LLM 集成
   └── 社区活跃

3. HTTP API 调用（最灵活）
   ├── 直接调 LLM API
   └── 调用 Dify / LangServe API
```

---

## 二、Spring AI 快速上手

### 2.1 依赖配置

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
```

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        model: gpt-4o
        temperature: 0.3
```

### 2.2 最简 Agent：ChatClient + Tools

```java
@RestController
@RequiredArgsConstructor
public class SimpleAgentController {

    private final ChatClient chatClient;

    @GetMapping("/agent/chat")
    public String chat(@RequestParam String query) {
        return chatClient.prompt()
            .system("""
                你是一个智能助手，可以调用以下工具来回答问题。
                如果不知道答案，调用工具查询，不要编造信息。
                """)
            .tools(new WeatherTool(), new SearchTool())
            .user(query)
            .call()
            .content();
    }
}
```

---

## 三、定义 Spring AI Tool

```java
@Component
public class WeatherTool {

    @Tool(description = "获取指定城市的天气信息")
    public String getWeather(
        @ToolParam(description = "城市名，如 北京、上海") String city) {

        Map<String, String> weather = Map.of(
            "北京", "25°C，晴，AQI 45",
            "上海", "28°C，阵雨，AQI 85",
            "深圳", "32°C，多云，AQI 60"
        );
        return weather.getOrDefault(city, "暂无" + city + "的天气数据");
    }
}

@Component
public class OrderTool {

    @Autowired
    private OrderService orderService;

    @Tool(description = "根据用户ID查询最近的订单")
    public List<Order> queryOrders(
        @ToolParam(description = "用户ID") String userId,
        @ToolParam(description = "最近N条") int limit) {

        return orderService.getRecentOrders(userId, limit);
    }
}
```

---

## 四、有状态的 Agent 会话

```java
@Service
public class StatefulAgentService {

    private final ChatClient chatClient;
    private final Map<String, List<Message>> sessionStore = new ConcurrentHashMap<>();

    public String chat(String sessionId, String userMessage) {
        List<Message> history = sessionStore
            .computeIfAbsent(sessionId, k -> new ArrayList<>());

        Prompt prompt = new Prompt(
            userMessage,
            ChatClient.DEFAULT_SYSTEM_PROMPT,
            history,
            List.of(new WeatherTool(), new OrderTool())
        );

        ChatResponse response = chatClient.call(prompt);
        String answer = response.getResult().getOutput().getContent();

        history.add(new UserMessage(userMessage));
        history.add(new AssistantMessage(answer));

        return answer;
    }
}
```

---

## 五、Agent + 业务数据库

```java
@Component
public class DatabaseTool {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Tool(description = "执行 SQL 查询数据库。只支持 SELECT 语句。")
    public List<Map<String, Object>> queryDatabase(
        @ToolParam(description = "SELECT 查询语句") String sql) {

        // 安全校验：只允许 SELECT
        String trimmed = sql.trim().toUpperCase();
        if (!trimmed.startsWith("SELECT")) {
            throw new SecurityException("仅允许 SELECT 查询");
        }
        if (trimmed.contains(";")) {
            throw new SecurityException("不允许执行多条语句");
        }
        if (trimmed.contains("DROP") || trimmed.contains("DELETE")
            || trimmed.contains("UPDATE") || trimmed.contains("INSERT")) {
            throw new SecurityException("不允许修改数据");
        }

        return jdbcTemplate.queryForList(sql);
    }
}
```

---

## 六、MCP Server——将 Java 微服务暴露为工具

```java
@Configuration
public class McpServerConfig {

    @Bean
    public McpServer mcpServer(UserService userService,
                                OrderService orderService,
                                ReportService reportService) {
        return McpServer.builder()
            .serverInfo("business-server", "1.0.0")
            .capabilities(Capabilities.builder()
                .tools(true)
                .resources(true)
                .build())
            .tool("get_user_info", "查询用户详情", args -> {
                String userId = (String) args.get("user_id");
                return userService.getById(userId);
            })
            .tool("query_orders", "查询用户订单", args -> {
                String userId = (String) args.get("user_id");
                int page = (int) args.getOrDefault("page", 1);
                return orderService.queryByUser(userId, page);
            })
            .resource("report://monthly/{month}", uri -> {
                String month = uri.variables().get("month");
                return new McpResource(reportService.getMonthlyReport(month));
            })
            .build();
    }
}
```

> **重点**：Client 端可以自动发现并调用这些工具，任何支持 MCP 的 Agent 都能使用。

---

## 七、LangChain4j（社区版 Java Agent）

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
```

```java
public class LangChain4jAgent {

    public String run(String query) {
        ChatLanguageModel model = OpenAiChatModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName("gpt-4o")
            .build();

        Tool weatherTool = Tool.from(
            "getWeather", "获取指定城市的天气",
            (city) -> {
                String cityStr = (String) city;
                return "北京今天 25°C，晴天";
            }
        );

        AiServices<Assistant> aiService = AiServices.builder(Assistant.class)
            .chatLanguageModel(model)
            .tools(weatherTool)
            .build();

        Assistant assistant = aiService.build();
        return assistant.chat(query);
    }
}

interface Assistant {
    String chat(String message);
}
```

---

## 八、企业级架构：SpringBoot + Dify

```
Java 微服务                     Dify（AI 编排层）
┌──────────────┐      HTTP      ┌──────────────────┐
│SpringBoot App│───调用 Dify ──→│ Agent 工作流       │
│              │←──返回结果────│ 知识库 + LLM + 工具 │
└──────────────┘                └──────────────────┘
```

```java
@RestController
public class DifyAgentProxy {

    private final RestTemplate restTemplate;

    @Value("${dify.api-key}")
    private String difyApiKey;

    @Value("${dify.endpoint}")
    private String difyEndpoint;

    @PostMapping("/agent/chat")
    public AgentResponse chat(@RequestBody AgentRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(difyApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
            "inputs", request.getInputs(),
            "query", request.getQuery(),
            "user", request.getUserId(),
            "response_mode", "blocking"
        );

        ResponseEntity<DifyResponse> response = restTemplate.postForEntity(
            difyEndpoint + "/v1/chat-messages",
            new HttpEntity<>(body, headers),
            DifyResponse.class
        );

        return convertResponse(response.getBody());
    }
}
```

---

## 核心要点回顾

- Java 接入 LLM 的三种方式：Spring AI / LangChain4j / HTTP 直调
- Spring AI 定义 Tool：使用 `@Tool` + `@ToolParam` 注解
- MCP Server：用标准协议将微服务暴露为 Agent 可用的工具
- 多轮对话实现：维护会话历史 `List<Message>`，每次注入 Prompt
- Dify + Java 分工：Dify 做 AI 编排，Java 做业务逻辑 + 通过 HTTP API 调用

---

## 参考资料

1. Spring AI 官方文档. ChatClient 与 Tool 使用指南
2. LangChain4j 官方文档. Java Agent 开发手册
3. MCP 协议官方文档. Java Server/Client 实现
4. Dify 官方文档. API 调用与集成
5. Spring Boot 官方文档. REST API 开发
