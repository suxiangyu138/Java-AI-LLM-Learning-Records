# RAG 检索增强生成（Java 实现）

> 2026 年的 Java RAG 已无需 Python 依赖——Spring AI + PGVector + Elasticsearch 覆盖从文档摄入到混合检索的完整链路，PGVector sparsevec 更让纯 PostgreSQL 内的原生混合检索成为现实

---

## 📚 目录

1. [RAG 架构全景](#1-rag-架构全景)
2. [文档摄入 Pipeline](#2-文档摄入-pipeline)
3. [向量存储与检索](#3-向量存储与检索)
4. [混合检索：PGVector + Elasticsearch](#4-混合检索pgvector--elasticsearch)
5. [PGVector sparsevec：2026 纯 PG 方案](#5-pgvector-sparsevec2026-纯-pg-方案)
6. [高级 RAG 模式](#6-高级-rag-模式)
7. [生产级实践清单](#7-生产级实践清单)

---

## 1. RAG 架构全景

### 1.1 标准 RAG 数据流

```text
┌─────────────────────────────────────────────────────────────┐
│                     Java RAG 数据流                           │
│                                                              │
│  📥 文档摄入（离线）                                           │
│  ┌──────┐   ┌────────┐   ┌────────┐   ┌───────────────┐    │
│  │ 原始  │→│ 文档    │→│ 文本    │→│ Embedding      │    │
│  │ 文档  │  │ 解析    │  │ 切片    │  │ 向量化         │    │
│  │(.pdf, │  │(Tika)  │  │(Splitter)│ │(bge-m3/OpenAI)│    │
│  │ .md…) │  └────────┘   └────────┘   └───┬───────┬───┘    │
│  └──────┘                                 │       │         │
│                                           ▼       ▼         │
│                                    ┌──────────┐ ┌────────┐  │
│                                    │ PGVector │ │   ES   │  │
│                                    │ 向量存储  │ │ 关键词  │  │
│                                    └────┬─────┘ └───┬────┘  │
│                                         │           │       │
│  🔍 在线检索                                              │   │
│  ┌──────┐   ┌────────┐   ┌────────────┐    │           │   │
│  │ 用户  │→│ Query  │→│ 混合检索    │←───┘           │   │
│  │ 问题  │  │ 向量化  │  │ 向量+关键词  │←───────────────┘   │
│  └──────┘   └────────┘   └──────┬─────┘                    │
│                                  │ RRF 融合                  │
│                                  ▼                           │
│  ┌──────┐   ┌────────┐   ┌────────────┐                    │
│  │ 用户  │←│ 最终    │←│ LLM 生成    │                    │
│  │      │  │ 回答    │  │ (上下文注入) │                    │
│  └──────┘   └────────┘   └────────────┘                    │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 核心依赖

```xml
<dependencies>
    <!-- Spring AI + OpenAI -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    </dependency>
    <!-- PGVector 向量存储 -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>
    </dependency>
    <!-- 文档解析（Apache Tika） -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-tika-document-reader</artifactId>
    </dependency>
</dependencies>
```

---

## 2. 文档摄入 Pipeline

### 2.1 智能切片策略

| 策略 | 原理 | 优点 | 缺点 | 推荐场景 |
|------|------|------|------|------|
| **固定大小** | 按 token 数切分 | 实现简单、速度快 | 破坏语义完整性 | 简单文档 |
| **语义切片** | 按段落/标题切分 | 语义完整 | 实现复杂 | 结构化文档 |
| **递归切片** | 先按大分隔符，再细分 | 平衡效率与语义 | 参数调优 | 通用场景 ✅ |
| **滑动窗口** | overlap 保证边界不丢失 | 关键信息不遗漏 | 存储冗余 | 关键信息密集型 |

```java
// Spring AI 中的切片配置
@Component
public class DocumentSplitterConfig {

    @Bean
    public TokenTextSplitter documentSplitter() {
        return new TokenTextSplitter(
            800,   // defaultChunkSize: 每块 800 token
            200,   // minChunkSizeChars: 最小字符
            200,   // minChunkLengthToEmbed: 最小嵌入长度
            100,   // maxNumChunks: 最大块数
            true   // keepSeparator: 保留分隔符
        );
    }
}
```

### 2.2 文档摄入服务（完整实现）

```java
@Service
@Slf4j
public class DocumentIngestionService {

    private final VectorStore vectorStore;
    private final ElasticsearchClient esClient;
    private final EmbeddingModel embeddingModel;
    private final TokenTextSplitter splitter;
    private final TikaDocumentReader reader;

    public DocumentIngestionService(
            VectorStore vectorStore,
            ElasticsearchClient esClient,
            EmbeddingModel embeddingModel,
            TokenTextSplitter splitter) {
        this.vectorStore = vectorStore;
        this.esClient = esClient;
        this.embeddingModel = embeddingModel;
        this.splitter = splitter;
        this.reader = new TikaDocumentReader();
    }

    /**
     * 摄入单个文档——并行写入 PGVector 和 Elasticsearch
     */
    public IngestionResult ingest(InputStream input, String fileName,
                                   Map<String, Object> metadata) {
        // Step 1: 文档解析（PDF/Word/Markdown → 纯文本）
        String rawText = reader.read(input);

        // Step 2: 文本切片
        List<Document> chunks = splitter.apply(
            List.of(new Document(rawText, metadata))
        );

        // Step 3: 批量向量化
        List<Document> embeddedChunks = embeddingModel.embed(chunks);

        // Step 4: 写入 PGVector（同步，保证数据一致性）
        vectorStore.add(embeddedChunks);

        // Step 5: 异步写入 Elasticsearch（不阻塞主流程）
        CompletableFuture.runAsync(() -> {
            try {
                indexToElasticsearch(embeddedChunks);
            } catch (Exception e) {
                log.error("ES 索引失败: {}", fileName, e);
                // 记录失败，稍后重试
            }
        });

        log.info("文档摄入完成: {} → {} 个分片", fileName, chunks.size());
        return new IngestionResult(fileName, chunks.size());
    }

    private void indexToElasticsearch(List<Document> chunks) {
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            esClient.index(IndexRequest.of(ir -> ir
                .index("knowledge_base")
                .id(chunk.getId())
                .document(Map.of(
                    "content", chunk.getContent(),
                    "chunk_index", i,
                    "metadata", chunk.getMetadata()
                ))
            ));
        }
    }
}
```

### 2.3 增量更新策略

```java
// 增量更新：删除旧索引 → 重新摄入
public void updateDocument(String docId, InputStream newContent) {
    // 1. 删除 PGVector 中的旧向量
    vectorStore.delete(List.of(docId));

    // 2. 删除 ES 中的旧索引
    esClient.deleteByQuery(dq -> dq
        .index("knowledge_base")
        .query(q -> q.term(t -> t.field("doc_id").value(docId)))
    );

    // 3. 重新摄入
    ingest(newContent, docId + ".pdf", Map.of("doc_id", docId));
}
```

---

## 3. 向量存储与检索

### 3.1 PGVector 表结构

```sql
-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 文档分片表
CREATE TABLE document_chunks (
    id          VARCHAR(64) PRIMARY KEY,
    content     TEXT NOT NULL,
    metadata    JSONB,
    embedding   VECTOR(1024)  -- 维度必须与 Embedding 模型匹配
);

-- HNSW 索引（生产级必备：加速检索 100x+）
CREATE INDEX ON document_chunks
    USING hnsw (embedding vector_cosine_ops)
    WITH (
        m = 24,               -- 每个节点的最大连接数
        ef_construction = 128  -- 构建时的搜索宽度
    );
```

### 3.2 向量检索

```java
@Service
public class VectorSearchService {

    private final VectorStore vectorStore;

    /**
     * 基础向量相似度检索
     */
    public List<Document> similaritySearch(String query, int topK) {
        return vectorStore.similaritySearch(
            SearchRequest.query(query)
                .withTopK(topK)
                .withSimilarityThreshold(0.7)  // 相似度阈值
        );
    }

    /**
     * 带元数据过滤的检索
     */
    public List<Document> filteredSearch(
            String query, String category, int topK) {

        // Spring AI 的 Filter.Expression 语法
        FilterExpression filter = new FilterExpression(
            FilterExpressionType.EQ,  // 等于
            new Key("category"),
            new Value(category)
        );

        return vectorStore.similaritySearch(
            SearchRequest.query(query)
                .withTopK(topK)
                .withFilterExpression(filter)
        );
    }
}
```

### 3.3 Embedding 模型维度对照

| 模型 | 维度 | 提供方 | 中文支持 | 推荐 |
|------|:---:|------|:---:|:---:|
| **bge-m3** | 1024 | BAAI（智源） | ✅ 优秀 | ⭐ 中文首选 |
| **bge-large-zh** | 1024 | BAAI（智源） | ✅ 优秀 | 中文专项 |
| **text-embedding-3-small** | 1536 | OpenAI | ⚠️ 可用 | 国际场景 |
| **text-embedding-3-large** | 3072 | OpenAI | ⚠️ 可用 | 高精度需求 |
| **nomic-embed-text** | 768 | Nomic AI | ⚠️ 可用 | 轻量级 |
| **mxbai-embed-large** | 1024 | mixedbread | ⚠️ 可用 | 多语言 |

---

## 4. 混合检索：PGVector + Elasticsearch

### 4.1 为什么需要混合检索

```text
向量检索擅长：语义匹配
  "如何提升系统性能" ≈ "优化响应速度的方法" ✅
  但："JVM调优" ≠ "JVM参数设置" ❌（语义相似但关键词不同）

关键词检索擅长：精确匹配
  "NullPointerException line 42" ✅
  但："空指针咋处理" ≠ "NullPointerException" ❌（同义不同词）

混合检索 = 向量 + 关键词 → RRF 融合 → 取长补短
```

### 4.2 混合检索完整实现

```java
@Service
@Slf4j
public class HybridSearchService {

    private static final int CANDIDATE_COUNT = 20;
    private static final double RRF_K = 60.0;  // RRF 常数

    private final VectorStore vectorStore;
    private final ElasticsearchClient esClient;

    /**
     * 混合检索入口
     */
    public List<SearchResult> hybridSearch(
            String query, int topK, Map<String, String> filters) {

        // 并行执行两种检索
        CompletableFuture<List<SearchResult>> denseFuture =
            CompletableFuture.supplyAsync(
                () -> denseSearch(query, filters));
        CompletableFuture<List<SearchResult>> sparseFuture =
            CompletableFuture.supplyAsync(
                () -> sparseSearch(query, filters));

        // 等待双方完成 → RRF 融合
        List<SearchResult> dense = denseFuture.join();
        List<SearchResult> sparse = sparseFuture.join();

        return rrfMerge(dense, sparse, topK);
    }

    /**
     * 密集检索：PGVector 向量相似度
     */
    private List<SearchResult> denseSearch(
            String query, Map<String, String> filters) {

        var request = SearchRequest.query(query)
            .withTopK(CANDIDATE_COUNT)
            .withSimilarityThreshold(0.65);

        if (filters != null && !filters.isEmpty()) {
            request.withFilterExpression(buildFilter(filters));
        }

        return vectorStore.similaritySearch(request)
            .stream()
            .map(doc -> new SearchResult(
                doc.getId(), doc.getContent(), doc.getMetadata(), "dense"))
            .toList();
    }

    /**
     * 稀疏检索：Elasticsearch BM25 关键词
     */
    private List<SearchResult> sparseSearch(
            String query, Map<String, String> filters) {

        try {
            var response = esClient.search(s -> s
                .index("knowledge_base")
                .query(q -> q.bool(b -> {
                    b.must(m -> m.match(ma -> ma
                        .field("content").query(query)));
                    // 可选：元数据过滤
                    if (filters != null) {
                        filters.forEach((key, value) ->
                            b.filter(f -> f.term(t ->
                                t.field("metadata." + key).value(value))));
                    }
                    return b;
                }))
                .size(CANDIDATE_COUNT),
                Map.class  // 或自定义 POJO
            );

            return response.hits().hits().stream()
                .map(hit -> new SearchResult(
                    hit.id(),
                    (String) ((Map<?,?>) hit.source()).get("content"),
                    (Map<String, Object>)
                        ((Map<?,?>) hit.source()).get("metadata"),
                    "sparse"))
                .toList();

        } catch (IOException e) {
            log.error("ES 检索失败", e);
            return List.of();  // 降级：只用向量结果
        }
    }

    /**
     * RRF（Reciprocal Rank Fusion）融合算法
     *
     * 核心思想：用排名而非分数合并两种检索结果，
     * 避免向量相似度和 BM25 分数不可比的问题
     */
    private List<SearchResult> rrfMerge(
            List<SearchResult> dense,
            List<SearchResult> sparse,
            int topK) {

        Map<String, SearchResult> idToResult = new HashMap<>();
        Map<String, Double> idToScore = new HashMap<>();

        // 密集检索结果
        for (int i = 0; i < dense.size(); i++) {
            SearchResult r = dense.get(i);
            idToResult.put(r.getId(), r);
            idToScore.merge(r.getId(),
                1.0 / (RRF_K + i + 1), Double::sum);
        }

        // 稀疏检索结果
        for (int i = 0; i < sparse.size(); i++) {
            SearchResult r = sparse.get(i);
            idToResult.putIfAbsent(r.getId(), r);
            idToScore.merge(r.getId(),
                1.0 / (RRF_K + i + 1), Double::sum);
        }

        // 按 RRF 得分排序，取 Top-K
        return idToScore.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue()
                .reversed())
            .limit(topK)
            .map(entry -> {
                SearchResult result = idToResult.get(entry.getKey());
                result.setScore(entry.getValue());
                return result;
            })
            .collect(Collectors.toList());
    }
}
```

---

## 5. PGVector sparsevec：2026 纯 PG 方案

### 5.1 概念

2026 年 PGVector 的 **sparsevec** 类型已生产可用，可以在**单个 PostgreSQL 实例**内完成向量 + 关键词混合检索，**无需维护 Elasticsearch 集群**。

```text
传统方案：PGVector + Elasticsearch（两个系统）
  PGVector（向量检索）──┐
                        ├── RRF 融合 → 结果
  Elasticsearch（关键词）──┘
  ❌ 需要维护两套基础设施
  ❌ 数据同步有延迟

2026 新方案：纯 PGVector（一个系统）
  PGVector dense vector（向量检索）──┐
                                     ├── 原生 RRF → 结果
  PGVector sparse vector（关键词）────┘
  ✅ 零同步延迟（ACID 事务覆盖）
  ✅ 一套 SQL 完成混合检索
```

### 5.2 sparsevec 表结构

```sql
-- 启用 pgvector（含 sparsevec）
CREATE EXTENSION vector;

CREATE TABLE document_chunks_v2 (
    id            VARCHAR(64) PRIMARY KEY,
    content       TEXT NOT NULL,
    metadata      JSONB,
    dense_embed   VECTOR(1024),    -- 密集向量（语义）
    sparse_embed  SPARSEVEC(250000) -- 稀疏向量（关键词，SPLADE 生成）
);

-- 双索引：密集 + 稀疏
CREATE INDEX ON document_chunks_v2
    USING hnsw (dense_embed vector_cosine_ops);

CREATE INDEX ON document_chunks_v2
    USING hnsw (sparse_embed vector_cosine_ops);
```

### 5.3 原生 SQL 混合检索

```sql
-- 2026 年 PGVector 原生混合检索：单条 SQL 搞定！
WITH dense_results AS (
    SELECT id, content, metadata,
           ROW_NUMBER() OVER (ORDER BY dense_embed <=> :query_dense) AS rank
    FROM document_chunks_v2
    ORDER BY dense_embed <=> :query_dense
    LIMIT 20
),
sparse_results AS (
    SELECT id, content, metadata,
           ROW_NUMBER() OVER (ORDER BY sparse_embed <=> :query_sparse) AS rank
    FROM document_chunks_v2
    ORDER BY sparse_embed <=> :query_sparse
    LIMIT 20
)
SELECT d.id, d.content, d.metadata,
       COALESCE(1.0 / (60 + dr.rank), 0) +
       COALESCE(1.0 / (60 + sr.rank), 0) AS rrf_score
FROM document_chunks_v2 d
LEFT JOIN dense_results dr ON d.id = dr.id
LEFT JOIN sparse_results sr ON d.id = sr.id
WHERE dr.id IS NOT NULL OR sr.id IS NOT NULL
ORDER BY rrf_score DESC
LIMIT 5;
```

---

## 6. 高级 RAG 模式

### 6.1 Agentic RAG

```text
Agentic RAG = Agent 自主决策检索策略的 RAG

传统 RAG：问题 → 检索 → 生成（固定流程）
Agentic RAG：
┌────────────────────────────────────────┐
│ 问题 → Agent 分析                       │
│   ├─ 需要拆解为子问题？→ 多步检索        │
│   ├─ 检索结果不够？→ 重写查询再检索       │
│   ├─ 需要元数据过滤？→ 提取过滤条件       │
│   ├─ 回答不够好？→ 自我修正              │
│   └─ 需要外部数据？→ 调用 Tool/API       │
└────────────────────────────────────────┘
```

```java
// Agentic RAG 的简化实现（LangChain4j AiServices）
@AiService
interface ResearchAgent {
    @SystemMessage("""
        你是一个研究助手。分析用户问题，确定合适的检索策略。
        如果检索结果不足以回答问题：
        1. 重写查询从不同角度检索
        2. 拆解复杂问题为子问题分别检索
        3. 如果仍无法回答，明确告知用户
        """)
    String research(@UserMessage String question);
}

// 工具：检索
class SearchTool {
    @Tool("在知识库中检索相关内容")
    List<String> search(@P("检索查询") String query) {
        return hybridSearchService.hybridSearch(query, 5, null)
            .stream().map(SearchResult::getContent).toList();
    }

    @Tool("按类别过滤检索")
    List<String> searchByCategory(
            @P("检索查询") String query,
            @P("文档类别") String category) {
        return hybridSearchService.hybridSearch(
            query, 5, Map.of("category", category))
            .stream().map(SearchResult::getContent).toList();
    }
}
```

### 6.2 查询重写（Query Rewriting）

```java
// 用 LLM 将用户口语化的查询改写为检索友好的查询
@Component
public class QueryRewriter {

    private final ChatClient chatClient;

    public String rewrite(String userQuery) {
        return chatClient.prompt()
            .system("""
                将用户问题改写为适合向量检索的查询语句。
                规则：
                1. 将口语化表达转为规范化术语
                2. 提取核心概念，去除无关修饰
                3. 如果可能，生成 2-3 个不同角度的查询
                4. 只输出改写后的查询，不要解释
                """)
            .user(userQuery)
            .call()
            .content();
    }
}
```

---

## 7. 生产级实践清单

### 7.1 幻觉防护五件套

| 措施 | 配置 | 效果 |
|------|------|------|
| **强约束 System Prompt** | "只基于检索结果回答，不要编造" | 减少编造 |
| **低 Temperature** | 0.1-0.3 | 减少随机性 |
| **高相似度阈值** | > 0.75 | 过滤低质量检索结果 |
| **明确拒答** | "知识库未找到相关信息" | 避免猜测 |
| **引用来源** | 回答中标注引用自哪个文档 | 可追溯 |

```java
@AiService
interface AccurateRagAgent {
    @SystemMessage("""
        你是基于知识库的问答助手。严格遵循以下规则：

        1. 只能使用检索结果中的信息回答问题
        2. 如果检索结果不足以回答问题，必须回复：
           "抱歉，当前知识库中暂未找到匹配的信息"
        3. 禁止编造、猜测或使用知识库以外的信息
        4. 回答中标注信息来源（文档名 + 段落编号）
        """)
    @Temperature(0.1)
    String answer(@UserMessage String question,
                  @V("context") String retrievedContext);
}
```

### 7.2 性能优化清单

| 优化项 | 操作 | 效果 |
|------|------|------|
| **HNSW 索引** | `REINDEX INDEX ...` 入库后重建 | 100x+ 检索加速 |
| **批量入库** | 每批 100 条，失败不中断 | 可靠性 + 吞吐 |
| **ef_search 调整** | `SET hnsw.ef_search = 64` | 精度 vs 速度权衡 |
| **连接池** | PGVector 连接池 > 20 | 高并发检索 |
| **结果缓存** | Redis 缓存常见查询 | 重复查询零延迟 |
| **异步 ES 写入** | CompletableFuture 不阻塞 | 摄入吞吐翻倍 |

### 7.3 常见坑与解决

| 坑 | 现象 | 解决 |
|------|------|------|
| **维度不匹配** | 向量写入失败 | Embedding 模型维度 = PGVector VECTOR(N) 的 N |
| **分片过大** | 检索结果不精确 | 减小 chunkSize 到 300-500 token |
| **分片过小** | 上下文断裂、缺失关键信息 | 增加 overlap 到 200 token |
| **检索结果重复** | Top-5 来自同一文档 | MMR 去重：按文档 ID 去重到至多 2 条 |
| **OOM** | 大批量向量化时内存溢出 | 分批处理 + 流式向量化 |
| **ES 同步延迟** | ES 检索不到刚入库的文档 | 改用 sparsevec 纯 PG 方案，或增加 refresh 频率 |

---

> 🎯 **核心要点**：Java RAG 在 2026 年已完全不需要 Python。完整链路由 Spring AI 驱动，PGVector + Elasticsearch 混合检索为核心，RRF 算法融合排序。PGVector sparsevec 的出现标志着"纯 PostgreSQL RAG"时代的到来——一套数据库解决所有检索需求。

---

**上一模块**：[02-MCP 协议 Java 实现](02-MCP协议Java实现.md) ｜ **下一模块**：[04-Agent 智能体开发](04-Agent智能体开发.md) ｜ **返回总览**：[00-Java AI 生态总览](00-Java AI生态总览.md)
