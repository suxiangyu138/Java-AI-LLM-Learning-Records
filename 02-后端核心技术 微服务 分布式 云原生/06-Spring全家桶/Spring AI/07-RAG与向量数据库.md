# 07-RAG 与向量数据库
> 检索增强生成在 Spring AI 的落地：RAG 四步、双检索 Advisor、20+ 向量库抽象、重排与 Agentic RAG

## 📚 目录
1. [RAG 四步流程](#1-rag-四步流程)
2. [向量数据库抽象](#2-向量数据库抽象)
3. [简单 RAG：QuestionAnswerAdvisor](#3-简单-ragquestionansweradvisor)
4. [模块化 RAG：RetrievalAugmentationAdvisor](#4-模块化-ragretrievalaugmentationadvisor)
5. [检索质量：重排、混合检索与元数据过滤](#5-检索质量重排混合检索与元数据过滤)
6. [Spring AI Alibaba：百炼知识库集成](#6-spring-ai-alibaba百炼知识库集成)
7. [传统 RAG vs Agentic RAG](#7-传统-rag-vs-agentic-rag)
8. [核心要点](#8-核心要点)
9. [参考来源](#9-参考来源)

## 1. RAG 四步流程

```text
① 索引（Indexing）   文档 → 分块 → 向量化 → 写入向量库（ETL，见 08）
② 检索（Retrieval）  用户问题 → 向量化 → 相似度检索 → Top-K 文档
③ 增强（Augment）   检索结果作为上下文拼入 prompt
④ 生成（Generate）   LLM 基于上下文 + 问题生成回答
```

| 步骤 | Spring AI 组件 |
|------|----------------|
| 索引 | Document ETL（Reader/Transformer/Writer）+ `EmbeddingModel` |
| 检索 | `DocumentRetriever`（向量库检索器 / 云端知识库检索器） |
| 增强 | `QuestionAnswerAdvisor` 或 `RetrievalAugmentationAdvisor` |
| 生成 | ChatClient 调用（Advisor 已在链上） |

> 🎯 **核心要点**：RAG 解决的是 LLM 的"静态知识 + 幻觉"问题——用检索把**权威上下文**注入生成。Spring AI 中 RAG ≈ "一个 Advisor + 一个向量库"，接入成本极低。

## 2. 向量数据库抽象

### 2.1 统一接口

```text
VectorStore
 ├── add(List<Document>)          写入（内部调用 EmbeddingModel 向量化）
 ├── delete(List<String>)         删除（按文档 ID）
 └── similaritySearch(SearchRequest)  相似检索（topK / filter / threshold）
```

### 2.2 20+ 实现与选型

| 向量库 | Starter | 场景 |
|--------|---------|------|
| Milvus | `spring-ai-starter-vector-store-milvus` | 大规模生产 RAG |
| PGVector | `spring-ai-starter-vector-store-pgvector` | 复用 PostgreSQL 事务体系 |
| Redis | `spring-ai-starter-vector-store-redis` | 缓存 + 向量一体 |
| Qdrant | `spring-ai-starter-vector-store-qdrant` | 独立生产服务 |
| Chroma | `spring-ai-starter-vector-store-chroma` | 原型/轻量 |
| Elasticsearch | `spring-ai-starter-vector-store-elasticsearch` | 文本+向量一体检索 |
| Weaviate / Neo4j / Cassandra / Azure Cosmos ... | 对应 starter | 企业存量 |

### 2.3 接入示例（PGVector）

```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        index-type: HNSW
        distance-type: COSINE_DISTANCE
    pgvector:
      properties:
        host: localhost
        port: 5432
        database: ragdb
        username: postgres
        password: postgres
```

```java
@Autowired VectorStore vectorStore;   // 自动装配

// 写入
vectorStore.add(List.of(new Document("Spring AI 是 Spring 官方 AI 框架...")));

// 检索
List<Document> docs = vectorStore.similaritySearch(
        SearchRequest.builder().query("Spring AI 是什么").topK(5).build());
```

## 3. 简单 RAG：QuestionAnswerAdvisor

```java
@Bean
QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
    return new QuestionAnswerAdvisor(vectorStore);
}

@Bean
ChatClient ragChatClient(ChatClient.Builder builder, QuestionAnswerAdvisor advisor) {
    return builder.defaultAdvisors(advisor).build();    // 挂到默认 Advisor 链
}

// 使用：普通对话即自动带 RAG
String answer = ragChatClient.prompt().user("Spring AI 支持哪些模型？").call().content();
```

| 特性 | 说明 |
|------|------|
| 自动检索 | 每次对话前自动从向量库检索 Top-K 拼入上下文 |
| 开箱即用 | 一个 Bean + 挂 Advisor 即可 |
| 局限 | 检索策略固定（无自定义查询改写、重排） |

> 💡 适用：快速验证 RAG 效果、内部知识问答 MVP。生产级检索策略用 `RetrievalAugmentationAdvisor`。

## 4. 模块化 RAG：RetrievalAugmentationAdvisor

```java
RetrievalAugmentationAdvisor advisor = RetrievalAugmentationAdvisor.builder()
        .documentRetriever(retriever)          // 检索器（可自定义）
        .queryAugmenter(queryAugmenter)        // 查询增强（改写/扩展）
        .contextAugmenter(contextAugmenter)    // 上下文增强（重排/过滤）
        .build();
```

| 扩展点 | 说明 | 自定义示例 |
|--------|------|-----------|
| `DocumentRetriever` | 检索来源 | 向量库、知识库 API、混合检索 |
| `QueryAugmenter` | 查询改写 | 多语言翻译、同义扩展 |
| `ContextAugmenter` | 上下文处理 | 重排、去重、按元数据过滤 |

## 5. 检索质量：重排、混合检索与元数据过滤

### 5.1 检索质量三层

| 层 | 手段 | 说明 |
|----|------|------|
| 粗筛 | 向量相似度 Top-K（默认） | 召回阶段 |
| 精排 | **Reranker 重排**（cross-encoder 类） | 用模型对候选重新打分 |
| 兜底 | 元数据过滤 + 相似度阈值 | `SearchRequest.filter` + `similarityThreshold` |

### 5.2 混合检索

| 模式 | 机制 | 适用 |
|------|------|------|
| 纯向量 | embedding 相似度 | 语义检索 |
| 混合（Hybrid） | 向量 + 关键词（BM25）融合 | 术语精确匹配 + 语义泛化并重 |
| 稀疏+稠密 | Milvus 3.x 原生支持 | 专业领域文档 |

### 5.3 元数据过滤（SQL 风格）

```java
List<Document> docs = vectorStore.similaritySearch(
        SearchRequest.builder()
                .query("退款政策")
                .topK(5)
                .similarityThreshold(0.7)
                .filterExpression("docType == 'policy' && region in ['CN', 'HK']")
                .build());
```

## 6. Spring AI Alibaba：百炼知识库集成

| 维度 | 说明 |
|------|------|
| 项目 | spring-ai-alibaba（阿里官方） |
| 能力 | 接入阿里云百炼（DashScope）知识库与模型 |
| 组件 | `DashScopeDocumentRetriever`：`IndexName` 指定知识库、`RerankMinScore` 重排阈值 |
| 场景 | 已有百炼知识库的国内项目，免自建向量库 |

> 🟢 按需：云上知识库已托管 ETL 与向量存储，适合快速交付；数据敏感/定制需求仍建议自建向量库（Milvus/PGVector）。

## 7. 传统 RAG vs Agentic RAG

| 维度 | 传统 RAG | Agentic RAG |
|------|----------|--------------|
| 检索来源 | 单一向量库 | 多 Agent + 多 MCP 工具（网页/API/多库） |
| 查询质量 | 依赖用户原问题 | 查询扩展（一个问 → 多个角度） |
| 结果判定 | 拿什么用什么 | Agent 评估结果，不足则再检索循环 |
| 落地成本 | 低（一个 Advisor） | 高（Agent 编排 + 护栏） |

```text
Agentic RAG 循环：问题 → 扩展 → 多路检索 → 评估 → 不足则再检索 → 生成
```

> 🎯 **核心要点**：先传统 RAG 跑通再演进 Agentic——绝大多数业务场景传统 RAG + 重排已够用；Agentic RAG 只在"查询复杂、来源多、要求高召回"时引入。

## 8. 核心要点

> 🎯 **核心要点**：
> - RAG = 索引（ETL）+ 检索（VectorStore）+ 增强（Advisor）+ 生成（ChatClient）；
> - 快速验证用 `QuestionAnswerAdvisor`，生产检索用 `RetrievalAugmentationAdvisor` 三扩展点；
> - 检索质量三件套：Top-K 粗筛 → 重排精排 → 元数据过滤/阈值兜底；
> - 20+ 向量库统一接口，按规模与存量选型（Milvus/PGVector/Redis 三主流）；
> - Agentic RAG 是演进方向，先跑通传统 RAG。

## 9. 参考来源

- [Spring AI Reference：RAG](https://docs.spring.io/spring-ai/reference/api/rag.html)
- [Spring AI Reference：Vector Databases](https://docs.spring.io/spring-ai/reference/api/vectordbs.html)
- [Spring AI Alibaba（GitHub）](https://github.com/alibaba/spring-ai-alibaba)
- [Spring AI Getting Started（RAG 起步）](https://docs.spring.io/spring-ai/reference/getting-started.html)

---

**下一模块**：[08-文档ETL管道](08-文档ETL管道.md)　/　**返回总览**：[00-总览](00-Spring%20AI总览.md)
