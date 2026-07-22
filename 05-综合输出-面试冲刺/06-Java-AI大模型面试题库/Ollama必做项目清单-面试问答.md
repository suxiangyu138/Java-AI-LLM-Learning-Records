# Ollama 面试问答清单
> 🎯 基于 Ollama 实战项目清单，涵盖面试高频问题与完美解答方案，帮助你在私有化大模型部署领域脱颖而出。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理

> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：Ollama 是什么？它解决了什么问题？与直接用 HuggingFace Transformers 推理有什么区别？
**面试官意图：** 考察你对 Ollama 定位的理解，以及为什么企业会选择它。

**完美解答：**
Ollama 是一个本地大模型运行工具，核心价值是"让大模型在本地一键跑起来"。它封装了模型下载、推理引擎、API 服务三个环节，用户只需一条命令就能启动一个完整的模型服务。

它解决的问题：
- **部署复杂性**：不用配置 Python 环境、安装 CUDA、处理依赖冲突
- **模型管理**：自动拉取模型权重、版本管理、多模型共存
- **API 标准化**：内置 OpenAI 兼容的 RESTful API，开箱即用

| 对比维度 | Ollama | HuggingFace Transformers |
|----------|--------|--------------------------|
| 安装复杂度 | 一条命令安装 | 需要 Python + Pip + 依赖 |
| API 支持 | 内置 REST + SSE 流式 | 需要自己封装 |
| 模型管理 | 自动拉取、版本管理 | 手动下载管理 |
| 多模型共存 | 一行命令切换 | 需要分别配置 |
| 生产可用性 | 中等（需配合网关） | 低（需大量工程开发） |
| 自定义灵活性 | 低（受 Ollama 能力限制） | 高（完全可控） |

**使用场景选择**：
- 快速原型验证、个人开发 → 选 Ollama
- 底层算法研究、自定义模型修改 → 用 Transformers
- 企业生产部署 → Ollama + SpringBoot 网关

**延伸追问应对：** 如果问"Ollama 内部用了什么推理引擎"，回答：Ollama 底层基于 llama.cpp，支持 GGUF 量化模型，利用了 CPU/GPU 混合推理和 KV 缓存优化。

---

### Q2：Ollama 的 Temperature 和 TopP 参数控制的是什么？在实际项目中你怎么调优？
**面试官意图：** 考察你对大模型生成参数的理解，以及实际调优经验。

**完美解答：**
这两个参数控制的是模型输出时的**随机性（多样性）**：

**Temperature（温度系数）**：
- 控制 softmax 输出概率分布的平滑程度
- 值越低（0-0.3）：概率分布更尖锐，输出更确定、更保守
- 值越高（0.7-1.5）：概率分布更平滑，输出更多样、更有创意
- 数学本质：`softmax(logits / temperature)`

**TopP（核采样阈值）**：
- 动态选择累计概率达到 P 的最小 token 集合
- 只在这个集合中采样，排除低概率的"长尾"token
- 与 Temperature 配合使用，通常二选一或同时使用

**我的调优经验**：

| 场景 | Temperature | TopP | 说明 |
|------|-------------|------|------|
| 代码生成 | 0.1-0.2 | 关闭 | 确定性高，减少语法错误 |
| NL2SQL | 0.0-0.1 | 关闭 | 必须精确，不允许创造 |
| RAG 问答 | 0.1-0.3 | 关闭 | 基于事实，不要发挥 |
| 翻译/摘要 | 0.3-0.5 | 0.9 | 适当多样性，避免死板 |
| 创意写作 | 0.7-0.9 | 0.95 | 鼓励创新和多样性 |
| 角色扮演 | 0.8-1.0 | 1.0 | 最大自由度 |

> ⚠️ 误区：很多人以为 Temperature 要按任务类型固定，实际上它应该根据实时反馈动态调整。我在 A/B 测试中会根据用户满意度动态调参。

---

### Q3：Ollama 支持流式输出（SSE）和非流式输出，你在什么场景下选择哪种方式？为什么？
**面试官意图：** 考察你对流式交互的理解和用户体验意识。

**完美解答：**
**流式输出（SSE/Server-Sent Events）**：模型逐 token 返回，前端逐步渲染。适合对话、生成类场景。

**非流式输出**：等待模型生成完整结果后一次性返回。适合分析、结构化输出场景。

我的选择原则：

```
场景 → 非流式：
- NL2SQL：需要拿到完整 SQL 再做安全性校验
- 信息提取：需要完整 JSON 才能解析
- 代码生成：部分代码可能导致语法错误
- 批处理任务：不需要即时展示

场景 → 流式：
- 对话系统：打字机效果提升用户体验
- 长文生成：用户不用等太久才能看到内容
- 实时翻译：逐句展示翻译结果
- 智能客服：用户等待感从 5 秒降到 1 秒
```

**技术实现要点**：
```java
// SpringBoot + WebClient 流式处理
@GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> chatStream(String message) {
    return webClient.post()
        .uri("http://localhost:11434/api/chat")
        .bodyValue(Map.of(
            "model", "qwen2.5",
            "messages", buildMessages(message),
            "stream", true
        ))
        .retrieve()
        .bodyToFlux(String.class)
        .map(this::parseToken);
}
```

> 💡 一个巧妙的方案：对于 NL2SQL，我用"先流式展示思考过程，最后一次性输出最终 SQL 和校验结果"的混合模式，既保证了用户体验又保证了安全性。

---

## 2. 项目实战深度问答

> 💡 面试官会深挖你的项目细节，验证你"真的做过"而不是"背过"

### Q4：你用 SpringBoot 封装过 Ollama 的对话接口，请讲一下你是如何管理多轮对话上下文的？
**面试官意图：** 考察 Java 后端与大模型整合的实际能力和上下文管理的工程思维。

**完美解答：**
多轮对话的核心问题是**随着轮数增加，上下文数组不断膨胀**，最终超出模型的上下文窗口限制（通常 4K-32K tokens）。

我的方案是**两级上下文管理**：

**第一级：Session 级管理（Redis 存储）**
```java
@Component
public class SessionManager {
    private final RedisTemplate<String, Object> redis;
    
    // 存储结构
    public void addMessage(String sessionId, Message msg) {
        String key = "session:" + sessionId;
        // List 结构存储历史消息
        redis.opsForList().rightPush(key, msg);
        // 控制总长度，超过阈值时压缩
        if (countTokens(key) > MAX_TOKENS) {
            compressHistory(key);
        }
    }
    
    private void compressHistory(String key) {
        // 1. 保留最近 3 轮完整对话
        // 2. 将更早的对话压缩为摘要
        // 3. 用摘要替换早期对话
        List<Message> recent = getRecentMessages(key, 3);
        String summary = llmClient.summarize(getEarlyMessages(key));
        // 重新构建上下文：[摘要] + [最近 3 轮]
        rebuildContext(key, summary, recent);
    }
}
```

**第二级：消息结构设计**
```java
@Data
public class Message {
    private String role;    // system / user / assistant
    private String content; // 消息内容
    private long timestamp; // 时间戳（用于会话过期清理）
    private int tokenCount; // 预计算的 token 数
}
```

**关键设计决策**：
- **消息压缩**：当上下文超过窗口 80% 时，触发自动压缩，用 LLM 把早期对话缩成一句话摘要
- **隔离性**：每个 session 独立上下文，互不干扰
- **过期清理**：30 分钟无活跃的 session 自动清理

---

### Q5：你在做轻量化 PDF 知识库问答时，文档分块的策略是怎么设计的？为什么？
**面试官意图：** 考察 RAG 系统中文本分块（Chunking）这一核心环节的工程经验。

**完美解答：**
分块策略直接决定了检索质量，进而影响最终回答的准确性。我的分块策略是**三层递进式**：

```yaml
第一层：按文档结构分割（粗分）:
  策略: 按 Markdown 标题 / PDF 章节拆分
  目的: 保证每个块是语义完整的段落
  效果: 避免在段落中间截断

第二层：按 Token 长度切割（精分）:
  策略: 
    - 目标长度: 512 tokens
    - 重叠长度: 128 tokens (滑动窗口)
  目的: 保证检索粒度适中，同时避免信息丢失

第三层：元数据标注:
  策略: 每个块标注来源、章节、页码
  目的: 引用溯源时能精确定位到原文
```

**我的代码实现**：
```java
public List<DocumentChunk> splitDocument(String content, String source) {
    List<DocumentChunk> chunks = new ArrayList<>();
    // Step 1: 按标题分割
    String[] sections = content.split("(?=^#{1,3}\\s)");
    
    for (String section : sections) {
        String sectionTitle = extractTitle(section);
        // Step 2: 按 token 长度细化
        List<String> subChunks = splitByTokens(section, 512, 128);
        for (int i = 0; i < subChunks.size(); i++) {
            chunks.add(new DocumentChunk(
                subChunks.get(i),
                source,
                sectionTitle,
                i,
                embed(subChunks.get(i))  // 实时向量化
            ));
        }
    }
    return chunks;
}
```

**关键经验**：
- 512 tokens + 128 重叠是最均衡的配置，既保证了语义完整性，又不会因为粒度太粗导致检索不精确
- 按标题分割比纯按 token 分割效果好 30% 以上
- 重叠窗口解决了"边界信息丢失"的问题

---

### Q6：你在实现工具调用 Agent 时，Function Calling 的工具体系是怎么设计的？遇到的最大问题是什么？
**面试官意图：** 考察 Agent 中工具调用的架构设计能力和问题排查经验。

**完美解答：**
Function Calling 是 Agent 的核心能力，我的设计采用**标准化+分层**策略：

**工具定义规范**：
```java
// 通用工具接口
public interface AgentTool {
    String getName();           // 工具名
    String getDescription();    // 工具描述（给模型看）
    String getParameters();     // JSON Schema 参数定义
    String execute(String paramsJson); // 执行逻辑
}

// 工具示例：联网搜索
@Component
public class WebSearchTool implements AgentTool {
    @Override
    public String getName() { return "web_search"; }
    
    @Override
    public String getDescription() { 
        return "搜索互联网获取实时信息，当需要最新数据时使用此工具。参数：query（搜索关键词）";
    }
    
    @Override
    public String execute(String paramsJson) {
        SearchRequest req = JsonUtils.parse(paramsJson, SearchRequest.class);
        return searchService.search(req.getQuery());
    }
}
```

**遇到的最大问题：工具选择不稳定**
模型经常选错工具或传错参数。根因是**工具描述写得太简单**。

**优化方案**：
1. **描述规范化**：每个工具描述包含"什么时候用 + 参数说明 + 输出说明"
2. **Few-shot 引导**：在 System Prompt 中加入工具选择的示例
3. **错误重试**：工具执行失败时，把错误信息反馈给模型，让它修正

```text
# System Prompt 中的工具选择引导
选择工具时遵循以下原则：
1. 需要最新信息 → web_search
2. 需要读写文件 → file_operation  
3. 需要执行计算 → calculator
4. 需要调用外部 API → http_request

如果不知道选什么工具，优先用 web_search。
```

---

### Q7：Spring AI 整合 Ollama 时，你做了哪些工程化处理（限流、熔断、缓存）？
**面试官意图：** 考察企业级 AI 服务工程化能力，区分"能用"和"生产可用"。

**完美解答：**
从 Demo 到生产，最大的差距就是工程化。我做了四层加固：

**1. 限流（Resilience4j RateLimiter）**
```java
@Bean
public RateLimiter ollamaRateLimiter() {
    return RateLimiter.of("ollama", RateLimiterConfig.custom()
        .limitForPeriod(10)           // 每秒 10 个请求
        .timeoutDuration(Duration.ofMillis(500))
        .build());
}

@RateLimiter(name = "ollama")
public String chat(String message) {
    return ollamaClient.chat(message);
}
```

**2. 熔断（Resilience4j CircuitBreaker）**
```java
@Bean
public CircuitBreaker ollamaCircuitBreaker() {
    return CircuitBreaker.of("ollama", CircuitBreakerConfig.custom()
        .failureRateThreshold(50)         // 50% 失败率触发熔断
        .waitDurationInOpenState(Duration.ofSeconds(30))
        .slidingWindowSize(10)
        .build());
}
```

**3. 缓存（Redis）**
```java
@Cacheable(value = "qa_cache", key = "#question.hashCode()")
public String ask(String question) {
    return ollamaClient.chat(question);
}
// 缓存热点问题，相同问题直接返回缓存结果
```

**4. 异步处理**
```java
@Async
public CompletableFuture<String> chatAsync(String message) {
    return CompletableFuture.completedFuture(ollamaClient.chat(message));
}
```

**效果数据**：
- 系统 QPS 从 50 提升到 200（缓存 + 异步）
- P99 响应时间从 5s 降到 1.2s
- 高峰期熔断触发 0 次（限流有效拦截了尖峰流量）

---

## 3. 进阶与系统设计

> 💡 这是拉开差距的环节，考察架构思维

### Q8：如果让你设计一个多模型调度系统，按场景自动选择最合适的模型，你会怎么做？
**面试官意图：** 考察系统架构能力和对多模型场景的理解。

**完美解答：**
我的核心思路是**路由策略 + 模型画像 + 动态调度**三位一体：

**系统架构**：
```
用户请求 → 请求分析器 → 路由决策 → 模型实例池
                              ↓
                         监控指标 ← Prometheus + Grafana
```

**路由决策引擎**：
```java
@Component
public class ModelRouter {
    @Autowired
    private List<RoutingStrategy> strategies;
    
    public ModelInstance route(Request request) {
        // 策略链：按权重投票
        Map<String, Integer> votes = new HashMap<>();
        for (RoutingStrategy strategy : strategies) {
            String model = strategy.evaluate(request);
            votes.merge(model, strategy.getWeight(), Integer::sum);
        }
        return modelPool.get(winner(votes));
    }
}

// 策略示例：任务类型匹配
@Component
public class TaskTypeStrategy implements RoutingStrategy {
    @Override
    public String evaluate(Request request) {
        return switch(request.getTaskType()) {
            case CODE_GENERATION -> "deepseek-coder";
            case CHINESE_CHAT -> "qwen2.5";
            case REASONING -> "llama3";
            case LIGHTWEIGHT -> "gemma:2b";
        };
    }
    
    @Override
    public int getWeight() { return 40; }
}
```

**模型负载监控**：每个模型实例上报 QPS、平均响应时间、错误率、GPU 使用率，路由决策时优先选择负载最低的模型。

**动态扩缩容**：当某个模型负载超过 80% 时，自动启动新实例；低于 20% 时自动回收。

---

### Q9：在 Docker 容器化部署 Ollama 集群时，你如何解决多模型容器隔离和持久化存储的问题？
**面试官意图：** 考察容器化部署能力和运维思维。

**完美解答：**
**多模型隔离方案**：
```yaml
# docker-compose.yml
version: '3.8'
services:
  ollama-general:      # 通用对话模型
    image: ollama/ollama
    volumes:
      - ./models/general:/root/.ollama
    ports:
      - "11434:11434"
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
              
  ollama-code:         # 代码生成模型（隔离部署）
    image: ollama/ollama
    volumes:
      - ./models/code:/root/.ollama
    ports:
      - "11435:11434"
    environment:
      - OLLAMA_HOST=0.0.0.0
    
  rag-service:         # RAG 服务
    build: ./rag
    depends_on:
      - ollama-general
      - milvus

  milvus:              # 向量数据库
    image: milvusdb/milvus:latest
    volumes:
      - ./data/milvus:/var/lib/milvus
```

**关键设计**：
1. **数据卷分离**：每个模型的权重文件通过独立的数据卷挂载，互不干扰
2. **端口映射**：不同模型服务映射到不同端口，通过 Nginx 统一入口
3. **GPU 限制**：通过 `deploy.resources` 控制每个容器使用的 GPU
4. **健康检查**：每个服务配置 healthcheck，Ollama 挂了自动重启

**持久化存储策略**：
- 模型权重：使用本地数据卷，避免每次重启重新下载（模型通常 4-10GB）
- 向量数据：Milvus 数据持久化到独立磁盘
- 日志：通过 Docker 的 log driver 收集到 ELK

---

## 4. 场景题与故障排查

> 💡 考察实际解决问题的能力

### Q10：Ollama 服务响应越来越慢，甚至超时，你怎么定位和解决？
**面试官意图：** 考察性能排查能力和对 Ollama 运行机制的理解。

**完美解答：**
我会按照"现象定位 → 资源分析 → 参数优化 → 架构升级"的路径排查：

**Step 1：定位瓶颈**
```yaml
排查工具:
  1. nvidia-smi: 检查 GPU 利用率、显存
  2. top / htop: 检查 CPU、内存
  3. iostat: 检查磁盘 IO
  4. ollama logs: 检查是否有错误日志
```

**常见根因分析**：

| 现象 | 可能原因 | 解决方案 |
|------|----------|----------|
| GPU 利用率 100% | 并发请求太多，排队 | 加限流、扩容 |
| 显存不足 OOM | 模型太大或并发太多 | 换小模型、量化 |
| 磁盘 IO 高 | 显存不足导致内存交换 | 增加显存、降量化 |
| CPU 高但 GPU 低 | 数据传输瓶颈 | 优化批处理 |

**Step 2：快速优化**：
**A. 换量化版本**：从 Q8 降到 Q4，显存占用减半，速度提升 30-50%
```bash
ollama pull qwen2.5:7b-q4_0  # 4bit 量化版本
```

**B. 调整并发参数**：
```bash
# Ollama 环境变量优化
OLLAMA_NUM_PARALLEL=4        # 并行请求数
OLLAMA_MAX_LOADED_MODELS=2   # 最多加载 2 个模型
OLLAMA_KEEP_ALIVE=5m         # 模型保持 5 分钟不卸载
```

**C. 增加缓存层**：在 SpringBoot 层加 Redis 缓存，相同问题不重复调用。

**Step 3：架构升级**：如果单机无法满足，升级为集群部署 + Nginx 负载均衡。

---

### Q11：PDF 知识库问答中，用户问的问题检索不到相关内容，但实际上文档里有答案，怎么排查？
**面试官意图：** 考察 RAG 检索链路的问题排查能力。

**完美解答：**
这是 RAG 系统最常见的问题之一。我会从检索链路逆向排查：

**排查路径**：
```
用户问题 → Embedding → 向量检索 → 返回结果
         ↑              ↑
    (1) 问题编码    (2) 索引质量
```

**Step 1：检查 Embedding 质量**
- 问题："今年的销售目标是多少？" → 向量化
- 文档中有："2024 年销售额目标为 5000 万元"
- 排查：直接比较问题向量和文档向量的相似度

如果相似度低，说明 Embedding 模型对"销售目标"和"销售额目标"的语义理解不够好。解决方案：换更好的 Embedding 模型（如 bge-large-zh）。

**Step 2：检查分块策略**
常见问题：文档被切成小块后，"2024 年"和"销售额目标为 5000 万元"被分到了不同的块中。

解决方案：增加重叠窗口大小，从 128 tokens 增加到 256 tokens。

**Step 3：检查检索参数**
- TopK 太小（如设为 1）→ 改为 5-10
- 相似度阈值太高（如 0.9）→ 降为 0.7

**Step 4：混合检索**
如果纯向量检索不行，加 BM25 关键词检索做互补：
```java
public List<Chunk> hybridSearch(String question, int topK) {
    // 向量检索
    List<Chunk> vectorResults = vectorSearch(embed(question), topK);
    // 关键词检索
    List<Chunk> keywordResults = bm25Search(question, topK);
    // 重排序融合
    return reranker.merge(vectorResults, keywordResults);
}
```

> 💡 90% 的"检索不到"问题都是分块策略不当导致的，这是 RAG 系统最值得优化的环节。

---

### Q12：你的 Agent 在执行复杂任务时陷入了无限循环，一直重复调用同一个工具，怎么解决？
**面试官意图：** 考察 Agent 稳定性保障的实战经验。

**完美解答：**
这是 Agent 系统的经典故障模式。我的解决策略是**预防 + 检测 + 干预**三层防护：

**预防层（Prompt 层面）**：
```text
## 执行规则
- 每个工具最多调用 3 次
- 如果连续 2 次调用同一个工具返回相同错误，换方案
- 最大执行步数：10 步
- 如果无法完成，告知用户并终止
```

**检测层（代码层面）**：
```java
public class AgentLoopDetector {
    private final Map<String, Integer> toolCallCount = new HashMap<>();
    private final List<String> callHistory = new ArrayList<>();
    private static final int MAX_LOOP = 3;
    
    public boolean isInLoop(String toolName, String result) {
        // 检测：同一工具连续调用超过 3 次
        if (toolCallCount.merge(toolName, 1, Integer::sum) > MAX_LOOP) {
            log.warn("检测到循环: {} 已调用 {} 次", toolName, MAX_LOOP);
            return true;
        }
        // 检测：同上一次结果相同（重复循环）
        if (!callHistory.isEmpty() && callHistory.get(callHistory.size()-1).equals(result)) {
            return true;
        }
        callHistory.add(result);
        return false;
    }
}
```

**干预层（自动恢复）**：
```java
// 检测到循环时，执行恢复策略
public String recover(AgentContext ctx) {
    // 策略 1：简化任务，跳过失步
    ctx.addConstraint("跳过当前步骤，尝试用其他方法完成");
    
    // 策略 2：询问用户
    ctx.pauseAndAskUser("遇到困难，请提供更多信息");
    
    // 策略 3：安全退出
    return "抱歉，我无法完成这个任务。已有进展：" + ctx.getProgress();
}
```

**实际效果**：加入这三级防护后，Agent 的无限循环故障率从 15% 降到 0.5% 以下。

---

### Q13：生产环境的 Ollama 突然崩溃了，Docker 日志显示 OOM，你怎么快速恢复和预防？
**面试官意图：** 考察生产应急响应能力和容量规划能力。

**完美解答：**
**快速恢复（5 分钟内）**：
1. 立即重启容器：`docker restart ollama`
2. 如果仍然 OOM，启动备用模型：切换为量化版本（如 Q4）
3. 如果还没有恢复，启动备用服务（另一个节点上的 Ollama 实例）

**根因分析**：
```bash
# 查看 OOM 日志
docker logs ollama --tail 100 | grep -i "memory\|oom\|killed"

# 查看系统内存使用
free -h
nvidia-smi

# 排查并发连接数过高
netstat -an | grep 11434 | wc -l
```

**常见原因**：
- 多个大模型同时加载（每个 4-8GB 显存）
- 并发请求太多，每个请求的 KV Cache 占满显存
- 内存泄漏（Ollama 旧版本的 bug）

**长期预防方案**：
```yaml
预防措施:
  1. 限制加载模型数: OLLAMA_MAX_LOADED_MODELS=1
  2. 限制并行请求数: OLLAMA_NUM_PARALLEL=4
  3. 容器资源限制: 
     deploy:
       resources:
         limits:
           memory: 16G
  4. 监控告警:
     - 内存使用率 > 80% 触发告警
     - 自动扩容脚本
  5. 模型量化:
     - 全部使用 Q4 量化版本
     - 减少 70% 显存占用
```

---

## 💎 面试加分金句

- "Ollama 不是生产级的全部方案，但它是最快的原型验证工具——我通常用 Ollama 做 PoC，用 vLLM 做生产部署。"
- "私有化部署的核心价值不是省钱，而是数据不出域——这在金融、医疗、政务场景中是刚需。"
- "RAG 系统的天花板不是模型能力，而是文档分块和检索质量——把这块做好，7B 模型也能干过 GPT-4。"
- "工具调用 Agent 的瓶颈不是模型能不能理解工具，而是工具本身的描述写得清不清楚——好的工具描述胜过复杂的约束。"
- "SpringBoot + Ollama 不是简单地调 API，而是要做好限流、熔断、缓存、监控四件套才能上生产。"

## 📋 高频追问清单

| 追问方向 | 应对策略 |
|----------|----------|
| Ollama 遇到中文乱码怎么办？ | 检查模型是否支持中文（Qwen 系列最优），检查 Prompt 编码 |
| 多用户隔离怎么做？ | SpringBoot 会话层隔离，每个 session 独立上下文 |
| 模型回答乱编怎么办？ | 降 Temperature、加 RAG、加引用约束 |
| Ollama API 鉴权怎么实现？ | Nginx 层加 Token 校验，或 SpringBoot 网关统一鉴权 |
| Embedding 模型选哪个？ | 中文场景：bge-large-zh / m3e，通用：nomic-embed-text |
| 向量库选型对比？ | Milvus（生产）、Chroma（开发）、Elasticsearch（已有 ES 场景） |

## 🔗 关联知识点

- [PromptEngineering必做项目-面试问答.md](./PromptEngineering必做项目-面试问答.md)
- [MCP必做项目清单-面试问答.md](./MCP必做项目清单-面试问答.md)
- [大模型部署必做项目清单-面试问答.md](./大模型部署必做项目清单-面试问答.md)
