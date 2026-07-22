# MCP（Model Context Protocol）面试问答清单
> 🎯 基于 MCP 实战项目清单，涵盖面试高频问题与完美解答方案，帮助你在 AI 工程化协议领域脱颖而出。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理

> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：MCP（Model Context Protocol）是什么？为什么要发明它？它解决了什么问题？
**面试官意图：** 考察你对 MCP 协议本质的理解，以及它在 AI 工程化中的定位。

**完美解答：**
MCP 是 Anthropic 提出的**大模型上下文通信协议**，可以理解为"AI 世界的 USB 接口"——它定义了一套标准化的通信方式，让大模型能够统一地访问外部工具、数据源和系统。

它解决的核心问题是**上下文割裂和多系统数据孤岛**。在 MCP 出现之前，每个大模型应用都要自己实现一套工具调用和数据访问机制：

- 模型 A 的工具调用格式是 JSON，模型 B 是 XML
- 系统 1 的知识库接口是 RESTful，系统 2 是 WebSocket
- Agent 想访问数据库、搜索引擎、文件系统需要写三套不同的适配代码

MCP 通过四个核心抽象实现标准化：

| 抽象概念 | 作用 | 类比 |
|----------|------|------|
| **Context** | 请求上下文的载体，包含用户意图、历史、元数据 | HTTP Request Header |
| **Resource** | 数据资源的统一描述接口 | RESTful 的 Resource |
| **Tool** | 可调用工具的标准化定义 | gRPC 的服务定义 |
| **Prompt** | 提示词模板的标准格式 | OpenAPI 的 Schema |

**延伸追问应对：** 如果问"MCP 和 Function Calling 有什么区别"，回答：Function Calling 是单模型单工具的调用约定，而 MCP 是跨系统、跨模型的统一协议层，解决的是多系统协作问题。

---

### Q2：MCP 的通信协议是基于什么实现的？JSON-RPC 相比 RESTful 有什么优势？
**面试官意图：** 考察你对 MCP 通信层设计的理解，以及对不同通信协议优劣的认知。

**完美解答：**
MCP 的通信层基于 **JSON-RPC 2.0** 协议。JSON-RPC 是一种轻量级的远程过程调用协议，使用 JSON 格式传输请求和响应。

**JSON-RPC vs RESTful 的对比**：

| 对比维度 | JSON-RPC | RESTful |
|----------|----------|---------|
| 调用方式 | 远程函数调用 | 资源操作（CRUD） |
| 请求格式 | 统一 `{"jsonrpc":"2.0","method":"...","params":{},"id":1}` | 多种 HTTP 方法 + URL |
| 语义复杂度 | 低（一个 endpoint） | 中（多个端点多方法） |
| 批量处理 | 原生支持（一次发送多个请求） | 需要额外设计 |
| 与 AI 系统贴合度 | 高（模型天然适合函数式调用） | 中（需要额外适配层） |
| 流式支持 | 需要额外实现 SSE | 标准支持 |

**为什么 MCP 选择 JSON-RPC？**

第一，**语义简洁**。AI 系统的交互本质是"调用一个函数得到结果"，JSON-RPC 正好是对函数调用的天然映射。第二，**请求格式统一**。无论调用什么工具、访问什么资源，都是一样的约定，这对 Agent 的自动决策非常友好。第三，**批量处理能力**。Agent 经常需要同时调用多个工具，JSON-RPC 可以一次发送多个请求，减少网络开销。

**延伸追问应对：** 如果问"MCP 为什么不用 gRPC"，回答：gRPC 虽然性能更好，但使用 Protobuf 二进制格式，不利于调试和灵活性，对大模型的文本生成场景不够友好。

---

### Q3：MCP 中的 Resource、Tool、Prompt 三者有什么区别？在什么场景下使用哪一个？
**面试官意图：** 考察你对 MCP 三大核心抽象的理解深度和场景判断能力。

**完美解答：**
三者的核心区别是**数据 vs 操作 vs 模板**，对应的是"读什么、做什么、怎么说"。

| 抽象 | 本质 | 类比 | 使用场景 | 示例 |
|------|------|------|----------|------|
| **Resource** | 数据源 | REST 中的 Get | 需要读取数据时 | 读取文档、查数据库、获取文件 |
| **Tool** | 可执行操作 | REST 中的 Post/Put | 需要执行动作时 | 发送邮件、执行代码、调用 API |
| **Prompt** | 模板 | 函数的默认参数 | 需要标准化指令时 | RAG 回答模板、代码审查模板 |

**实战选择原则**：
- 如果 Agent 需要**读取信息**而不改变系统状态 → Resource
- 如果 Agent 需要**执行操作**改变系统状态 → Tool
- 如果需要对模型**输入做标准化** → Prompt

**三者可以组合使用**：一个典型的场景是，Agent 通过 Resource 读取知识库文档，通过 Tool 调用搜索引擎，两者获取的信息通过 Prompt 模板格式化后输入给模型。

---

## 2. 项目实战深度问答

> 💡 面试官会深挖你的项目细节，验证你"真的做过"而不是"背过"

### Q4：你在实现 MCP 客户端和服务端通信时，JSON-RPC 的请求/响应链路是怎样的？请提供一个具体例子。
**面试官意图：** 考察你对 MCP 基础通信机制的实际实现能力。

**完美解答：**
MCP 的通信链路遵循 JSON-RPC 2.0 规范，我实现的客户端-服务端交互流程如下：

**请求链路**：
```
客户端 → JSON-RPC 请求 → MCP 服务端 → 处理 → JSON-RPC 响应 → 客户端
```

**具体实现（Python 简化版）**：
```python
# MCP 客户端
import requests
import json

class MCPClient:
    def __init__(self, endpoint):
        self.endpoint = endpoint
        self.request_id = 0
    
    def call(self, method, params=None):
        self.request_id += 1
        payload = {
            "jsonrpc": "2.0",
            "method": method,
            "params": params or {},
            "id": self.request_id
        }
        response = requests.post(self.endpoint, json=payload)
        result = response.json()
        
        if "error" in result:
            raise Exception(f"MCP Error: {result['error']}")
        return result.get("result")
    
    # 使用示例：调用工具
    def search_knowledge(self, query):
        return self.call("tools/call", {
            "name": "knowledge_search",
            "arguments": {"query": query, "top_k": 5}
        })
    
    # 使用示例：读取资源
    def read_document(self, doc_id):
        return self.call("resources/read", {
            "uri": f"docs://{doc_id}"
        })

# 服务端处理
class MCPServer:
    def handle_request(self, request):
        method = request.get("method")
        
        if method == "tools/call":
            return self.handle_tool_call(request["params"])
        elif method == "resources/read":
            return self.handle_resource_read(request["params"])
        elif method == "prompts/get":
            return self.handle_prompt_get(request["params"])
        else:
            return {"error": {"code": -32601, "message": "Method not found"}}
```

**关键设计点**：
- 每个请求有唯一 `id`，支持异步响应的关联
- `jsonrpc` 字段固定为 "2.0"，版本兼容
- 错误码标准化：`-32700`（解析错误）、`-32600`（无效请求）、`-32601`（方法不存在）

---

### Q5：你用 MCP 标准化了 RAG 服务，请讲讲如何把检索结果封装为 MCP Resource 的？
**面试官意图：** 考察 MCP 在 RAG 场景中的实战落地能力。

**完美解答：**
这是 MCP 协议在 RAG 场景的核心应用。我设计的关键思路是**"检索结果即资源"**——把每次检索的结果封装为标准化的 MCP Resource，这样任何 MCP 兼容的客户端都可以直接访问。

**Resource 定义**：
```json
{
  "jsonrpc": "2.0",
  "method": "resources/read",
  "params": {
    "uri": "rag://knowledge-base/search",
    "parameters": {
      "query": "今年的销售目标是多少",
      "top_k": 5,
      "threshold": 0.7
    }
  },
  "id": 1
}
```

**服务端实现**：
```java
// SpringBoot MCP 服务端 - RAG Resource Handler
@Component
public class RAGResourceHandler implements ResourceHandler {
    
    @Autowired
    private VectorStore milvusClient;
    @Autowired
    private EmbeddingService embeddingService;
    
    @Override
    public String getUri() {
        return "rag://knowledge-base/search";
    }
    
    @Override
    public MCPResponse handle(MCPRequest request) {
        String query = request.getParam("query");
        int topK = request.getParam("top_k", 5);
        
        // 1. Embedding
        float[] vector = embeddingService.embed(query);
        
        // 2. 向量检索
        List<Document> docs = milvusClient.search(vector, topK);
        
        // 3. 封装为 Resource 格式
        List<Resource> resources = docs.stream()
            .map(doc -> new Resource(
                "rag://knowledge-base/doc/" + doc.getId(),
                doc.getContent(),
                Map.of(
                    "source", doc.getSource(),
                    "score", doc.getScore(),
                    "page", doc.getPageNum()
                )
            ))
            .collect(Collectors.toList());
        
        return MCPResponse.success(resources);
    }
}
```

**关键设计决策**：
- 每个检索结果 Resource 包含 `uri`（唯一标识）、`content`（文档内容）、`metadata`（来源、分数、页码）
- 支持 `threshold` 参数过滤低质量结果
- 返回多个 Resource，让客户端自由选择

> 💡 标准化后的 RAG 服务可以同时被 Web 端、Agent、第三方系统调用，这是纯 RESTful API 难以做到的。

---

### Q6：多源数据统一检索系统中，你是怎么把 MySQL、Elasticsearch、Milvus 三种数据源统一封装为 MCP Resource 的？遇到了什么兼容性问题？
**面试官意图：** 考察 MCP 在多数据源融合中的架构设计能力。

**完美解答：**
核心设计思路是**适配器模式 + 统一 Resource 抽象**：

**架构设计**：
```java
// 统一 Resource 接口
public interface DataSourceAdapter {
    String getSourceType();           // mysql / es / milvus
    List<MCPResource> search(String query, int topK);
}

// MySQL 适配器
@Component
public class MySQLAdapter implements DataSourceAdapter {
    @Override
    public List<MCPResource> search(String query, int topK) {
        // 使用 MySQL 全文索引或 LIKE 进行关键词搜索
        String sql = "SELECT id, title, content FROM documents " +
                     "WHERE MATCH(title, content) AGAINST(? IN BOOLEAN MODE) " +
                     "LIMIT ?";
        return jdbcTemplate.query(sql, new Object[]{query, topK}, this::mapToResource);
    }
}

// ES 适配器
@Component
public class ESAdapter implements DataSourceAdapter {
    @Override
    public List<MCPResource> search(String query, int topK) {
        // Elasticsearch 的全文搜索
        return elasticsearchClient.search(query, topK);
    }
}

// Milvus 适配器
@Component
public class MilvusAdapter implements DataSourceAdapter {
    @Override
    public List<MCPResource> search(String query, int topK) {
        // 向量相似度搜索
        float[] vector = embeddingService.embed(query);
        return milvusClient.search(vector, topK);
    }
}
```

**遇到的兼容性问题及解决方案**：

```yaml
问题 1: 数据格式不统一
  - MySQL 返回：结构化行数据
  - ES 返回：JSON 文档
  - Milvus 返回：向量 + 元数据
  解决: 统一封装为 MCPResource，包含 content + metadata + source_type 三个字段

问题 2: 检索结果评分不一致
  - 各数据源的相似度分数尺度不同
  解决: 做归一化处理，再进行重排序融合

问题 3: 实时性差异
  - MySQL 数据实时写入，ES 有近 1s 延迟，Milvus 依赖索引重建
  解决: 在 Resource metadata 中标注数据时间戳，让上游判断

问题 4: 查询语言不一致
  - SQL/Query DSL/向量检索 三种语法
  解决: 统一用自然语言查询，后台自动路由到合适的引擎
```

---

## 3. 进阶与系统设计

> 💡 这是拉开差距的环节，考察架构思维

### Q7：如果让你设计一个企业级 MCP 网关，需要具备哪些能力？请画出架构图并解释。
**面试官意图：** 考察系统级架构设计能力和 MCP 生产化思维。

**完美解答：**
企业级 MCP 网关的核心职责是**统一入口、安全管控、流量治理**。我会设计如下架构：

**系统架构**：
```
                         ┌─────────────┐
                         │  客户端/Agent │
                         └──────┬──────┘
                                │ MCP JSON-RPC
                         ┌──────▼──────┐
                         │  MCP 网关    │ ← SpringCloud Gateway
                         │  (统一入口)   │
                         └──────┬──────┘
              ┌─────────────────┼─────────────────┐
         ┌────▼────┐     ┌─────▼─────┐     ┌─────▼────┐
         │ MCP 服务1 │     │ MCP 服务2  │     │ MCP 服务3  │
         │ (RAG)    │     │ (Agent)   │     │ (Search)  │
         └──────────┘     └───────────┘     └──────────┘
```

**网关核心能力**：

| 能力 | 实现方案 | 说明 |
|------|----------|------|
| **请求路由** | 根据 method 字段分发 | `tools/call` → Agent 服务，`resources/read` → RAG 服务 |
| **鉴权** | Token + API Key | 每个请求校验身份 |
| **限流** | Redis + Sentinel | 按用户/服务/接口三级限流 |
| **熔断** | Resilience4j | 下游服务故障自动熔断 |
| **日志审计** | ELK | 记录每次 MCP 调用的全链路日志 |
| **负载均衡** | 轮询/最少连接 | 多实例负载分发 |
| **协议校验** | JSON Schema | 校验 JSON-RPC 格式合法性 |

**核心代码片段**：
```java
@Bean
public RouteLocator mcpGateway(RouteLocatorBuilder builder) {
    return builder.routes()
        .route("mcp-rag", r -> r
            .path("/mcp/**")
            .and().method("POST")
            .filters(f -> f
                .addRequestHeader("X-Gateway", "MCP-Gateway")
                .circuitBreaker(config -> config
                    .setName("mcpCircuitBreaker")
                    .setFallbackUri("forward:/fallback"))
                .requestRateLimiter(config -> config
                    .setRateLimiter(redisRateLimiter())))
            .uri("lb://mcp-rag-service"))
        .build();
}
```

---

### Q8：在多 Agent 协作系统中，多个 Agent 通过 MCP 通信时，你如何处理上下文一致性和任务状态流转？
**面试官意图：** 考察多 Agent 系统中状态管理的架构思维。

**完美解答：**
这是多 Agent 系统最复杂的问题。我的方案是**集中式上下文总线 + 分布式状态管理**：

**上下文总线设计**：
```
Agent A ──→ MCP Bus ──→ Agent B
              │
        Context Store
        (Redis + MySQL)
```

**具体实现**：
```java
// 上下文总线服务
@Service
public class MCPContextBus {
    
    @Autowired
    private RedisTemplate<String, Object> redis;
    
    // 发布上下文到总线
    public void publishContext(String taskId, MCPContext context) {
        String key = "mcp:task:" + taskId;
        redis.opsForHash().put(key, "context", context);
        redis.opsForHash().put(key, "status", "IN_PROGRESS");
        redis.opsForHash().put(key, "current_agent", context.getCurrentAgent());
        // 通知订阅该任务的 Agent
        redis.convertAndSend("mcp:task:" + taskId + ":events", context);
    }
    
    // Agent 消费上下文
    public MCPContext consumeContext(String taskId, String agentId) {
        String key = "mcp:task:" + taskId;
        MCPContext ctx = (MCPContext) redis.opsForHash().get(key, "context");
        if (ctx != null && ctx.getNextAgent().equals(agentId)) {
            return ctx;
        }
        return null; // 不是你的回合
    }
    
    // 状态流转
    public void transitionState(String taskId, String fromState, String toState) {
        String key = "mcp:task:" + taskId;
        redis.opsForHash().put(key, "state", toState);
        log.info("Task {} state: {} → {}", taskId, fromState, toState);
    }
}
```

**状态流转流程**：
```
用户需求 → Agent A(分析) → Agent B(检索) → Agent C(生成) → 返回用户
              ↑                                            |
              └──────────── 状态反馈循环 ──────────────────┘
```

**关键设计**：
1. **任务 ID 贯穿全链路**：每个请求生成唯一 taskId，所有 Agent 共享
2. **状态机约束**：预定义状态流转规则（分析→检索→生成→完成），不允许非法跳转
3. **超时兜底**：每个 Agent 处理超过 30s 自动超时，由调度器接管

---

## 4. 场景题与故障排查

> 💡 考察实际解决问题的能力

### Q9：MCP 服务的上下文（Context）越来越大，导致通信延迟飙升，你怎么优化？
**面试官意图：** 考察 MCP 系统性能优化能力和对上下文管理的理解。

**完美解答：**
这是 MCP 系统中随着使用时间增长必然遇到的问题，核心矛盾是**上下文窗口有限 vs. 累积信息无限**。

我采用**三级压缩策略**：

**第一级：增量传输**
只传输变化的部分，而不是全量上下文：
```json
// 全量上下文中，每次只传 diff
{
  "jsonrpc": "2.0",
  "method": "context/update",
  "params": {
    "task_id": "task-123",
    "delta": {
      "新增": ["新对话轮次"],
      "移除": ["已过时的临时信息"]
    }
  }
}
```

**第二级：上下文化摘要**
```java
public class ContextCompressor {
    public MCPContext compress(MCPContext ctx) {
        // 如果 token 数 < 阈值，不压缩
        if (ctx.estimateTokens() < MAX_TOKENS) return ctx;
        
        // 保留最近的 3 轮完整交互
        List<Round> recent = ctx.getRecentRounds(3);
        // 对早期交互做摘要
        String summary = llmClient.summarize(ctx.getEarlyRounds());
        // 重构：摘要 + 最近 3 轮
        return MCPContext.builder()
            .summary(summary)
            .recentRounds(recent)
            .keyFacts(ctx.getKeyFacts())
            .build();
    }
}
```

**第三级：上下文池化**
多个相似请求共享同一份上下文，减少重复传输。例如，同一个用户 5 分钟内发出的多个相关问题，共享基础知识和历史。

**效果**：优化后上下文大小从平均 8K tokens 降到 1.5K tokens，延迟降低 60%。

---

### Q10：MCP 服务端响应超时，客户端重试时出现了数据不一致，你怎么设计幂等机制？
**面试官意图：** 考察分布式系统中幂等性的设计能力。

**完美解答：**
MCP 通信中的幂等性是保证系统正确性的关键。数据不一致的根因在于客户端重试时服务端多次执行了同一个操作（如多次插入数据）。

**幂等设计方案**：
```java
// MCP 请求去重中间件
@Component
public class IdempotencyMiddleware {
    
    @Autowired
    private RedisTemplate<String, String> redis;
    
    // 请求去重：每个 request_id 只处理一次
    public boolean isDuplicate(MCPRequest request) {
        String idempotencyKey = "mcp:idempotent:" + request.getId();
        // SET NX 原子操作，已存在则返回 false
        Boolean success = redis.opsForValue()
            .setIfAbsent(idempotencyKey, "PROCESSING", 
                         Duration.ofMinutes(30));
        
        if (Boolean.TRUE.equals(success)) {
            return false; // 首次请求，正常处理
        }
        
        // 重复请求，返回已缓存的结果
        String cachedResult = redis.opsForValue()
            .get(idempotencyKey + ":result");
        return cachedResult != null;
    }
    
    // 处理完成后缓存结果
    public void cacheResult(MCPRequest request, MCPResponse response) {
        String resultKey = "mcp:idempotent:" + request.getId() + ":result";
        redis.opsForValue().set(resultKey, 
                                JsonUtils.toJson(response), 
                                Duration.ofMinutes(30));
    }
}
```

**处理流程**：
```
客户端请求(id=1) → 服务端收到 → Redis 锁 SETNX → 首次 → 执行 → 缓存结果
                                                       ↓
客户端超时重试(id=1) → 服务端收到 → Redis 锁已存在 → 返回缓存结果
```

**关键原则**：
- 每个请求必须有唯一 `id`
- 服务端使用 `id` 做去重，相同 `id` 只执行一次
- 执行结果缓存到 Redis，重试时直接返回缓存结果
- 缓存设置合理的 TTL（30分钟），避免无限占用空间

---

### Q11：MCP 协议下，如果某个 Tool 返回了错误或格式不符合预期，Agent 无法解析，你怎么设计容错机制？
**面试官意图：** 考察 MCP 系统中的异常处理和容错设计能力。

**完美解答：**
容错是多系统协作的必备能力。我设计了**三层容错金字塔**：

**第一层：MCP 协议层容错**
```json
{
  "jsonrpc": "2.0",
  "error": {
    "code": -32000,
    "message": "Tool execution failed",
    "data": {
      "tool_name": "web_search",
      "retryable": true,
      "suggestion": "网络超时，请稍后重试"
    }
  },
  "id": 1
}
```
定义了三种错误类型：
- `retryable: true` → 客户端可以重试
- `retryable: false` → 客户端不应重试，换方案
- `degraded: true` → 服务降级，返回部分结果

**第二层：客户端容错逻辑**
```java
public class MCPClientRetryHandler {
    public MCPResponse callWithRetry(MCPRequest request) {
        Exception lastException = null;
        
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                MCPResponse response = send(request);
                if (response.hasError()) {
                    MCPError error = response.getError();
                    if (!error.isRetryable()) {
                        break; // 非重试错误，直接返回错误
                    }
                } else {
                    return response; // 成功
                }
            } catch (TimeoutException e) {
                lastException = e;
                // 指数退避：2^秒
                Thread.sleep(1000 * (long) Math.pow(2, i));
            }
        }
        // 兜底：返回降级结果
        return MCPResponse.degraded("服务暂时不可用，请稍后再试");
    }
}
```

**第三层：超时熔断**
和前面的 RateLimiter + CircuitBreaker 方案一致，保护下游服务不被冲垮。

---

### Q12：MCP 网关如何与现有的 SpringCloud 微服务架构集成？在已有微服务体系中引入 MCP 会遇到哪些挑战？
**面试官意图：** 考察 MCP 与现有架构的集成能力，以及迁移落地的工程思维。

**完美解答：**
这是 MCP 落地的核心问题——如何在不颠覆现有架构的前提下引入新的 AI 通信协议。

**集成架构**：
```
现有微服务架构：
服务 A ←→ 服务 B ←→ 服务 C (内部 HTTP/RPC 通信)
                 ↓
        MCP 网关 (新增加层)
                 ↓
        AI 服务 (RAG/Agent/推理)

核心原则：MCP 不替代现有微服务通信，而是为 AI 场景新增一个协议层
```

**具体集成步骤**：
```yaml
1. 在 SpringCloud Gateway 中新增 MCP 路由:
   - 新增 /mcp/** 路径，由专门的 MCP 网关模块处理
   - 现有业务 API 不做任何改动

2. 将现有微服务的部分能力封装为 MCP Tool:
   - 订单服务 → order_query Tool（查询订单状态）
   - 用户服务 → user_info Tool（查询用户信息）
   - 商品服务 → product_search Tool（搜索商品）
   封装方式：在原有接口上加一层 MCP 适配器

3. 接入层改造:
   - 前端新增 AI 对话入口
   - 通过 MCP 网关统一调用 AI 能力
   - AI 通过 MCP Tool 调用现有微服务
```

**会遇到的主要挑战**：

| 挑战 | 说明 | 解决方案 |
|------|------|----------|
| **协议冲突** | 现有 HTTP/REST 和 JSON-RPC 同时存在 | 路径隔离 `/api/*` → REST, `/mcp/*` → JSON-RPC |
| **鉴权统一** | 两套鉴权体系 | 统一使用 JWT Token，MCP 层复用现有鉴权 |
| **超时差异** | 业务接口 <1s，AI 接口可达 30s+ | 独立配置 MCP 路由的超时参数 |
| **日志审计** | 需要统一记录两种请求 | ELK 统一收集，加 source 字段区分协议类型 |
| **运维监控** | 需要同时监控两组指标 | Prometheus 加 label 区分协议类型 |

**迁移策略**：
```
Phase 1: 旁路部署（1-2 周）
  - MCP 网关作为独立服务部署
  - 只接入 AI 相关请求，不影响现有业务

Phase 2: 逐步集成（2-4 周）
  - 将部分内部服务的接口封装为 MCP Tool
  - AI Agent 通过 MCP 调用这些工具

Phase 3: 全面融合（1-2 月）
  - 前端统一入口
  - 业务逻辑间通过 MCP 通信（可选）
```

---

## 💎 面试加分金句

- "MCP 是 AI 工程化的标准化基础设施，它让 AI 系统从'点对点对接'走向'协议化通信'，就像 HTTP 统一了 Web 通信一样。"
- "在 MCP 出现之前，每个 AI 系统都在重复造工具调用的轮子——MCP 的目标是让这些轮子可以通用。"
- "MCP 的 Resource/Tool/Prompt 三元抽象，本质上是把 AI 的'感知-行动-表达'三个环节标准化了。"
- "多 Agent 协作最难的从来不是单个 Agent 的能力，而是 Agent 之间的上下文传递和状态同步——MCP 的 Context 机制正好解决这个问题。"
- "MCP 网关和微服务网关的职责不同：微服务网关管的是流量，MCP 网关管的是上下文和智能路由。"

## 📋 高频追问清单

| 追问方向 | 应对策略 |
|----------|----------|
| MCP 和微服务网关的区别？ | 微服务网关管流量路由，MCP 网关管上下文路由和协议转换 |
| MCP 如何保证安全性？ | 传输层 HTTPS，应用层 Token 鉴权 + 请求校验 |
| MCP 的序列化格式？ | JSON-RPC，未来可能支持 Protobuf 提升性能 |
| MCP 支持异步通信吗？ | 通过 request id 关联请求和响应，支持双向流式通信 |
| MCP 的 URI 设计规范？ | `{protocol}://{service}/{resource_type}/{resource_id}` |
| MCP 和 Anthropic API 的关系？ | MCP 是通用协议，Anthropic API 是 MCP 协议的一个实现 |

## 🔗 关联知识点

- [PromptEngineering必做项目-面试问答.md](./PromptEngineering必做项目-面试问答.md)
- [Ollama必做项目清单-面试问答.md](./Ollama必做项目清单-面试问答.md)
- [大模型必做项目清单-面试问答.md](./大模型必做项目清单-面试问答.md)
