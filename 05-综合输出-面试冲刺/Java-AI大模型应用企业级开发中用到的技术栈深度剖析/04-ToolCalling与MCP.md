# 04 - Tool Calling 与 MCP

> 定位：工具调用原理、@Tool 设计、MCP 协议、Java 封装企业能力——让 LLM 从"会说话"到"能办事"

## 📚 目录

1. [Tool Calling 原理](#1-tool-calling-原理)
2. [@Tool 声明与设计](#2-tool-声明与设计)
3. [工具设计原则](#3-工具设计原则)
4. [MCP 协议](#4-mcp-协议)
5. [Java 封装 MCP Server](#5-java-封装-mcp-server)
6. [安全与幂等](#6-安全与幂等)

---

## 1. Tool Calling 原理

```
Tool Calling = LLM 调用外部函数的能力

流程：
  ① 声明工具（函数 + Schema）
  ② LLM 判断需要工具 → 返回调用请求（函数名 + 参数）
  ③ 应用执行工具 → 结果返回给 LLM
  ④ LLM 基于结果生成最终回答

⚠️ 面试必答：
"Tool Calling 三步——LLM 决定调哪个工具、
 应用执行工具、结果回填 LLM；
 这是 Agent 的'手'（执行能力）。"
```

---

## 2. @Tool 声明与设计

```java
// Spring AI @Tool 注解声明工具
@Service
public class OrderTools {

    // ⚠️ @Tool：自动生成 Schema，LLM 可见
    @Tool(description = "根据订单号查询订单状态")
    public String queryOrderStatus(@ToolParam(description = "订单号") String orderId) {
        // 实际业务逻辑（查库/调服务）
        Order order = orderService.getById(orderId);
        return order == null ? "订单不存在"
                : "订单 " + orderId + " 状态：" + order.getStatus();
    }

    @Tool(description = "查询商品库存")
    public int queryStock(@ToolParam(description = "商品 ID") Long productId) {
        return stockService.getStock(productId);
    }
}

// 注册给 ChatClient
chatClient.prompt()
        .user("帮我查一下订单 20260701 的状态")
        .tools(orderTools)                      // ⚠️ 提供工具
        .call().content();
```

### 2.1 Tool 的执行链路

```
用户："查订单状态" 
  → LLM 分析 → 需要 queryOrderStatus → 返回调用请求
  → Spring AI 反射调用方法 → 结果"已发货"
  → LLM 组织回答："您的订单已发货"
```

> 🎯 **要点**：@Tool = 声明式工具（自动 Schema + 反射调用）。**描述要写清楚**（LLM 靠描述判断何时用哪个工具）。

---

## 3. 工具设计原则

### 3.1 原子化

```
工具设计五原则：
  ① 原子化：一个工具只做一件事
  ② 参数清晰：描述 + 类型 + 必填
  ③ 返回结构化：便于 LLM 理解
  ④ 命名语义化：query_/create_/update_
  ⑤ 错误返回：不抛异常，返回错误信息

⚠️ 面试必答：
"工具设计 = 原子化 + 语义化——
 工具是 LLM 的'API'，质量决定
 Agent 能否正确使用。"
```

### 3.2 工具 Schema 示例

```json
// LLM 看到的工具描述（自动生成）
{
  "name": "queryOrderStatus",
  "description": "根据订单号查询订单状态",
  "parameters": {
    "type": "object",
    "properties": {
      "orderId": {
        "type": "string",
        "description": "订单号"
      }
    },
    "required": ["orderId"]
  }
}
```

---

## 4. MCP 协议

### 4.1 MCP 是什么

```
MCP（Model Context Protocol）= AI 世界的"USB-C 接口"
  Anthropic 2024 年底推出，2025 大厂跟进，2026 标配

核心抽象：
  Resources（资源）：数据（文档/DB）
  Tools（工具）：可执行能力
  Prompts（提示模板）：复用指令
  Sampling（采样）：模型间调用

⚠️ 面试必答：
"MCP = 模型上下文协议——
 统一'模型如何连接外部工具和数据'，
 类似 USB-C：一次接入、万物可用。"
```

### 4.2 为什么对 Java 重要

```
Java 工程师的 MCP 价值：
  ① 把现有 Spring Boot 服务/DB/MQ 封装成 MCP Server
  ② 权限/鉴权/限流/审计/脱敏都在 Java 层做
  ③ 模型无关：同一 Server 被 OpenAI/Claude/DeepSeek 调用
  ④ 保护现有投资（不用重写）

⚠️ 面试必答：
"MCP 让 Java 存量系统变成 AI 能力——
 封装服务为 MCP Server、安全控制在
 Java 层、任何模型可调用。"
```

---

## 5. Java 封装 MCP Server

```java
// ⚠️ Spring AI MCP Server 配置
// 方式一：工具自动暴露（Spring AI 1.0+）
@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider mcpTools(OrderTools orderTools,
                                         ProductTools productTools) {
        // ⚠️ 把 @Tool 方法自动暴露为 MCP 工具
        return MethodToolCallbackProvider.builder()
                .toolObjects(orderTools, productTools)
                .build();
    }
}

// 方式二：标准 MCP Server（跨框架）
// spring-ai-starter-mcp-server 或 mcp-java-sdk
McpServer server = McpServer.sync(transport).serverInfo("order-server", "1.0")
        .tools(List.of(new QueryOrderTool(), new QueryStockTool()))
        .build();
```

```java
// MCP Client 调用（消费其他系统的 MCP Server）
@Configuration
public class McpClientConfig {

    @Bean
    public McpSyncClient mcpClient() {
        return McpClient.sync(
                HttpTransport.builder("http://product-service/mcp").build())
            .build();
    }
}

// ⚠️ 注解式 Client Handler（Spring AI）
public class ProductMcpHandler {
    @McpServerMethod("query_product")
    public Product queryProduct(@RequestParam String id) { }
}
```

> 🎯 **要点**：MCP 两条路——**Server**（暴露自己的 @Tool 能力）、**Client**（调用别人暴露的能力）。Spring AI 的 MethodToolCallbackProvider 一行把 Service 变成 MCP 工具。

---

## 6. 安全与幂等

### 6.1 工具安全

```
工具调用安全四道防线：
  ① 权限校验：工具级别鉴权（谁可调）
  ② 参数校验：白名单/格式（防注入）
  ③ 审计日志：谁调了什么工具（可追溯）
  ④ 人工接管：高危操作（转账/删除）人工确认

⚠️ 面试必答：
"工具安全 = 权限 + 参数校验 + 审计 + 人工接管；
 LLM 可能被诱导调工具（注入），
 权限最小化是第一防线。"
```

### 6.2 幂等设计（关键）

```
⚠️ LLM 会重试！工具必须幂等：
  例：退款工具被调用两次 → 重复退款！

幂等方案：
  ① 幂等键（请求头 Idempotency-Key）
  ② Redis 去重（键 = 会话 + 工具 + 参数）
  ③ 状态机校验（已退款 → 拒绝）

⚠️ 面试必答：
"工具幂等 = LLM 重试防御——
 Redis 幂等键 + 状态机校验；
 资金类工具（退款/转账）必须幂等。"
```

```java
// 幂等实现示例
@Tool(description = "发起订单退款")
public String refundOrder(@ToolParam(description = "订单号") String orderId) {
    String idempotentKey = "refund:" + orderId;
    // ⚠️ Redis setIfAbsent：已处理过则直接返回
    if (!redis.opsForValue()
            .setIfAbsent(idempotentKey, "1", 24, TimeUnit.HOURS)) {
        return "该订单退款已处理，请勿重复操作";
    }
    return refundService.refund(orderId);   // 只执行一次
}
```

---

> 🎯 **核心要点**：工具与 MCP 体系 = **Tool Calling 三步**（LLM 决策/执行/回填）+ **@Tool 声明式**（Schema 自动生成）+ **原子化设计**（一个工具一件事）+ **MCP 协议**（USB-C 接口 + Java 存量封装）+ **安全四防线 + 幂等**（LLM 重试防御）。"工具是 Agent 的手、MCP 是手的接口"。

---

**返回总览**：[00-JavaAI技术栈总览与全景架构](00-JavaAI技术栈总览与全景架构.md) | **上一篇**：[03-RAG知识库](03-RAG知识库.md) | **下一篇**：[05-Agent架构设计](05-Agent架构设计.md)
