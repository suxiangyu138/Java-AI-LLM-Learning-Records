# 04 - Spring AI 与 AI 应用生态

> 🎯 Spring AI 是 Java 开发者构建企业级大模型应用（RAG / Agent / MCP）的官方一站式框架 — 2025 年中 1.0 GA，2026 年 1.0/1.1 稳定双轨 + 2.0 适配 Boot 4。与 LangChain4j 并称 Java AI 两大框架

---

## 目录

1. [Spring AI 定位与版本双轨](#1-spring-ai-定位与版本双轨)
2. [核心 API：ChatClient](#2-核心-apichatclient)
3. [RAG 检索增强生成](#3-rag-检索增强生成)
4. [Agent 与五种工作流](#4-agent-与五种工作流)
5. [MCP 模型上下文协议](#5-mcp-模型上下文协议)
6. [对话记忆与工具调用](#6-对话记忆与工具调用)
7. [可观测性与模型评估](#7-可观测性与模型评估)
8. [与 LangChain4j 的对比选型](#8-与-langchain4j-的对比选型)
9. [Spring AI Alibaba](#9-spring-ai-alibaba)

---

## 1. Spring AI 定位与版本双轨

| 版本线 | 状态 | 基线 | 适用 |
|--------|------|------|------|
| **1.0.x** | GA 稳定 | Spring Boot 3.x / Java 17 | 存量 Boot 3 项目 |
| **1.1.x** | GA 稳定 | Boot 3.5 / Java 17 | 增强 MCP、Prompt 缓存、Agent |
| **2.0.x** | 里程碑（M7） | **Boot 4 + Framework 7 / Java 21+** | 新项目首选（对齐 2026 生态） |

```xml
<!-- 1.x：Boot 3 项目 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
<!-- 2.x：Boot 4 项目，Java 21+ -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

**核心设计哲学：** AI 是"新数据库" — 模型是驱动、向量库是索引、Advisor 是查询管道，全部走 Spring 风格的抽象与自动配置。

---

## 2. 核心 API：ChatClient

**统一模型 API** — 支持 20+ 模型：OpenAI、Anthropic、DeepSeek、通义千问、Ollama 等，**切换模型只改配置不改代码**。

```java
// 声明式客户端（Builder 风格，类似 WebClient）
ChatClient chatClient = ChatClient.builder(chatModel).build();

// 流式 + 结构化输出
Flux<String> stream = chatClient.prompt()
    .user("用一句话介绍 Spring AI")
    .stream().content();

// 结构化输出：LLM 直接产出对象（自动 JSON 解析）
public record OrderSummary(String status, double amount) {}
OrderSummary summary = chatClient.prompt()
    .user("解析订单文本")
    .entities(OrderSummary.class)
    .call().entity();
```

**追问：** 与直接调 SDK 的区别？→ 统一抽象（换模型零成本）+ Advisor 管道（RAG/记忆/工具注入）+ 与 Spring 生态（可观测性/配置）深度融合。

---

## 3. RAG 检索增强生成

### 3.1 能力全景

| 能力 | 说明 |
|------|------|
| 向量数据库抽象 | 20+ 种：Redis、pgvector、Milvus、Chroma、Pinecone、Weaviate、Cosmos DB… |
| ETL 框架 | DocumentReader 读本地文件/网页/GitHub/云存储/数据库 → 分块 → 元数据 → 嵌入 |
| Advisor 管道 | QuestionAnswerAdvisor（基础）/ RetrievalAugmentationAdvisor（模块化） |
| 混合查询 | 语义搜索 + SQL-like 过滤语言 |

### 3.2 最小 RAG 应用

```java
// 1. 依赖：spring-ai-starter-vector-store-pgvector（或 redis）

// 2. 注入文档并写入向量库（ETL）
@Autowired VectorStore vectorStore;
Document doc = new Document("Spring Boot 4 于 2025 年 11 月发布...");
vectorStore.add(List.of(doc));          // 自动分块 + 嵌入

// 3. 问答（检索增强）
String answer = chatClient.prompt()
    .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())  // 自动检索注入上下文
    .user("Spring Boot 4 什么时候发布的？")
    .call().content();
```

**追问：** 生产级 RAG 的坑？→ ① 分块策略（chunk size/overlap 影响召回）② 嵌入模型维度与向量库匹配 ③ 检索质量评估（RelevancyEvaluator）④ 知识更新策略（增量写入 vs 重建索引）。

---

## 4. Agent 与五种工作流

Spring AI 官方示例覆盖 **5 种 Agent 工作流模式**：

| 模式 | 机制 | 典型场景 |
|------|------|----------|
| Evaluator Optimizer | 模型自我评估并优化响应 | 高质量内容生成 |
| Routing | 按意图路由到专门处理器 | 客服分流（退换货/物流/投诉） |
| Orchestrator Workers | 动态任务分解 + 专业化处理 | 复杂调研报告 |
| Chaining | 步骤序列逐步处理 | 流水线（提取→翻译→总结） |
| Parallelization | LLM 调用并行执行 + 聚合 | 多维度分析一次给出 |

```java
// 工具调用：Agent 能力的关键 —— @Tool 注解让模型"反向调用"Java
@Component
public class OrderTools {
    @Tool(name = "query_order_status", description = "查询订单状态")
    public String queryOrderStatus(@ToolParam("订单号") String orderId) {
        return orderService.getStatus(orderId);   // 调真实业务
    }
}
```

**追问：** 自主 Agent 需要什么？→ 工具（MCP 动态发现）+ 记忆（短期/长期）+ 循环策略（计划-执行-评估）；Spring 官方有 MCP Agent 孵化项目，2.0 全面重构 Agent 开发体系。

---

## 5. MCP 模型上下文协议

**生态地位：** Spring AI 团队本身就是 **MCP 协议的 Java SDK 提供方**（客户端、服务端、认证授权全套）。

| 方向 | 用法 |
|------|------|
| 作为 MCP **客户端** | `spring-ai-starter-mcp-client`，连接任意 MCP 服务器（stdio / HTTP-SSE） |
| 作为 MCP **服务端** | `spring-ai-starter-mcp-server` + @Tool，暴露给任何 AI 应用 |
| 工具注入 | McpFunctionCallback 把 MCP 工具接入 ChatClient |

```java
// 构建 MCP 服务器：把业务方法暴露给外部 AI 应用
@RestController
public class MCPEndpoint {
    @Tool(description = "查询商品库存")
    public String queryStock(@ToolParam String sku) { ... }
}
```

**追问：** MCP 解决什么？→ 工具生态标准化 — 模型/客户端/工具三方解耦：AI 应用按 MCP 协议接入"任何工具"，工具方写一次到处可用。2.0 中 **Streamable HTTP 取代 SSE** 成为默认服务端协议。

---

## 6. 对话记忆与工具调用

| 记忆层 | 实现 | 说明 |
|--------|------|------|
| 短期记忆 | MessageWindowChatMemory | 最近 N 轮；支持 JDBC/Cassandra/Neo4j 持久化 |
| 长期记忆 | 向量嵌入存储 + 模糊召回 | 跨会话"记住用户" |
| 记忆压缩 | 自动摘要 | 可降低 60% 存储开销 |

```java
ChatMemory memory = MessageWindowChatMemory.builder()
    .chatMemoryStore(new JdbcChatMemoryStore(...))   // 持久化
    .maxMessages(20)
    .build();

ChatClient client = ChatClient.builder(chatModel)
    .defaultAdvisors(new MessageChatMemoryAdvisor(memory))
    .build();
```

**追问：** 多轮对话的工程要点？→ ① 上下文窗口管理（截断/压缩/摘要）② 记忆按用户隔离（chatId 维度）③ 敏感信息脱敏 ④ 成本控制（Token 即钱）。

---

## 7. 可观测性与模型评估

| 能力 | 说明 |
|------|------|
| Micrometer 集成 | Token 消耗、延迟、错误率指标 |
| 评估器 | RelevancyEvaluator（相关性）、FactCheckingEvaluator（事实性） |
| 追踪 | 深度集成 OpenTelemetry（Boot 4 可观测性 2.0） |

**追问：** 为什么 AI 应用评估难？→ 输出不固定，"对不对"需要 LLM 当裁判（LLM-as-Judge）— Spring AI 的 Evaluator 就是这个思路，配合回归测试集做持续评估。

---

## 8. 与 LangChain4j 的对比选型

| 对比 | Spring AI | LangChain4j |
|------|-----------|--------------|
| 出身 | Spring 官方（同 Boot/Cloud 血缘） | 社区项目（对齐 LangChain 概念） |
| 集成 | 与 Spring 生态无缝（自动配置/安全/可观测） | 也支持 Spring 集成，更贴近 LangChain API |
| MCP | 官方 Java SDK 提供方 | 有支持，生态地位不同 |
| 版本节奏 | 1.0/1.1/2.0 双轨清晰 | 版本迭代较快 |
| 学习成本 | 会 Spring 就会一半 | 需要懂 LangChain 概念迁移 |
| 适用 | 已有 Spring 体系的 Java 团队 | 需要 LangChain 生态兼容/概念熟悉的团队 |

**追问：** 怎么选？→ 团队在 Spring 栈内 → Spring AI（一致性最好）；需要 LangChain 工具链兼容/参考开源多 → LangChain4j。两者可共存评估，生产建议二选一。

---

## 9. Spring AI Alibaba

| 能力 | 说明 |
|------|------|
| 模型 | 通义千问（Qwen）系列、百炼平台（Model Studio） |
| 版本 | 1.0 GA（2026） |
| 特色 | 国内模型优化 + Graph 工作流 + AgentFramework |
| MultiAgent | ReactAgent 构建，开发周期从数天压缩到数小时 |

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
</dependency>
```

**追问：** 国内部署怎么选？→ 国内模型（DeepSeek/通义）走 Spring AI Alibaba 或标准 Spring AI + DashScope/DeepSeek starter；需要私有化/内网 → Ollama 或 vLLM 部署 + OpenAI 兼容端点接入。

---

> 🎯 **核心要点**：Spring AI 记忆三句话 — ① **ChatClient** 是唯一入口（模型/流式/结构化）② **RAG + Agent + MCP** 是三大能力（检索增强、工作流、工具生态）③ **1.x 给 Boot 3、2.0 给 Boot 4**。面试被问"Java 怎么做 AI 应用"：Spring AI + RAG + MCP 是标准答案，能讲 5 种 Agent 工作流就是加分。

**下一模块**：[05-Spring技术选型实战指南](05-Spring技术选型实战指南.md) / **返回总览**：[00-Spring生态知识体系总览](00-Spring生态知识体系总览.md)
