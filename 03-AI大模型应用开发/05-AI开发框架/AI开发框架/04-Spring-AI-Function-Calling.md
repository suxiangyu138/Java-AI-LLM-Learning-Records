# 04 - Spring AI Function Calling

> 🎯 Function Calling = LLM 能"动手"调用你的 Java 方法。Spring AI 用 `@Tool` 注解 + 自动 JSON Schema 生成实现了零侵入式工具注册。本章覆盖工具注册、参数解析、多工具编排、异常处理、并行调用、Agent 模式

---

## 目录

1. [Function Calling 原理：四步循环](#1-function-calling-原理四步循环)
2. [@Tool 注解与参数定义](#2-tool-注解与参数定义)
3. [工具注册三种方式](#3-工具注册三种方式)
4. [多工具 Agent 完整示例](#4-多工具-agent-完整示例)
5. [错误处理与工具调用约束](#5-错误处理与工具调用约束)
6. [工具调用流程深度解析](#6-工具调用流程深度解析)
7. [生产级工具设计规范](#7-生产级工具设计规范)

---

## 1. Function Calling 原理：四步循环

```text
① LLM 收到用户请求 + 工具 Schema（自动生成）
② LLM 判断：需要调工具吗？
   ├── 需要 → 返回 function_call(name, arguments)
   │         ③ Spring AI 反射执行被 @Tool 标注的方法
   │         ④ 调用结果返回 LLM → 回到 ①（循环，直到 LLM 决定不再调工具）
   └── 不需要 → 直接生成文本回复
```

---

## 2. @Tool 注解与参数定义

```java
@Service
public class OrderService {

    @Tool(description = "根据订单号查询订单状态，返回订单详情")
    public OrderStatus queryOrder(
        @ToolParam(description = "订单号，格式 ORD-年月日-序号，如 ORD-20260801-001")
        String orderId) {
        return orderRepo.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Tool(description = "取消指定订单，仅支持未发货的订单")
    public String cancelOrder(
        @ToolParam(description = "订单号") String orderId,
        @ToolParam(description = "取消原因") String reason) {
        orderService.cancel(orderId, reason);
        return "订单 " + orderId + " 已取消";
    }
}
```

**@ToolParam 最佳实践：**
| 技巧 | 示例 | 效果 |
|------|------|------|
| 详细描述 | `"城市名称，如 北京、上海，支持中英文"` | LLM 更准确提取参数 |
| 格式约束 | `"订单号格式 ORD-年月日-序号"` | 减少格式错误 |
| 默认值 | `required = false` + 默认值 | 可选参数 |

---

## 3. 工具注册三种方式

```java
// 方式 1：单个 Bean 注册
var callback = MethodToolCallback.builder()
    .toolObject(weatherService)
    .build();
chatClient.prompt().tools(callback).user("天气").call();

// 方式 2：批量扫描（推荐 — 自动发现所有 @Tool 方法）
@Bean
public ToolCallbacks tools(WeatherService weather, OrderService order) {
    return ToolCallbacks.from(weather, order);
    // 框架内部：扫描每个 Bean → 找到 @Tool 方法 → 生成 JSON Schema
}

// 方式 3：ChatClient 默认工具（全局生效）
@Bean
public ChatClient agentClient(ChatModel model, ToolCallbacks tools) {
    return ChatClient.builder(model)
        .defaultTools(tools)  // ← 所有调用自动带上这些工具
        .build();
}

@Autowired private ChatClient agentClient;
// 所有 agentClient 调用自动有 weather 和 order 工具可用
String reply = agentClient.prompt().user("查 ORD-001 订单").call().content();
```

---

## 4. 多工具 Agent 完整示例

```java
@Component
public class DataAnalysisTools {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PythonExecutor python;

    @Tool(description = "查询数据库中的销售数据，仅支持 SELECT 语句")
    public List<Map<String, Object>> queryDB(
        @ToolParam(description = "SQL SELECT 语句，请确保只查询不修改") String sql) {
        return jdbc.queryForList(sql);
    }

    @Tool(description = "对数据进行统计分析，返回计算结果")
    public double analyze(
        @ToolParam(description = "数据集（JSON 数组格式）") String dataJson,
        @ToolParam(description = "分析类型：avg/sum/max/min/count") String type) {
        return python.executeAnalysis(dataJson, type);
    }

    @Tool(description = "生成图表并保存为 PNG，返回文件路径")
    public String chart(
        @ToolParam(description = "图表数据（JSON）") String data,
        @ToolParam(description = "图表类型：bar/line/pie") String chartType) {
        return python.generateChart(data, chartType);
    }
}

// Agent 对话示例：
// 用户："分析上个月各产品线的销售额，做成柱状图"
// LLM 自动编排：
//   ① queryDB("SELECT product, SUM(amount) FROM sales WHERE month=5 GROUP BY product")
//   ② analyze(jsonData, "sum")
//   ③ chart(jsonData, "bar")
//   ④ "上个月产品A销售额120万，产品B销售额85万...图表已生成：/charts/may-sales.png"
```

---

## 5. 错误处理与工具调用约束

```java
@Tool(description = "查询用户信息")
public String queryUser(@ToolParam(description = "用户ID") String userId) {
    try {
        return userService.query(userId).toString();
    } catch (UserNotFoundException e) {
        // ★ 关键：返回可读的错误信息给 LLM，让它决定下一步
        return "未找到用户：" + userId + "，请核实用户ID是否正确";
        // ❌ 不要抛异常 — LLM 收到 exception message 后会困惑
    }
}

// 工具调用配置
OpenAiChatOptions options = OpenAiChatOptions.builder()
    .toolCallbacks(tools)
    .toolChoice("auto")           // auto|none|required — LLM 自主决定是否调工具
    .parallelToolCalls(true)      // 允许并行调用多个工具（GPT-4+ 支持）
    .build();
```

**工具设计规范：**

| 规范 | 原因 |
|------|------|
| **工具方法幂等** | 同一参数多次调用结果一致（LLM 可能重复调用） |
| **catch 后返回友好描述** | 让 LLM 理解错误并调整（而非收到异常堆栈） |
| **工具超时控制** | 单个工具 ≤10s（更长的用异步 + 状态查询模式） |
| **敏感操作需确认** | 取消订单/删除等 → 增加确认步骤或限制参数 |
| **返回值精简** | 大量数据时只返回摘要或分页（LLM 上下文有限） |

---

## 6. 工具调用流程深度解析

```text
完整的一次 Function Calling 流程（源码追踪）：

① Spring AI 扫描 @Tool → 生成 ToolFunction（含 name/description/parameters JSON Schema）
② 构建 ChatRequest 时，ToolFunction 序列化为 OpenAI 兼容的 tools 参数
③ LLM 返回 ChatResponse → 检查 getOutput().hasToolCalls()
   ├── 是 → ④ 从 ToolCall 中提取 functionName + arguments（JSON String）
   │        ⑤ ToolCallback.call(functionName, arguments)
   │        ⑥ 内部通过反射调用被 @Tool 标注的方法
   │        ⑦ 参数自动从 JSON 反序列化为 Java 类型
   │        ⑧ 方法返回值序列化为 String → 构建 ToolResponseMessage
   │        ⑨ 附加到对话历史中 → 重新调用 LLM（goto ③）
   └── 否 → 直接返回文本内容

关键类：
  - ToolCallback：工具执行器接口
  - MethodToolCallback：基于反射的工具执行器
  - DefaultToolCallManager：管理工具调用的默认实现
```

---

## 7. 生产级工具设计规范

```java
// ★ 推荐的 Agent Service 结构模板
@Component
public class ProductionAgent {

    // ① 工具：每个 @Tool 方法职责单一
    @Tool(description = "查询指定时间范围内的销售数据")
    public List<SaleRecord> querySales(
        @ToolParam(description = "开始日期，格式 yyyy-MM-dd") String startDate,
        @ToolParam(description = "结束日期，格式 yyyy-MM-dd") String endDate) {
        // ② 参数校验
        LocalDate.parse(startDate);  // 格式校验
        LocalDate.parse(endDate);
        // ③ 限流/计数控制（防 LLM 循环调用）
        toolCallCounter.incrementAndGet();
        if (toolCallCounter.get() > 10) {
            return List.of();  // 超过10次不再返回数据
        }
        // ④ 返回精简结果
        List<SaleRecord> results = repo.query(startDate, endDate);
        if (results.size() > 50) {
            return results.subList(0, 50);  // 截断，加"更多数据请查系统"提示
        }
        return results;
    }
}
```

---

> 🎯 **核心要点**：Spring AI Function Calling 三件事 — **① @Tool 注解声明工具（LLM 通过 JSON Schema 理解）② ToolCallbacks.from() 批量注册 ③ ChatClient.defaultTools() 全局注入**。工具设计黄金法则：**幂等方法 + catch 返回友好描述（不给 LLM 异常堆栈）+ 超时/次数上限（防死循环）+ 返回值精简（大结果截断）**。

**下一模块**：[05-Spring AI 多模态与生产](05-Spring-AI-多模态与生产.md) / **返回总览**：[00-总览](00-Java-AI开发框架体系总览.md)
