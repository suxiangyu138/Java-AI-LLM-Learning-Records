# Spring AI 大模型面试问答清单
> 🎯 基于实战项目清单，涵盖 Spring AI 大模型应用开发面试高频问题与完美解答方案，帮助你在面试中脱颖而出。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理

> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：Spring AI 是什么？它和直接调用 OpenAI API 有什么区别？

**面试官意图：** 考察候选人对 Spring AI 框架的定位和理解，以及为什么选择它而不是直接调用 API。

**完美解答：**

Spring AI 是 Spring 官方推出的 AI 应用开发框架，它提供了一套统一的 API 来对接各种大语言模型（OpenAI、通义千问、Ollama 本地模型等），同时提供了 Prompt 管理、RAG 检索增强、Function Calling 函数调用、Agent 智能体编排等开箱即用的能力。

**和直接调用 API 的对比：**

| 维度 | Spring AI | 直接调用 API |
|------|-----------|-------------|
| 模型切换 | 一行配置切换模型 | 需要重写代码 |
| 抽象层次 | 统一 ChatClient / Prompt API | 自己封装 HTTP 请求 |
| RAG 集成 | 内置文档解析、向量化、检索链路 | 需要自己实现全流程 |
| Function Calling | 注解式 @Tool 注册，自动绑定 | 需要手动维护函数描述 |
| 流式响应 | Flux 响应式支持 | 需要处理 SSE 协议 |
| Spring 生态整合 | 和 SpringBoot、SpringCloud 无缝对接 | 需要自行整合 |

**核心代码对比：**

```java
// Spring AI 方式：统一 API 调用
@Autowired
private ChatClient chatClient;

public String chat(String message) {
    return chatClient.prompt()
        .user(message)
        .call()
        .content();
}

// 直接调用 API 方式：需要自己处理 HTTP 请求
public String chat(String message) {
    // 需要自己构造 HTTP 请求、处理认证、解析 JSON、处理异常
    HttpClient client = HttpClient.newHttpClient();
    String body = """
        {
            "model": "gpt-4",
            "messages": [{"role": "user", "content": "%s"}]
        }
        """.formatted(message);
    // ... 几十行模板代码
}
```

> 💡 **面试核心观点**：Spring AI 最大的价值不是"调用大模型"本身，而是提供了一套标准化的 AI 开发范式，让 Java 开发者可以在不学习 Python 的情况下，用 Spring 的方式开发 AI 应用。

**延伸追问应对：** 面试官可能追问"Spring AI 和 LangChain4j 怎么选"，可以回答：Spring AI 是 Spring 官方出品，和 Spring 生态整合最好，更新快；LangChain4j 社区版本，功能更丰富但和 Spring 的整合不如 Spring AI 紧密。

---

### Q2：什么是 RAG（检索增强生成）？为什么要用 RAG 而不是直接让大模型回答？

**面试官意图：** 考察对 RAG 核心概念的理解，是否清楚它解决了什么问题。

**完美解答：**

RAG（Retrieval-Augmented Generation）是一种"先检索、后生成"的技术架构。当用户提问时，先从知识库中检索出相关文档片段，再将这些片段拼入 Prompt 中，最后让大模型基于检索结果生成回答。

**为什么要用 RAG（而不是直接让大模型回答）：**

**1. 解决大模型的"知识截止"问题**
- 大模型的知识停留在训练数据截止时间
- RAG 可以接入最新文档、内部知识库，回答实时信息

**2. 解决"幻觉"问题**
- 大模型在不确定时会"编造"答案
- RAG 提供的检索结果是事实依据，大模型只能基于检索结果回答，从源头消除幻觉

**3. 解决"私有知识"问题**
- 大模型训练时没有企业的私有文档
- RAG 将企业内部的 PDF、Word、Wiki 等内容作为知识源，实现私有知识问答

**4. 成本优势**
- 微调需要标注大量数据并重新训练模型，成本高
- RAG 只需上传文档即可"教"大模型回答新知识，零训练成本

**RAG 工作流程：**

```
用户提问 -> 问题向量化 -> 向量库检索（找相似片段）
  -> 文档召回 -> 拼接 Prompt（检索结果 + 用户问题）
    -> 大模型生成回答（基于检索结果）-> 返回带引用的答案
```

> 🎯 **核心总结**：RAG = 检索（Retrieval） + 增强（Augmentation） + 生成（Generation）。检索解决"有没有"的问题，增强解决"准不准"的问题，生成解决"好不好"的问题。

---

### Q3：Spring AI 中的 Function Calling 是什么？它和大模型的工具调用有什么关系？

**面试官意图：** 考察对 Function Calling 原理的理解，以及如何将大模型与现有的 Java 业务系统结合。

**完美解答：**

Function Calling（函数调用）是大模型的一项关键能力——大模型可以在回答问题时，自动识别"这不是我能靠知识回答的，我需要调一个工具来获取数据"，然后生成工具调用指令，由应用系统执行该指令并返回结果。

**Spring AI 中的 Function Calling 实现：**

```java
// 1. 定义工具类（普通的 Spring Bean）
@Component
public class WeatherTools {
    
    @Tool(name = "query_weather", description = "根据城市名称查询实时天气")
    public String queryWeather(String city) {
        // 调用第三方天气 API 获取实时数据
        WeatherResponse response = weatherApiClient.getCurrentWeather(city);
        return "城市：%s，温度：%s°C，天气：%s".formatted(city, response.getTemp(), response.getCondition());
    }
    
    @Tool(name = "query_stock_price", description = "根据股票代码查询实时股价")
    public String queryStockPrice(String stockCode) {
        // 调用股票 API
        StockPrice price = stockApiClient.getPrice(stockCode);
        return "股票：%s，当前价格：%.2f 元，涨跌幅：%.2f%%".formatted(
            stockCode, price.getPrice(), price.getChangePercent());
    }
}

// 2. 调用时传入工具
public String chatWithTools(String userMessage) {
    return chatClient.prompt()
        .user(userMessage)
        .tools(new WeatherTools())  // 告诉大模型我有哪些工具可用
        .call()
        .content();
}
```

**工作原理：**

```
用户: "明天北京适合穿什么衣服？"

Step 1: 大模型分析-> 我需要天气数据，调用 query_weather("北京")
Step 2: Spring AI 拦截工具调用请求 -> 执行对应的 Java 方法
Step 3: Java 方法返回实时天气数据 -> 返回给大模型
Step 4: 大模型基于天气数据生成回答 -> "明天北京 32°C，建议穿短袖"
```

> ⚠️ **关键认知**：Function Calling 不是大模型自己调用你的 API，而是大模型"告诉你应该调用哪个 API"，由你的代码去执行。大模型只决定"要不要调用"和"传入什么参数"，不直接执行代码。

---

## 2. 项目实战深度问答

> 💡 面试官会深挖你的项目细节，验证你"真的做过"而不是"背过"

### Q4：你们项目的 RAG 知识库问答系统具体是怎么实现的？文档上传后怎么处理？

**面试官意图：** 验证候选人是否真正做过 RAG 项目，还是只停留在概念层面。

**完美解答：**

我们的 RAG 系统实现了"文档上传 -> 解析 -> 分块 -> 向量化 -> 存储 -> 检索 -> 问答"的完整链路。

**文档处理流程：**

```java
// 1. 文档上传与解析
@Service
public class DocumentService {
    
    public void processDocument(MultipartFile file) {
        // 1.1 文档解析（支持 PDF、Word、TXT）
        Document document = documentParser.parse(file);
        
        // 1.2 文本分块（Chunking）
        //    为什么分块：大模型上下文有限，且需要精准检索
        //    策略：按段落分块，每块 500 个 token，重叠 100 个 token
        List<DocumentChunk> chunks = textSplitter.split(document, SplitterConfig.builder()
            .maxTokens(500)
            .overlapTokens(100)
            .build());
        
        // 1.3 向量化与存储
        for (DocumentChunk chunk : chunks) {
            // 调用 Embedding 模型将文本转为向量
            float[] embedding = embeddingModel.embed(chunk.getContent());
            
            // 存储到向量数据库（Milvus）
            VectorEntity entity = VectorEntity.builder()
                .id(chunk.getId())
                .content(chunk.getContent())
                .embedding(embedding)
                .metadata(chunk.getMetadata())
                .build();
            vectorStore.insert(entity);
        }
    }
}

// 2. 检索与问答
@Service
public class RagService {
    
    public String ask(String question, String knowledgeBaseId) {
        // 2.1 将用户问题转为向量
        float[] questionVector = embeddingModel.embed(question);
        
        // 2.2 在向量库中检索最相似的 5 个片段
        List<VectorEntity> relevantDocs = vectorStore.search(
            VectorSearchQuery.builder()
                .vector(questionVector)
                .topK(5)
                .filter("knowledge_base_id = " + knowledgeBaseId)
                .build()
        );
        
        // 2.3 构建增强 Prompt
        String context = relevantDocs.stream()
            .map(doc -> "【文档引用】" + doc.getContent())
            .collect(Collectors.joining("\n\n"));
        
        // 2.4 让大模型基于检索结果回答
        return chatClient.prompt()
            .system("你是一个专业知识库问答助手。请基于以下文档内容回答问题。" +
                    "如果你无法从文档中找到答案，请如实说'知识库中没有相关信息'。")
            .user("文档内容：\n" + context + "\n\n问题：" + question)
            .call()
            .content();
    }
}
```

**文本分块策略对比：**

| 策略 | 方式 | 适用场景 |
|------|------|---------|
| 固定长度分块 | 按 token 数量切割 | 通用场景 |
| 段落分块 | 按换行符分割 | 结构清晰的文档 |
| 语义分块 | 基于语义边界（标题、段落） | 长文档 |
| 递归分块 | 先按段落再按句子 | 混合格式文档 |

> 💡 **经验总结**：RAG 的效果好坏，80% 取决于检索质量。而检索质量的关键在于三点：文本分块的粒度（太粗检索不准，太细上下文不完整）、Embedding 模型的选型、TopK 值的选择。我们经过测试后选择 500 token + 100 token 重叠作为分块策略。

---

### Q5：你们项目中的 Prompt 模板是怎么工程化管理的？如何保证 Prompt 的质量？

**面试官意图：** 考察 Prompt 工程化的实战经验，以及能否把 Prompt 当成代码来管理。

**完美解答：**

我们把 Prompt 当成"代码"来管理，有版本控制、有模板管理、有测试流程。

**Prompt 模板管理：**

```java
// 1. 将 Prompt 模板放在 resources/prompts/ 目录下
// resources/prompts/summary.st
你是一个专业文档总结助手。

请对以下文档进行总结：
{document}

要求：
- 总结长度控制在 {max_length} 字以内
- 使用 {language} 语言回答
- 以"本文主要介绍了"开头
- 输出 JSON 格式：{"summary": "xxx", "keywords": ["k1", "k2"]}

// 2. Java 中加载模板
@Service
public class PromptService {
    
    @Autowired
    private PromptTemplate promptTemplate;
    
    public StructuredOutput<SummaryResult> generateSummary(String document, String language) {
        Prompt prompt = promptTemplate.create("""
            你是一个专业文档总结助手。
            
            请对以下文档进行总结：
            {document}
            
            要求：
            - 总结长度控制在 200 字以内
            - 使用 {language} 语言回答
            - 输出 JSON 格式：{"summary": "xxx", "keywords": ["k1", "k2"]}
            """, Map.of(
                "document", document,
                "language", language
            ));
        
        // 结构化输出
        return chatClient.prompt(prompt)
            .call()
            .entity(SummaryResult.class);  // 自动反序列化为 Java 对象
    }
}

// 3. 总结结果对象
@Data
public class SummaryResult {
    private String summary;
    private List<String> keywords;
}
```

**Prompt 质量保证体系：**

| 措施 | 具体做法 |
|------|----------|
| 模板版本化 | Prompt 模板放在 Git 中管理，Review 后才可修改 |
| 结构化输出 | 使用 `StructuredOutputConverter` 确保输出格式可控 |
| 参数校验 | 对用户传入的参数做长度、格式校验 |
| 效果测评 | 准备"黄金测试集"，每次 Prompt 修改后回归测试 |
| 日志审计 | 记录每次 Prompt 的输入输出，方便追查问题 |

> 💡 **面试加分点**：Prompt 工程化的核心要义是"把 Prompt 从灵机一动的文案变成可测试、可维护、可迭代的工程资产"。我们团队有一个"Prompt 评审"环节，每次修改 Prompt 都需要提交效果对比数据。

---

### Q6：你们是怎么实现 Agent 智能体的？大模型做了哪些自主决策？

**面试官意图：** 考察对 Agent 模式的理解深度，以及实现过程中遇到的工程挑战。

**完美解答：**

我们实现的 Agent 是"智能客服助手"，它可以自主判断用户意图、选择合适的工具、完成多步骤任务。

**Agent 工作流程：**

```
用户提问: "帮我查一下订单 12345 的状态，然后给客户发个短信通知"

Step 1: 意图识别 -> 需要查订单 + 发短信
Step 2: 工具选择 -> queryOrder(orderNo) + sendSMS(phone, message)
Step 3: 第一个工具调用 -> queryOrder("12345") -> 订单状态: 已发货
Step 4: 状态判断 -> 已发货，需要获取用户手机号
Step 5: 第二个工具调用 -> getUserInfo(userId) -> 手机号: 138xxxx
Step 6: 第三个工具调用 -> sendSMS("138xxxx", "您的订单已发货")
Step 7: 结果汇总 -> "已为您查询到订单 12345 状态为'已发货'，并已发送短信通知"
```

**Spring AI Agent 实现：**

```java
@Service
public class CustomerServiceAgent {
    
    @Autowired
    private ChatClient chatClient;
    
    @Autowired
    private OrderTool orderTool;
    
    @Autowired
    private SmsTool smsTool;
    
    @Autowired
    private UserTool userTool;
    
    public String handleUserRequest(String userMessage, String userId) {
        // Agent 核心：自主决策 + 多工具编排
        return chatClient.prompt()
            .user(userMessage)
            .tools(orderTool, smsTool, userTool)
            .system("""
                你是一个智能客服助手。你可以使用以下工具：
                1. queryOrder：查询订单状态
                2. sendSMS：发送短信通知
                3. getUserInfo：查询用户信息
                
                注意：
                - 如果用户需要多步操作，请一步一步来
                - 每一步都先确认执行结果再决定下一步
                - 如果工具调用失败，请告知用户并提供解决方案
                """)
            .call()
            .content();
    }
}

// 工具定义
@Component
public class OrderTool {
    @Tool("根据订单号查询订单状态")
    public OrderVO queryOrder(@P("订单编号") String orderNo) {
        return orderService.queryByOrderNo(orderNo);
    }
}

@Component
public class SmsTool {
    @Tool("发送短信通知")
    public boolean sendSMS(@P("手机号") String phone, @P("短信内容") String message) {
        return smsService.send(phone, message);
    }
}
```

**遇到的问题和解决方案：**

> ⚠️ **关键挑战：大模型会在工具调用中"乱跑"**。比如用户问天气，它可能自己去翻订单系统。解决方案是为每个 Agent 设定明确的"职责边界"，在 System Prompt 中声明它只能做什么，并限制可用的工具集合。

另外，Agent 的记忆管理也是一个工程难点：
- **短期记忆**：当前对话上下文（截取最近 10 轮对话）
- **长期记忆**：用户偏好、历史问题（存储到 Redis）
- **工具调用历史**：记录每一步的输入输出，方便回溯

---

### Q7：你们项目中的流式对话是怎么实现的？WebSocket 如何和大模型结合？

**面试官意图：** 考察流式响应的实现方案，以及实时通信与大模型结合的实战经验。

**完美解答：**

流式对话的核心是：大模型生成一个字，前端就展示一个字，给用户"AI 正在思考"的实时感受。我们使用 **WebSocket + Spring AI 流式 API + SSE** 的方案。

**后端实现：**

```java
// 1. WebSocket 处理器
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    @Autowired
    private ChatService chatService;
    
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        String userMessage = message.getPayload();
        String sessionId = session.getId();
        
        // 2. 流式调用大模型
        Flux<String> stream = chatService.streamChat(sessionId, userMessage);
        
        // 3. 将流式结果逐条推送给前端
        stream.subscribe(
            content -> {
                try {
                    session.sendMessage(new TextMessage(content));
                } catch (IOException e) {
                    log.error("推送消息失败", e);
                }
            },
            error -> {
                log.error("流式调用出错", error);
                // 发送错误信息给前端
                sendErrorMessage(session, error.getMessage());
            },
            () -> {
                // 流结束，发送结束标记
                sendEndSignal(session);
            }
        );
    }
}

// 4. Spring AI 流式 API
@Service
public class ChatService {
    
    public Flux<String> streamChat(String sessionId, String userMessage) {
        return chatClient.prompt()
            .user(userMessage)
            .stream()
            .content();  // 返回 Flux<String>，每个元素是生成的一个文本片段
    }
}
```

**前端实现（Vue3）：**

```javascript
// 建立 WebSocket 连接
const ws = new WebSocket('ws://localhost:8080/chat');

ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    if (data.type === 'end') {
        // 流结束
        return;
    }
    // 逐字追加显示
    messages.value[messages.value.length - 1].content += data.content;
};

function sendMessage(text) {
    ws.send(JSON.stringify({ content: text }));
}
```

**关键考虑因素：**

| 因素 | 方案 | 原因 |
|------|------|------|
| 通信协议 | WebSocket | 服务端可以主动推送，适合流式场景 |
| 会话管理 | Redis 存储上下文 | WebSocket 断开后重连恢复对话 |
| 超时控制 | 30 秒无响应自动断开 | 防止大模型请求卡死连接 |
| 并发控制 | 单用户单会话，队列处理 | 防止多个请求冲突 |
| 异常处理 | 局部重试 + 友好提示 | 大模型服务偶尔超时是常态 |

> 💡 **核心要点**：流式对话的体验关键是"快"。第一次响应时间（TTFB，Time to First Byte）不能超过 2 秒，否则用户会觉得系统慢了。所以我们选择的是响应速度快的大模型，并在应用层做了超时和重试机制。

---

## 3. 进阶与系统设计

> 💡 这是拉开差距的环节，考察架构思维

### Q8：如果让你设计一个支持万人同时在线的 AI 对话平台，你会怎么做架构设计？

**面试官意图：** 考察将 AI 应用工程化的架构能力，从单机 Demo 到高可用服务的思维转变。

**完美解答：**

万人同时在线的 AI 对话平台，核心挑战是：**高并发请求 + 大模型响应慢 + Token 成本控制**。

**整体架构：**

```
客户端 -> WebSocket 集群（水平扩展）
  -> Gateway（限流 + 鉴权 + 路由）
    -> AI 对话服务集群
      -> 模型路由层
        -> OpenAI / 通义千问 / Ollama 本地模型
      -> 知识库（Milvus 向量库）
      -> 会话存储（Redis Cluster）
  -> 日志与监控（ELK + Prometheus）
```

**关键技术设计：**

**1. 多模型路由与负载均衡：**

```java
@Component
public class ModelRouter {
    
    @Autowired
    private List<ChatClient> chatClients;  // 多个模型实例
    
    public ChatClient selectModel(UserLevel level, BusinessType bizType) {
        // VIP 用户 -> GPT-4（高质量）
        if (level == UserLevel.VIP) {
            return chatClients.stream()
                .filter(c -> c.getModelName().equals("gpt-4"))
                .findFirst()
                .orElse(chatClients.get(0));
        }
        
        // 普通用户 -> 通义千问（高性价比）
        // 内部知识库 -> 本地 Ollama 模型（数据安全）
        // 高峰期 -> 自动降级到小模型
    }
}
```

**2. 请求排队与队列削峰：**

```java
// 大模型并发有限，高峰期请求排队处理
public class ChatQueueService {
    private final Queue<ChatRequest> queue = new LinkedBlockingQueue<>(10000);
    
    public CompletableFuture<String> submitChat(ChatRequest request) {
        CompletableFuture<String> future = new CompletableFuture<>();
        queue.offer(request);
        // 消费者从队列取请求，逐个调用大模型
        processQueue();
        return future;
    }
}
```

**3. 流式响应的连接管理：**

- 每个用户一个 WebSocket 连接，连接绑定到 AI 对话服务实例
- 连接断开后，Redis 中保存会话历史，重连后恢复
- 服务端设置心跳检测，30 秒无消息断开连接释放资源

**4. Token 成本控制：**

| 策略 | 说明 |
|------|------|
| Prompt 压缩 | 去除历史消息中的冗余信息 |
| 上下文窗口限制 | 只保留最近 N 轮对话 |
| 缓存命中 | 相同问题的回答直接返回缓存 |
| 模型降级 | 高峰时段自动切到成本更低的小模型 |

> 🎯 **架构核心思想**：AI 应用和传统应用最大的区别是——大模型是"慢资源"。传统应用 99% 的请求在 100ms 内返回，但大模型一次调用可能需要 2-10 秒。所以架构设计的关键是"排队 + 异步 + 流式"。

---

### Q9：大模型应用中的"幻觉"问题你们是怎么解决的？请说几个具体方案。

**面试官意图：** 考察对 AI 应用落地中核心痛点的理解和解决方案。

**完美解答：**

幻觉是大模型在落地时最大的障碍。我们从四个层面做了系统性的防范：

**第一层：RAG 检索增强（最有效）**

```java
// 强制模型基于检索结果回答，不得自由发挥
String response = chatClient.prompt()
    .system("""
        重要规则：
        1. 你必须严格基于以下文档内容回答用户问题
        2. 如果文档中没有相关信息，必须回答："抱歉，我的知识库中没有相关信息"
        3. 不得添加文档之外的任何信息
        4. 在回答末尾标注引用来源：[文档名称]
        """)
    .user("【文档】" + context + "\n【问题】" + question)
    .call()
    .content();
```

**第二层：温度参数控制**

```java
// 降低 Temperature 减少模型的"创造性"
ChatClient client = ChatClient.builder()
    .defaultSystem("你是一个严谨的文档问答助手，请精确回答")
    .build();

// 事实性问答：temperature=0.1（几乎不做创新）
// 创意性问答：temperature=0.7（适度发挥）
```

**第三层：答案验证与溯源**

```java
@Service
public class AnswerValidator {
    
    public ValidatedAnswer validate(String question, String answer, List<Document> sources) {
        // 检查答案是否来源于提供的文档
        boolean hasSource = sources.stream()
            .anyMatch(source -> answer.contains(source.getContent().substring(0, 50)));
        
        if (!hasSource) {
            log.warn("答案可能包含幻觉：question={}, answer={}", question, answer);
            return ValidatedAnswer.uncertain(answer, "答案可能未经文档验证");
        }
        
        // 引用溯源：答案中的关键句在文档中能找到对应
        return ValidatedAnswer.confident(answer, sources);
    }
}
```

**第四层：用户交互设计**

```
在 UI 层面上：
- 每条回答都标注"引用来源"，用户可以点击查看原文
- 显示置信度（高/中/低），低置信度时提示用户"建议人工核实"
- 提供"重新生成"按钮，多次生成结果对比
```

> 💡 **经验结论**：没有任何方案能 100% 消除幻觉。最好的策略是"让模型不敢乱说"——通过 System Prompt 约束 + RAG 检索限定 + Temperature 控制，三管齐下将幻觉率降到可接受范围。

---

### Q10：Spring AI 应用的 Token 使用量怎么监控和控制？如果用户恶意刷 API 怎么办？

**面试官意图：** 考察 AI 应用工程化落地的运维能力和安全意识。

**完美解答：**

Token 就是钱，监控和控制 Token 使用是大模型应用上线前的必备工作。

**Token 监控体系：**

```java
// 1. AOP 切面统计 Token 消耗
@Aspect
@Component
public class TokenUsageAspect {
    
    @Around("@annotation(com.example.annotation.TrackToken)")
    public Object trackTokenUsage(ProceedingJoinPoint pjp) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = pjp.proceed();
        long duration = System.currentTimeMillis() - startTime;
        
        // 从 ChatClient 的响应中获取 Token 使用量
        if (result instanceof ChatResponse response) {
            TokenUsage tokenUsage = response.getMetadata().getTokenUsage();
            
            // 记录到数据库
            tokenUsageService.record(TokenRecord.builder()
                .userId(getCurrentUserId())
                .promptTokens(tokenUsage.getPromptTokens())
                .completionTokens(tokenUsage.getCompletionTokens())
                .totalTokens(tokenUsage.getTotalTokens())
                .duration(duration)
                .modelName(response.getMetadata().getModel())
                .build()
            );
            
            // 检查用户配额
            checkUserQuota(getCurrentUserId(), tokenUsage.getTotalTokens());
        }
        
        return result;
    }
    
    // 2. 用户配额控制
    private void checkUserQuota(Long userId, int tokensUsed) {
        // 获取用户当日累计 Token 使用量
        int dailyTokens = tokenUsageService.getDailyTokens(userId);
        int dailyLimit = userQuotaService.getDailyLimit(userId);
        
        if (dailyTokens + tokensUsed > dailyLimit) {
            throw new QuotaExceededException("您的每日 Token 配额已用完");
        }
    }
}
```

**防滥用策略：**

| 策略 | 实现 | 效果 |
|------|------|------|
| 单用户 QPS 限流 | Sentinel 按用户 ID 限流 | 控制调用频率 |
| Token 配额管理 | 每日/每月配额限制 | 控制总消耗 |
| 内容安全校验 | 敏感词拦截 | 防止生成违规内容 |
| 异常行为检测 | 同 IP 高频请求自动封禁 | 防止恶意刷 API |
| 分级计费 | 免费用户使用低配模型 | 控制成本 |

> ⚠️ **核心经验**：AI 应用的防滥用和传统接口的防滥用有本质区别——AI 接口的每一次调用都有"成本"。如果被恶意刷一次，可能产生几十上百元的 Token 费用。所以必须做 Token 配额 + 费用预警双重保障。

---

## 4. 场景题与故障排查

> 💡 考察实际解决问题的能力

### Q11：线上 RAG 系统突然回答质量变差，很多问题答非所问，你怎么排查？

**面试官意图：** 考察 RAG 系统的故障排查能力和检索质量的调优经验。

**完美解答：**

RAG 系统回答质量下降，排查要沿着"检索 -> 生成"的链路一步一步来。

**排查流程：**

```
Step 1: 确认是"检索不准确"还是"生成有问题"
  - 查看日志中的检索结果片段，看召回的文档是否相关
  - 查看日志中的最终 Prompt，看给大模型的信息是否正确

Step 2: 如果是检索不准确
  - 检查向量库中的数据是否正常（有没有被误删除）
  - 检查 Embedding 模型是否正常（有没有返回异常向量）
  - 检查文本分块策略有没有被意外修改

Step 3: 如果是生成有问题
  - 检查 System Prompt 有没有被修改
  - 检查大模型有没有被切换（比如从通义切换到其他模型）
  - 检查 Temperature 等参数有没有被调高
```

**典型问题及解决方案：**

```java
// 问题 1：检索召回的相关文档不准确
// 解决方案：优化检索策略
public List<VectorEntity> hybridSearch(String question) {
    // 向量检索（语义相似度）
    List<VectorEntity> vectorResults = vectorStore.similaritySearch(question, 5);
    
    // 关键词检索（精确匹配）
    List<VectorEntity> keywordResults = keywordSearch(question, 5);
    
    // 混合检索：合并结果并去重
    return mergeAndRerank(vectorResults, keywordResults);
}

// 问题 2：召回了正确文档但大模型没有用
// 解决方案：优化 Prompt 模板，强制要求基于文档回答
String prompt = """
    你是一个严谨的知识库问答助手。
    
    以下是参考资料：
    ---
    {context}
    ---
    
    请基于上述参考资料回答以下问题。如果你的知识库中不包含相关信息，请直接说"知识库中未找到相关信息"。
    不要使用你的内置知识来回答，因为用户只关心知识库中的内容。
    
    问题：{question}
    """;

// 问题 3：文档被更新后向量库还是旧数据
// 解决方案：文档变更时同步更新向量库
@EventListener
public void onDocumentUpdate(DocumentUpdateEvent event) {
    // 删除旧向量
    vectorStore.deleteByDocumentId(event.getDocumentId());
    // 重新解析和向量化
    processDocument(event.getDocument());
}
```

> 💡 **关键指标**：RAG 系统的检索质量可以用"召回率"和"准确率"两个指标衡量。我们内部定义了"Top-3 召回率"——排名前三的检索结果至少包含一个正确答案的比例。目标值：> 90%。

---

### Q12：线上大模型调用突然全部超时，服务几乎不可用，你怎么处理？

**面试官意图：** 考察 AI 应用的应急响应能力和容错方案设计。

**完美解答：**

这是一个 AI 服务依赖故障的紧急场景。我的处理流程是"先恢复、再排查、后根治"。

**第一阶段：快速恢复（5 分钟内）**

```java
// 方案 1：快速切换到备用模型
// 大模型服务配置：支持多模型自动切换
spring:
  ai:
    chat:
      client:
        primary-model: openai
        fallback-models: 
          - tongyi
          - ollama-local

// 方案 2：开启本地降级服务（大模型不可用时使用预置答案）
@Component
public class ChatFallbackService {
    private static final Map<String, String> FAQ = Map.of(
        "退款流程", "退款流程：登录后 -> 我的订单 -> 申请退款 -> 填写原因 -> 提交审核",
        "营业时间", "我们的营业时间是 9:00-21:00"
    );
    
    public String fallbackAnswer(String question) {
        // 关键词匹配预置答案
        for (Map.Entry<String, String> entry : FAQ.entrySet()) {
            if (question.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "AI 助手暂时无法服务，请稍后再试或联系人工客服";
    }
}

// 方案 3：前端友好提示
// 后端返回特定错误码
{
    "code": 503,
    "message": "AI 服务暂不可用，已切换至基础问答模式",
    "data": {
        "answer": "退款流程：登录后 -> 我的订单...",
        "mode": "fallback"
    }
}
```

**第二阶段：根因排查**

排查方向 | 可能原因 | 确认方式
---|---|---
大模型 API 限流 | 触发了供应商的速率限制 | 检查 API 返回状态码 429
网络问题 | 代理服务器故障或网络中断 | `curl` 测试连通性
API Key 失效 | 密钥过期或被盗刷后被禁用 | 检查 API 控制台
模型服务宕机 | 大模型供应商服务故障 | 查看供应商状态页

**第三阶段：长期根治**

- **多模型冗余**：至少接入 2 家以上大模型供应商
- **本地模型兜底**：部署 Ollama 本地模型作为最后一道防线
- **缓存热点问题**：高频问题的回答缓存到 Redis，减少 API 调用
- **熔断机制**：连续 5 次调用失败后熔断 30 秒，避免无谓重试

> 🎯 **核心理念**：不要把大模型当成"稳定可靠的基础设施"。大模型 API 随时可能挂、可能限流、可能变慢。AI 应用必须在架构层面做好"大模型挂了怎么办"的预案，这是一个 AI 工程师区别于普通开发者的关键能力。

---

### Q13：你们的 AI 对话系统上线后，发现用户反馈"回答太啰嗦"和"回答太简洁"都有，怎么调优？

**面试官意图：** 考察对 Prompt 调优的实战经验，以及如何根据用户反馈持续优化产品体验。

**完美解答：**

这是一个典型的"用户期望管理"问题，需要对 Prompt 做精细化的控制。

**分层调优方案：**

```java
// 1. 根据用户意图自动选择回答风格
@Component
public class StyleRouter {
    
    public ChatClient.PromptSpec applyStyle(String userMessage, ChatClient.PromptSpec promptSpec) {
        if (isSimpleQuestion(userMessage)) {
            // 简单问题：简洁回答
            return promptSpec.system("请用 50 字以内简洁回答");
        } else if (isComplexQuestion(userMessage)) {
            // 复杂问题：结构化的详细回答
            return promptSpec.system("请用分点形式详细回答，包含：定义、原因、解决方案");
        } else {
            // 默认：中等长度
            return promptSpec.system("请用 150 字左右回答，重点突出");
        }
    }
    
    private boolean isSimpleQuestion(String message) {
        // 简单问题特征：短（< 15 字）、问时间/地点/状态
        return message.length() < 15 
            || message.contains("几点") 
            || message.contains("在哪")
            || message.contains("多少");
    }
}

// 2. 用户可调参数（前端可配置）
参数面板设计：
  - 回答风格：简洁 / 标准 / 详细
  - 专业程度：通俗 / 标准 / 专业
  - 语气：正式 / 友好 / 幽默

// 3. Prompt 动态注入用户偏好
public String generateResponse(String question, UserPreference pref) {
    String styleInstruction = switch (pref.getStyle()) {
        case CONCISE -> "请用 50 字以内简洁回答，只说核心结论";
        case STANDARD -> "请用 150 字左右清晰回答";
        case DETAILED -> "请分三点以上详细分析，包含定义、原因、例子";
    };
    
    return chatClient.prompt()
        .system(styleInstruction)
        .user(question)
        .call()
        .content();
}
```

> 💡 **核心经验**：Prompt 调优没有银弹，最好的方法是——数据驱动。"简洁/标准/详细"三级可调 + 用户反馈收集 + A/B 测试，根据数据而不是感觉来优化。

---

## 💎 面试加分金句
- "Spring AI 最大的价值不是封装了 API 调用，而是提供了一套 Java 开发者熟悉的编程范式来开发 AI 应用。"
- "RAG 的核心不是检索，而是'增强'——如何把检索到的信息有效地注入到生成过程中，是决定问答质量的关键。"
- "Agent 智能体的难点不在于单个工具调用，而在于复杂任务拆解和失败重试的策略设计。"
- "大模型应用落地最大的障碍不是技术能力不够，而是'幻觉'控制不住——RAG + 低温度 + 引用溯源是必选的三件套。"
- "AI 应用工程化要考虑的不只是功能实现，Token 成本管控、服务降级、数据安全、内容审核都是上线前必须解决的。"

## 📋 高频追问清单
| 追问方向 | 应对策略 |
|----------|----------|
| Spring AI 如何切换大模型？ | 在配置中修改 `spring.ai.chat.model`，API 不变，只需改配置 |
| RAG 中文本分块大小怎么确定？ | 根据文档结构和模型上下文窗口调整，一般 300-1000 token，配合重叠 |
| Function Calling 和 Agent 有什么区别？ | Function Calling 是单次工具调用，Agent 是多次自主决策+工具编排 |
| Embedding 模型怎么选？ | 中文场景推荐 text2vec 或通义 Embedding，英文推荐 OpenAI Embedding |
| 向量数据库怎么选？ | Milvus（大规模）、Chroma（轻量）、PgVector（PostgreSQL 插件） |
| 大模型的 Temperature 参数有什么用？ | 控制输出的随机性，0=精确，1=创意，事实问答用 0.1，创意场景用 0.7 |

## 🔗 关联知识点
- [SpringCloudAlibaba 面试问答](./SpringCloudAlibaba必做项目-面试问答.md)
- [Spring 7个必做项目面试问答](./Spring7个必做项目-面试问答.md)
- [MyBatis 面试问答](./MyBatis必做项目清单-面试问答.md)
