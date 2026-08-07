# 07 向量检索与 RAG 集成速查

> dense_vector 字段配置、kNN 检索、BM25+kNN 混合检索、与 Embedding 模型/Spring AI 的集成——"ES 做向量数据库的完整姿势"（2026 高频考点）

---

## 📚 目录

1. [向量检索在 ES 中的定位](#1-向量检索在-es-中的定位)
2. [dense_vector 字段配置速查](#2-dense_vector-字段配置速查)
3. [kNN 检索（NativeQuery）](#3-knn-检索nativequery)
4. [混合检索：BM25 + kNN](#4-混合检索bm25--knn)
5. [向量索引算法选型（HNSW / FLAT / INT8）](#5-向量索引算法选型hnsw--flat--int8)
6. [与 Spring AI / RAG 集成](#6-与-spring-ai--rag-集成)
7. [向量检索最佳实践与坑](#7-向量检索最佳实践与坑)

---

## 1. 向量检索在 ES 中的定位

```text
RAG 链路中的 ES 位置：

  文档 → 切片 → Embedding 模型 → 向量
                                ↓
                    ES 索引（dense_vector 字段 + 原文）
                                ↓
  用户问题 → Embedding 模型 → 查询向量 → kNN 检索 → Top-K 片段
                                          ↓
                              拼装上下文 → LLM 生成答案
```

| 方案 | 向量库能力 | 与全文检索结合 | 运维成本 |
|------|:---:|:---:|------|
| **ES 9（本组件）** | ✅ kNN + HNSW | ✅ 原生（混合检索） | 中（已有 ES 则最低） |
| Milvus | ✅ 最强 | ⚠️ 需配合 ES/BM25 | 高（独立集群） |
| Redis Stack | ✅ 基础 | ⚠️ | 低（但大规模弱） |
| PgVector | ✅ 中 | ⚠️ 扩展 | 低 |

> 🎯 **选型结论（2026）**：**已有 ES 集群的项目，向量检索首选 ES 9 本身**——一个集群同时扛"全文搜索 + 向量检索 + 聚合"，避免 Milvus 双集群运维；只有向量规模超大（亿级+）、对召回延迟极其苛刻才考虑独立向量库。完整选型对比见 [08-向量数据库总览](../../../../../03-AI大模型应用开发/08-向量数据库/向量数据库/00-向量数据库知识体系总览.md)。

### 1.1 两种向量类型

| 类型 | FieldType | 适用 | 说明 |
|------|-----------|------|------|
| dense_vector | `FieldType.Dense_Vector` | 语义检索（主流） | 稠密向量（float/int8），维度固定 |
| sparse_vector | `FieldType.Sparse_Vector` | 稀疏词权重 | 与 BM25 思路同源，少场景用 |

> 💡 本系列重点讲 dense_vector；稀疏向量（如 ELSER 模型产物）在 ES 中也有支持，但工程上仍是稠密向量的天下。

## 2. dense_vector 字段配置速查

```java
@Document(indexName = "knowledge")
public class KnowledgeChunk {
    @Id private String id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String content;                              // 原文（RAG 上下文）

    @Field(type = FieldType.Keyword)
    private String docId;                                // 所属文档

    @Field(type = FieldType.Dense_Vector,
           dims = 1024,                                 // ★ 维度必须与 Embedding 模型一致（4.2+）
           elementType = "float",                       // 元素类型（5.4+）：float / int8 / bit
           knnSimilarity = KnnSimilarity.Cosine,        // 相似度度量（5.4+）
           knnIndexOptions = {
               @KnnIndexOptions(
                   name = "hnsw",                       // HNSW 算法（默认）
                   m = 32,                              // 每节点邻居数
                   numCandidates = 200,                 // 构建期候选数
                   efConstruction = 200)
           })
    private float[] embedding;                          // 向量本体
}
```

| @Field 属性 | 说明 | 常见取值 |
|------------|------|---------|
| `dims` | 向量维度（**必须与 Embedding 模型输出一致**） | 384 / 768 / 1024 / 1536 |
| `elementType` | 元素精度（5.4+） | `float`（默认）/ `int8`（压缩）/ `bit` |
| `knnSimilarity` | 相似度度量（5.4+） | `Cosine`（默认）/ `L2Norm` / `DotProduct` |
| `knnIndexOptions` | 向量索引参数（5.4+） | HNSW 参数或 FLAT |

> ⚠️ **维度锁死警告**：dims 在创建索引时定死，**不可改**——换 Embedding 模型（如 bge-large→bge-m3，1024→4096）必须重建索引！换模型前先确认新旧维度，规划好重建窗口。

### 2.1 相似度度量选型

| 度量 | 语义 | 适合 |
|------|------|------|
| `Cosine` | 余弦相似度（忽略向量模长） | **通用首选**（OpenAI/bge 系模型输出与余弦配） |
| `L2Norm` | 欧氏距离（越小越相似） | 归一化向量或特定模型 |
| `DotProduct` | 点积 | 模型输出已归一化时（与余弦等价且更快） |

> 💡 **经验法则**：不确定时选 Cosine——绝大多数中文开源模型（bge 系列、gte 系列）都按余弦语义训练；部分模型输出自带归一化，可切 DotProduct 省一步计算。**同索引内度量不可变，选错要重建**。

## 3. kNN 检索（NativeQuery）

```java
// ① 查询文本 → 向量（由 Embedding 模型生成，见第 6 节）
float[] queryVector = embeddingService.embed("ES 的倒排索引原理是什么？");

// ② kNN 检索（5.3.1+：NativeQuery.withKnnSearches）
NativeQuery query = NativeQuery.builder()
    .withKnnSearches(k -> k
        .field("embedding")                            // 向量字段
        .queryVector(queryVector)                      // 查询向量
        .k(10)                                         // 返回 Top-10
        .numCandidates(100))                           // 候选数（越大越准，越慢）
    .withPageable(PageRequest.of(0, 10))
    .build();

SearchHits<KnowledgeChunk> hits = operations.search(query, KnowledgeChunk.class,
        IndexCoordinates.of("knowledge"));

// ③ 分数解读（cosine 语义下：约 1.0 越近，越接近 0 越远）
hits.getSearchHit(0).getScore();
```

| 参数 | 含义 | 调优 |
|------|------|------|
| `k` | 返回条数 | 与业务 Top-K 一致 |
| `numCandidates` | 每分片候选数 | 越大召回越准，延迟越高（经验：k×5~10） |
| `filter` | 过滤条件 | 结合 docId/租户过滤，大幅减少无效候选 |
| `preFilter` / `postFilter` | 过滤时机 | 见 4.2 |

> 🎯 **kNN 是近似搜索**：HNSW 不保证 100% 精确 Top-K——`numCandidates` 越高越接近暴力精确；对召回率敏感的场景（对账、精确匹配）用 `knn` + 大 numCandidates 或 FLAT 索引。

## 4. 混合检索：BM25 + kNN

### 4.1 为什么需要混合检索

| 检索方式 | 优势 | 盲区 |
|---------|------|------|
| BM25（全文） | 关键词精确命中、术语匹配强 | 语义相近但无共同词（"苹果" vs "水果公司") |
| kNN（向量） | 语义泛化、同义改写可召回 | 精确术语、产品型号（"iPhone 15 Pro Max"） |

**经典失败案例**：搜"AI 编程助手"，BM25 靠"编程"命中，向量靠语义命中——两路召回各有偏科，**混合 + 融合**才是 RAG 检索质量的正解（业界 2025-2026 的共识方案）。

### 4.2 混合检索实现（RRF 融合）

```java
// 方式一：NativeQuery 同时携带 query + knn（SDE 原生支持）
NativeQuery query = NativeQuery.builder()
    // BM25 路
    .withQuery(q -> q.multiMatch(mm -> mm
        .fields("content").query(question)))
    // 向量路
    .withKnnSearches(k -> k
        .field("embedding")
        .queryVector(queryVector)
        .k(20).numCandidates(200))
    // 可选：kNN 结果重打分（knn 里再套 query 过滤）
    .withPageable(PageRequest.of(0, 20))
    .build();

// 方式二（更精细）：ES 9 的 RRF 端点 / 或应用侧融合两路结果（详见深度 RAG 篇）
//   rank fusion = 各路的 1/(rank+60) 相加 → 综合排序
```

| 融合方式 | 实现位置 | 优点 | 缺点 |
|---------|---------|------|------|
| 同请求混合（query+knn） | ES 一次请求 | 简单、延迟低 | 分数融合规则由 ES 定 |
| 双路召回 + 应用侧 RRF | 应用代码 | 可定制权重 | 两次请求、代码多 |
| Rerank 模型精排 | 独立模型（BGE-Reranker） | 质量最高 | 额外延迟/成本 |

> 🎯 **2026 推荐链路**：**BM25 + kNN 双路召回 → RRF 融合 → （可选）Rerank 精排 → Top-K**——前两步是标配，第三步按预算取舍；检索质量评估方法见 [RAG-07 混合检索与Rerank](../../../../../03-AI大模型应用开发/04-RAG检索增强生成/07-混合检索与Rerank.md)。

## 5. 向量索引算法选型（HNSW / FLAT / INT8）

```java
// HNSW（默认，最常用）
@KnnIndexOptions(name = "hnsw", m = 32, efConstruction = 200)

// FLAT（暴力精确，小数据量）
@KnnIndexOptions(name = "flat")

// INT8 量化（内存压缩，5.4+）
@Field(type = FieldType.Dense_Vector, dims = 768, elementType = "int8")
```

| 算法 | 搜索精度 | 内存 | 构建时间 | 适合规模 |
|------|:---:|:---:|:---:|------|
| HNSW | 近似（可调） | 中 | 中 | 十万~千万级（**默认主力**） |
| FLAT | **100% 精确** | 高（不压缩） | 低 | ≤ 万级、精度敏感 |
| INT8 量化（HNSW/FLAT） | 略降 | **省 4 倍** | 中 | 内存吃紧的大规模 |

> 💡 面试高频问"为什么 HNSW 是默认"：图结构 + 多尺度的思想——每层稀疏跳转 + 底层精搜，复杂度从 O(n) 降到对数级；精度与速度通过 numCandidates/efSearch 权衡。`KnnAlgorithmType` 枚举（DEFAULT/FLAT/HNSW/INT8_FLAT/INT8_HNSW）在注解层直接可选，5.4+ 起配置粒度更细。

## 6. 与 Spring AI / RAG 集成

### 6.1 完整 RAG 写入链路（Spring AI + 本组件）

```java
// ① Embedding 模型（Spring AI 统一接口）
EmbeddingModel embeddingModel;      // OpenAI / Ollama(bge-m3) / DashScope 等

// ② 写入：切片 → 向量化 → 存 ES（本组件）
@Transactional // ❌ 不行——ES 无事务；用"先删后写 + 失败补偿"
public void indexChunk(KnowledgeChunk chunk) {
    float[] vector = embeddingModel.embed(chunk.getContent());
    chunk.setEmbedding(vector);
    operations.save(chunk, IndexCoordinates.of("knowledge"));
}

// ③ 查询：问题 → 向量化 → 混合检索 → Top-K
public List<KnowledgeChunk> retrieve(String question) {
    float[] qv = embeddingModel.embed(question);
    NativeQuery query = NativeQuery.builder()
        .withQuery(q -> q.multiMatch(mm -> mm.fields("content").query(question)))
        .withKnnSearches(k -> k.field("embedding").queryVector(qv).k(10).numCandidates(100))
        .build();
    return operations.search(query, KnowledgeChunk.class, IndexCoordinates.of("knowledge"))
        .getSearchHits().stream().map(SearchHit::getContent).toList();
}
```

### 6.2 RAG 落地检查清单（组件侧）

| 环节 | 组件侧要做的 | 深度参考 |
|------|-------------|---------|
| 切片策略 | 决定 content 粒度（对应 ES 一条文档） | [RAG-04 文本切片](../../../../../03-AI大模型应用开发/04-RAG检索增强生成/04-文本切片策略.md) |
| Embedding 选型 | dims 与模型对齐（bge-m3=1024、OpenAI=1536） | [RAG-05 Embedding与向量索引](../../../../../03-AI大模型应用开发/04-RAG检索增强生成/05-Embedding与向量索引.md) |
| 检索 | BM25 + kNN + RRF | [RAG-07 混合检索与Rerank](../../../../../03-AI大模型应用开发/04-RAG检索增强生成/07-混合检索与Rerank.md) |
| 评估 | 召回率/准确率回归（换模型/切参数后必测） | [RAG-08 评估与质量保障](../../../../../03-AI大模型应用开发/04-RAG检索增强生成/08-RAG评估与质量保障.md) |
| 生产运维 | 增量更新（upsert）、删除（文档下线） | 本系列 08 篇 |

> ⚠️ **RAG 生产三坑**：① 换 Embedding 模型 = 重建索引（dims 锁死）；② **向量与原文必须同文档**（kNN 只返回向量字段 + 其他字段，别把原文丢到另一个索引）；③ 检索质量必须有回归基准——"感觉准了"不算数，用评估集量化。

### 6.3 文档级元数据过滤（RAG 必备）

```java
// 租户隔离 / 文档过滤：filter 子句 + kNN 预过滤
NativeQuery query = NativeQuery.builder()
    .withKnnSearches(k -> k
        .field("embedding")
        .queryVector(qv)
        .k(10).numCandidates(100)
        .filter(f -> f.term(t -> t.field("docId").value(docId))))   // 限定单文档内检索
    .build();
```

## 7. 向量检索最佳实践与坑

| 坑/问题 | 现象 | 解法 |
|--------|------|------|
| 维度不匹配 | 写入报 `VectorDimensionMismatchException` | 检查模型输出与 dims；换模型重建索引 |
| 相似度方向反 | 结果分数含义反直觉 | 确认 knnSimilarity（cosine 越大越近；l2_norm 越小越近） |
| 召回质量差 | 语义相关的没召回 | 调大 numCandidates；检查向量字段是否被 refresh 过；混合检索 |
| 内存暴涨 | 大索引 OOM | INT8 量化 / 减少分片冗余 / 评估是否真需要 HNSW |
| 混合检索分数无意义 | RRF 结果无法直接比 | RRF 是排序融合不是分数融合，勿用分数做阈值 |
| 空向量 | 全零向量返回"不相似" | 空内容跳过向量化（长文本/纯符号） |
| 数据一致性 | 删除文档后向量残留 | 文档主键=ES _id，删除走 deleteById（RAG 运维脚本） |
| 隐私合规 | 外部 API Embedding 出网 | 数据敏感用本地模型（Ollama/bge-m3），向量字段加 `@Field(index=false)` 选项按需 |

> 🎯 **面试收尾**："ES 能当向量数据库用吗？"——能，且 2026 年是**首选默认**：kNN + HNSW + 混合检索原生支持，一套集群解决全文+向量；极限规模（亿级+高并发）再评估专用向量库（[Milvus](../../../../../03-AI大模型应用开发/08-向量数据库/Milvus/00-Milvus知识体系总览.md)）。回答加分项：提 dims 锁死、量化压缩、RRF 融合三个工程细节。

---

**下一模块**：[08-集成地图与常见问题](08-集成地图与常见问题.md)　**返回总览**：[00-Spring Data Elasticsearch组件总览](00-Spring Data Elasticsearch组件总览.md)

**【参考来源】**：[NativeQuery getKnnSearches（官方 API，5.3.1+）](https://docs.spring.io/spring-data/elasticsearch/docs/current/api/org/springframework/data/elasticsearch/client/elc/NativeQuery.html)、[@Field Javadoc（dims/elementType/knnSimilarity/knnIndexOptions）](https://docs.spring.io/spring-data/elasticsearch/reference/api/java/org/springframework/data/elasticsearch/annotations/Field.html)、[KnnAlgorithmType Javadoc](https://docs.spring.io/spring-data/elasticsearch/reference/api/java/org/springframework/data/elasticsearch/annotations/KnnAlgorithmType.html)、[RAG-07 混合检索与Rerank（深度）](../../../../../03-AI大模型应用开发/04-RAG检索增强生成/07-混合检索与Rerank.md)
