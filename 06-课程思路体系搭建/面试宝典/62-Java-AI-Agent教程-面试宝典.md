# Java AI Agent + Spring AI 面试宝典
> 基于课程大纲全面覆盖面试高频考点 — Spring AI 生态、Tools、RAG、多 Agent 全解析

## 目录
1. [一、基础概念速答](#一基础概念速答)
2. [二、深度原理剖析](#二深度原理剖析)
3. [三、实战场景题](#三实战场景题)
4. [四、手写代码题](#四手写代码题)
5. [五、系统设计题](#五系统设计题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板)
8. [八、快速查漏补缺Checklist](#八快速查漏补缺checklist)

---

## 一、基础概念速答

### 1. What is Spring AI? Spring AI 是什么？
> Spring AI 是 Spring 官方推出的 AI 应用开发框架，提供统一的 ChatModel、Embedding、Tool 等抽象层，简化 Java 项目对接大模型。

| 核心模块 | 功能 |
|---------|------|
| **ChatModel** | 统一调用各类大模型的聊天接口（同步/流式） |
| **ChatClient** | 更高层的流式 Fluent API，封装 ChatModel |
| **Tool / Function-Call** | 让 LLM 调用外部 API/数据库 |
| **Document** | RAG 文档加载、拆分、写入管线 |
| **Advisors** | 拦截链——日志、安全、RAG 上下文增强 |
| **VectorStore** | 向量数据库抽象（Redis/ PGVector/ Milvus） |

> 💡 Spring AI 对标 LangChain4j，但深度集成 Spring 生态（Bean 管理、事务、配置、AOP）。

### 2. Spring AI vs LangChain4j — 关键对比
> 两者都是 Java 生态的 LLM 框架，面试高频问题。

| 对比维度 | Spring AI | LangChain4j |
|---------|-----------|-------------|
| 框架定位 | Spring 官方第一方项目 | 社区驱动，LangChain 的 Java 移植 |
| Spring 集成 | 原生集成（自动配置、Starter） | 需手动整合 |
| 模型接入 | ChatModel 统一接口 + Auto-Configuration | Model 接口 + SPI |
| Tools 声明 | `@Tool` 注解 + `ToolCallback` | `@Tool` 注解 + `ToolSpecification` |
| RAG 管线 | DocumentReader → Transformer → Writer | ContentRetriever 体系 |
| 记忆管理 | `ChatMemory` 接口 + Advisor | `ChatMemory` 接口 |
| Advisor 链 | 内置责任链模式（Around/Before/After） | 无内置拦截链 |
| MCP 支持 | 内置 STDIO/SSE/Streamable HTTP 传输 | 需第三方扩展 |
| 社区生态 | Spring 生态（Cloud、Boot、AI） | 更灵活但分散 |
| 学习曲线 | 平缓（Spring Boot 风格） | 较陡（Function-Call 需手动） |

> 🎯 选型建议：已有 Spring Boot 项目用 Spring AI；非 Spring 项目或需要 LangChain 兼容性用 LangChain4j。

### 3. ChatClient vs ChatModel vs StreamingChatModel
> 三种模型调用方式的区别。

| 类型 | 特点 | 适用场景 |
|------|------|---------|
| **ChatModel** | 同步阻塞调用，返回完整响应 | 对话一次完成，不需要流式输出 |
| **StreamingChatModel** | 异步流式返回 `Flux<ChatResponse>` | 打字机效果、实时输出 |
| **ChatClient** | Fluent API 封装，支持 `.stream()` 和 `.call()` | 推荐首选，灵活切换同步/流式 |

```java
// ChatClient 推荐用法
ChatClient chatClient = ChatClient.builder(chatModel).build();

// 同步调用
String result = chatClient.prompt()
    .user("What is RAG?")
    .call()
    .content();

// 流式调用
Flux<String> stream = chatClient.prompt()
    .user("Explain RAG in detail")
    .stream()
    .content();
```

### 4. @Tool annotation 与 Function-Call 机制
> Tools 是让 LLM 调用真实 API/数据库的桥梁。

```java
@Component
public class FlightTools {
    
    @Tool(name = "query_flight", description = "Query flight information by flight number")
    public String queryFlight(@ToolParam(required = true, description = "flight number, e.g. CA1234") String flightNo) {
        // 查询真实航班数据
        return "Flight " + flightNo + ": Beijing → Shanghai, Depart 08:00, Arrive 10:30";
    }
    
    @Tool(name = "cancel_booking", description = "Cancel a ticket booking")
    public String cancelBooking(@ToolParam(required = true) String bookingId,
                                 @ToolParam(required = false) String reason) {
        return "Booking " + bookingId + " has been cancelled. Reason: " + reason;
    }
}
```

> 💡 `@ToolParam` 上的 `description` 对 LLM 推理至关重要，直接影响工具调用准确率。

### 5. Advisor 是什么？责任链模式如何工作？
> Advisor = LLM 交互拦截器，基于责任链（Chain of Responsibility）模式。

| Advisor 类型 | 作用 | 示例 |
|-------------|------|------|
| **Before Advisors** | 用户消息发送前处理 | 敏感词过滤、日志记录、上下文注入 |
| **Around Advisors** | 包裹整个对话过程 | RAG 上下文增强、Token 计数 |
| **After Advisors** | 模型响应返回后处理 | 结果格式化、溯源记录、审核检查 |

```java
@Bean
public Advisor sensitiveWordAdvisor() {
    return new BeforeAdvisor((request, next) -> {
        // 敏感词过滤
        String filtered = filterSensitiveWords(request.userText());
        AdvisorRequest newRequest = AdvisorRequest.builder()
            .withUserText(filtered)
            .build();
        return next.next(newRequest);
    });
}
```

> 🎯 面试回答：Advisor 基于责任链模式，每个 Advisor 可以决定是否拦截或放行，最终由 `next.next()` 传递到链尾。

### 6. 什么是 RAG？Spring AI 如何实现 RAG 管线？
> RAG = Retrieval-Augmented Generation，检索增强生成。

Spring AI 的 RAG 管线三阶段：

| 阶段 | Spring AI 组件 | 说明 |
|------|--------------|------|
| **DocumentReader** | `PagePdfDocumentReader`, `JsonReader`, `TextReader` | 从文件加载文档 |
| **DocumentTransformer** | `TokenTextSplitter`, `ContentFormatTransformer` | 文档拆分、清洗 |
| **DocumentWriter** | `VectorStore` | 写入向量数据库 |

```java
// RAG 管线示例
@Bean
public VectorStore vectorStore(EmbeddingModel embeddingModel) {
    return new RedisVectorStore(embeddingModel, 
        VectorStoreProperties.builder()
            .indexName("knowledge_base")
            .build());
}

// 注入 RAG 上下文到对话
@Bean
public Advisor ragAdvisor(VectorStore vectorStore) {
    return new AroundAdvisor((request, next) -> {
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.query(request.userText()).withTopK(5));
        String context = docs.stream()
            .map(Document::getContent)
            .collect(Collectors.joining("\n---\n"));
        AdvisorRequest newRequest = AdvisorRequest.builder()
            .withUserText(request.userText())
            .withSystemText("Based on the following context:\n" + context)
            .build();
        return next.next(newRequest);
    });
}
```

### 7. 分片策略有哪几种？各自的优缺点？
> 文档分片是 RAG 质量的关键。

| 分片策略 | 原理 | 优点 | 缺点 |
|---------|------|------|------|
| **Fixed Size Chunking** | 固定 Token 数切分 | 简单高效 | 切断语义 |
| **Recursive Chunking** | 按分隔符递归切分 | 保留语义边界 | 速度较慢 |
| **Semantic Chunking** | 按句意相似度切分 | 语义完整 | 计算成本高 |
| **Agentic Chunking** | LLM 判断切分点 | 质量最高 | 最慢最贵 |
| **Document Specific** | 按文档结构切分（段落/章节） | 结构清晰 | 依赖文档格式 |

> 💡 生产实践：Recursive Chunking（段落级） + 20% Token Overlap 是最常用的方案。

### 8. 什么是 MCP？三种传输机制的区别？
> MCP = Model Context Protocol，LLM 与外部工具之间的标准化通信协议。

| 传输机制 | 通信方式 | 适用场景 |
|---------|---------|---------|
| **STDIO** | 子进程标准输入/输出 | 本地工具、安全要求高的场景 |
| **SSE** | Server-Sent Events | 远程服务、实时推送 |
| **Streamable HTTP** | 可流式 HTTP 长连接 | 跨网络、企业级分布式部署 |

> 🎯 STDIO 适合本地安全隔离，SSE/Streamable HTTP 适合分布式微服务架构。

### 9. Memory 管理如何实现多轮对话？
> Spring AI 使用 `ChatMemory` + `Advisor` 实现记忆。

```java
@Bean
public ChatMemory chatMemory() {
    return new InMemoryChatMemory(20); // 最大保留 20 条消息
}

@Bean
public Advisor memoryAdvisor(ChatMemory chatMemory) {
    return new MessageChatMemoryAdvisor(chatMemory);
}
```

| 存储方案 | 特点 | 适用 |
|---------|------|------|
| **InMemoryChatMemory** | JVM 内存，重启丢失 | 开发测试 |
| **RedisChatMemory** | 分布式、持久化、支持 TTL | 生产环境 |
| **DatabaseChatMemory** | 关系型数据库存储 | 需要事务保证 |
| **MultiLayerMemory** | 短期 + 长期记忆分层 | 复杂对话场景 |

### 10. 什么是 Text-to-SQL？与 Function-Call 的关系？
> Text-to-SQL 让自然语言生成 SQL 查询结构化数据。Function-Call 是更通用的工具调用。

| 对比 | Text-to-SQL | Function-Call |
|------|------------|--------------|
| 输出 | 结构化 SQL | 工具调用 JSON |
| 安全性 | 需权限控制（只读/限表） | 工具级别权限 |
| 复杂度 | 依赖 Schema 描述 | 依赖 Tool 描述 |
| Spring AI 实现 | Tool + SQL 执行器 | `@Tool` 注解 |

```java
@Component
public class SqlQueryTool {
    
    @Tool(name = "query_database", description = "Execute SQL query against the order database")
    public String queryDatabase(@ToolParam(description = "SQL query") String sql) {
        // 安全校验：只允许 SELECT
        if (!sql.trim().toUpperCase().startsWith("SELECT")) {
            return "Only SELECT queries are allowed";
        }
        return jdbcTemplate.queryForList(sql).toString();
    }
}
```

### 11. 什么是 Multi-Agent？Spring AI Alibaba 如何实现？
> Multi-Agent = 多个 Agent 协同完成复杂任务。

| 架构模式 | 说明 | 适用场景 |
|---------|------|---------|
| **Orchestration** | 中心调度 Agent 分配子任务 | 线性流程任务 |
| **Peer-to-Peer** | Agent 间自由通信协作 | 复杂开放问题 |
| **Skills-Agent** | 模块化 Skill 注入单一 Agent（不一定要多 Agent） | 工具密集型场景 |

```java
// Spring AI Alibaba 的 Skills 机制
@Component
public class CustomerServiceAgent {
    
    @Skill
    private FlightTools flightTools;
    
    @Skill
    private OrderTools orderTools;
    
    @Skill
    private RefundTools refundTools;
    
    @Tool("handle_customer_request")
    public String handleRequest(@ToolParam(description = "customer query") String query) {
        // Agent 自动调用 Skill 中的 Tool
        return "Agent processed: " + query;
    }
}
```

### 12. What is Embedding / Vector Embedding?
> Embedding 是将文本/图像转化为固定维度的浮点数向量，语义相近的文本在向量空间中距离相近。

| 模型 | 维度 | 特点 |
|------|------|------|
| BGE-Large | 1024 | 中文效果好，可本地部署 |
| Qwen3-Embedding | 1024 | 通义千问系列 |
| OpenAI-Embedding-3 | 1536 | 英文好，需调用 API |

### 13. Top-K 与长上下文检索的平衡
> Top-K 限制返回的文档块数量，过小可能遗漏、过大可能超 Token 限额。

| 策略 | 说明 |
|------|------|
| Top-K = 3~5 | 最常用，平衡相关性和 Token 消耗 |
| 跨文档长上下文 | 将多个相关文档合并后重排序再截取 |
| 动态 Token 预算 | 根据模型上下文窗口动态计算 K 值 |

### 14. Query Rewrite 与 Rerank 是什么？
> 提升 RAG 检索命中率的两大手段。

| 技术 | 作用 | 实现方式 |
|------|------|---------|
| **Query Rewrite** | 改写用户模糊查询为更精确的查询 | LLM 改写 + 同义词扩展 |
| **Rerank** | 对召回结果重排，提高 Top-K 命中率 | Cross-Encoder 模型打分重排 |

```java
// 查询改写（Query Rewrite）
@Tool("rewrite_query")
public String rewriteQuery(String original) {
    return chatClient.prompt()
        .system("Rewrite the user query to be more specific for document retrieval.")
        .user(original)
        .call()
        .content();
}
```

### 15. 多模态模型怎么处理 PDF 图表？
> 多模态模型（如 Qwen-VL、GPT-4o）能够理解图片/图表内容。

| 技术 | 说明 |
|------|------|
| **OCR** | 提取 PDF 中的文字 |
| **图表解析** | 多模态模型直接理解图表并总结 |
| **Layout Parsing** | PDF 版面分析，区分标题/段落/表格 |

### 16. Spring AI Alibaba Skill 机制是什么？
> Skill 是 Spring AI Alibaba 对 Agent 能力的模块化封装。将一组相关 `@Tool` 组织为一个 Skill 单元，可插拔、可复用、按需注入。

```
┌─────────────────────────────────────┐
│           CustomerAgent              │
│  ┌─────────┐ ┌────────┐ ┌────────┐  │
│  │ Flight  │ │ Order  │ │ Refund │  │
│  │ Skill   │ │ Skill  │ │ Skill  │  │
│  └─────────┘ └────────┘ └────────┘  │
│  Tools: @Skill + @Tool 方法集合      │
└─────────────────────────────────────┘
```

> 💡 当 Tools 超过 30 个时，Skill 的分组策略可以显著提升 LLM 的工具选择准确率。

### 17. DeepSeek 深度思考与流式响应的实现
> DeepSeek 支持深度思考模式（reasoning），Spring AI 通过 StreamingChatModel 实现流式打字机效果。

```java
// 流式响应 + 深度思考
StreamingChatModel model = new DeepSeekChatModel(apiKey);

chatClient.prompt()
    .system("Think step by step before answering.")
    .user("What's the best RAG chunking strategy?")
    .stream()
    .content()
    .subscribe(chunk -> System.out.print(chunk)); // 打印流式输出
```

### 18. 大模型选型：国产 vs 国外
| 模型 | 特点 | 适用场景 |
|------|------|---------|
| DeepSeek | 性价比高，中文理解强 | 通用对话、代码生成 |
| Qwen (通义千问) | 阿里生态，多模态支持 | 企业级应用 |
| GLM (智谱) | 中文优化好 | 知识库、RAG |
| Ollama | 本地部署，隐私保护 | 私有化场景 |
| GPT-4o / Claude | 综合能力强 | 复杂推理、英文场景 |

---

## 二、深度原理剖析

### 1. Advisor 责任链模式源码解析
> Spring AI 的 Advisor 核心接口：

```java
public interface Advisor {
    // Before: 请求发送前拦截
    default AdvisorResponse before(AdvisorRequest request) {
        return null;
    }
    // Around: 请求/响应均可拦截
    default AdvisorResponse around(AdvisorRequest request, AdvisorChain chain) {
        return chain.next(request);
    }
    // After: 响应返回后拦截
    default AdvisorResponse after(AdvisorResponse response) {
        return null;
    }
}
```

责任链顺序：`List<Advisor>` → `BeforeAdvisor.before()` → `AroundAdvisor.around()` → `ChatModel` → `AfterAdvisor.after()`。

> 🎯 面试要点：Advisor 链的调用顺序由 `@Order` 注解控制，类似于 Spring 的 `Filter` 链。

### 2. Tools 参数推理的原理与痛点
> LLM 调用 Tool 时，JSON Schema 由 `@ToolParam` 注解自动生成。

```json
{
  "name": "query_flight",
  "description": "...",
  "parameters": {
    "type": "object",
    "properties": {
      "flightNo": {
        "type": "string",
        "description": "flight number, e.g. CA1234"
      }
    },
    "required": ["flightNo"]
  }
}
```

**核心痛点：**

| 痛点 | 原因 | 解决方案 |
|------|------|---------|
| 参数无法自动推理 | LLM 不确定参数来源 | `@ToolParam` 加详细 `description` |
| Tool 幻觉 | LLM 强行调用不相关 Tool | 限制 Tool 数量 + 增强 System Prompt |
| 参数个数过多 | LLM 选择困难 | Skill 分组，每组 <10 个 Tool |
| 权限越界 | 恶意用户绕开限制 | 参数校验 + 用户身份 Token 传递 |

### 3. RAG 检索全流程原理
```
User Query
    │
    ▼
[Query Rewrite] ── LLM 改写查询
    │
    ▼
[Embedding] ── 向量化
    │
    ▼
[Vector Search] ── Top-K 召回
    │
    ▼
[Rerank] ── Cross-Encoder 重排序
    │
    ▼
[Context Assembly] ── 合并 + Token 截断
    │
    ▼
[LLM Generation] ── 生成最终回答
```

### 4. Memory 存储与淘汰策略源码解读
> Spring AI 的 `ChatMemory` 核心接口：

```java
public interface ChatMemory {
    void add(String conversationId, Message message);
    List<Message> get(String conversationId, int lastN);
    void clear(String conversationId);
}
```

**淘汰策略：**

| 策略 | 说明 | 实现 |
|------|------|------|
| **Size-Based Eviction** | 消息数超过阈值时 FIFO 淘汰 | `lastN` 参数 |
| **Token-Based Eviction** | Token 总数超过上下文窗口时淘汰 | 需自定义实现 |
| **Time-Based Eviction** | 超过 TTL 的旧消息删除 | Redis TTL |

**多层记忆架构：**
```
短期记忆（当前对话） → 汇总 → 长期记忆（用户画像）
     ↓                          ↓
   InMemory/Redis            DB/VectorStore
```

### 5. Structured Output 格式化输出原理
> LLM 输出结构化 JSON 供下游程序解析。

```java
// Spring AI 结构化输出
public record AddressInfo(String province, String city, String district, String detail) {}

AddressInfo address = chatClient.prompt()
    .user("Address: 北京市海淀区中关村大街1号")
    .call()
    .entity(AddressInfo.class);

System.out.println(address.city()); // "北京市"
```

实现原理：Spring AI 在 System Prompt 中注入 JSON Schema 约束，LLM 返回 JSON 后自动反序列化为 POJO。

### 6. MCP STDIO / SSE 原理与安全控制
> MCP 是 LLM 和外部系统的通信桥梁。

| 传输 | 安全性 | 性能 | 适用 |
|------|--------|------|------|
| **STDIO** | 子进程隔离，无网络暴露 | 高 | 本地 Tool |
| **SSE** | 需认证 + HTTPS | 中 | 远程服务 |
| **Streamable HTTP** | OAuth2 / API Key | 中高 | 企业级 |

```java
// MCP STDIO 配置
@Bean
public McpClient stdioClient() {
    return McpClient.using(
        new StdioTransport("/usr/local/bin/my-tool-server")
    ).build();
}
```

### 7. 向量化模型底层原理
> Embedding Model 将文本映射到高维向量空间。

| 阶段 | 说明 |
|------|------|
| Tokenization | 分词 |
| Transformer Encoding | 多层 Attention 编码语义 |
| Pooling | 取 CLS Token 或 Mean Pooling |
| Normalization | L2 归一化，余弦相似度 |

> 💡 余弦相似度 = `cos(A,B) = (A·B)/(||A||·||B||)`，值越接近 1 语义越相似。

### 8. Spring AI vs Dify 企业级选型对比
| 对比维度 | Spring AI | Dify |
|---------|-----------|------|
| 架构模式 | Java 代码 SDK，嵌入业务系统 | 独立平台，Low-Code |
| 自定义能力 | 完全可控 | 受限于平台能力 |
| 部署方式 | 随应用部署 | 独立 Docker 部署 |
| 多模型管理 | 代码配置 | 可视化配置 |
| 工作流编排 | 代码编排 | 拖拽编排 |
| 适用团队 | Java 全栈团队 | 非技术团队也可用 |

> 🎯 选型：深度定制 + Java 团队 → Spring AI；快速验证 + 业务团队 → Dify。

---

## 三、实战场景题

### 1. 航空智能客服
> 场景：用户要求退票，系统需要先查询订单状态再判断是否可以退票。

**回答要点：**
1. 使用 `@Tool` 定义 `queryBooking` 和 `cancelBooking` 两个 Tool
2. 使用 `ChatMemory` 保存多轮对话上下文
3. 使用 `Advisor` 记录操作日志（谁在什么时候做了什么操作）
4. LLM 自动判断：如果订单已出发 → 提示不可退票；如未出发 → 执行退票

### 2. 知识库问答系统（RAG）
> 场景：企业内部文档查询，员工问"去年的报销政策是什么？"

**回答要点：**
1. Recursive Chunking 分片 + 20% Overlap
2. Query Rewrite 改写：LLM 将模糊指代补充完整
3. Rerank 保证 Top-3 召回质量
4. 注入文档版本信息 → 回答中带 "根据 v2.3 版报销政策..."

### 3. 多平台多模型动态配置
> 场景：一个系统同时支持 DeepSeek + 阿里百炼 + Ollama，用户可切换。

```java
@Configuration
public class ModelConfig {
    @Bean
    @ConditionalOnProperty(name = "ai.model", havingValue = "deepseek")
    public ChatModel deepseekChatModel() { return new DeepSeekChatModel(); }
    
    @Bean
    @ConditionalOnProperty(name = "ai.model", havingValue = "aliyun")
    public ChatModel aliyunChatModel() { return new AlibabaAiChatModel(); }
    
    @Bean
    @ConditionalOnProperty(name = "ai.model", havingValue = "ollama")
    public ChatModel ollamaChatModel() { return new OllamaChatModel(); }
}
```

### 4. 投诉工单自动分类
> 场景：用户投诉文本 → 自动提取地址、订单号、投诉类别。

```java
public record ComplaintExtract(
    String category,    // 投诉类别
    String orderId,     // 订单号
    String address,     // 地址
    int urgencyLevel    // 紧急程度 1-5
) {}

ComplaintExtract result = chatClient.prompt()
    .user("我要投诉，订单号12345，快递送到楼下但没通知我，很生气")
    .call()
    .entity(ComplaintExtract.class);
```

### 5. 多 Agent 协同文档分析
> 场景：多个 Agent 分别处理不同模块，最终汇总。

| Agent | 职责 | Skill |
|-------|------|-------|
| SummaryAgent | 文档摘要 | SummarizeSkill |
| QAExtractAgent | QA 对提取 | QASkill |
| DataExtractAgent | 数据抽取 | DataExtractSkill |
| FinalAgent | 汇总整合 | MergeSkill |

### 6. 数据库自然语言查询（Text-to-SQL）
> 场景："上个月销量最高的商品是什么？"

```java
@Component
public class SQLGenerator {
    @Tool("generate_query")
    public String generateSQL(@ToolParam("user question in natural language") String question,
                               @ToolParam("database schema description") String schema) {
        return chatClient.prompt()
            .system("Convert to MySQL query. Schema:\n" + schema)
            .user(question)
            .call()
            .content();
    }
}
```

### 7. PDF 解析 + OCR 多模态方案
> 场景：扫描版 PDF 合同，需提取关键条款。

```java
@Tool("parse_pdf")
public String parsePDF(@ToolParam("PDF file path") String filePath) {
    // Spring AI Document Reader
    var reader = new PagePdfDocumentReader(filePath);
    var docs = reader.get();
    // 多模态模型解析图表
    return chatClient.prompt()
        .system("分析PDF合同内容，提取关键条款")
        .user(docs.stream().map(Document::getContent).collect(Collectors.joining()))
        .call()
        .content();
}
```

### 8. Memory 用户隔离方案
> 场景：多租户系统中每个用户的对话不互相干扰。

```java
@Bean
public ChatMemory chatMemory() {
    return new RedisChatMemory(redisTemplate, userId -> {
        // 用户隔离：每个用户独立的 Redis Key
        return "chat:memory:" + userId;
    });
}
```

### 9. 知识库版本隔离与增量更新
> 当一个知识库文件更新时，需要隔离新旧版本。

| 方案 | 说明 |
|------|------|
| 向量库 Collection 隔离 | 每个版本对应独立 Collection |
| 文档元数据标记 | Document 中带 version 字段，检索时过滤 |
| 全量重建 + 增量清洗 | 定期全量重建 + 增量更新索引 |

### 10. Tools 权限控制
> 不同角色的用户可调用的 Tool 不同。

```java
@Component
public class PermissionControl {
    
    @Tool("query_order")
    public String queryOrder(@ToolParam("order id") String orderId,
                              @ToolParam(hidden = true) String userId) {
        // 校验权限
        if (!hasPermission(userId, "order:query")) {
            throw new SecurityException("No permission");
        }
        return orderService.query(orderId);
    }
}
```

---

## 四、手写代码题

### 1. Handwrite: ChatClient 流式调用 + Tool 方法
> 面试题：请实现一个通过自然语言查询天气的 Agent。

```java
@Component
public class WeatherAgent {
    
    private final ChatClient chatClient;
    
    public WeatherAgent(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }
    
    @Tool(name = "get_weather", description = "Get weather info by city name")
    public String getWeather(@ToolParam(description = "city name, e.g. Beijing") String city) {
        // 模拟调用天气预报 API
        return "Weather in " + city + ": Sunny, 25°C, Humidity 60%";
    }
    
    public void chat(String userMessage) {
        chatClient.prompt()
            .user(userMessage)
            .tools(this)  // 注册 @Tool 方法
            .stream()
            .content()
            .subscribe(System.out::print);
    }
}
```

### 2. Handwrite: 自定义 Advisor 实现日志记录
> 面试题：实现一个记录每次 LLM 请求和响应耗时的 Advisor。

```java
@Order(1)
public class LoggingAdvisor implements Advisor {
    private static final Logger log = LoggerFactory.getLogger(LoggingAdvisor.class);
    
    @Override
    public AdvisorResponse around(AdvisorRequest request, AdvisorChain chain) {
        long start = System.currentTimeMillis();
        log.info("=== LLM Request === User: {}", request.userText());
        
        AdvisorResponse response = chain.next(request);
        
        long elapsed = System.currentTimeMillis() - start;
        log.info("=== LLM Response ({}ms) === Content: {}", elapsed, response.response());
        return response;
    }
}
```

### 3. Handwrite: RAG Document 管线
> 面试题：实现 PDF 文档加载 → 拆分 → 向量化的完整管线。

```java
@Service
public class RagPipelineService {
    
    private final VectorStore vectorStore;
    
    public RagPipelineService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }
    
    public void ingestPdf(String pdfPath) {
        // 1. Load
        var reader = new PagePdfDocumentReader(pdfPath);
        List<Document> docs = reader.get();
        
        // 2. Transform (Split)
        var splitter = new TokenTextSplitter(500, 100, 5, 1000, true);
        List<Document> chunks = splitter.apply(docs);
        
        // 3. Write to VectorStore
        vectorStore.add(chunks);
    }
    
    public List<Document> search(String query, int topK) {
        return vectorStore.similaritySearch(
            SearchRequest.query(query).withTopK(topK));
    }
}
```

### 4. Handwrite: 多层记忆架构
> 面试题：实现短期记忆（Redis）+ 长期记忆（DB）的双层记忆系统。

```java
@Component
public class MultiLayerMemory {
    
    private final StringRedisTemplate redisTemplate;  // 短期记忆（1h TTL）
    private final JdbcTemplate jdbcTemplate;         // 长期记忆
    
    public void addShortTerm(String userId, String message) {
        redisTemplate.opsForList()
            .leftPush("chat:short:" + userId, message);
        redisTemplate.expire("chat:short:" + userId, 1, TimeUnit.HOURS);
    }
    
    public void summarizeToLongTerm(String userId) {
        List<String> shortTerm = redisTemplate.opsForList()
            .range("chat:short:" + userId, 0, -1);
        // LLM 总结后存入 DB
        String summary = chatClient.prompt()
            .user("Summarize this conversation: " + String.join("\n", shortTerm))
            .call()
            .content();
        jdbcTemplate.update(
            "INSERT INTO long_term_memory(user_id, summary, created_at) VALUES(?,?,NOW())",
            userId, summary);
        // 清空短期记忆
        redisTemplate.delete("chat:short:" + userId);
    }
}
```

### 5. Handwrite: MCP Client STDIO 连接
> 面试题：创建一个 MCP Client 连接本地的 Tool 服务。

```java
@Component
public class McpClientConfig {
    
    @Bean
    public McpClient mcpStdioClient() {
        return McpClient.using(
            McpTransport.STDIO,
            new ProcessBuilder("node", "tools-server.js").start()
        ).build();
    }
    
    public void callTool(String toolName, Map<String, Object> args) {
        var result = mcpStdioClient.callTool(
            new McpToolCall(toolName, args));
        System.out.println("Tool result: " + result);
    }
}
```

### 6. Handwrite: 查询改写 + Rerank 完整流程
> 面试题：实现 RAG 检索前改写、检索后排名的完整逻辑。

```java
@Component
public class AdvancedRetriever {
    
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    
    public List<Document> retrieve(String query, int topK) {
        // Step 1: Query Rewrite
        String rewritten = chatClient.prompt()
            .system("Expand the query with synonyms and specific terms for better retrieval.")
            .user(query)
            .call()
            .content();
        
        // Step 2: Vector Search (retrieve more candidates)
        List<Document> candidates = vectorStore.similaritySearch(
            SearchRequest.query(rewritten).withTopK(topK * 3));
        
        // Step 3: Rerank (simple boost)
        return candidates.stream()
            .sorted(Comparator.comparingDouble(doc -> 
                -calculateRelevance(query, doc.getContent())))
            .limit(topK)
            .collect(Collectors.toList());
    }
    
    private double calculateRelevance(String query, String content) {
        // 余弦相似度 or LLM Judge
        return content.contains(query) ? 1.0 : 0.5;
    }
}
```

### 7. Handwrite: 动态多模型配置
> 面试题：实现一个可以根据用户选择动态切换模型的配置。

```java
@Component
public class DynamicModelRouter {
    
    private final Map<String, ChatModel> models;
    
    public DynamicModelRouter(List<ChatModel> allModels) {
        this.models = allModels.stream()
            .collect(Collectors.toMap(m -> m.getClass().getSimpleName(), Function.identity()));
    }
    
    public ChatModel selectModel(String modelName) {
        return models.getOrDefault(modelName, models.get("DefaultChatModel"));
    }
    
    public String chat(String modelName, String message) {
        ChatModel model = selectModel(modelName);
        return ChatClient.builder(model).build()
            .prompt().user(message).call().content();
    }
}
```

### 8. Handwrite: @Tool 权限控制实现
> 面试题：实现一个只有管理员才能调用的 Tool。

```java
@Component
public class AdminTools {
    
    private static final Set<String> ADMIN_USERS = Set.of("admin001", "admin002");
    
    @Tool(name = "system_shutdown", description = "Shutdown the AI service system")
    public String shutdown(@ToolParam(hidden = true) String currentUser) {
        if (!ADMIN_USERS.contains(currentUser)) {
            return "ERROR: Only admin can call this tool. Your access level: user";
        }
        // 实际调用关闭逻辑
        return "System shutdown initiated by " + currentUser;
    }
}
```

---

## 五、系统设计题

### 1. 设计一个企业级航空智能客服系统
> 面试题：请设计一个高可用的航空智能客服系统。

| 模块 | 技术选型 | 说明 |
|------|---------|------|
| **LLM** | DeepSeek + Spring AI | 核心对话引擎 |
| **Tools** | `@Tool` + REST 调用中台 | 查机票、查订单、退票、改签 |
| **RAG** | Redis VectorStore + PDF Reader | 客户 FAQ + 政策文档 |
| **Memory** | RedisChatMemory + DB 持久化 | 对话历史、用户画像 |
| **Advisor** | 日志、权限、敏感词、上下文 | 拦截链 |
| **权限** | OAuth2 + Tools 级别校验 | 不同角色不同能力 |
| **部署** | Kubernetes + Spring Cloud | 弹性伸缩，服务注册 |

**架构图（面试口述）：**
```
用户 → Gateway → ChatClient → Advisor Chain
                                    ↓
                              ChatModel (DeepSeek)
                                    ↓
                          ┌────────┼────────┐
                          ↓        ↓        ↓
                       Tools    RAG     Memory
                       (订单API) (向量库)  (Redis)
```

### 2. 设计一个多租户知识库 RAG 系统
> 面试题：多企业客户共用一个 RAG 系统，如何保证数据隔离？

| 隔离层次 | 方案 |
|---------|------|
| **Collection 隔离** | 每个租户独立 VectorStore Collection |
| **用户隔离** | Redis Key 带租户前缀：`chat:memory:{tenantId}:{userId}` |
| **文档版本隔离** | Document Metadata 带 `tenant_id` 和 `version` |
| **检索过滤** | SearchRequest 时加 Metadata 过滤条件 |

```java
List<Document> docs = vectorStore.similaritySearch(
    SearchRequest.query(text)
        .withFilterExpression("tenant_id == 'tenantA'")
        .withTopK(5));
```

### 3. 设计一个高并发 RAG 系统（性能调优）
> 面试题：QPS 1000+ 的 RAG 系统如何设计？

| 优化点 | 方案 |
|--------|------|
| 向量检索 | 索引预构建 + Redis/Milvus 集群 |
| Embedding 缓存 | LRU 缓存高频 Query 的向量 |
| LLM 调用 | 池化 + 限流 + 降级 |
| 文档预处理 | 异步 Pipeline 写入向量库 |
| 分片策略 | Recursive Chunking + 固定 Overlap |
| 缓存 | Redis 缓存 RAG 结果（相同/相似问题命中缓存） |

### 4. 设计一个 MCP 工具网关
> 面试题：企业内部有上百个老系统需要通过 MCP 暴露给 LLM，如何设计？

```
LLM → MCP Gateway → STDIO/SSE Router
                          ↓
        ┌────────┬────────┼────────┬────────┐
        ↓        ↓        ↓        ↓        ↓
    CRM Tool  ERP Tool  Log Sys   DB Tool  BI Tool
      (STDIO)  (SSE)    (SSE)    (STDIO)  (HTTPS)
```

**关键设计：**
| 需求 | 方案 |
|------|------|
| 工具注册 | MCP 服务端启动时注册 Schema |
| 权限 | OAuth2 + Tool 级别 ACL |
| 限流 | 按用户/工具配额分别限流 |
| 监控 | 每次调用记录耗时、成功率 |

### 5. 设计一个 Multi-Agent 协同系统
> 面试题：设计一个保险理赔的 Multi-Agent 系统。

| Agent | 职责 | 依赖的 Tools |
|-------|------|------------|
| **入口 Agent** | 意图识别，路由到子 Agent | IntentClassifier |
| **文档 Agent** | 解析保单 PDF | PDFParser, OCR |
| **查勘 Agent** | 查询维修报价 | PriceQuery |
| **审核 Agent** | 赔付规则判定 | RuleEngine, PolicyDB |
| **支付 Agent** | 执行赔付 | PaymentAPI |
| **汇总 Agent** | 生成理赔报告 | ReportGenerator |

---

## 六、常见坑点与最佳实践

### 6.1 常见坑点（表格）

| 坑点 | 现象 | 根因 | 解决方案 |
|------|------|------|---------|
| **Tool 参数幻觉** | LLM 虚构参数值（如虚构航班号） | Tool 描述不够精确，或 LLM 强行适配 | `@ToolParam` 加 `description` + `required` + 可选值枚举 |
| **Tool 选择困难** | Tools > 30 个时 LLM 决策变慢或选错 | LLM 在大量 Tools 中推理困难 | 使用 Skill 分组，每组 Tool < 10 个 |
| **记忆溢出** | Token 超限导致对话中断 | ChatMemory 未限制消息数量 | 设置 `lastN` 结合 Token 预算计算 |
| **RAG 上下文冲突** | 旧版本文档干扰新版本文档 | 向量库中同时存在多版本 | Metadata 隔离 + Version 过滤 |
| **流式输出断连** | 流式输出中断或乱码 | 网络抖动 / 模型超时 | 重试机制 + 断线重连逻辑 |
| **Advisor 顺序错误** | 敏感词过滤失效 | Advisor @Order 顺序不对 | 敏感词 Advisor 设为最高优先级 `@Order(0)` |
| **Embedding 不一致** | 检索结果和语义不匹配 | Query 和 Document 使用不同 Embedding 模型 | 统一使用同一个 Embedding Model |
| **多租户数据泄露** | 用户查看到其他租户数据 | 未做 Collection 隔离 | 按 `tenantId` 隔离 Collection + 过滤 |
| **Ollama 流式 BUG** | Ollama 返回流式数据时偶发异常 | Ollama Server 版本兼容性 | 升级 Ollama + 降级到非流式兜底 |

### 6.2 最佳实践

| 领域 | 最佳实践 |
|------|---------|
| **Tools** | 每个 Tool 的 `description` 用自然语言写清楚参数含义和格式 |
| **RAG** | Recursive Chunking + 500 Token/块 + 20% Overlap 是黄金起点 |
| **Advisor** | `@Order(0)` 敏感词 → `@Order(1)` 日志 → `@Order(2)` RAG 上下文 |
| **Memory** | Redis 存储 + TTL 设置 + 多层记忆架构（短期 + 长期） |
| **多模型** | 通过 `@ConditionalOnProperty` 实现运行时动态切换 |
| **权限** | 每个 Tool 中通过 `hidden=true` 参数传递 `userId`，内部做权限校验 |
| **部署** | 使用 GraalVM Native Image 优化 Spring AI 启动速度和内存占用 |
| **测试** | 每个 Tool 单独编写 JUnit 测试，模拟 LLM 调用的 Tool JSON |

---

## 七、面试回答模板

### Template 1: "Describe Spring AI's architecture"
> 请用中文/英文简要描述 Spring AI 的核心架构。

**中文回答：**
Spring AI 的核心架构分为四层：最底层是 ChatModel 抽象，统一对接不同的大模型；第二层是 ChatClient，提供 Fluent API 供业务调用；第三层是 Advisors 拦截链，实现日志、安全、RAG 上下文注入等横切关注点；最上层是业务代码，通过 @Tool 注解暴露服务和 API。

**English Answer:**
Spring AI architecture has four layers: (1) **ChatModel** abstraction layer standardizes LLM access (DeepSeek, Qwen, Ollama); (2) **ChatClient** provides a fluent builder API for prompt construction; (3) **Advisor chain** implements cross-cutting concerns (logging, security, RAG context injection) via Chain of Responsibility pattern; (4) **@Tool annotations** expose business APIs to the LLM through Function-Calling.

### Template 2: "How does Function-Call / @Tool work?"
> 请解释 Spring AI 中 Tools 的工作原理。

**中文回答：**
Spring AI 通过两个层面实现 Tools：第一是 `@Tool` 注解，开发者在 Bean 方法上加注解并编写 `description`，框架自动生成 OpenAPI 兼容的 JSON Schema；第二是运行时，LLM 推理后返回一个 Tool Call 的 JSON，框架自动反射调用对应 Java 方法，并把结果传回 LLM。

### Template 3: "How do you handle RAG quality issues?"
> RAG 检索结果质量不好怎么优化？

**回答要点链：**
1. 分片策略：检查是否用了 Recursive Chunking + Overlap
2. 查询改写：用户问题是否完整/明确 → 用 LLM 改写
3. Embedding 模型：是否使用最适配的模型（BGE-Large 对中文）
4. Top-K + Rerank：多召回 + 重排提升准确率
5. PDF 解析质量：OCR 是否准确
6. 知识库版本：是否有过时文档干扰

### Template 4: "Spring AI 项目的难点和亮点？"
> 面试必问题：讲一个你项目中的亮点或难点。

**模板：**
"一个难点是 Tools 参数推理的幻觉问题。用户说'帮我查一下航班信息'，LLM 经常虚构航班号。我们的解决方案是：第一，在 `@ToolParam` 的 description 中写明参数格式和示例值；第二，增加 Tool 数量限制，分组使用 Skill 机制；第三，添加 Advisor 校验 LLM 输出的参数是否合法，不合法则提示 LLM 重新询问用户。"

### Template 5: "How to design a production-grade RAG system?"
> 如何设计一个生产级别的 RAG 系统？

**中文回答：**
生产级 RAG 系统的关键设计点：1）**文档处理**：使用 Recursive Chunking + Metadata 标记，支持增量更新；2）**检索增强**：Query Rewrite + Rerank 双保险；3）**性能和缓存**：高频查询缓存、Embedding 缓存、异步写入向量库；4）**多租户隔离**：每个租户独立 Collection 或 Metadata 过滤；5）**监控**：检索命中率、LLM 响应时间、Token 消耗等全链路可观测。

---

## 八、快速查漏补缺 Checklist

### Spring AI 基础
- [ ] 知道 Spring AI 与 LangChain4j 的核心区别
- [ ] 会用 ChatClient 的 `.call()` 和 `.stream()` 两种模式
- [ ] 清楚 ChatModel / StreamingChatModel / ChatClient 的层次关系
- [ ] 能够配置多种模型（DeepSeek、阿里百炼、Ollama）

### Tools / Function-Call
- [ ] 会写 `@Tool` 注解方法，理解 `@ToolParam` 的作用
- [ ] 知道 Tool 参数推理的痛点及其解决方案
- [ ] 理解 ToolCallback 的内部机制
- [ ] 知道如何做 Tool 的权限控制
- [ ] 理解 Skill 机制的作用（>30 个 Tool 时的分组策略）

### Advisor 责任链
- [ ] 能手写 Before / Around / After Advisor
- [ ] 知道 `@Order` 控制 Advisor 执行顺序
- [ ] 会通过 Advisor 实现日志、敏感词过滤、RAG 上下文注入

### RAG 检索增强
- [ ] 能说出 5 种分片策略及适用场景
- [ ] 理解 Top-K 与长上下文检索的平衡策略
- [ ] 会实现 RAG 全流程：Reader → Transformer → Writer → Search
- [ ] 理解 Query Rewrite 和 Rerank 的作用
- [ ] 知道 Embedding 的基本原理（余弦相似度）

### Memory 管理
- [ ] 会配置 ChatMemory 实现多轮对话
- [ ] 理解多用户记忆隔离的实现方式
- [ ] 知道 Redis / DB 存储记忆的方案
- [ ] 理解多层记忆架构

### MCP 协议
- [ ] 知道 MCP STDIO / SSE / Streamable HTTP 三种传输机制
- [ ] 了解 MCP 的安全和权限控制方案

### 结构化输出
- [ ] 会使用 `.entity(Class)` 将 LLM 输出转为 POJO
- [ ] 理解结构化输出的原理（System Prompt + JSON Schema）

### 部署与性能
- [ ] 知道 RAG 高并发下的性能优化点
- [ ] 了解向量数据库的扩展策略
- [ ] 知道 GraalVM Native Image 对 Spring AI 的优化意义

### 项目经验准备
- [ ] 能描述一个完整的 RAG 项目（技术栈 + 实现 + 难点）
- [ ] 能描述航空智能客服的 Tools 设计和 Advisor 链
- [ ] 能说出至少 3 个 Tools 相关的坑点和解决方案
- [ ] 能设计 Multi-Agent 协同方案

---

> 🎯 此面试宝典覆盖约 90% 的 Java AI Agent 面试考点。重点掌握：Tools 实践、Advisor 责任链、RAG 全流程、Memory 架构设计。祝面试顺利！
