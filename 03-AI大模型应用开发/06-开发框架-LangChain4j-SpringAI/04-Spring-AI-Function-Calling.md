# 04 - Spring AI Function Calling

> 🎯 Spring AI 的 Function Calling = @Tool 注解 + 自动 Schema 生成 — 让 LLM 能调用 Java Bean，就像调用本地方法一样简单

## 1. @Tool 注解

```java
@Service
public class WeatherService {
    
    @Tool(description = "获取指定城市的天气信息")
    public String getWeather(
        @ToolParam(description = "城市名称") String city) {
        return weatherApi.query(city);
    }
}

// Spring AI 自动：
// ① 扫描 @Tool 方法 → 生成 JSON Schema
// ② 传给 LLM → LLM 决定调用
// ③ 执行方法 → 结果返回 LLM → 生成最终回答
```

## 2. 注册与调用

```java
@RestController
public class ToolController {
    
    @Autowired private WeatherService weatherService;
    @Autowired private ChatModel chatModel;
    
    @GetMapping("/weather/ask")
    public String ask(@RequestParam String question) {
        // 注册工具
        var toolCallback = MethodToolCallback.builder()
            .toolObject(weatherService)
            .build();
        
        // 带工具的 LLM 调用
        var response = chatModel.call(
            new Prompt(question, 
                OpenAiChatOptions.builder()
                    .withToolCallbacks(List.of(toolCallback))
                    .build()
            )
        );
        
        return response.getResult().getOutput().getContent();
    }
}
```

## 3. 多工具注册

```java
@Configuration
public class ToolConfig {
    
    @Bean
    public List<ToolCallback> tools(
            WeatherService weatherService,
            OrderService orderService,
            CalculatorService calculatorService) {
        return ToolCallbacks.from(
            weatherService,    // 扫描所有 @Tool 方法
            orderService,
            calculatorService
        );
    }
}

// 使用
ChatClient client = ChatClient.builder(model)
    .defaultTools(tools)
    .build();
```

## 4. 完整 Agent 示例

```java
@Service
public class AgentService {
    
    @Tool(description = "查询 MySQL 数据库中的销售数据")
    public List<SaleRecord> queryDatabase(
        @ToolParam(description = "SQL SELECT 语句，仅支持SELECT") String sql) {
        return jdbcTemplate.query(sql, saleRowMapper);
    }
    
    @Tool(description = "执行数学计算")
    public double calculate(
        @ToolParam(description = "数学表达式") String expression) {
        return new ExpressionEvaluator().evaluate(expression);
    }
    
    @Tool(description = "发送邮件")
    public String sendEmail(
        @ToolParam(description = "收件人") String to,
        @ToolParam(description = "邮件主题") String subject,
        @ToolParam(description = "邮件正文") String body) {
        emailService.send(to, subject, body);
        return "邮件已发送";
    }
}

// Agent 对话
// 用户："上个月销售额多少？用邮件发给老板"
// LLM 自动：
//   ① 调用 queryDatabase("SELECT SUM(amount) FROM sales WHERE month=5")
//   ② 调用 sendEmail("boss@company.com", "5月销售报告", "销售额为...")
```

## 5. 工具调用流程

```text
  User: "北京天气怎么样？"
    ↓
  LLM 收到请求 + Tool Schema
    ↓
  LLM 决策 → 需要调用 getWeather({city: "北京"})
    ↓
  Spring AI 执行 getWeather("北京") → "22°C, 晴天"
    ↓
  结果返回 LLM → "北京今天 22°C，晴天，适合户外活动"
```
