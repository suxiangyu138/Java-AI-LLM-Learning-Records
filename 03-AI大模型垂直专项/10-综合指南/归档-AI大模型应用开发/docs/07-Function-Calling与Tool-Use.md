# 07 — Function Calling 与 Tool Use

> **目标**：深入掌握 Function Calling 的机制和 Java 实现。这是连接 LLM 与业务系统的"桥梁"。

---

## 1. 核心概念

### 1.1 什么是 Function Calling

> **Function Calling（函数调用/工具使用）**：LLM 在需要时生成结构化的函数调用请求，由开发者的代码实际执行函数，结果再回传给 LLM。**LLM 不执行任何代码，它只决定"何时调用、调用哪个函数、传什么参数"。**

```
普通调用：
  User → LLM → Text Response
  (纯文本输入输出)

Function Calling：
  User → LLM → Function Call Request (JSON)
                     │
                     ▼  (你的代码执行)
               Function Result
                     │
                     ▼
              LLM → Text Response
  (LLM 决定调用函数，你的代码执行，结果回传)
```

### 1.2 完整时序图

```
User                 Backend               LLM API            外部服务
  │                     │                     │                  │
  │ "北京天气怎么样？"    │                     │                  │
  │────────────────────►│                     │                  │
  │                     │  Chat Request        │                  │
  │                     │  + Tool Definitions  │                  │
  │                     │────────────────────►│                  │
  │                     │                     │                  │
  │                     │  Tool Call:          │                  │
  │                     │  get_weather(        │                  │
  │                     │    city="北京")       │                  │
  │                     │◄────────────────────│                  │
  │                     │                     │                  │
  │                     │  HTTP GET /weather?city=北京            │
  │                     │─────────────────────────────────────►│
  │                     │                     │                  │
  │                     │  {"temp": 25, "weather": "晴"}        │
  │                     │◄─────────────────────────────────────│
  │                     │                     │                  │
  │                     │  Chat Request        │                  │
  │                     │  + Tool Result       │                  │
  │                     │────────────────────►│                  │
  │                     │                     │                  │
  │                     │  "北京今天晴，        │                  │
  │                     │   气温25°C"          │                  │
  │                     │◄────────────────────│                  │
  │                     │                     │                  │
  │ "北京今天晴，25°C"    │                     │                  │
  │◄────────────────────│                     │                  │
```

---

## 2. Tool Schema 设计

### 2.1 JSON Schema 规范

```json
{
  "type": "function",
  "function": {
    "name": "get_weather",
    "description": "获取指定城市的实时天气信息。返回温度、天气状况、湿度等。",
    "parameters": {
      "type": "object",
      "properties": {
        "city": {
          "type": "string",
          "description": "城市名称，如'北京'、'上海'。支持中文和英文。"
        },
        "unit": {
          "type": "string",
          "enum": ["celsius", "fahrenheit"],
          "description": "温度单位。celsius=摄氏度，fahrenheit=华氏度。默认celsius。"
        }
      },
      "required": ["city"]
    }
  }
}
```

### 2.2 Schema 设计最佳实践

```
✅ 好的 Tool 定义：
  1. name: 使用 snake_case，动词_名词 (get_weather, search_documents)
  2. description: 详细描述功能和使用场景
     → 坏的："获取天气"
     → 好的："获取指定城市的实时天气信息，包括温度、湿度、风速和天气状况"
  3. 参数 description: 说明格式、约束、默认值
     → 坏的："city - 城市"
     → 好的："city - 城市名称，支持中文和英文，如'北京'或'Beijing'"
  4. enum: 枚举值用语义化命名 (celsius / fahrenheit)
  5. required: 标注必需参数

❌ 避免：
  1. 参数名过于简短/模糊 (x, arg1, data)
  2. 一个 Tool 做太多事情（会导致 LLM 困惑）
  3. 不加 description（LLM 不知道何时该用）
```

---

## 3. Spring AI Function Calling

### 3.1 声明式 Tool 定义

```java
// ===== 方式 1：@Tool 注解（推荐） =====
@Component
public class WeatherTools {

    @Tool(description = "获取指定城市的实时天气信息，包括温度、湿度、天气状况")
    public WeatherInfo getWeather(
        @ToolParam(description = "城市名称，如'北京'、'上海'") String city,
        @ToolParam(description = "温度单位：celsius(摄氏度) 或 fahrenheit(华氏度)")
        String unit
    ) {
        // 调用实际天气 API
        double temp = fetchTemperature(city, unit);
        String condition = fetchCondition(city);
        double humidity = fetchHumidity(city);
        return new WeatherInfo(city, temp, unit, condition, humidity);
    }
}

// 返回类型会被自动序列化为 JSON
record WeatherInfo(String city, double temperature, String unit,
                   String condition, double humidity) {}

// ===== 方式 2：手动定义 Function =====
@Component
public class DatabaseQueryTool implements Function<DatabaseQueryTool.Request,
                                                    DatabaseQueryTool.Response> {

    private final JdbcTemplate jdbcTemplate;

    @JsonPropertyDescription("数据库查询工具，用于查询业务数据")
    public record Request(
        @JsonPropertyDescription("SQL查询语句，仅支持SELECT") String sql,
        @JsonPropertyDescription("数据库名称，如'orders_db'或'users_db'") String database
    ) {}

    public record Response(List<Map<String, Object>> rows, int count) {}

    @Override
    public Response apply(Request request) {
        var rows = jdbcTemplate.queryForList(request.sql);
        return new Response(rows, rows.size());
    }
}
```

### 3.2 注册和使用 Tool

```java
@Configuration
public class ToolConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                  WeatherTools weatherTools,
                                  DatabaseQueryTool dbTool,
                                  SearchTool searchTool) {
        return builder
            .defaultTools(weatherTools)     // 全局注册的 Tool
            .build();
    }

    // 或者在每次调用时动态注册
    // chatClient.prompt()
    //     .tools(weatherTools)     // 仅本次调用可用
    //     .user("...")
    //     .call()
}

// 使用示例
@RestController
public class AssistantController {

    private final ChatClient chatClient;

    @PostMapping("/assistant")
    public String assistant(@RequestBody String userMessage) {
        return chatClient.prompt()
            .user(userMessage)
            .call()
            .content();
    }
}
```

### 3.3 手动处理 Tool Call（底层 API）

```java
@Service
public class ManualToolCallService {

    private final ChatModel chatModel;
    private final Map<String, Function<Map<String, Object>, String>> tools = new HashMap<>();

    public String chat(String userMessage) {
        List<Message> messages = new ArrayList<>();
        messages.add(new UserMessage(userMessage));

        // 构建 Tool 定义
        List<org.springframework.ai.model.tool.ToolDefinition> toolDefs =
            buildToolDefinitions();

        while (true) {
            // 调用 LLM
            var prompt = new Prompt(messages,
                OpenAiChatOptions.builder()
                    .withTools(toolDefs)
                    .build());

            var response = chatModel.call(prompt);
            var assistantMsg = response.getResult().getOutput();

            // 检查是否有 Tool Call
            if (!assistantMsg.hasToolCalls()) {
                return assistantMsg.getContent();  // 纯文本回复，结束
            }

            // 添加 Assistant Message
            messages.add(assistantMsg);

            // 执行每个 Tool Call
            for (var toolCall : assistantMsg.getToolCalls()) {
                String toolName = toolCall.name();
                String args = toolCall.arguments();
                String result = executeTool(toolName, args);

                // 添加 Tool Result Message
                messages.add(new ToolResponseMessage(
                    List.of(new ToolResponseMessage.ToolResponse(
                        toolCall.id(), toolName, result))));
            }
        }
    }

    private String executeTool(String name, String argsJson) {
        Function<Map<String, Object>, String> tool = tools.get(name);
        if (tool == null) return "Error: tool not found";

        try {
            Map<String, Object> args = objectMapper.readValue(argsJson,
                new TypeReference<>() {});
            return tool.apply(args);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
```

---

## 4. 并行 Function Calling

### 4.1 何时并行调用

```
LLM 自动判断何时并行：
  独立调用 → 并行
  依赖调用 → 串行

示例 1（并行）：
  "比较北京、上海、广州的天气" → 同时调用 get_weather × 3

示例 2（串行）：
  "先查北京天气，如果下雨就推荐室内活动" →
    get_weather("北京") → 结果是"雨"
    → recommend_indoor_activity("北京")
```

### 4.2 Java 并行工具执行

```java
@Service
public class ParallelToolExecutor {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 并行执行多个 Tool Call
     */
    public Map<String, String> executeParallel(
            List<ToolCall> toolCalls) {

        List<CompletableFuture<Map.Entry<String, String>>> futures =
            toolCalls.stream()
                .map(tc -> CompletableFuture.supplyAsync(() -> {
                    String result = executeSingleTool(tc);
                    return Map.entry(tc.id(), result);
                }, executor))
                .toList();

        // 等待所有完成
        return futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
```

---

## 5. 流式 Tool Call

### 5.1 流式 Tool Call 流程

```
流式 Function Calling 的特殊之处：
  LLM 在流式输出过程中可能"插入"Tool Call

处理流程：
  SSE Stream → 逐 chunk 接收
    ├── 如果是文本 chunk → 直接推给前端
    ├── 如果检测到 Tool Call 开始 → 缓存 Tool Call 参数
    ├── Tool Call 参数接收完毕 → 执行 Tool
    └── Tool 结果回传 → 继续流式接收
```

### 5.2 Spring AI 流式处理

```java
@RestController
public class StreamingToolController {

    private final ChatClient chatClient;
    private final Sinks.Many<ServerSentEvent<String>> sink;

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@RequestParam String message) {
        return chatClient.prompt()
            .user(message)
            .tools(new WeatherTools())
            .stream()
            .chatResponse()
            .flatMap(response -> {
                if (response.hasToolCalls()) {
                    // Tool Call 事件（不显示给用户）
                    return Flux.just(ServerSentEvent.<String>builder()
                        .event("tool-call")
                        .data(toJson(response.getToolCalls()))
                        .build());
                } else {
                    // 文本流
                    return Flux.just(ServerSentEvent.<String>builder()
                        .data(response.getContent())
                        .build());
                }
            });
    }
}
```

---

## 6. 企业级 Tool 管理

### 6.1 工具分组与权限

```java
@Component
public class EnterpriseToolManager {

    // 工具分组
    public enum ToolGroup {
        READ_ONLY,      // 只读操作：查询、搜索
        WRITE,          // 写操作：创建、更新
        DANGEROUS,      // 危险操作：删除、发邮件
        SYSTEM          // 系统级操作
    }

    // 工具注册（带元数据）
    private final Map<String, ToolMetadata> toolRegistry = new ConcurrentHashMap<>();

    record ToolMetadata(
        String name,
        String description,
        ToolGroup group,
        boolean requiresApproval,
        long maxCallsPerMinute,     // 频率限制
        long maxTokensPerCall,      // 单次调用 Token 限制
        Function<Map<String, Object>, String> executor
    ) {}

    public void register(ToolMetadata tool) {
        toolRegistry.put(tool.name(), tool);
    }

    /**
     * 为特定用户组生成可用工具列表
     * 不同权限等级的用户看到不同的工具
     */
    public List<Map<String, Object>> getToolsForUser(UserRole role) {
        return toolRegistry.values().stream()
            .filter(tool -> isAuthorized(tool, role))
            .map(this::toOpenAIFunction)
            .collect(Collectors.toList());
    }

    private boolean isAuthorized(ToolMetadata tool, UserRole role) {
        return switch (tool.group()) {
            case READ_ONLY -> true;
            case WRITE    -> role == UserRole.ADMIN || role == UserRole.EDITOR;
            case DANGEROUS -> role == UserRole.ADMIN;
            case SYSTEM    -> false;
        };
    }
}
```

### 6.2 工具超时与熔断

```java
@Component
public class ResilientToolExecutor {

    // 为每个工具创建独立的熔断器
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    public String execute(String toolName, Map<String, Object> params) {
        // 1. 限流检查
        RateLimiter limiter = rateLimiters.computeIfAbsent(toolName,
            k -> RateLimiter.ofDefaults(toolName));
        if (!limiter.acquirePermission()) {
            return "Error: Rate limit exceeded for " + toolName;
        }

        // 2. 熔断检查 + 执行
        CircuitBreaker cb = circuitBreakers.computeIfAbsent(toolName,
            k -> CircuitBreaker.ofDefaults(toolName));

        return cb.executeSupplier(() -> {
            try {
                return doExecute(toolName, params);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
```

---

## 7. 常见陷阱与调试

### 7.1 常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| LLM 不调用 Tool | Tool 描述不够清晰 | 优化 description，明确使用场景 |
| LLM 调用了错误的 Tool | Tool 功能重叠 | 区分 Tool 边界，考虑合并 |
| Tool 参数错误（类型/格式） | LLM 对参数理解有偏差 | 参数 description 加格式说明和示例 |
| 死循环调用 | LLM 不断调同一个 Tool | 设置 max iterations |
| Tool 结果太大 | 返回了整个数据库表 | 结果截断、分页、摘要 |
| 结果时序混乱 | 并行调用结果返回顺序不确定 | 用 tool_call_id 关联 |

### 7.2 调试技巧

```java
// 添加 Tool Call 日志
@Component
public class ToolCallLogger implements Advisor {

    @Override
    public AdvisedResponse advise(AdvisedRequest request, ...) {
        // 记录 Tool 调用详情
        log.info("=== Tool Call Debug ===");
        log.info("User Input: {}", request.userText());
        log.info("Available Tools: {}", request.toolDefinitions());

        AdvisedResponse response = next.advise(request, context);

        if (response.hasToolCalls()) {
            for (var tc : response.getToolCalls()) {
                log.info("Tool Called: {} | Args: {} | Result: {}",
                    tc.name(), tc.arguments(), tc.result());
            }
        }
        return response;
    }
}
```

---

## 8. 快速复习

```
□ Function Calling 机制：LLM 不执行代码，只决定何时调谁传什么参
□ Tool Schema 设计：snake_case 命名、详细 description、参数约束
□ Spring AI @Tool 注解：声明式定义 + 自动注册
□ 并行 Tool Call：独立调用并行，依赖调用串行
□ 流式 Tool Call：text chunk + tool_call chunk 交替
□ 企业级 Tool 管理：分组、权限、限流、熔断
□ 常见问题：参数错误、死循环、结果过大
```

---

> **下一步**：[08 — MCP 模型上下文协议](./08-MCP模型上下文协议.md)
