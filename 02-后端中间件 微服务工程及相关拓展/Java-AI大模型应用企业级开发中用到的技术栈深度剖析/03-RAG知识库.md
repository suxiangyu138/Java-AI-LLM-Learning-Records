# 03 - RAG 知识库

> 定位：RAG 原理、ETL 管线、向量库选型、检索调优、Agentic RAG——Java AI 最高频场景的完整体系

## 📚 目录

1. [RAG 原理与价值](#1-rag-原理与价值)
2. [ETL 管线](#2-etl-管线)
3. [向量库选型](#3-向量库选型)
4. [检索与调优](#4-检索与调优)
5. [RAG 的硬伤与升级](#5-rag-的硬伤与升级)
6. [Agentic RAG](#6-agenic-rag)

---

## 1. RAG 原理与价值

### 1.1 为什么需要 RAG

```
LLM 的三大问题：
  ① 知识截止（不知道新数据）
  ② 幻觉（不懂的会编造）
  ③ 无私有数据（企业文档/业务数据）

RAG（检索增强生成）解决方案：
  先检索相关知识 → 放入上下文 → 模型基于知识回答

⚠️ 面试必答：
"RAG = 检索 + 生成——
 从知识库检索相关内容拼进 Prompt，
 让模型'基于证据回答'（可溯源、低幻觉）。"
```

### 1.2 RAG 流程

```
离线（索引阶段）：
  文档 → 解析 → 分块 → 向量化（Embedding）→ 存入向量库

在线（查询阶段）：
  问题 → 向量化 → 向量库检索 TopK → 拼入 Prompt → 生成

⚠️ 面试必答：
"RAG 两阶段——离线建索引（ETL）、
 在线检索增强（查 + 拼 + 生成）；
 核心是'检索质量决定回答质量'。"
```

---

## 2. ETL 管线

### 2.1 四步管线

```
ETL 四步：
  ① Extract（解析）：文档 → 文本（Tika 支持 PDF/Word/HTML）
  ② Transform（分块）：文本 → 块（大小/重叠）
  ③ Enrich（增强）：块 → 带元数据（来源/标题/时间）
  ④ Load（入库）：块 → 向量化 → 向量库

⚠️ 面试必答：
"ETL 四步——解析、分块、增强、入库；
 文档质量是 RAG 效果的第一责任人。"
```

### 2.2 分块策略

```java
// Spring AI 分块（TokenTextSplitter）
// ⚠️ 关键参数：chunk size 与 overlap
TokenTextSplitter splitter = TokenTextSplitter.builder()
        .withChunkSize(800)            // 每块约 800 token
        .withChunkOverlap(100)         // 重叠 100（保持上下文连贯）
        .build();
List<Document> chunks = splitter.split(document);

// 分块策略选择：
//   小块（300-500）：检索精准但上下文割裂
//   中块（800-1000）：均衡（默认推荐）
//   大块（1500+）：上下文完整但检索噪音
// ⚠️ 块大小与模型窗口、检索粒度匹配
```

### 2.3 元数据增强

```java
// ⚠️ 元数据 = 检索过滤的基础（分类/部门/时间）
Document doc = new Document(content);
doc.getMetadata().put("category", "订单手册");
doc.getMetadata().put("department", "客服部");
doc.getMetadata().put("updated", "2026-07-01");

// 检索时按元数据过滤（精确命中）：
// filterExpression = "department == '客服部'"
```

> 🎯 **要点**：分块（800 + 100 重叠）+ 元数据（分类/部门/时间）是 ETL 的两大关键——分块决定检索粒度、元数据决定过滤能力。

---

## 3. 向量库选型

| 向量库 | 规模 | 延迟 | 场景 |
|--------|:---:|:---:|------|
| PgVector | 中 | 中 | **已有 PostgreSQL 零额外运维** |
| Redis | 中 | **<5ms** | 低延迟场景 |
| Milvus | **>1000 万** | 中 | 大规模检索 |
| Chroma | 小 | 快 | 开发测试 |
| Elasticsearch | 大 | 中 | 已有 ES 复用（BM25+向量） |

```
选型决策：
  已有 PostgreSQL → PgVector（零运维）
  低延迟（实时客服） → Redis
  大规模（>千万文档） → Milvus
  已有 ES 生态 → ES（混合检索）

⚠️ 面试必答：
"向量库选型看'规模 + 延迟 + 现有基建'——
 PgVector 零成本起步、Milvus 上规模、
 Redis 低延迟；与 Java 框架均有集成。"
```

```java
// Spring AI 向量库统一抽象（VectorStore）
VectorStore vectorStore;    // 实现：PgVector/Redis/Milvus

// 写入
vectorStore.add(List.of(documents));

// 检索
List<Document> results = vectorStore.similaritySearch(
    SearchRequest.builder()
        .query("订单如何退款")
        .topK(5)
        .build());
```

---

## 4. 检索与调优

### 4.1 检索参数

```java
// ⚠️ 关键参数调优
SearchRequest.builder()
        .query(userQuestion)
        .topK(3)                        // ⚠️ 知识库 <100 篇用 3、>1000 篇用 5-10
        .similarityThreshold(0.7)       // ⚠️ 相似度阈值（生产 0.65-0.75）
        .filterExpression("department == '客服部'")   // 元数据过滤
        .build();
```

### 4.2 检索质量优化

```
检索效果三板斧：
  ① TopK 调优（太少漏、太多噪）
  ② 相似度阈值（过滤无关内容）
  ③ 元数据过滤（精确缩小范围）

进阶手段：
  混合检索（向量 + BM25 关键词）
  Rerank 重排（检索后精排）
  引用溯源（回答带文档来源）

⚠️ 面试必答：
"检索优化 = TopK + 阈值 + 过滤 三板斧，
 进阶 = 混合检索 + Rerank + 溯源；
 '检索质量决定回答质量'。"
```

---

## 5. RAG 的硬伤与升级

### 5.1 传统 RAG 的三大硬伤

```
① 跨文档联合推理（答案分散在多文档）
② 动态数据（知识库更新不及时）
③ 多条件判断（复杂查询）

⚠️ 面试必答：
"传统 RAG 单轮检索——跨文档、动态、
 复杂条件场景失效；
 升级方向 = Agentic RAG。"
```

### 5.2 升级路线

```
RAG 演进：
  朴素 RAG（单轮检索）→ 进阶 RAG（查询改写/重排）
  → Agentic RAG（Agent 动态决策检索）

⚠️ 面试必答：
"RAG 不是终点——业务复杂度上来后
 升级 Agentic RAG：让 Agent 决定
 '何时检索、检索什么、怎么检索'。"
```

---

## 6. Agentic RAG

### 6.1 核心思想

```
Agentic RAG = Agent 驱动的检索：
  ① 动态决策：是否检索、检索什么（工具选择）
  ② 多路召回：向量 + BM25 + 知识图谱 + API
  ③ 自我纠错：检索结果不满意 → 改写查询重试
  ④ 引用溯源：回答附来源（可验证）

对比朴素 RAG：
  朴素：固定"问题 → 检索 → 回答"
  Agentic：动态编排检索策略

⚠️ 面试必答：
"Agentic RAG = Agent 管理检索过程——
 多路召回、查询改写、自我纠错、
 溯源验证；解决朴素 RAG 的
 跨文档与动态数据硬伤。"
```

### 6.2 多路召回架构

```
多路召回（Multi-Recall）：
  向量检索（语义相似）
  + BM25（关键词精确）
  + 知识图谱（实体关系）
  + API 查询（实时数据）
  → 合并 → Rerank → 精选上下文

⚠️ 面试必答：
"多路召回 = 不同检索手段互补——
 向量找语义、BM25 找关键词、
 图谱找关系、API 找实时；
 合并后 Rerank 保证质量。"
```

---

> 🎯 **核心要点**：RAG 体系 = **原理**（检索增强 + 两阶段）+ **ETL**（解析/分块 800+100/元数据）+ **向量库**（PgVector 起步/Milvus 规模/Redis 延迟）+ **检索调优**（TopK/阈值/过滤 + 混合 + Rerank）+ **升级**（Agentic RAG 多路召回）。"检索质量决定回答质量、文档质量决定检索质量"是 RAG 的第一原理。

---

**返回总览**：[00-JavaAI技术栈总览与全景架构](00-JavaAI技术栈总览与全景架构.md) | **上一篇**：[02-Prompt工程](02-Prompt工程.md) | **下一篇**：[04-ToolCalling与MCP](04-ToolCalling与MCP.md)
