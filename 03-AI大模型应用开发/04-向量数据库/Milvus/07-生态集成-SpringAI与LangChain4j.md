# 07 生态集成：Spring AI 与 LangChain4j

> Java 生态接入 Milvus 的两条主路——Spring AI 的 VectorStore 与 LangChain4j 的 langchain4j-milvus，配合 Python 数据管道，构成完整的 RAG 工程链路

---

## 📚 目录

1. [Java 生态集成总览](#1-java-生态集成总览)
2. [Spring AI：VectorStore 配置与使用](#2-spring-aivectorstore-配置与使用)
3. [LangChain4j：langchain4j-milvus 与重排序](#3-langchain4jlangchain4j-milvus-与重排序)
4. [Python 生态：pymilvus / LangChain / Dify](#4-python-生态pymilvus--langchain--dify)
5. [完整 RAG 集成链路（Java 视角）](#5-完整-rag-集成链路java-视角)
6. [Agent Memory：动态记忆与按需检索](#6-agent-memory动态记忆与按需检索)

---

## 1. Java 生态集成总览

| 框架 | 依赖 | 抽象 | 特点 |
|------|------|------|------|
| **Spring AI** | `spring-ai-milvus-store` + `milvus-sdk-java` | `VectorStore` 接口 | 官方出品、配置化、与 Spring 生态一体 |
| **LangChain4j** | `langchain4j-milvus` | `EmbeddingStore` 接口 | 功能丰富、贴近 Python LangChain 设计 |
| 原生 SDK | `milvus-sdk-java` | 直接调用 | 底层控制（自定义检索逻辑） |

```xml
<!-- Spring AI + Milvus 依赖 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-milvus-store</artifactId>
    <version>1.0.0-M6</version>
</dependency>
<dependency>
    <groupId>io.milvus</groupId>
    <artifactId>milvus-sdk-java</artifactId>
    <version>2.4.0</version>
</dependency>
```

**选型建议**：Spring Boot 项目无脑 Spring AI（官方维护、配置即用）；需要更细的检索控制（重排/多路召回编排）用 LangChain4j；两者皆可混合（底层都是同一个 milvus-sdk-java）。

> 🎯 **核心要点**：Java 生态的集成路径 = "**Spring AI（配置化默认）或 LangChain4j（灵活控制）**"，底层共用 `milvus-sdk-java`——**先选框架抽象，再落 SDK 细节**。

---

## 2. Spring AI：VectorStore 配置与使用

```yaml
# application.yml —— 配置即用（自动装配 VectorStore Bean）
spring:
  ai:
    vectorstore:
      milvus:
        host: localhost
        port: 19530
        database-name: default
        collection-name: document_kb
        index-type: HNSW                # 索引类型
        metric-type: COSINE             # 度量
        embedding-dimension: 1536       # 与 Embedding 模型输出对齐
        initialize-schema: true         # 首次自动建集（开发期方便）
```

```java
@Service
public class RagService {
    private final VectorStore vectorStore;        // 自动注入（Spring AI 抽象）

    // ① 文档入库（自动切分 + Embedding + 写入 Milvus）
    public void ingest(Document doc) {
        vectorStore.add(List.of(doc));            // 内部：切分 → Embedding → upsert
    }

    // ② 相似检索（语义）
    public List<Document> search(String query) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(10)                 // 召回数
                        .similarityThreshold(0.3) // 相似度阈值（过滤低分）
                        .build());
    }

    // ③ 过滤检索（元数据过滤）
    public List<Document> searchFiltered(String query, String documentId) {
        return vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(5)
                        .filterExpression("document_id == '" + documentId + "'")
                        .build());
    }

    // ④ 删除
    public void remove(String documentId) {
        vectorStore.delete(List.of(documentId));
    }
}
```

**Spring AI + LLM 的完整 RAG 调用**（衔接 `06-开发框架-LangChain4j-SpringAI/`）：

```java
@Service
public class ChatWithRag {
    private final ChatClient chatClient;         // LLM 客户端
    private final VectorStore vectorStore;

    public String ask(String question) {
        // ① 检索相关文档
        List<Document> docs = vectorStore.similaritySearch(question, 5);
        // ② 拼装上下文
        String context = docs.stream()
                .map(Document::getText).collect(Collectors.joining("\n---\n"));
        // ③ 交给 LLM 生成（含系统提示与上下文）
        return chatClient.prompt()
                .system("你是客服助手，只根据提供的资料回答：" + context)
                .user(question)
                .call().content();
    }
}
```

> 🎯 **核心要点**：Spring AI 的集成 = "**配置（yml）+ 注入（VectorStore）+ 三方法（add/similaritySearch/delete）**"——框架替你处理切分、Embedding、Milvus 写入的全部细节；`filterExpression` 是检索过滤的入口（对应 Milvus 的 expr）。

---

## 3. LangChain4j：langchain4j-milvus 与重排序

**LangChain4j 的 EmbeddingStore 抽象**（更细的控制面）：

```java
// ① 配置
EmbeddingStore<TextSegment> milvusStore = MilvusEmbeddingStore.builder()
        .host("localhost").port(19530)
        .collectionName("document_kb")
        .dimension(1536)
        .build();

// ② 入库：Embedding 模型 + 存储
EmbeddingModel embeddingModel = new OpenAiEmbeddingModel(...);   // 或 BGE 本地模型
TextSegment segment = TextSegment.from(content, metadata);
String id = milvusStore.add(embeddingModel.embed(segment).content(), segment);

// ③ 检索
Embedding queryVec = embeddingModel.embed(question).content();
List<EmbeddingMatch<TextSegment>> matches = milvusStore.findRelevant(queryVec, 50);
```

**重排序（Cross-Encoder）—— LangChain4j 生态的质量利器**：

```java
// 召回 Top-50 → Cross-Encoder 精细重排 → Top-5 给 LLM
RerankModel reranker = new OnnxRerankModel(Path.of("bge-reranker-v2-m3.onnx"));  // 本地重排模型
List<EmbeddingMatch<TextSegment>> reranked = reranker.rerank(
        question, matches, 5);        // 重排后取 Top-5

// 完整链路：召回（Milvus）→ 重排（Cross-Encoder）→ 生成（LLM）
// 收益：准确率显著提升（重排是"召回质量"之后第二重要的优化点）
```

**Spring AI vs LangChain4j 对比**（面试选型题）：

| 维度 | Spring AI | LangChain4j |
|------|:---------:|:-----------:|
| 出品 | Spring 官方 | 社区（借鉴 Python LangChain） |
| 集成度 | Spring 生态一体（自动装配） | 独立库 |
| 检索控制 | VectorStore 抽象（够用） | EmbeddingStore + 重排等更细 |
| 文档生态 | 与 Boot 文档一体 | 功能多但文档较散 |
| 适合 | **Spring Boot 标准项目** | 需要细粒度编排的项目 |

> 🎯 **核心要点**：LangChain4j 的价值 = "**更细的检索编排**"——**召回 Top-50 → Cross-Encoder 重排 → Top-5** 是 RAG 质量优化的标准动作（本地 ONNX 重排模型免 API 调用）。Java 重排链路是 LangChain4j 相对 Spring AI 的差异化能力。

---

## 4. Python 生态：pymilvus / LangChain / Dify

**Python 生态是数据管道的标配**（清洗/切分/Embedding 生态最全）：

```python
# pymilvus（底层）
from pymilvus import Collection
collection = Collection("document_kb")
collection.load()
results = collection.search(query_vec, "embedding", param={...}, limit=10)

# LangChain（高层编排）
from langchain_milvus import Milvus
from langchain_openai import OpenAIEmbeddings

vector_store = Milvus(
    embedding_function=OpenAIEmbeddings(model="text-embedding-3-small"),
    collection_name="document_kb",
    connection_args={"host": "localhost", "port": "19530"},
    search_params={"search_type": "hybrid", "dense_weight": 0.7, "sparse_weight": 0.3},  # 混合检索
)
docs = vector_store.similarity_search(question, k=10)
```

**工程分工（Java + Python 混合架构）**：

```text
Python：数据管道（文档解析 → 切分 → Embedding → 写入 Milvus）
        —— 生态最全（PyMuPDF/切分器/Embedding 模型全在 Python）
Java：业务服务（检索 → 重排 → LLM 回答）
        —— Spring AI/LangChain4j 直接查 Milvus
Dify/Coze：低代码 RAG 应用（内置 Milvus 集成，快速搭建）
```

> 🎯 **核心要点**：生态分工的行业实践 = "**Python 管"造数据"（管道）、Java 管"用数据"（检索+生成）**"——两端都直连 Milvus，数据管道与业务服务解耦。

---

## 5. 完整 RAG 集成链路（Java 视角）

**Java 后端的一个完整 RAG 模块**（生产骨架）：

```text
┌─ 入库链路（管道）────────────────────────────┐
│ 文档上传 → 解析（PDF/Word）→ 切分（chunk）     │
│ → Embedding（BGE/OpenAI）→ Milvus upsert      │
└──────────────────────────────────────────────┘
┌─ 问答链路（在线）────────────────────────────┐
│ 用户问题 → Embedding → Milvus 检索（混合）     │
│ → 重排序（Cross-Encoder）→ 拼上下文 → LLM 回答  │
│ → 附引用（document_id + chunk 定位）           │
└──────────────────────────────────────────────┘
```

```java
@RestController
public class RagController {
    private final ChatWithRag chatService;

    @PostMapping("/ask")
    public Answer ask(@RequestBody Question q) {
        return chatService.ask(q.text());
        // 返回：answer + 引用的文档片段（溯源！）
    }
}
```

**生产注意**：

1. **溯源必做**：返回 `document_id/title/chunk`——用户与审计都能追溯答案来源；
2. **阈值与兜底**：检索相似度低于阈值 → 回答"知识库中未找到相关内容"（别硬编）；
3. **权限过滤**：多租户/权限在检索的 `filterExpression` 中注入（不能只靠 LLM）；
4. **评估闭环**：上线前用评测集测 Hit Rate/Recall（见 06 模块第 6 节）。

> 🎯 **核心要点**：RAG 集成链路的两个"不要"——**不要无阈值硬回答**（检索不到就明说）、**不要无溯源输出**（答案要能回指原文）——做到这两点，RAG 才从"Demo"变成"产品"。

---

## 6. Agent Memory：动态记忆与按需检索

**Agent + Milvus 的进阶形态**（2025 热点，衔接 `02-RAG检索增强生成/10-RAG与Agent结合`）：

```text
Simple RAG：每次查询强制检索（知识库只读）
Agentic RAG：检索成为 Agent 的工具（按需决定是否检索）
Agent Memory：记忆分类存储与动态管理：
  ├── 程序性记忆（长期偏好，importance > 0.8 才存）
  ├── 情景记忆（对话历史，30-90 天过期——TTL 场景！）
  └── 语义记忆（事实知识，长期）
```

**Milvus 作为 Agent 记忆的实践**：

```python
# ① 按记忆类型分 Collection（或分区）——语义空间不混杂（召回率关键）
memory_kb = Collection("agent_memory")      # 长期语义记忆
conversation_log = Collection("session_log") # 短期情景（TTL 过期）

# ② 情景记忆用 3.0 的实体级 TTL（TIMESTAMPTZ）自动过期
# ③ 检索 = Agent 的工具调用（Agent 自主决定何时查记忆）
# ④ 动态写入：对话结束 → 提炼关键信息 → upsert 到记忆库
```

> 🎯 **核心要点**：Agent Memory 的 Milvus 实践 = "**按记忆类型分库 + 实体 TTL 过期 + 检索作为工具**"——多 Collection 架构（不同语义空间分开）是召回率的关键（单库混装会导致语义空间混杂，见 `02-RAG检索增强生成/10`）。

---

**下一模块**：[08-生产实践与性能优化](./08-生产实践与性能优化.md) / **返回总览**：[00-Milvus知识体系总览](./00-Milvus知识体系总览.md)
