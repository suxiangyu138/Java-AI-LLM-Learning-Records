# 向量搜索与GraphRAG
> 原生 Vector 类型、SEARCH 语法与索引内过滤、GraphRAG 实现、GenAI Plugin 与 Aura Agent：Neo4j 在 AI 时代的核心战场。

---

## 📚 目录

1. [Neo4j 的 AI 定位](#1-neo4j-的-ai-定位)
2. [原生 Vector 类型](#2-原生-vector-类型)
3. [向量索引与 SEARCH 语法](#3-向量索引与-search-语法)
4. [索引内过滤](#4-索引内过滤)
5. [GraphRAG 实现](#5-graphrag-实现)
6. [GenAI Plugin 与 Aura Agent](#6-genai-plugin-与-aura-agent)

---

## 1. Neo4j 的 AI 定位

### 1.1 为什么图数据库需要向量

```text
RAG 的两类检索：
  向量检索：语义相似（embedding 相似度）
  图检索：结构关系（多跳遍历/知识图谱）

各自短板：
  纯向量：无法回答结构性问题（"X 的供应商的供应商"）
  纯图：无法处理语义模糊查询（"相关的文档"）

Neo4j 的答案：两者合一（一个库存向量 + 图 + 元数据）
  → 混合检索（语义 + 结构）在同一查询
  → 2025-2026 的 GraphRAG 基础设施
```

### 1.2 演进时间线（2024-2026）

| 版本 | 能力 |
|------|------|
| 5.x（2023-2024） | 向量索引（列表存储 + HNSW） |
| 2025.10 | **原生 Vector 类型**（VECTOR\<FLOAT32\>） |
| Cypher 25 | **SEARCH 语句**（原生向量语法） |
| 2026.02 | **索引内过滤 GA**（带过滤的向量搜索） |
| 2026.06 | HFQ 量化向量搜索预览、GRAPH TYPE GA |

> 🎯 核心认知：**Neo4j 的 AI 战略 = "图 + 向量"双引擎**——用图表达知识结构（实体/关系）、用向量表达语义相似（embedding），同一数据库内完成"语义检索 + 结构推理"的混合查询。这是 GraphRAG 区别于纯向量 RAG 的核心。

---

## 2. 原生 Vector 类型

### 2.1 定义与用法

```cypher
// 原生 Vector 类型（2025.10+）
CREATE CONSTRAINT chunk_vec
FOR (c:Chunk) REQUIRE c.vector_embedding IS :: VECTOR<FLOAT32>(1024);

// 写入向量（属性类型约束保证一致性）
CREATE (c:Chunk {
    text: '...',
    vector_embedding: $embedding    // 驱动传入 float 数组
});

// 读取
MATCH (c:Chunk) RETURN c.vector_embedding;
```

```text
Vector 类型的优势：
  ① 一等类型（驱动/协议/Cypher/存储全链路支持）
  ② 属性类型约束（数据完整性：维度/元素类型）
  ③ 未来的向量优化基础（存储/运算）
  ④ 代码更简洁（无 list 转换）

要求：
  Neo4j 2025.10+ / Aura 2025.10+
  官方驱动 v6.0+
  Cypher 25
  Block 格式存储

兼容：旧 list 向量索引继续可用（新项目用 Vector 类型）
```

### 2.2 向量操作符

| 操作符 | 含义 | 适用距离 |
|--------|------|---------|
| <-> | 欧氏距离 | L2 |
| <\|\|> | 内积 | 点积 |
| <=> | 余弦相似度 | cosine（默认推荐） |

```cypher
MATCH (a:Chunk {id: 1}), (b:Chunk {id: 2})
RETURN a.vector_embedding <-> b.vector_embedding AS l2_distance,
       a.vector_embedding <=> b.vector_embedding AS cosine_sim;
```

---

## 3. 向量索引与 SEARCH 语法

### 3.1 创建向量索引

```cypher
// 创建向量索引（Lucene HNSW 实现）
CREATE VECTOR INDEX chunk_vec_idx
FOR (c:Chunk) ON (c.vector_embedding)
OPTIONS {indexConfig: {
    `vector.dimensions`: 1024,
    `vector.similarity_function`: 'cosine'
}};

// 参数说明：
//   dimensions：向量维度（与 embedding 模型匹配）
//   similarity_function：cosine（默认）/ euclidean / dot
```

### 3.2 SEARCH 语句（Cypher 25）

```cypher
-- 向量搜索（原生 SEARCH 语法，替代过程调用）
SEARCH docs
WHERE docs.vector_embedding <=> $queryVector > 0.8
RETURN docs.title, docs.vector_embedding <=> $queryVector AS score
ORDER BY score DESC LIMIT 10;

-- 等价于旧的 db.index.vector.queryNodes（已弃用方向）
-- SEARCH 是 Cypher 25 的原生语法
```

```text
SEARCH 的特性：
  ① 原生语法（与 MATCH 集成）
  ② 支持 WHERE 过滤（索引内过滤，见下）
  ③ 阈值/排序/LIMIT
  ④ 语义更清晰（声明式）

2026.06 增强：
  IN 谓词支持（WHERE movie.genre IN [...]）
  HFQ 量化搜索预览（更快/更省内存）
```

### 3.3 向量 + 图混合查询

```cypher
-- 混合检索：向量找相似 + 图做结构（GraphRAG 核心）
SEARCH docs
WHERE docs.vector_embedding <=> $query > 0.7
RETURN docs.id AS docId, docs.vector_embedding <=> $query AS score
ORDER BY score DESC LIMIT 20
// → 得到相似文档后
MATCH (d:Doc {id: $docId})-[:MENTIONS]->(e:Entity)
RETURN e.name;                 -- 从相似文档出发探索实体关系

-- 或一个查询完成：
SEARCH docs
WHERE docs.vector_embedding <=> $query > 0.7
WITH docs LIMIT 20
MATCH (docs)-[:ABOUT]->(topic:Topic)
RETURN topic.name, count(*) AS mentions ORDER BY mentions DESC;
-- 语义检索 + 图结构分析一体化
```

---

## 4. 索引内过滤

### 4.1 三种过滤模式（v2026.01 预览 / v2026.02 GA）

| 模式 | 机制 | 特点 |
|------|------|------|
| **索引内过滤** | WHERE 下推到向量索引内 | 低延迟 + 高召回（推荐） |
| 查询后过滤 | SEARCH 后再 WHERE | 需超量获取（可能漏） |
| 查询前过滤 | 先 Cypher 定候选子图再精确评分 | 100% 召回但代价高 |

### 4.2 索引内过滤的使用

```cypher
-- 创建索引时声明可过滤属性
CREATE VECTOR INDEX chunk_vec_idx
FOR (c:Chunk) ON (c.vector_embedding)
OPTIONS {indexConfig: {
    `vector.dimensions`: 1024,
    `vector.similarity_function`: 'cosine'
}}
WITH [chunk.author, chunk.published_year];   -- 声明过滤属性

-- 查询：过滤在索引内完成（10M 向量实测低延迟高召回）
SEARCH chunks
WHERE chunks.vector_embedding <=> $query <= 0.5
  AND chunks.author = '张三'                  -- 索引内过滤
  AND chunks.published_year >= 2020
RETURN chunks.title, chunks.vector_embedding <=> $query AS score
ORDER BY score LIMIT 10;
```

### 4.3 过滤模式的选择

```text
选型建议：
  高频过滤 + 大数据集 → 索引内过滤（性能最优）
  过滤条件动态多变 → 查询后过滤（灵活，超量获取）
  需要 100% 召回 → 查询前过滤（候选子图）
  混合：核心过滤走索引内 + 复杂过滤查询后

实测参考（官方 10M 向量）：
  索引内过滤：宽/窄过滤均保持低延迟高召回
  → 2026 年生产首选
```

---

## 5. GraphRAG 实现

### 5.1 GraphRAG 架构（Neo4j 版）

```text
GraphRAG = 知识图谱 + RAG（图增强检索）

构建阶段：
  ① 文档 → LLM 抽取实体/关系（三元组）
  ② 写入图（节点/关系）
  ③ 切片 + embedding → 向量索引
  ④ 社区检测（Louvain）→ 社区摘要（可选）

查询阶段（混合检索）：
  ① 向量检索（语义相似文档/实体）
  ② 图遍历（实体关联扩展/多跳）
  ③ 社区摘要（全局视角）
  ④ 结果融合 → LLM 生成

对比纯向量 RAG：
  向量：语义召回（碎片化）
  GraphRAG：语义 + 结构（可回答"关系型问题"）
```

### 5.2 典型实现（Neo4j 栈）

```cypher
-- ① 实体写入图
MERGE (e:Entity {name: '张三'})
MERGE (o:Org {name: '某公司'})
MERGE (e)-[:WORKS_AT]->(o);

-- ② 向量索引（切片 embedding）
CREATE VECTOR INDEX chunk_vec FOR (c:Chunk) ON (c.embedding)
OPTIONS {indexConfig: {`vector.dimensions`: 1024}};

-- ③ 查询：向量召回 + 图扩展
SEARCH chunks
WHERE chunks.embedding <=> $query > 0.7
WITH chunks LIMIT 10
MATCH (chunks)-[:MENTIONS]->(e:Entity)
MATCH (e)-[r*1..2]-(related:Entity)
RETURN DISTINCT related.name LIMIT 20;
-- 语义召回 → 实体 → 多跳扩展（图结构增强上下文）
```

### 5.3 GraphRAG 的工程要点

```text
① 实体抽取质量：LLM 抽取的三元组决定图质量
   （去重/别名归一化——图建模）
② 向量维度匹配：embedding 模型与索引维度一致
③ 混合检索调优：向量 Top-N 与图跳数的平衡
④ 社区摘要：大规模图用社区摘要做全局问答
⑤ 延迟控制：图遍历跳数上限（性能）

与 LangChain4j/Spring AI 集成：
  Neo4j 提供向量存储适配（LangChain4j Neo4j 模块）
  图查询工具（Cypher 生成——text2Cypher）
```

---

## 6. GenAI Plugin 与 Aura Agent

### 6.1 GenAI Plugin

```text
GenAI Plugin：Neo4j 的 AI 函数扩展（从 Cypher 调用）
  能力：
    ① embed：文本 → embedding（调用 AI 提供商）
    ② search：向量搜索（SEARCH 集成）
    ③ generate：LLM 生成（调用模型）

提供商：OpenAI、Vertex AI、Amazon Bedrock 等

示例：
  // 从 Cypher 直接 embedding + 写入
  MATCH (c:Chunk) WHERE c.embedding IS NULL
  SET c.embedding = ai.embed('openai:gpt-4o-mini', c.text, {dimensions: 1024});

  // 生成（LLM 调用）
  RETURN ai.generate('openai:gpt-4o-mini',
    '总结：' + c.text, {system: '你是文档助手'});
```

### 6.2 Aura Agent（GraphRAG Agent）

```text
Aura Agent：Neo4j 的 Agent 产品（2026）
  能力：
    ① 本体驱动的专家 Agent（基于知识图谱生成）
    ② 向量 + 图检索工具（Agentic GraphRAG）
    ③ 链式推理/多跳（可解释）
    ④ text2Cypher（微调 Gemini Flash 生成图查询）
    ⑤ REST/MCP 部署（Agent 即服务）

部署：
  一键云部署（AuraDB Free/Pro/Business）
  价格：$0.35/agent/小时（公共端点）

定位：把"知识图谱 → 可用的 AI Agent"产品化
  → 降低 GraphRAG 工程门槛
```

### 6.3 2026 生态总结

```text
Neo4j 的 AI 栈（2026）：
  存储：图 + 向量（原生 Vector 类型）
  索引：HNSW（Lucene）+ 索引内过滤
  查询：Cypher 25（SEARCH）+ 混合检索
  算法：GDS（社区检测 → 社区摘要）
  函数：GenAI Plugin（embed/search/generate）
  产品：Aura Agent（开箱即用的 GraphRAG Agent）

对开发者的意义：
  ① GraphRAG 从"自研拼装"走向"平台能力"
  ② 图 + 向量混合检索成为 RAG 的标准形态
  ③ 与 LangChain4j/Spring AI 生态对接（07 模块）
```

> 🎯 **核心要点**：Neo4j 的 AI 主线 = **原生 Vector 类型 + SEARCH 语法 + 索引内过滤**（2025.10-2026.06 三代演进）——让"向量检索 + 图结构推理"在同一数据库、同一查询中完成。GraphRAG 的实现路径：实体抽取入图 + 切片向量化 + 混合检索（向量召回 + 图扩展 + 社区摘要）。GenAI Plugin 与 Aura Agent 把 GraphRAG 从工程变成平台能力。

---

## 7. 小结

| 问题 | 答案 |
|------|------|
| 原生 Vector 类型？ | VECTOR\<FLOAT32\>(n)（2025.10+，全链路支持） |
| SEARCH 语法？ | Cypher 25 原生向量搜索（替代过程调用） |
| 索引内过滤？ | WHERE 下推索引内（2026.02 GA，低延迟高召回） |
| GraphRAG 怎么做？ | 实体入图 + 切片向量化 + 混合检索 |
| GenAI Plugin？ | Cypher 内 embed/search/generate |
| Aura Agent？ | 开箱即用的 GraphRAG Agent（$0.35/时） |

**下一模块**：[07-Java与Spring Data Neo4j](07-Java与Spring Data Neo4j.md)　**返回总览**：[00-Neo4j知识体系总览](00-Neo4j知识体系总览.md)
