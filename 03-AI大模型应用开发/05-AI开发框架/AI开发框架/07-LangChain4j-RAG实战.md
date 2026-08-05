# 07 - LangChain4j RAG 实战

> 🎯 LangChain4j 的 RAG = DocumentLoader（加载）→ Splitter（分块）→ EmbeddingStore（向量化入库）→ ContentRetriever（检索）→ LLM（生成）。30+ 加载器覆盖几乎所有数据源

---

## 目录

1. [RAG 完整 Pipeline 五步](#1-rag-完整-pipeline-五步)
2. [文档加载与分块策略](#2-文档加载与分块策略)
3. [向量化入库与检索](#3-向量化入库与检索)
4. [AiServices + RAG 声明式集成](#4-aiservices--rag-声明式集成)
5. [高级检索：混合搜索与查询优化](#5-高级检索混合搜索与查询优化)
6. [多数据源联合检索](#6-多数据源联合检索)
7. [生产级 RAG 架构](#7-生产级-rag-架构)

---

## 1. RAG 完整 Pipeline 五步

```text
① DocumentLoader：加载 PDF/Word/网页/GitHub/数据库...
② DocumentSplitter：递归分块（500字符 + 50重叠）
③ EmbeddingModel：文本 → 向量（1536维或768维）
④ EmbeddingStore：向量持久化存储（Chroma/Milvus/Pinecone/Redis）
⑤ ContentRetriever：相似度检索 TopK → 注入 LLM Prompt → 生成
```

---

## 2. 文档加载与分块策略

```java
// ① 多格式加载（LangChain4j 支持 30+ 数据源）
Document pdfDoc = FileSystemDocumentLoader.loadDocument(
    Path.of("handbook.pdf"), new ApacheTikaDocumentParser()     // PDF/Word/Excel/PPT
);
Document webDoc = UrlDocumentLoader.load(
    URL.of("https://docs.example.com"), new HtmlDocumentParser()
);
List<Document> githubDocs = GitHubDocumentLoader.loader()
    .owner("my-org").repo("docs").branch("main").load();

// ② 分块策略对比
//   递归分块（推荐）— 按段落/换行递归切分，语义完整性最好
DocumentSplitter recursive = DocumentSplitters.recursive(
    500,    // 最大字符数
    50      // 重叠字符数（保证跨块连续语义）
);
//   按句子分块 — NLP 场景
DocumentSplitter bySentence = DocumentSplitters.bySentence(500, 50);
//   按段落分块 — 结构化文档
DocumentSplitter byParagraph = DocumentSplitters.byParagraph(500, 50);

List<TextSegment> chunks = recursive.split(List.of(pdfDoc, webDoc));

// ③ 元数据增强（检索时可按 source/date/author 过滤）
chunks.forEach(chunk -> {
    chunk.metadata().put("source", pdfDoc.metadata().get("file_name"));
    chunk.metadata().put("chunk_index", chunk.metadata().get("index"));
});
```

**分块参数经验值：**

| 场景 | 块大小 | 重叠 | 理由 |
|------|--------|:---:|------|
| 通用 FAQ/手册 | 500 字符 | 50 | 平衡精度与召回 |
| 代码/技术文档 | 800-1000 字符 | 100 | 函数/类通常较长 |
| 法律/合同 | 300 字符 | 50 | 精确段落匹配 |

---

## 3. 向量化入库与检索

```java
// ① Embedding 模型
EmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
    .baseUrl("http://localhost:11434")
    .modelName("bge-m3").build();          // BGE-M3：1024维，中英文均优秀

// ② 向量存储（开发用内存、生产用持久化）
// 开发：
EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();  // 重启丢失！
// 生产：
EmbeddingStore<TextSegment> store = ChromaEmbeddingStore.builder()
    .baseUrl("http://chroma:8000").collectionName("company-docs").build();

// ③ 批量向量化入库
List<Embedding> embeddings = embeddingModel.embedAll(
    chunks.stream().map(TextSegment::text).toList()
).content();
store.addAll(embeddings, chunks);

// ④ 检索器
EmbeddingStoreRetriever retriever = EmbeddingStoreRetriever.builder()
    .embeddingStore(store)
    .embeddingModel(embeddingModel)
    .maxResults(5)
    .minScore(0.7)                          // 低于 0.7 的不返回
    .filter(Filter.contains("source", "handbook.pdf"))  // 元数据过滤
    .build();

List<TextSegment> relevant = retriever.findRelevant("年假怎么算？");
```

---

## 4. AiServices + RAG 声明式集成

```java
// ① 定义 RAG 助手接口
interface RagAssistant {
    @SystemMessage("""
        你是企业知识库助手。请基于提供的参考文档回答。
        回答格式：
        [答案]
        📎 来源：{文档名称}
        """)
    String answer(@MemoryId String sessionId, @UserMessage String question);
}

// ② ContentRetriever 注入 → AiServices 自动在每次调用时检索并注入上下文
ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
    .embeddingStore(store)
    .embeddingModel(embeddingModel)
    .maxResults(5)
    .minScore(0.7)
    .build();

RagAssistant rag = AiServices.builder(RagAssistant.class)
    .chatLanguageModel(model)
    .contentRetriever(contentRetriever)          // ★ 注入检索器
    .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
    .build();

// 每次调用 answer() 时，AiServices 内部自动执行：
// ① 向量化用户问题 ② 检索 TopK 片段 ③ 拼接上下文 ④ 注入 Memory ⑤ 调 LLM
String answer = rag.answer("user1", "请假要提前几天申请？");
```

---

## 5. 高级检索：混合搜索与查询优化

### 5.1 查询改写（HyDE）

```java
// HyDE：先生成假设性答案 → 用假设答案做向量检索（提升召回率）
String hypothesisAnswer = model.generate(
    "请对以下问题给出一个简短的假设性回答：" + userQuery
);
List<TextSegment> results = retriever.findRelevant(hypothesisAnswer);
```

### 5.2 Dense + Sparse 混合检索 + RRF 融合

```java
// Dense 检索（语义向量）
List<TextSegment> denseResults = retriever.findRelevant(userQuery);

// Sparse 检索（BM25 关键词）
List<TextSegment> sparseResults = bm25Index.search(userQuery, 10);

// RRF（Reciprocal Rank Fusion）融合
List<TextSegment> merged = rrfFusion(denseResults, sparseResults, k=60);
// 公式：RRF(d) = Σ 1/(k + rank_i(d))，k 默认 60
```

### 5.3 检索后重排序（Re-ranking）

```java
// 粗排 Top20 → 精排 Top3
List<TextSegment> candidates = retriever.findRelevant(userQuery, 20);
// 用 Cross-Encoder 模型精排
List<TextSegment> reranked = crossEncoder.rerank(userQuery, candidates, 3);
```

---

## 6. 多数据源联合检索

```java
// 场景：内部文档 + 维基百科 + 代码库 联合检索

ContentRetriever wikiRetriever = ...;   // 维基百科
ContentRetriever docRetriever = ...;    // 公司内部文档
ContentRetriever codeRetriever = ...;   // GitHub 代码

ContentRetriever union = new UnionContentRetriever(
    wikiRetriever, docRetriever, codeRetriever
);

// AiServices 注入多个检索器（自动合并结果）
Assistant agent = AiServices.builder(Assistant.class)
    .chatLanguageModel(model)
    .contentRetrievers(wikiRetriever, docRetriever, codeRetriever)
    .build();
```

---

## 7. 生产级 RAG 架构

```text
文档上传 → TikaParser（PDF/Word/PPT 全格式）
         ↓
      递归分块（500字符 + 50重叠 + metadata标记）
         ↓
      EmbeddingModel 批量向量化 → ← EmbeddingStore 持久化（Milvus/Chroma/Pinecone）
         ↓
      用户提问 → HyDE 查询改写 → 混合检索(Dense + BM25) → RRF融合 → CrossEncoder重排序
         ↓
      TopK 上下文 + System Prompt + Memory → LLM 生成
         ↓
      返回答案 + 引用来源
```

| 组件 | 开发环境 | 生产环境 |
|------|----------|----------|
| EmbeddingStore | InMemoryEmbeddingStore | Milvus / Pinecone / pgvector / Chroma |
| DocumentParser | ApacheTikaDocumentParser（全格式） | 同左 |
| EmbeddingModel | Ollama（bge-m3, 免费本地） | OpenAI text-embedding-3-small / Cohere |
| 分块策略 | recursive(500, 50) | 按文档类型调整（见上表） |
| 检索优化 | 基础向量检索 | HyDE改写 + 混合检索 + RRF + CrossEncoder |

---

> 🎯 **核心要点**：LangChain4j RAG 的标准五步 — **加载→分块→向量化→入库→检索生成**。三个生产级优化必做：① **HyDE 查询改写**（提升召回率）② **混合检索 Dense + BM25 + RRF**（语义+关键词互补）③ **CrossEncoder 重排序**（粗排→精排）。向量存储一旦上线就换掉 InMemory → Milvus/Chroma 持久化。

**下一模块**：[08-LangChain4j Agent与高级特性](08-LangChain4j-Agent与高级特性.md) / **返回总览**：[00-总览](00-Java-AI开发框架体系总览.md)
