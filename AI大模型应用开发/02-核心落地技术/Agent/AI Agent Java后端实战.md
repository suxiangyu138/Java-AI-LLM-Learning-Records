# AI Agent Java 后端实战（Java 后端 + AI 全栈实战版）

> **文档定位**：AI Agent 核心技术文档 | Java/SpringBoot 集成 Agent 全方案
> **版本**：SpringBoot 3.x | Spring AI | LangChain4j
> **核心场景**：用 Java 构建 Agent，调用 LLM，暴露业务工具

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
            .tools(new WeatherTool(), new SearchTool())   // 注册工具
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
        
        // 实际调用天气 API
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
        // 获取或创建会话历史
        List<Message> history = sessionStore
            .computeIfAbsent(sessionId, k -> new ArrayList<>());

        // 构建完整 Prompt（含历史）
        Prompt prompt = new Prompt(
            userMessage,
            ChatClient.DEFAULT_SYSTEM_PROMPT,   // System Prompt
            history,                              // 历史消息
            List.of(new WeatherTool(), new OrderTool())  // 工具
        );

        ChatResponse response = chatClient.call(prompt);
        String answer = response.getResult().getOutput().getContent();

        // 更新会话历史
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

    @Tool(description = """
        执行 SQL 查询数据库。只支持 SELECT 语句。
        适用场景：查询用户信息、订单数据、统计数据等。
        """)
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

## 六、MCP Server —— 把 Java 微服务暴露为工具

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

            // 用户工具
            .tool("get_user_info", "查询用户详情", args -> {
                String userId = (String) args.get("user_id");
                return userService.getById(userId);
            })

            // 订单工具
            .tool("query_orders", "查询用户订单", args -> {
                String userId = (String) args.get("user_id");
                int page = (int) args.getOrDefault("page", 1);
                return orderService.queryByUser(userId, page);
            })

            // 报表资源
            .resource("report://monthly/{month}", uri -> {
                String month = uri.variables().get("month");
                return new McpResource(reportService.getMonthlyReport(month));
            })

            .build();
    }
}
```

Client 端自动发现并调用这些工具（任何支持 MCP 的 Agent 都能用）。

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
// LangChain4j 风格 Agent
public class LangChain4jAgent {

    public String run(String query) {
        ChatLanguageModel model = OpenAiChatModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName("gpt-4o")
            .build();

        // 定义工具
        Tool weatherTool = Tool.from(
            "getWeather",
            "获取指定城市的天气",
            (city) -> {
                String cityStr = (String) city;
                return "北京今天 25°C，晴天";
            }
        );

        // 构建 Agent
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

## 九、面试核心要点

1. **Java 怎么接入 LLM？** Spring AI / LangChain4j / HTTP 直调
2. **Spring AI 怎么定义 Tool？** `@Tool` + `@ToolParam` 注解
3. **MCP Server 给 Java 带来什么？** 用标准协议把微服务暴露为 Agent 可用的工具
4. **多轮对话怎么实现？** 维护会话历史 List<Message>，每次注入 Prompt
5. **Dify + Java 什么关系？** Dify 做 AI 编排，Java 做业务逻辑 + 通过 HTTP API 调用

---

## 十、极简总结

```
Spring AI = 原生 SpringBoot AI 方案，2026 年首选
LangChain4j = LangChain 的 Java 移植版，社区活跃
MCP Server = 把 Java 微服务暴露为标准 MCP 工具
Dify API = Dify 编排 Agent，Java 通过 HTTP 调用
Simple Agent = ChatClient + @Tool 注解 + 多轮历史
关键词 = 用 Java 的 Spring 生态来构建/调用 Agent
```
