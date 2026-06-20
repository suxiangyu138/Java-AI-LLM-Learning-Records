# 05 — RAG 检索增强生成

> **目标**：掌握 RAG 的完整技术栈——从文档处理到高级检索策略。RAG 是当前企业 AI 应用落地的最主流模式。

---

## 1. RAG 全景架构

### 1.1 为什么需要 RAG

```
LLM 的三大缺陷：
  ❌ 幻觉 (Hallucination)     → 生成不实信息
  ❌ 知识截止                    → 不知道训练后的新知识
  ❌ 无法访问私有数据              → 不知道企业内部文档

RAG 的解决方案：
  ✅ 用外部知识库提供 Ground Truth → 减少幻觉
  ✅ 实时更新知识库                → 突破时间限制
  ✅ 接入企业私有文档              → 打通企业数据
```

### 1.2 完整 RAG 流水线

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Offline (索引构建)                                │
│                                                                       │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────────┐      │
│  │ 文档加载  │ → │ 文本分割  │ → │ 向量化    │ → │ 向量数据库存储 │      │
│  │ (Loader) │   │(Splitter)│   │(Embedding)│   │ (VectorStore) │      │
│  └──────────┘   └──────────┘   └──────────┘   └──────────────┘      │
│                                                                       │
├─────────────────────────────────────────────────────────────────────┤
│                     Online (检索 & 生成)                              │
│                                                                       │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────────┐      │
│  │ 用户提问  │ → │ Query改写 │ → │ 向量检索  │ → │ 结果重排序   │      │
│  └──────────┘   └──────────┘   └──────────┘   └──────┬───────┘      │
│                                                       │               │
│              ┌────────────────────────────────────────┘               │
│              ▼                                                       │
│    ┌──────────────────────────────────────────────┐                 │
│    │  Prompt 拼接: {检索文档} + {用户问题}          │                 │
│    └───────────────────┬──────────────────────────┘                 │
│                        ▼                                              │
│    ┌──────────────────────────────────────────────┐                 │
│    │  LLM 生成：带来源引用的回答                    │                 │
│    └──────────────────────────────────────────────┘                 │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. 文档处理 (Ingestion)

### 2.1 文档加载器

```java
// ===== Spring AI 文档加载器 =====

// 1. PDF 加载
@Component
public class PdfDocumentLoader {
    public List<Document> load(Resource pdfResource) {
        // 使用 Apache PDFBox
        try (PDDocument pdfDoc = PDDocument.load(pdfResource.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdfDoc);
            return List.of(new Document(text, Map.of("source", pdfResource.getFilename())));
        }
    }
}

// 2. Word 文档加载
public List<Document> loadWord(InputStream inputStream) {
    try (XWPFDocument doc = new XWPFDocument(inputStream)) {
        StringBuilder text = new StringBuilder();
        for (XWPFParagraph paragraph : doc.getParagraphs()) {
            text.append(paragraph.getText()).append("\n");
        }
        return List.of(new Document(text.toString()));
    }
}

// 3. 数据库加载
public List<Document> loadFromDatabase(DataSource dataSource, String sql) {
    List<Document> docs = new ArrayList<>();
    try (Connection conn = dataSource.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        while (rs.next()) {
            String content = rs.getString("content");
            String title = rs.getString("title");
            docs.add(new Document(content, Map.of("title", title)));
        }
    }
    return docs;
}

// 4. Markdown/Text 加载
// Spring AI 内置了 TextReader、JsonReader 等
@Component
public class TextDocumentReader {
    public List<Document> read(Resource resource) {
        TextReader reader = new TextReader(resource);
        reader.setCharset(StandardCharsets.UTF_8);
        reader.getCustomMetadata().put("filename", resource.getFilename());
        return reader.read();
    }
}
```

### 2.2 文档解析企业级方案

| 工具 | 适用场景 | 特点 |
|------|---------|------|
| **Apache Tika** | 通用格式提取 | 支持 1000+ 格式，Java 原生 |
| **Apache PDFBox** | PDF 文本提取 | 纯文本提取，简单 |
| **Apache POI** | Word/Excel | Office 文档处理 |
| **Unstructured** | 智能文档解析 | Python 库，保留表格/层级结构 |
| **LlamaParse** | 复杂 PDF | 云端服务，表格/图表识别强 |
| **MinerU (PDF-Extract-Kit)** | PDF 转 Markdown | 开源，保留格式好 |

### 2.3 文档解析注意事项

```
企业文档解析常见坑：
├── 表格：PDF 中的表格可能变乱 → 用 LlamaParse / Unstructured
├── 图片中的文字：扫描件 → 需要 OCR (Tesseract / PaddleOCR)
├── 层级结构：标题/子标题 → 保留为元数据，方便后续检索
├── 页眉页脚：PDF 水印干扰 → 预处理过滤
├── 分栏：多栏 PDF → 需要分区识别
└── 编码问题：GBK/UTF-8 混合 → 统一转 UTF-8
```

---

## 3. 文本分割 (Chunking)

### 3.1 Chunking 策略对比

```
┌──────────────┬──────────────┬──────────────┬──────────────┐
│ 策略          │ 原理          │ 优点          │ 缺点          │
├──────────────┼──────────────┼──────────────┼──────────────┤
│ 固定长度      │ 按 token/字符│ 简单高效      │ 可能切断语义   │
│ (Fixed Size) │ 数截断        │              │ 和句子        │
├──────────────┼──────────────┼──────────────┼──────────────┤
│ 按句子        │ 在句号/换行处 │ 语义完整      │ 长度不均匀    │
│ (Sentence)   │ 分割          │              │              │
├──────────────┼──────────────┼──────────────┼──────────────┤
│ 按段落        │ 在段落边界分割 │ 结构良好      │ 某些段落过长  │
│ (Paragraph)  │              │              │              │
├──────────────┼──────────────┼──────────────┼──────────────┤
│ 递归分割      │ 先按段落→句子 │ 综合最优      │ 实现稍复杂    │
│ (Recursive)  │ →固定长度分割 │              │              │
├──────────────┼──────────────┼──────────────┼──────────────┤
│ 语义分割      │ 用 Embedding  │ 最语义完整    │ 需要额外模型  │
│ (Semantic)   │ 相似度判断断点 │              │ 速度慢        │
├──────────────┼──────────────┼──────────────┼──────────────┤
│ 结构化分割    │ 按 Markdown   │ 保留文档结构  │ 仅限结构化文档 │
│ (Structural) │ 标题层级分割  │              │              │
└──────────────┴──────────────┴──────────────┴──────────────┘
```

### 3.2 Java 实现

```java
@Component
public class SmartTextSplitter {

    private static final int DEFAULT_CHUNK_SIZE = 512;   // tokens
    private static final int DEFAULT_OVERLAP = 50;        // tokens

    /**
     * 递归文本分割：优先在自然断点处（段落 → 句子 → 固定长度）
     */
    public List<String> recursiveSplit(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();

        // 1. 先尝试按段落分割
        String[] paragraphs = text.split("\n\n");

        for (String paragraph : paragraphs) {
            if (estimateTokens(paragraph) <= chunkSize) {
                chunks.add(paragraph);
            } else {
                // 2. 段落太长，按句子分割
                String[] sentences = paragraph.split("(?<=[。！？.!?])\\s*");
                StringBuilder currentChunk = new StringBuilder();

                for (String sentence : sentences) {
                    if (estimateTokens(currentChunk + sentence) > chunkSize
                            && currentChunk.length() > 0) {
                        chunks.add(currentChunk.toString().trim());
                        // 保留 overlap
                        currentChunk = new StringBuilder(
                            getLastNTokens(currentChunk.toString(), overlap)
                        );
                    }
                    currentChunk.append(sentence);
                }
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                }
            }
        }
        return chunks;
    }

    private int estimateTokens(String text) {
        // 粗略估算：中文 1 字 ≈ 0.7 token，英文 1 词 ≈ 1.3 token
        return text.length();
    }

    private String getLastNTokens(String text, int n) {
        // 简化实现
        if (text.length() <= n) return "";
        return text.substring(Math.max(0, text.length() - n));
    }
}
```

### 3.3 关键参数

| 参数 | 建议值 | 说明 |
|------|-------|------|
| **Chunk Size** | 256-1024 tokens | 太小=信息不足，太大=检索精度下降 |
| **Chunk Overlap** | 10-20% 的 chunkSize | 防止关键信息在边界断裂 |
| **中文 Chunk Size** | 512-1024 字 | 中文信息密度高，可以稍大 |

### 3.4 Spring AI TokenTextSplitter

```java
// Spring AI 内置的 TokenTextSplitter
TokenTextSplitter splitter = TokenTextSplitter.builder()
    .withChunkSize(512)
    .withMinChunkSizeChars(100)
    .withMinChunkLengthToEmbed(20)
    .withMaxNumChunks(1000)
    .withKeepSeparator(true)      // 保留分隔符（如换行）
    .build();

List<Document> chunks = splitter.apply(documents);
```

---

## 4. Embedding（向量嵌入）

### 4.1 Embedding 模型选型

| 模型 | 提供方 | 维度 | 最大长度 | 中文能力 | 价格 |
|------|-------|------|---------|---------|------|
| **text-embedding-3-small** | OpenAI | 512/1536 | 8191 | ⭐⭐⭐ | $0.02/1M tokens |
| **text-embedding-3-large** | OpenAI | 256/1024/3072 | 8191 | ⭐⭐⭐ | $0.13/1M tokens |
| **bge-large-zh-v1.5** | 智源(BAAI) | 1024 | 512 | ⭐⭐⭐⭐⭐ | 免费开源 |
| **bge-m3** | 智源(BAAI) | 1024 | 8192 | ⭐⭐⭐⭐⭐ | 免费开源 |
| **text2vec-large-chinese** | shibing624 | 1024 | 512 | ⭐⭐⭐⭐ | 免费开源 |
| **gte-Qwen2-7B-instruct** | 阿里 | 3584 | 32K | ⭐⭐⭐⭐⭐ | 免费开源 |
| **jina-embeddings-v3** | Jina AI | 1024 | 8192 | ⭐⭐⭐⭐ | 免费/有 API |
| **voyage-3** | Anthropic | 1024 | 32K | ⭐⭐⭐ | $0.06/1M |

### 4.2 Java 实现

```java
// Spring AI Embedding（自动适配多种模型）
@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    // 单文本嵌入
    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }

    // 批量嵌入（性能优化）
    public List<float[]> embedBatch(List<String> texts) {
        List<Embedding> embeddings = embeddingModel.embedForResponse(texts)
            .getResults();
        return embeddings.stream()
            .map(Embedding::getOutput)
            .collect(Collectors.toList());
    }

    // 文档嵌入
    public void embedDocuments(List<Document> documents) {
        for (Document doc : documents) {
            float[] embedding = embed(doc.getContent());
            doc.setEmbedding(embedding);
        }
    }
}

// application.yml 配置示例
// spring:
//   ai:
//     openai:
//       api-key: ${OPENAI_API_KEY}
//       embedding:
//         options:
//           model: text-embedding-3-small
//           dimensions: 512           # 可选，减少维度节省存储
```

### 4.3 Embedding 维度选择

```
维度选择权衡：
  1536 维 → 精度最高，但存储大、检索慢
  1024 维 → 大多数场景的甜点位置
  512 维  → 适度降低精度，显著节省存储和加速检索
  256 维  → 快速原型，精度损失明显

建议：
  - 一般场景：1024 维
  - 大规模（亿级+）：512 维
  - 精度优先：1536 或更高
```

---

## 5. 检索策略进阶

### 5.1 基础检索

```java
@Service
public class RagRetrievalService {

    private final VectorStore vectorStore;

    // 基础向量检索
    public List<Document> basicSearch(String query, int topK) {
        return vectorStore.similaritySearch(
            SearchRequest.query(query).withTopK(topK)
        );
    }

    // 带相似度阈值（过滤低相关性结果）
    public List<Document> searchWithThreshold(String query, int topK, double threshold) {
        return vectorStore.similaritySearch(
            SearchRequest.query(query)
                .withTopK(topK)
                .withSimilarityThreshold(threshold)     // 如 0.7
        );
    }

    // 带元数据过滤
    public List<Document> searchWithFilter(String query, String dept, String year) {
        return vectorStore.similaritySearch(
            SearchRequest.query(query)
                .withTopK(5)
                .withFilterExpression(
                    "department == '" + dept + "' AND year == '" + year + "'"
                )
        );
    }
}
```

### 5.2 混合检索 (Hybrid Search)

```java
/**
 * 混合检索 = 向量检索 + 关键词检索（BM25）
 * 向量：擅长语义匹配
 * BM25：擅长精确关键词匹配、专有名词
 * 两者互补，效果最好
 */
@Service
public class HybridSearchService {

    private final VectorStore vectorStore;
    private final ElasticsearchService esService;  // 或其他全文检索引擎

    public List<Document> hybridSearch(String query, int topK) {
        // 1. 向量检索
        List<Document> vectorResults = vectorStore.similaritySearch(
            SearchRequest.query(query).withTopK(topK)
        );

        // 2. BM25 关键词检索
        List<Document> keywordResults = esService.bm25Search(query, topK);

        // 3. RRF (Reciprocal Rank Fusion) 融合
        return rrfMerge(vectorResults, keywordResults, topK);
    }

    /**
     * RRF 融合算法
     * score = Σ 1/(k + rank_i)
     */
    private List<Document> rrfMerge(List<Document> listA,
                                     List<Document> listB, int topK) {
        double k = 60.0;  // RRF 常数
        Map<String, Double> scores = new HashMap<>();
        Map<String, Document> docMap = new LinkedHashMap<>();

        // 计算 RRF 分数
        for (int i = 0; i < listA.size(); i++) {
            String id = listA.get(i).getId();
            scores.merge(id, 1.0 / (k + i + 1), Double::sum);
            docMap.putIfAbsent(id, listA.get(i));
        }
        for (int i = 0; i < listB.size(); i++) {
            String id = listB.get(i).getId();
            scores.merge(id, 1.0 / (k + i + 1), Double::sum);
            docMap.putIfAbsent(id, listB.get(i));
        }

        // 按 RRF 分数排序取 Top-K
        return scores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(topK)
            .map(e -> docMap.get(e.getKey()))
            .collect(Collectors.toList());
    }
}
```

### 5.3 Query 改写

```java
/**
 * Query Rewriting: 在检索前优化用户问题
 */
@Service
public class QueryRewriter {

    private final ChatClient chatClient;

    // 方法 1：HyDE (Hypothetical Document Embeddings)
    // 先让 LLM 生成"假设的理想答案"，用这个答案做检索
    public String hydeRewrite(String userQuery) {
        return chatClient.prompt()
            .system("""
                你是一个文档写作助手。根据用户问题，写一段假设的答案。
                不需要真实准确，只要风格和结构与可能存在的文档一致即可。
                直接输出答案内容，不要任何前缀。
                """)
            .user(userQuery)
            .call()
            .content();
    }

    // 方法 2：多视角生成
    // 从不同角度重写查询，扩大检索覆盖面
    public List<String> multiQueryRewrite(String userQuery) {
        String result = chatClient.prompt()
            .system("""
                将用户问题改写为 3 个不同角度的检索查询，每个查询聚焦不同方面。
                以 JSON 数组返回：["查询1", "查询2", "查询3"]
                """)
            .user(userQuery)
            .call()
            .content();
        return parseJsonArray(result);
    }

    // 方法 3：查询分解（复杂问题拆成子问题）
    public List<String> decomposeQuery(String complexQuery) {
        String result = chatClient.prompt()
            .system("""
                将复杂问题分解为 2-4 个简单的子问题。
                以 JSON 数组返回。
                """)
            .user(complexQuery)
            .call()
            .content();
        return parseJsonArray(result);
    }
}
```

---

## 6. Re-ranking（重排序）

### 6.1 为什么需要 Re-ranking

```
初检（向量/BM25）：速度快，精度一般 → 召回 Top-20~50
重排序（Cross-Encoder）：速度慢，精度高 → 精排 Top-3~5

效果提升：
  向量检索 → 60-70% 准确率
  向量检索 + Re-ranking → 85-95% 准确率
```

### 6.2 Re-ranking 实现

```java
@Service
public class ReRankingService {

    // 方案 1：使用 Cohere / Jina Reranker API
    // 方案 2：本地部署 BGE-Reranker / bge-reranker-v2-m3
    // 方案 3：用 LLM 做重排序

    /**
     * LLM-based Re-ranking
     */
    public List<Document> llmReRank(String query, List<Document> candidates, int topK) {
        String prompt = buildReRankPrompt(query, candidates);

        String result = chatClient.prompt()
            .system("""
                你是一个文档相关性判断专家。
                根据用户问题，对每篇文档打分（0-10），按相关性排序。
                返回 JSON 数组：[{"id": "1", "score": 9, "reason": "..."}]
                只返回前 %d 篇最相关的。
                """.formatted(topK))
            .user(prompt)
            .call()
            .content();

        return parseReRankResult(result, candidates);
    }

    private String buildReRankPrompt(String query, List<Document> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户问题：").append(query).append("\n\n");
        sb.append("候选文档：\n");
        for (int i = 0; i < candidates.size(); i++) {
            sb.append(String.format("[文档 %d]\n%s\n\n", i + 1,
                candidates.get(i).getContent()));
        }
        return sb.toString();
    }
}
```

### 6.3 Reranker 模型对比

| 模型 | 语言 | 速度 | 效果 | 部署 |
|------|------|------|------|------|
| **bge-reranker-v2-m3** | 多语言（含中文）| 快 | 好 | 本地 GPU |
| **bge-reranker-large** | 中英 | 快 | 好 | 本地 GPU |
| **Cohere Rerank** | 多语言 | 快 | 很好 | API 调用 |
| **Jina Reranker v2** | 多语言 | 快 | 好 | API 调用 |
| **gte-multilingual-reranker** | 多语言 | 中 | 好 | 本地 GPU |

---

## 7. 高级 RAG 策略

### 7.1 Self-RAG

```
Self-RAG 流程：
  1. 用户提问
  2. LLM 判断："我需要检索吗？" → Yes/No
  3. 如果需要 → 检索
  4. LLM 判断："检索结果相关吗？" → 相关/不相关
  5. 如果不相关 → Web 搜索或其他来源（CRAG 思路）
  6. 生成回答，同时自我评估："回答是否得到文档支持？"
```

### 7.2 Parent Document Retriever

```java
/**
 * Parent Document Retriever：
 * 检索时用小 chunk（精度高），返回时用大 chunk（信息完整）
 */
@Service
public class ParentDocumentRetriever {

    private final VectorStore smallChunkStore;   // 存储小 chunk 的向量库
    private final Map<String, Document> parentDocStore;  // 存储完整文档

    public List<Document> retrieve(String query, int topK) {
        // 1. 用小 chunk 检索
        List<Document> smallChunks = smallChunkStore.similaritySearch(
            SearchRequest.query(query).withTopK(topK)
        );

        // 2. 找到对应的 Parent Document（去重）
        Set<String> parentIds = smallChunks.stream()
            .map(d -> d.getMetadata().get("parent_id"))
            .filter(Objects::nonNull)
            .map(Object::toString)
            .collect(Collectors.toSet());

        // 3. 返回完整 Parent Document
        return parentIds.stream()
            .map(parentDocStore::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
```

### 7.3 Graph RAG

```
Graph RAG 流程：
  1. 从文档中提取实体（人物/公司/地点/概念...）
  2. 构建知识图谱（Entity + Relationship）
  3. 检索时：
     a. 向量检索找到相关实体
     b. 沿图谱边扩展（1-hop / 2-hop）
     c. 将实体、关系、相关文档一起给 LLM

  优势：能回答需要"跨文档推理"的复杂问题
  劣势：构建成本高，适合知识密集型场景
```

---

## 8. 完整 RAG 服务实现

```java
@Service
public class RagService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final QueryRewriter queryRewriter;
    private final ReRankingService reRanker;

    /**
     * 高级 RAG 问答（含 Query Rewriting + Hybrid Search + Re-ranking）
     */
    public RagResponse ask(String question) {
        // Step 1: Query 改写（HyDE）
        String hydeDoc = queryRewriter.hydeRewrite(question);

        // Step 2: 混合检索（向量 + 关键词融合）
        // 同时用原始问题向量检索，用 HyDE 文档向量检索，然后融合
        List<Document> retrieved = hybridRetrieve(question, hydeDoc, 20);

        // Step 3: Re-ranking（精排）
        List<Document> reranked = reRanker.llmReRank(question, retrieved, 5);

        // Step 4: 组装 Prompt
        String context = buildContext(reranked);
        String systemPrompt = """
            根据以下参考资料回答用户问题。
            如果资料中没有相关信息，请如实说明。
            引用资料时，注明来源。

            参考资料：
            %s
            """.formatted(context);

        // Step 5: LLM 生成
        String answer = chatClient.prompt()
            .system(systemPrompt)
            .user(question)
            .call()
            .content();

        return new RagResponse(answer, reranked);
    }

    private String buildContext(List<Document> docs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            sb.append(String.format("[来源 %d] %s\n文件: %s\n\n",
                i + 1,
                doc.getContent(),
                doc.getMetadata().getOrDefault("source", "未知")));
        }
        return sb.toString();
    }
}
```

---

## 9. RAG 评估体系

### 9.1 评估指标

| 指标 | 全称 | 衡量什么 | 计算方式 |
|------|------|---------|---------|
| **MRR** | Mean Reciprocal Rank | 第一个相关文档的排名 | 1/rank 的平均值 |
| **NDCG** | Normalized Discounted Cumulative Gain | 排序质量（考虑位置权重） | DCG / IDCG |
| **MAP** | Mean Average Precision | 综合检索质量 | AP 的平均值 |
| **Recall@K** | — | Top-K 中相关文档占所有相关文档的比例 | 相关数 / 总相关数 |
| **Precision@K** | — | Top-K 中相关文档的比例 | 相关数 / K |
| **Faithfulness** | — | 回答是否与检索文档一致 | 需要 LLM 或人工评估 |
| **Answer Relevance** | — | 回答是否回答了问题 | 需要 LLM 或人工评估 |

### 9.2 RAGAS 评估框架

```java
/**
 * 集成 RAGAS 评估（简化版）
 * ragas: https://github.com/explodinggradients/ragas
 */
@Service
public class RagasEvaluator {

    // 忠实度评估：回答中每条陈述是否被检索文档支持
    public double evaluateFaithfulness(String answer, List<Document> docs) {
        String result = chatClient.prompt()
            .system("""
                评估以下回答是否忠实于提供的参考资料。
                找出回答中的每一条事实性陈述，判断它是否能从资料中推出。
                返回 JSON: {"supported": N, "total": M, "score": 0.X}
                """)
            .user("回答: %s\n\n资料: %s".formatted(answer, buildContext(docs)))
            .call()
            .content();
        return extractScore(result);
    }
}
```

---

## 10. 快速复习

```
□ RAG 完整流水线：Load → Split → Embed → Store → Retrieve → Re-rank → Generate
□ Chunking 策略：固定/递归/语义 的选择
□ Chunk Size 与 Overlap 的平衡
□ Embedding 模型选型（中文：bge-m3 > text2vec > OpenAI）
□ 混合检索 = 向量 + BM25 + RRF 融合
□ Query Rewriting 三种方法：HyDE / Multi-Query / Decompose
□ Re-ranking 的作用和常见方案
□ Advanced RAG：Self-RAG / Parent Document Retriever / Graph RAG
□ RAG 评估指标：MRR / NDCG / Faithfulness / Answer Relevance
□ 企业 RAG 性能：检索 < 200ms，端到端 < 3s
```

---

> **下一步**：[06 — Agent 智能体](./06-Agent智能体.md)
