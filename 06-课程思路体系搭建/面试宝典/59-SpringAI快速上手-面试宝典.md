# Spring AI 快速上手 面试宝典
> 基于课程大纲全面覆盖面试高频考点 — Spring AI 生态、RAG 实现、LCEL 语法全解析

## 目录

1. [一、基础概念速答（18题）](#一基础概念速答18题)
2. [二、深度原理剖析（12题）](#二深度原理剖析12题)
3. [三、实战场景题（10题）](#三实战场景题10题)
4. [四、手写代码题（8题）](#四手写代码题8题)
5. [五、系统设计题（5题）](#五系统设计题5题)
6. [六、常见坑点与最佳实践（表格）](#六常见坑点与最佳实践表格)
7. [七、面试回答模板（Top 5）](#七面试回答模板top-5)
8. [八、快速查漏补缺 Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答（18题）

### Q1：什么是 Spring AI？

Spring AI 是 Spring 生态官方推出的 AI 框架，提供抽象层用于集成 LLM（大语言模型）、向量数据库、Embedding 模型和文档处理管道。核心目标是让 Java 开发者用最少的样板代码构建 AI 应用。

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0-M6</version>
</dependency>
```

### Q2：Spring AI 核心接口有哪些？

| 接口 | 职责 | 类比 |
|------|------|------|
| `ChatClient` | 同步/流式调用 LLM，支持 Prompt Template | RestTemplate 之于 HTTP |
| `ChatModel` | 底层 LLM 调用抽象 | JDBC 之于数据库 |
| `StreamingChatModel` | 流式返回 Chat 结果 | SSE 流式响应 |
| `EmbeddingClient` | 文本 -> 向量转换 | 编码器 |
| `VectorStore` | 向量存储与相似性搜索 | 向量数据库的统一抽象 |
| `DocumentReader` | 读取不同格式文档（PDF, TXT, HTML） | 文件解析器 |
| `DocumentTransformer` | 文档拆分、清洗 | ETL 中的 Transform |
| `DocumentWriter` | 将文档写入 VectorStore | ETL 中的 Load |

### Q3：ChatClient 的基本用法？

```java
@RestController
public class ChatController {
    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/ai/chat")
    public String chat(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
```

### Q4：如何实现流式聊天（StreamingChatModel）？

```java
@GetMapping("/ai/stream")
public Flux<String> stream(@RequestParam String message) {
    return chatClient.prompt()
            .user(message)
            .stream()
            .content();  // 返回 Flux<String>
}
```

### Q5：Spring AI 如何配置模型参数？

```java
// 方式一：application.yml 全局配置
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          temperature: 0.7
          max-tokens: 2048
          model: gpt-4o

// 方式二：运行时动态设置
ChatResponse response = chatClient.prompt()
        .user("Hello")
        .options(ChatOptionsBuilder.builder()
                .withTemperature(0.5)
                .withMaxTokens(1000)
                .build())
        .call()
        .chatResponse();
```

### Q6：Spring AI 支持哪些模型供应商？

- OpenAI（GPT-4o, GPT-4, GPT-3.5）
- DeepSeek（DeepSeek V2/V3）
- Zhipu AI（GLM-4, GLM-4-Flash）
- 通义千问（Qwen2, Qwen3）
- Ollama（本地部署，支持 Llama、Mistral、Qwen 等）
- Azure OpenAI
- Anthropic Claude
- Google Vertex AI Gemini

### Q7：什么是 Embedding？

Embedding 是将文本映射为高维向量的过程，使语义相近的文本在向量空间中距离相近。Spring AI 通过 `EmbeddingClient` 抽象：

```java
EmbeddingRequest request = new EmbeddingRequest(List.of("Spring AI is great"),
        EmbeddingOptions.EMPTY);
EmbeddingResponse response = embeddingClient.call(request);
List<Double> vector = response.getResult().getOutput();
// vector 维度：OpenAI text-embedding-3-small -> 1536 维
```

### Q8：如何使用 Zhipu AI Embedding？

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-zhipuai-spring-boot-starter</artifactId>
</dependency>
```

```yaml
spring:
  ai:
    zhipuai:
      api-key: ${ZHIPU_API_KEY}
      embedding:
        enabled: true
        options:
          model: embedding-2
```

```java
@Autowired
private EmbeddingClient embeddingClient;

public double[] embed(String text) {
    return embeddingClient.embed(text);  // 返回 double[]
}
```

### Q9：Embedding vs One-Hot 编码的区别？

| 维度 | Embedding | One-Hot |
|------|-----------|---------|
| 维度大小 | 低维（几百到几千） | 高维（等于词表大小） |
| 语义关系 | 能表示"猫"和"狗"相似 | 任意两个词正交，无语义关系 |
| 稀疏性 | 密集向量 | 稀疏向量 |
| 学习能力 | 可训练，能捕捉上下文 | 静态，无上下文信息 |
| 适用范围 | NLP 全场景 | 传统 ML 分类特征 |

### Q10：什么是 RAG（检索增强生成）？

RAG = Retrieval Augmented Generation。核心流程：用户提问 → 检索相关知识 → 将上下文拼入 Prompt → LLM 生成答案。解决 LLM 知识过时和幻觉问题。

```
用户问题 --> 向量检索(相似度搜索) --> 检索结果 + 原始问题
                                         |
                                         v
                                      LLM 生成 --> 上下文增强的回答
```

### Q11：Spring AI 中 RAG 的核心组件？

- **DocumentReader**: 读取源文档（PDF, TXT, HTML）
- **DocumentTransformer**: 文本分块（TextSplitter）
- **DocumentWriter**: 写入 VectorStore
- **VectorStore**: 存储向量并执行相似性搜索
- **QuestionAnswerAdvisor**: 将检索结果注入 Prompt
- **RetrievalAugmentationAdvisor**: 高级 RAG 编排

### Q12：什么是余弦相似度（Cosine Similarity）？

余弦相似度是两个向量之间夹角的余弦值，范围 `[-1, 1]`。值越接近 1，表示两个向量越相似。

```java
public static double cosineSimilarity(List<Double> v1, List<Double> v2) {
    double dotProduct = 0.0, normA = 0.0, normB = 0.0;
    for (int i = 0; i < v1.size(); i++) {
        dotProduct += v1.get(i) * v2.get(i);
        normA += Math.pow(v1.get(i), 2);
        normB += Math.pow(v2.get(i), 2);
    }
    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
}
```

### Q13：Spring AI 支持的向量数据库有哪些？

| 数据库 | 类型 | 特点 |
|--------|------|------|
| FAISS | 本地内存 | 高性能，适合小规模原型 |
| Chroma | 嵌入式/服务端 | Python 原生，Spring AI 有 Java 客户端 |
| Pinecone | 托管服务 | 云端 SaaS，免运维 |
| Redis | 内存数据库 | 支持向量搜索模块 |
| Milvus | 分布式向量数据库 | 企业级大规模部署 |
| Weaviate | 云原生 | GraphQL 查询接口 |
| PostgreSQL (pgvector) | 关系型+向量 | 混合搜索 |

### Q14：什么是 Tools（函数调用）？

Tool 允许 LLM 调用外部函数获取实时数据或执行操作。Spring AI 通过 `@Tool` 注解实现：

```java
@Component
public class WeatherTools {
    @Tool(name = "get_weather", description = "获取指定城市的天气")
    public String getWeather(@ToolParam("城市名") String city) {
        // 调用外部天气 API
        return "北京 25°C 晴";
    }
}
```

### Q15：ChatClient 的 Advisor 机制是什么？

Advisor 是 Spring AI 的拦截器模式，在 Chat 请求前后插入增强逻辑：

```java
// 注入检索增强 Advisor
ChatClient chatClient = ChatClient.builder(chatModel)
        .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
        .build();
```

常用 Advisor：
- `QuestionAnswerAdvisor`: RAG 上下文注入
- `RetrievalAugmentationAdvisor`: 高级 RAG 编排
- `SimpleLoggerAdvisor`: 请求/响应日志记录

### Q16：Spring AI vs LangChain4j 对比？

| 维度 | Spring AI | LangChain4j |
|------|-----------|-------------|
| 所属生态 | Spring 官方 | 社区驱动 |
| 抽象层 | 紧贴 Spring 生态（RestTemplate, JdbcTemplate 风格） | 独立框架 |
| 学习曲线 | 对 Spring 开发者友好 | 偏 Python LangChain 风格 |
| 成熟度 | 较新（2023 年底发布） | 相对成熟 |
| 接口设计 | ChatClient / ChatModel 分层 | LLM / ChatLanguageModel |
| 流式支持 | Flux<String> 原生响应式 | StreamingResponseHandler |
| RAG 支持 | QuestionAnswerAdvisor 等 | ContentRetriever 体系 |
| 中文模型 | 有官方 ZhipuAI 适配 | 社区驱动 |

### Q17：什么是 LCEL（LangChain Expression Language）？

LCEL 是 Spring AI 借鉴 LangChain 的声明式 Chain 构建语法，支持将多个组件串联为处理管道：

```java
// Chain: 用户输入 -> Prompt -> LLM -> 输出解析
ChatClient chain = ChatClient.builder(chatModel)
        .defaultSystem("你是一个翻译助手，将中文翻译成英文")
        .build();

String result = chain.prompt()
        .user("你好世界")
        .call()
        .content();
// 输出: Hello World
```

### Q18：Spring AI 的 Prompt Template 如何使用？

```java
@GetMapping("/ai/joke")
public String tellJoke(@RequestParam String topic) {
    return chatClient.prompt()
            .user(u -> u.text("讲一个关于 {topic} 的笑话")
                        .param("topic", topic))
            .call()
            .content();
}
```

---

## 二、深度原理剖析（12题）

### Q1：Spring AI 的 ETL Pipeline 如何工作？

Spring AI 的文档处理遵循 ETL 三阶段：

```java
// 1. Extract（读取）
DocumentReader reader = new PagePdfDocumentReader("classpath:/docs/sample.pdf");
List<Document> documents = reader.read();

// 2. Transform（转换 + 拆分）
DocumentTransformer splitter = new TokenTextSplitter(500, 100);
List<Document> chunks = splitter.apply(documents);

// 3. Load（写入向量库）
VectorStore vectorStore = new ChromaVectorStore(embeddingClient, collectionName);
vectorStore.accept(chunks);
```

完整流程：原始文档 → DocumentReader → List\<Document\> → DocumentTransformer → 分块 Documents → DocumentWriter → VectorStore。

### Q2：文本分块策略如何选择？

| 分块器 | 分块依据 | 适用场景 |
|--------|----------|----------|
| `TokenTextSplitter` | Token 数量 | 通用，按 LLM 上下文窗口 |
| `SentenceTextSplitter` | 句子边界 | 需要语义完整段落 |
| `ParagraphTextSplitter` | 段落边界 | 长文档分块 |
| `RecursiveTextSplitter` | 递归尝试多种分隔符 | 复杂文档，LangChain 推荐 |

```java
// 推荐配置：重叠分块避免信息断裂
TextSplitter splitter = TokenTextSplitter.builder()
        .withChunkSize(500)       // 每块 500 token
        .withChunkOverlap(100)    // 重叠 100 token
        .build();
```

### Q3：手动实现 RAG 的完整代码？

```java
@Service
public class ManualRagService {
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final EmbeddingClient embeddingClient;

    public ManualRagService(ChatClient.Builder builder,
                            VectorStore vectorStore,
                            EmbeddingClient embeddingClient) {
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
        this.embeddingClient = embeddingClient;
    }

    public String ask(String question) {
        // 1. 检索：将问题转为向量并搜索最相似文档
        List<Document> similarDocs = vectorStore.similaritySearch(
                SearchRequest.query(question)
                        .withTopK(3)      // 取 Top-3
                        .withSimilarityThreshold(0.7)); // 相似度阈值

        // 2. 构建增强 Prompt
        String context = similarDocs.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n---\n"));

        // 3. 生成：LLM 基于上下文回答问题
        return chatClient.prompt()
                .user(u -> u.text("基于以下上下文回答问题：\n\n{context}\n\n问题：{question}")
                        .param("context", context)
                        .param("question", question))
                .call()
                .content();
    }
}
```

### Q4：Spring AI 内部 Retriever 如何工作？

Spring AI 提供 `DocumentRetriever` 接口，`VectorStoreDocumentRetriever` 是其默认实现：

```java
// 底层逻辑等价于：
DocumentRetriever retriever = new VectorStoreDocumentRetriever(vectorStore);

// 检索时执行的核心步骤：
// 1. question -> embeddingClient.embed(question) -> 向量 q
// 2. vectorStore.similaritySearch(q, topK) -> 候选文档列表
// 3. 按 cosineSimilarity 降序排列，返回 Top-K
```

相似度搜索的底层 SQL（以 pgvector 为例）：
```sql
SELECT content, 1 - (embedding <=> :query_vector) AS similarity
FROM documents
WHERE 1 - (embedding <=> :query_vector) > :threshold
ORDER BY similarity DESC
LIMIT :top_k;
```

### Q5：OpenAI Embedding 模型有哪些？

| 模型 | 维度 | 最大输入 | 特点 |
|------|------|----------|------|
| text-embedding-3-small | 1536 | 8191 token | 性价比最高，推荐 |
| text-embedding-3-large | 3072 | 8191 token | 精度最高 |
| text-embedding-ada-002 | 1536 | 8191 token | 旧版，逐步退役 |

```yaml
spring:
  ai:
    openai:
      embedding:
        options:
          model: text-embedding-3-small
          dimensions: 512  # 支持降维，节省存储
```

### Q6：BGE-Large Embedding 模型如何部署使用？

BGE（BAAI General Embedding）是北京智源研究院的开源 Embedding 模型，可通过 Ollama 部署：

```bash
# 本地部署 BGE-Large
ollama pull bge-large
```

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      embedding:
        options:
          model: bge-large
```

对比特性：
- 中文表现优异，超过同尺寸 OpenAI 模型
- 输出维度 1024
- 支持通过 `query:` 和 `passage:` 前缀区分检索和存储

### Q7：Qwen3 Embedding 模型如何使用？

```yaml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      embedding:
        enabled: true
        options:
          model: text-embedding-v3  # 通义千问 Embedding V3
```

### Q8：FAISS 向量数据库的使用？

FAISS（Facebook AI Similarity Search）是本地内存向量库，适合原型和小规模场景：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-faiss-store</artifactId>
</dependency>
```

```java
@Configuration
public class FaissConfig {
    @Bean
    public VectorStore vectorStore(EmbeddingClient embeddingClient) {
        return new FaissVectorStore(embeddingClient,
                new FaissVectorStoreConfig()
                        .withDistanceType(DistanceType.COSINE));
    }
}
```

```yaml
spring:
  ai:
    vectorstore:
      faiss:
        similarity-top-k: 5
        initialize-schema: true
```

### Q9：Chroma DB 的 Spring AI 集成？

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-chroma-store</artifactId>
</dependency>
```

```yaml
spring:
  ai:
    vectorstore:
      chroma:
        host: localhost
        port: 8000
        collection-name: spring_ai_docs
```

```java
@Bean
public VectorStore chromaVectorStore(EmbeddingClient embeddingClient,
                                      ChromaApi chromaApi) {
    return new ChromaVectorStore(embeddingClient, chromaApi, "spring_ai_docs");
}
```

### Q10：自定义 Tool（Function Calling）深度解析？

```java
@Component
@Slf4j
public class DatabaseTools {
    private final JdbcTemplate jdbcTemplate;

    @Tool(name = "query_user_orders", description = "根据用户ID查询最近订单")
    public String queryUserOrders(@ToolParam("用户ID") Long userId,
                                  @ToolParam("查询条数") @Nullable Integer limit) {
        log.info("Tool called: query_user_orders, userId={}", userId);
        if (limit == null) limit = 5;

        List<Map<String, Object>> orders = jdbcTemplate.queryForList(
                "SELECT order_id, amount, status, create_time FROM orders " +
                "WHERE user_id = ? ORDER BY create_time DESC LIMIT ?",
                userId, limit);

        return orders.isEmpty() ? "无订单记录" : orders.toString();
    }

    @Tool(name = "create_order", description = "创建新订单（写操作示例）")
    public String createOrder(@ToolParam("用户ID") Long userId,
                              @ToolParam("商品ID") Long productId,
                              @ToolParam("数量") Integer quantity) {
        // 执行写操作
        return "订单创建成功，订单号：ORD" + System.currentTimeMillis();
    }
}
```

```java
// 注册 ToolCallback
@Bean
public ToolCallback weatherTool(WeatherTools weatherTools) {
    return ToolCallbacks.from(weatherTools);
}

// ChatClient 使用 Tools
ChatClient client = ChatClient.builder(chatModel)
        .defaultTools("get_weather", "query_user_orders")
        .build();
```

### Q11：GLM 数据库集成（思考链与工具调用）？

Zhipu GLM-4 支持 Function Calling + 数据库查询的深度集成。Spring AI 中通过 ChatClient + Tools 实现：

```java
// GLM-4 自动规划：需要查数据库 -> 调用 query_user_orders -> 返回结果 -> 组织回答
// 整个过程由 LLM 自主决策调用顺序

String response = chatClient.prompt()
        .user("查询用户 1001 昨天的订单总量和总金额")
        .functions("query_user_orders", "query_order_stats") // 可供调用的工具
        .call()
        .content();

// GLM-4 可能先后调用：
// 1. query_order_stats(1001, "2026-07-21") -> 总金额 ￥3,280
// 2. 组织回答："用户 1001 昨日订单总金额为 ￥3,280"
```

### Q12：上下文感知 RAG（Context-Aware RAG）的实现？

传统 RAG 每次查询独立检索，上下文感知 RAG 会考虑对话历史，重写查询后再检索：

```java
@Service
public class ContextAwareRagService {
    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    // 查询重写 Advisor
    @Bean
    public Advisor queryRewriteAdvisor() {
        return (request, next) -> {
            // 检查是否有历史上下文
            if (request.getConversationHistory() != null && !request.getConversationHistory().isEmpty()) {
                // 用 LLM 重写查询，将指代消解
                String rewrittenQuery = chatClient.prompt()
                        .user("基于对话历史，将最新问题改写为独立的检索查询：" +
                              request.getConversationHistory() + " | " + request.getUserText())
                        .call()
                        .content();
                request.withUserText(rewrittenQuery);
            }
            return next.call(request);
        };
    }

    public String ask(String question, String conversationId) {
        return chatClient.prompt()
                .user(question)
                .advisors(a -> a
                        .param("chat_memory", new InMemoryChatMemory())
                        .param("chat_memory_conversation_id", conversationId))
                .advisors(new QuestionAnswerAdvisor(vectorStore))
                .call()
                .content();
    }
}
```

关键改进：查询重写解决代词指代问题。例如用户说"它的作者是谁？"，上下文感知 RAG 会将"它"还原为前文提到的书名，然后再检索。

---

## 三、实战场景题（10题）

### Q1：如何实现 DeepSeek Chat 可视化聊天？

```java
@RestController
@RequestMapping("/api/chat")
public class DeepSeekChatController {
    private final ChatClient chatClient;

    public DeepSeekChatController(ChatClient.Builder builder) {
        // 配置 DeepSeek
        this.chatClient = builder
                .defaultSystem("你是 DeepSeek 助手，用中文回答")
                .build();
    }

    // SSE 流式接口，前端用 EventSource 接收
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .stream()
                .content()
                .map(content -> ServerSentEvent.<String>builder()
                        .data(content)
                        .build());
    }
}
```

```yaml
spring:
  ai:
    openai:
      base-url: https://api.deepseek.com  # DeepSeek 兼容 OpenAI API
      api-key: ${DEEPSEEK_API_KEY}
      chat:
        options:
          model: deepseek-chat
```

### Q2：如何用 Spring AI 构建知识库问答系统？

```java
@Service
public class KnowledgeBaseService {
    // 1. 文档入库
    public void ingestDocument(MultipartFile file) {
        DocumentReader reader = new PagePdfDocumentReader(new InputStreamResource(file.getInputStream()));
        List<Document> documents = reader.read();

        TextSplitter splitter = new TokenTextSplitter(300, 50);
        List<Document> chunks = splitter.apply(documents);

        vectorStore.accept(chunks);  // 写入向量库
    }

    // 2. 问答
    public String ask(String question) {
        return chatClient.prompt()
                .user(question)
                .advisors(new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults()
                        .withTopK(3)))
                .call()
                .content();
    }
}
```

### Q3：如何使用向量数据库实现语义搜索？

```java
@RestController
@RequestMapping("/api/search")
public class SemanticSearchController {
    @Autowired
    private VectorStore vectorStore;

    @GetMapping("/semantic")
    public List<Document> semanticSearch(@RequestParam String query,
                                          @RequestParam(defaultValue = "5") int topK) {
        return vectorStore.similaritySearch(
                SearchRequest.query(query)
                        .withTopK(topK)
                        .withSimilarityThreshold(0.75));
    }

    @GetMapping("/hybrid")
    public List<Document> hybridSearch(@RequestParam String query,
                                        @RequestParam String keyword) {
        // 语义搜索 + 关键词过滤
        SearchRequest.FilterExpression filter = new SearchRequest.FilterExpression(
                "category == :cat", Map.of("cat", keyword));
        return vectorStore.similaritySearch(
                SearchRequest.query(query)
                        .withTopK(5)
                        .withFilterExpression(filter));
    }
}
```

### Q4：如何实现带记忆的多轮对话？

```java
@Bean
public ChatClient chatClientWithMemory(ChatClient.Builder builder) {
    return builder
            .defaultAdvisors(new MessageChatMemoryAdvisor(
                    new InMemoryChatMemory())) // 对话记忆
            .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore)) // RAG
            .build();
}

// 使用时传入 conversationId
String response = chatClient.prompt()
        .user("继续说下去")
        .advisors(a -> a.param("chat_memory_conversation_id", "session-123"))
        .call()
        .content();
```

### Q5：如何实现 LCEL 声明式 Chain（10种常见模式）？

```java
// 1. 基础 Chain：输入 -> Prompt -> LLM -> 输出
ChatClient simpleChain = ChatClient.builder(chatModel).build();

// 2. System Prompt + User Prompt
ChatClient sysChain = ChatClient.builder(chatModel)
        .defaultSystem("你是旅游顾问，回答需包含费用、季节、风险")
        .build();

// 3. Prompt Template Chain
ChatClient templateChain = ChatClient.builder(chatModel)
        .defaultSystem("你是一个{role}专家")
        .defaultUser("请解释{concept}的概念")
        .build();

// 4. 带工具的 Chain
ChatClient toolChain = ChatClient.builder(chatModel)
        .defaultTools("get_weather", "exchange_rate")
        .build();

// 5. 带 Advisor 的 Chain
ChatClient ragChain = ChatClient.builder(chatModel)
        .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
        .build();

// 6. 输出解析 Chain
record Joke(String setup, String punchline) {}
ChatClient parseChain = ChatClient.builder(chatModel)
        .defaultSystem("以JSON格式返回笑话，包含 setup 和 punchline 字段")
        .build();
// 手工解析 JSON -> Joke 对象

// 7. 流式 Chain
Flux<String> streamResult = chatClient.prompt()
        .user("写一首诗")
        .stream()
        .content();

// 8. 条件 Chain（根据结果选择不同后续）
String result = chatClient.prompt()
        .user("分析这段文本的情感：{text}")
        .call()
        .content();
// 根据情感分析结果决定后续调用哪个 prompt

// 9. 并行 Chain
ChatClient englishChain = ChatClient.builder(chatModel)
        .defaultSystem("用英文回答").build();
ChatClient chineseChain = ChatClient.builder(chatModel)
        .defaultSystem("用中文回答").build();

// 10. 嵌套 Chain
ChatClient routerChain = ChatClient.builder(chatModel)
        .defaultSystem("判断用户问题是技术还是非技术，回复 TECH 或 NON_TECH")
        .build();
// 根据回复路由到不同的子 Chain
```

### Q6：如何集成 GLM 数据库查询？

```java
@Component
public class GlmDatabaseService {
    private final JdbcTemplate jdbc;

    @Tool(name = "execute_sql", description = "执行SQL查询并返回结果")
    public String executeSql(@ToolParam("SQL查询语句") String sql) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql);
        if (rows.isEmpty()) return "查询无结果";
        // 限制返回行数，避免 Token 超限
        return rows.stream().limit(10)
                .map(Object::toString)
                .collect(Collectors.joining("\n"));
    }

    public String askDatabase(String question) {
        return chatClient.prompt()
                .user(question)
                .advisors(a -> a
                        .param("system_text", "你是数据库分析师，通过 execute_sql " +
                              "工具查询数据回答用户问题。注意SQL注入风险。"))
                .build();
    }
}
```

### Q7：开源 Embedding 模型（BGE-Large）部署方案？

```yaml
# 方案一：Ollama 本地部署
spring:
  ai:
    ollama:
      embedding:
        options:
          model: bge-large
          # BGE-Large 需要前缀处理
          # query 时用 "query: " + text
          # 存储时用 "passage: " + text

# 方案二：HuggingFace + ONNX Runtime
# 自行部署推理服务后通过 API 调用
```

```java
// BGE 模型使用注意事项
public List<Double> embedForQuery(String text) {
    return embeddingClient.embed("query: " + text);  // 查询前缀
}

public List<Double> embedForDocument(String text) {
    return embeddingClient.embed("passage: " + text);  // 文档前缀
}
```

### Q8：如何通过 RAG 文件加载器读取多种格式？

```java
// PDF 读取
DocumentReader pdfReader = new PagePdfDocumentReader("classpath:docs/report.pdf");

// TXT 读取
DocumentReader txtReader = new TextDocumentReader("classpath:docs/notes.txt");

// JSON 读取
DocumentReader jsonReader = new JsonDocumentReader("classpath:docs/data.json");

// HTML 读取
DocumentReader htmlReader = new HtmlDocumentReader("classpath:docs/page.html");

// 统一处理
List<Document> allDocs = new ArrayList<>();
for (DocumentReader reader : List.of(pdfReader, txtReader, jsonReader, htmlReader)) {
    allDocs.addAll(reader.read());
}
```

### Q9：如何选择合适的 Embedding 模型？

| 场景 | 推荐模型 | 原因 |
|------|----------|------|
| 通用中英文 | OpenAI text-embedding-3-small | 性价比高，1536 维 |
| 纯中文场景 | BGE-Large / Qwen3 Embedding | 中文语义理解更优 |
| 高精度需要 | OpenAI text-embedding-3-large | 3072 维，精度最高 |
| 本地部署 | BGE-Large (Ollama) / BGE-Small | 无需外网，数据安全 |
| 开源可控 | BGE-M3 | 支持多语言、多粒度 |

### Q10：RAG 综合案例（向量数据库 + 检索 + 生成）？

```java
@SpringBootApplication
public class RagApplication {
    public static void main(String[] args) {
        SpringApplication.run(RagApplication.class, args);
    }
}

@RestController
@RequestMapping("/rag")
class RagController {
    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    // 初始化：加载文档
    @PostMapping("/ingest")
    public String ingest(@RequestParam String docPath) {
        DocumentReader reader = new PagePdfDocumentReader(docPath);
        TextSplitter splitter = new TokenTextSplitter(400, 80);
        vectorStore.accept(splitter.apply(reader.read()));
        return "文档已入库";
    }

    // 对话：RAG 检索增强
    @PostMapping("/chat")
    public String chat(@RequestBody String question) {
        return chatClient.prompt()
                .user(question)
                .advisors(new QuestionAnswerAdvisor(vectorStore,
                        SearchRequest.defaults().withTopK(3)))
                .call()
                .content();
    }

    // 对话流式版本
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody String question) {
        return chatClient.prompt()
                .user(question)
                .advisors(new QuestionAnswerAdvisor(vectorStore,
                        SearchRequest.defaults().withTopK(3)))
                .stream()
                .content();
    }
}
```

---

## 四、手写代码题（8题）

### Q1：手写 ChatClient + Streaming 聊天接口

```java
@RestController
public class ChatApi {
    private final ChatClient chatClient;

    public ChatApi(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @PostMapping("/chat")
    public String syncChat(@RequestBody String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody String message) {
        return chatClient.prompt()
                .user(message)
                .stream()
                .content();
    }
}
```

### Q2：手写 Embedding 向量化 + 相似度计算

```java
@Service
public class EmbeddingService {
    private final EmbeddingClient embeddingClient;

    public float[] embed(String text) {
        List<Double> vector = embeddingClient.embed(text);
        float[] result = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            result[i] = vector.get(i).floatValue();
        }
        return result;
    }

    public float cosineSimilarity(float[] v1, float[] v2) {
        float dot = 0f, n1 = 0f, n2 = 0f;
        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
            n1 += v1[i] * v1[i];
            n2 += v2[i] * v2[i];
        }
        return dot / (float) (Math.sqrt(n1) * Math.sqrt(n2));
    }

    public List<String> findSimilar(String query, List<String> candidates) {
        float[] queryVec = embed(query);
        return candidates.stream()
                .map(c -> Map.entry(c, cosineSimilarity(queryVec, embed(c))))
                .sorted((a, b) -> Float.compare(b.getValue(), a.getValue()))
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();
    }
}
```

### Q3：手写 RAG 完整流程（不使用 Advisor）

```java
@Service
public class ManualRagService {
    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public String ragQuery(String question) {
        List<Document> relevantDocs = vectorStore.similaritySearch(
                SearchRequest.query(question).withTopK(3));

        String context = relevantDocs.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n\n"));

        String prompt = """
                基于以下上下文信息回答问题。
                如果上下文中没有相关信息，请如实告知无法回答。

                上下文：
                %s

                问题：%s
                回答：
                """.formatted(context, question);

        return chatClient.prompt().user(prompt).call().content();
    }
}
```

### Q4：手写 VectorStore 增删查

```java
@Service
public class VectorStoreService {
    private final VectorStore vectorStore;

    // 添加文档
    public void addDocument(String content, Map<String, Object> metadata) {
        Document doc = new Document(content, metadata);
        vectorStore.add(List.of(doc));
    }

    // 批量添加
    public void addDocuments(List<String> contents) {
        List<Document> docs = contents.stream()
                .map(c -> new Document(c, Map.of("source", "manual")))
                .toList();
        vectorStore.add(docs);
    }

    // 相似度搜索
    public List<Document> search(String query, int topK, double threshold) {
        return vectorStore.similaritySearch(
                SearchRequest.query(query)
                        .withTopK(topK)
                        .withSimilarityThreshold(threshold));
    }

    // 删除文档
    public void deleteDocument(List<String> docIds) {
        vectorStore.delete(docIds);
    }
}
```

### Q5：手写 TextSplitter 分块逻辑

```java
public class SimpleTextSplitter {
    private final int chunkSize;
    private final int chunkOverlap;

    public SimpleTextSplitter(int chunkSize, int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    public List<Document> split(Document document) {
        String text = document.getContent();
        List<Document> chunks = new ArrayList<>();

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            // 尝试在句子边界断开
            if (end < text.length()) {
                int periodIdx = text.lastIndexOf('.', end);
                if (periodIdx > start) end = periodIdx + 1;
            }

            String chunkText = text.substring(start, end);
            Document chunk = new Document(chunkText,
                    Map.of("chunk_start", start, "chunk_end", end));
            chunks.add(chunk);

            start = end - chunkOverlap; // 重叠
            if (start >= text.length()) break;
        }
        return chunks;
    }
}
```

### Q6：手写 Tool 注解的数据库查询工具

```java
@Component
public class OrderTool {
    private final JdbcTemplate jdbc;

    @Tool(name = "get_order_details",
          description = "根据订单ID查询订单详情，包含商品、金额、状态")
    public String getOrderDetails(@ToolParam("订单ID") String orderId) {
        String sql = """
                SELECT o.order_id, o.amount, o.status, o.create_time,
                       GROUP_CONCAT(p.product_name) as products
                FROM orders o
                JOIN order_items oi ON o.order_id = oi.order_id
                JOIN products p ON oi.product_id = p.product_id
                WHERE o.order_id = ?
                GROUP BY o.order_id
                """;
        Map<String, Object> result = jdbc.queryForMap(sql, orderId);
        return "订单 %s: 金额=%.2f, 状态=%s, 商品=%s".formatted(
                result.get("order_id"), result.get("amount"),
                result.get("status"), result.get("products"));
    }
}
```

### Q7：手写自定义 Advisor（日志 + 审计）

```java
public class LoggingAdvisor implements Advisor {
    private static final Logger log = LoggerFactory.getLogger(LoggingAdvisor.class);

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallNext next) {
        log.info("[Advisor] Request: userText={}, systemText={}",
                request.getUserText(), request.getSystemText());

        long start = System.currentTimeMillis();
        AdvisedResponse response = next.call(request);
        long elapsed = System.currentTimeMillis() - start;

        log.info("[Advisor] Response taken {}ms, content length={}",
                elapsed, response.getResponse().getResult().getOutput().getContent().length());

        return response;
    }
}
```

### Q8：手写配置多种 Embedding 模型的 Factory

```java
@Configuration
public class EmbeddingConfig {
    @Bean
    @Primary
    public EmbeddingClient openAiEmbedding(OpenAiEmbeddingProperties props) {
        return new OpenAiEmbeddingClient(props);
    }

    @Bean
    public EmbeddingClient zhipuEmbedding(ZhipuAiEmbeddingProperties props) {
        return new ZhipuAiEmbeddingClient(props);
    }

    @Bean
    public EmbeddingClient ollamaEmbedding(OllamaEmbeddingProperties props) {
        return new OllamaEmbeddingClient(props);
    }

    @Bean
    public EmbeddingClient dashscopeEmbedding(DashScopeEmbeddingOptions options) {
        return new DashScopeEmbeddingClient(options);
    }
}
```

---

## 五、系统设计题（5题）

### Q1：设计一个企业级 RAG 问答系统

**需求**：支持百万级文档，低延迟，高可用

**架构方案**：

```
用户请求 --> 负载均衡 --> API Gateway --> 查询改写 --> 检索路由
                                                    |
               +-------------------+------------------+
               |                   |                  |
         Elasticsearch        向量库(Redis)        Cache(Redis)
               |                   |                  |
               +--------+----------+------------------+
                        |
                    重排序(Reranker)
                        |
                     LLM 生成
                        |
                     响应返回
```

**关键设计点**：
1. **多路召回**：BM25（ES）+ 向量搜索 + 缓存，提升召回率
2. **Reranker**：对召回结果重新排序，提高 Top-K 准确率
3. **缓存层**：Redis 缓存高频问题的答案，降低 LLM 延迟
4. **分片策略**：按文档来源/领域分 Collection，减少搜索范围
5. **流式响应**：SSE/WebSocket 推送，改善用户体验

### Q2：设计一个多模型路由系统

**需求**：根据任务类型自动选择最优模型

```java
@Component
public class ModelRouter {
    private final Map<String, ChatClient> modelClients = new HashMap<>();

    public ModelRouter(List<ChatClient> clients) {
        modelClients.put("simple",    ChatClient.builder(lightweightModel).build());
        modelClients.put("reasoning", ChatClient.builder(gpt4Model).build());
        modelClients.put("creative",  ChatClient.builder(creativeModel).build());
    }

    public ChatClient route(String taskType) {
        return switch (taskType) {
            case "分类", "关键词提取" -> modelClients.get("simple");
            case "数学推理", "代码生成" -> modelClients.get("reasoning");
            case "文案写作", "头脑风暴" -> modelClients.get("creative");
            default -> modelClients.get("simple");
        };
    }
}
```

**路由策略**：
| 策略 | 描述 | 适用场景 |
|------|------|----------|
| 关键词路由 | 根据问题关键词匹配 | 简单分类，可预知 |
| 语义路由 | LLM 分类后路由 | 复杂场景，灵活 |
| 混合路由 | 关键词 + LLM 兜底 | 最佳实践 |

### Q3：设计一个 Embedding 模型的 A/B 测试框架

```java
@Service
public class EmbeddingABTestService {
    private final EmbeddingClient modelA; // text-embedding-3-small
    private final EmbeddingClient modelB; // BGE-Large

    public SearchResult search(String query,
                               @RequestParam(defaultValue = "A") String variant) {
        EmbeddingClient current = "A".equals(variant) ? modelA : modelB;
        var start = System.nanoTime();
        var results = vectorStore.similaritySearch(
                SearchRequest.query(query)
                        .withTopK(10)
                        .withEmbeddingClient(current));
        var latency = System.nanoTime() - start;

        // 记录 A/B 实验数据
        metricsCollector.record("embedding_ab", Map.of(
                "variant", variant,
                "latency_ns", latency,
                "result_count", results.size(),
                "query", sanitize(query)
        ));
        return new SearchResult(variant, results);
    }
}
```

**评估指标**：召回率（Recall）、MRR（Mean Reciprocal Rank）、延迟 P99、成本/千次查询。

### Q4：设计一个支持多数据源的知识库系统

**数据源抽象**：

```java
public interface DataSourceConnector {
    String getType();               // "confluence", "notion", "local"
    List<Document> fetchDocuments();
    DocumentReader getReader();
}

@Component
public class ConfluenceConnector implements DataSourceConnector {
    @Override public String getType() { return "confluence"; }

    @Override
    public List<Document> fetchDocuments() {
        // Confluence REST API -> 解析 HTML -> Document 列表
    }

    @Override
    public DocumentReader getReader() { return new HtmlDocumentReader(); }
}
```

**同步管道**：
```java
// 定时任务：每 30 分钟同步所有数据源
@Scheduled(cron = "0 */30 * * * *")
public void syncAllSources() {
    for (DataSourceConnector source : connectors) {
        List<Document> docs = source.fetchDocuments();
        TextSplitter splitter = new TokenTextSplitter(300, 50);
        vectorStore.accept(splitter.apply(docs));
    }
}
```

### Q5：设计一个具备记忆和上下文的智能客服系统

**系统架构**：

1. **短期记忆**：Redis 缓存当前会话（TTL 30分钟）
2. **长期记忆**：向量库存储历史重要对话摘要
3. **知识库**：RAG 检索公司 FAQ/知识文档
4. **工具链**：查询订单/退换货/物流信息的 Tool

```java
@Service
public class SmartCustomerService {
    private final ChatClient chatClient;

    public Flux<String> handleConversation(String userId, String message) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a
                        .param("chat_memory_conversation_id", userId)
                        .param("chat_memory_response_size", 2)) // 保留最近2轮
                .advisors(new QuestionAnswerAdvisor(
                        knowledgeBaseVectorStore,
                        SearchRequest.defaults().withTopK(5)))
                .stream()
                .content();
                // Tools 自动注入：queryOrder, returnProduct, checkLogistics
    }
}
```

**对话状态管理**：
```
用户: "我要退货"
助手: "请提供订单号"
用户: "ORD20260721"
系统: 自动调用 queryOrder("ORD20260721")
      -> 检测订单可退货
      -> 生成退货指引
助手: "您的订单 ORD20260721 符合退货条件，..."
```

---

## 六、常见坑点与最佳实践（表格）

| 坑点 | 现象 | 解决方案 |
|------|------|----------|
| API Key 硬编码 | 泄露风险 | 使用环境变量 `${VAR}` 或 Vault |
| Token 超限 | 请求失败 400 | 设置 `max-tokens`，使用 `TokenTextSplitter` 截断 |
| 流式连接断开 | 客户端收到不完整响应 | 前端自动重连，后端设置超时重试 |
| 向量维度不匹配 | 搜索返回 0 结果 | 确保 Embedding 模型和 VectorStore 维度一致 |
| Embedding 模型不一致 | 存储用 A 模型，检索用 B 模型 | 统一使用同一模型，或模型版本化 |
| 相似度阈值过高 | 检索不到结果 | 设为 0.7-0.75 的合理值，或默认不设阈值 |
| 分块大小不合理 | 块太小丢失语义，太大包含噪声 | 300-500 token + 50-100 重叠 |
| 分块跨段落 | 语义不完整 | 使用 `ParagraphTextSplitter` 保持段落边界 |
| Tool 调用循环 | LLM 反复调用同一工具 | 设置 `maxToolCallbacks` 限制调用次数 |
| Advisor 顺序 | 检索注入的上下文被覆盖 | Advisors 顺序：记忆 -> 重写 -> RAG -> 日志 |
| Memory 无限增长 | Token 超限，成本飙升 | 设置窗口大小或摘要总结历史 |
| 并发请求冲突 | VectorStore 写覆盖 | 使用隔离的 Collection/Namespace |
| 非流式请求超时 | 大模型返回慢导致超时 | 使用流式 + Chunked Transfer 或增加超时 |
| 同义词检索失败 | "计算机"搜不到"电脑" | 使用查询扩展或混合检索（BM25 + 向量） |
| 中文分句不准确 | `SentenceTextSplitter` 分割错误 | 使用中文字符（。！？）作为分隔符，或自定规则 |

---

## 七、面试回答模板（Top 5）

### 模板 1：说说 Spring AI 的核心架构

> "Spring AI 是 Spring 官方推出的 AI 集成框架，核心设计理念是**提供一套统一的抽象层**，屏蔽不同 AI 提供商（OpenAI、Zhipu、DeepSeek 等）的 API 差异。
>
> 核心接口包括 `ChatModel`（LLM 调用）、`EmbeddingClient`（向量化）、`VectorStore`（向量存储）、`DocumentReader/Transformer/Writer`（文档 ETL 管道）。最常用的是 `ChatClient` —— 一个流式 Builder API，类似 `RestTemplate` 之于 HTTP 调用。
>
> 它还内置了 RAG 支持（通过 `QuestionAnswerAdvisor`）、函数调用（通过 `@Tool` 注解）、以及对话记忆管理，让 Java 开发者可以像写普通 Spring Boot 应用一样构建 AI 功能。"

### 模板 2：RAG 的原理和实现

> "RAG 即检索增强生成，核心是解决 LLM 知识过时和幻觉问题。它不是一个微调方案，而是一种**运行时知识注入**方法。
>
> 流程分三步：**索引阶段**将文档分块 -> Embedding -> 存入向量库；**检索阶段**将用户问题转向量 -> 余弦相似度搜索 -> 取 Top-K 文档；**生成阶段**将检索结果拼入 Prompt -> LLM 生成答案。
>
> Spring AI 中实现 RAG 非常简洁：`ChatClient` + `QuestionAnswerAdvisor(vectorStore)` 即可。如果需要精细化控制，也可以手动调用 `vectorStore.similaritySearch()` 自行拼接 Prompt。"

### 模板 3：如何选择合适的 Embedding 模型？

> "选 Embedding 模型需要考虑四个维度：**语言**（中文优先 BGE/Qwen3）、**精度要求**（text-embedding-3-large 1536/3072 维）、**部署方式**（云端 vs 本地 Ollama）、**成本预算**。
>
> 我的经验是：通用中英文场景用 `text-embedding-3-small`（1536 维，性价比最高）；纯中文高精度用 `BGE-Large`（1024 维，Ollama 本地部署无需额外费用）；多模态或构建私有知识库用 `BGE-M3`。
>
> **一个常见坑点**：存储和查询必须用**同一个** Embedding 模型，否则向量空间不一致导致检索结果全零。另外 BGE 模型需要 `query:`/`passage:` 前缀。"

### 模板 4：Spring AI 文档分块策略的经验

> "分块是 RAG 中决定检索质量最关键的一环。核心原则是**保证每个 Chunk 语义完整**，避免跨段落截断。
>
> 实践上有几个要点：一是**块大小**，通用 300-500 token，太小块丢失上下文，太大块包含噪声；二是**重叠**，50-100 token 的重叠能避免切在关键信息中间；三是**分块策略选择**，Markdown/HTML 文档用 `RecursiveTextSplitter`，纯文本用 `ParagraphTextSplitter`，代码文档按函数分块。
>
> 建议先做实验：抽样标注 100 个问答对，对比不同分块策略的检索命中率来调优。"

### 模板 5：Spring AI 与 LangChain4j 如何选择？

> "这两个框架目标相同但风格不同。Spring AI 是**Spring 官方出品**，API 设计紧贴 Spring 生态——`ChatClient.Builder` 的链式风格、`@Tool` 注解注入、Advisor 拦截器机制，对 Spring 开发者零学习成本。
>
> LangChain4j 社区更活跃，功能更丰富，API 设计贴近 Python LangChain，适合有 LangChain 经验的团队。
>
> 我的建议：如果团队技术栈是 Spring Boot 为主，选 Spring AI（集成成本最低）；如果需要最新的 LLM 功能或社区支持，LangChain4j 更多。我们项目中因为大量使用 Spring Cloud 生态，选 Spring AI 的集成更顺滑。"

---

## 八、快速查漏补缺 Checklist

### 核心概念
- [ ] 知道 Spring AI 5 大核心接口（ChatModel, ChatClient, EmbeddingClient, VectorStore, Document*）
- [ ] 能说出 ChatClient 的 Builder 模式和 3 种调用方式（sync, stream, response）
- [ ] 理解 StreamingChatModel 返回 Flux\<String\> 的原因和 SSE 协议
- [ ] 能解释 Spring AI vs LangChain4j 的核心差异

### Embedding 与向量
- [ ] 理解 Embedding 的数学含义（高维向量映射，语义相近 -> 距离相近）
- [ ] 会手写余弦相似度代码
- [ ] 知道 BGE-Large 需要 `query:`/`passage:` 前缀
- [ ] 能列出至少 3 个 Embedding 模型及其维度

### RAG 实现
- [ ] 能画出 RAG 完整流程图（Query -> Retrieve -> Augment -> Generate）
- [ ] 会手动实现 RAG（不使用 Advisor 的版本）
- [ ] 知道 ETL 管道三阶段（Extract -> Transform -> Load）
- [ ] 能说出 2 个以上 TextSplitter 类型及区别
- [ ] 理解 Advisor 拦截器模式

### Tools 与 Function Calling
- [ ] 会使用 `@Tool` 注解定义工具
- [ ] 理解 ToolCallback 注册方式
- [ ] 知道如何限制 Tool 调用次数

### 向量数据库
- [ ] 知道 FAISS、Chroma、Redis、Pgvector 等选项及场景
- [ ] 会配置 VectorStore 并进行增删查

### LCEL 与 Chain
- [ ] 能说出 3 种以上 LCEL Chain 模式
- [ ] 理解嵌套 Chain 和条件 Chain 的设计

### 模型配置
- [ ] 知道如何在 application.yml 和运行时配置模型参数
- [ ] 会配置 DeepSeek（基于 OpenAI 兼容 API）
- [ ] 理解不同模型路由策略

### 实战能力
- [ ] 独立搭建过完整的 RAG 应用
- [ ] 处理过 Token 超限、维度不匹配、分块质量差的问题
- [ ] 能设计多轮对话记忆方案
- [ ] 能设计多数据源知识库系统

---

> **参考资料**
> - [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
> - [Spring AI GitHub](https://github.com/spring-projects/spring-ai)
> - BGE 模型: [BAAI/bge-large](https://huggingface.co/BAAI/bge-large)
> - Qwen3 Embedding: 阿里云百炼平台文档
