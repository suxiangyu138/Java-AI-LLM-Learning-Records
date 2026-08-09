# 生态集成与 RAG 实战

> Spring AI 官方 Java 集成、LangChain/LangChain4j、Chroma Sync 自动摄取、多模态检索——把 Chroma 接进真实 RAG 应用的完整链路

## 1. Java 集成：Spring AI 官方 starter

Chroma 是 Spring AI `VectorStore` 官方支持的向量库之一，Java 接入不需要第三方客户端，**官方 starter 一条依赖**：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-chroma</artifactId>
</dependency>
```

核心类两个：`ChromaApi`（对标 Chroma REST API 的轻量客户端，支持 tenant/database/collection CRUD、upsert/get/query/count/delete，认证支持 `withKeyToken()` 与 `withBasicAuthCredentials()`）与 `ChromaVectorStore`（实现 Spring AI 统一 `VectorStore` 接口）。最小配置：

```yaml
spring:
  ai:
    vectorstore:
      chroma:
        client:
          host: http://localhost
          port: 8000
        collection-name: knowledge_base
        initialize-schema: true      # 自动建集合
```

```java
@Bean
public VectorStore vectorStore(ChromaApi chromaApi, EmbeddingModel embeddingModel) {
    return ChromaVectorStore.builder(chromaApi, embeddingModel)
            .collectionName("knowledge_base")
            .initializeSchema(true)
            .build();
}
```

业务代码只面向 `VectorStore` 接口：`add(List<Document>)` 入库、`delete(...)` 清理、`similaritySearch(SearchRequest)` 检索（相似度分数经 `doc.getScore()` 获取）。**换向量库零成本**——切 Milvus/Qdrant 只换依赖和配置，业务代码不动，这是 Spring AI 统一抽象的卖点。手写 Bean 时记得排除自动配置类 `ChromaVectorStoreAutoConfiguration`，避免重复初始化。

## 2. LangChain / LangChain4j / LlamaIndex

- **LangChain（Python）**：`chroma` 是 LangChain 内置 VectorStore 实现（`langchain_chroma.Chroma`），用法与 Spring AI 同构——embedding 模型 + 向量库双注入。
- **LangChain4j（Java）**：`langchain4j-chroma` 模块提供 `ChromaEmbeddingStore`，配合 `OpenAiEmbeddingModel` 使用；注意 LangChain4j 社区维护的 Chroma 适配器在 1.x API 迁移时有过破坏性变更，**升级 chromadb 版本前先确认 LangChain4j 版本兼容矩阵**。
- **LlamaIndex**：`chroma` 集成成熟，VectorStoreIndex 一行切换。

无论哪个框架，三条集成铁律一致：embedding 模型必须与建库时一致（写读同源）；collection 名与命名空间（tenant/database）配置一致；检索返回的 distance 要按 space 语义转换（l2 越小越近、cosine 越接近 1 越近）再展示给用户。LangChain4j 的接入形态：

```java
EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
        .apiKey("...").build();

ChromaEmbeddingStore store = ChromaEmbeddingStore.builder()
        .baseUrl("http://localhost:8000")
        .collectionName("knowledge_base")
        .build();

EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
        .documentSplitter(new DocumentByParagraphSplitter(500, 50))
        .embeddingModel(embeddingModel)
        .embeddingStore(store)
        .build();
ingestor.ingest(textSegmenter.split(document));   // 切分 → 向量化 → 入库

EmbeddingSearchResult<TextSegment> result = store.search(
        EmbeddingSearchRequest.builder()
                .queryEmbedding(embeddingModel.embed("向量数据库怎么选型").content())
                .maxResults(5)
                .minScore(0.6)
                .build());
```

与 Spring AI 相比，LangChain4j 是更细粒度的控制（`minScore` 阈值、分块器注入）——Java 生态两条路都通：Spring AI 走统一 VectorStore 抽象（换库零成本），LangChain4j 走组件级 API（调参更直接）。

## 3. Chroma Sync：S3 / GitHub / Web 自动摄取

1.5.x 新增的 Sync 功能把"数据入库"从手动代码变成声明式任务：配置数据源（S3 桶、GitHub 仓库、Web 页面）与同步策略，Chroma 自动拉取、切分、embedding、入库，并持续增量同步。适用场景是文档库随外部源更新的 RAG 应用（如产品文档同步、GitHub 知识库镜像）。注意它是"摄取管道"而非 ETL 全功能——复杂清洗（去重、脱敏、格式转换）仍需要前置管道，Sync 负责"同步 + 入库"这一段。典型声明式配置示意：

```yaml
sync:
  sources:
    - type: s3
      bucket: docs-bucket
      prefix: product/          # 只同步指定前缀
      interval: 3600            # 每小时增量同步
    - type: github
      repo: my-org/tech-wiki
      branch: main
      interval: 86400
  target:
    collection: knowledge_base  # 目标集合，自动建
    embedding: default          # 复用集合的 embedding 配置
```

落库前的处理策略（切分大小、去重键、元数据映射）在同一份配置里声明——生产实践是"Sync 管同步频率，应用管内容质量"，不要把两者耦合在代码里。

Sync 的适用边界要认清：它服务"源会持续更新"的知识库（文档站、GitHub wiki），不适合一次性导入（直接代码 upsert 更简单）与强清洗场景（数据质量要求高时前置管道不可省）。启用前先回答"源变更多久同步一次才够"——同步频率定高了是资源浪费，定低了用户问不到新内容，按业务容忍度取值。首次启用建议先跑全量同步验证数据质量（条数、元数据、抽样 query），再开增量周期——全量验证通过前不碰增量，是摄取管道落地的基本纪律。

## 4. 多模态与检索增强模型

- **图像/多模态检索**：官方提供 OpenCLIP 示例——文本到图像、图像到图像的检索链路（OpenCLIP 生成图像 embedding，Chroma 存储与检索，多模态应用如"以图搜图"、"图文互搜"）。元数据存图片路径/URI，向量存 CLIP embedding。核心思路：**文本与图像进同一个向量空间**，因此检索时用文本 embedding 也能命中图像：

```python
# 入库：图像 → OpenCLIP 向量 + 路径元数据
image_emb = openclip.encode_image(image)      # 与文本同空间
collection.upsert(ids=[img_id], embeddings=[image_emb],
                  metadatas=[{"uri": "/img/a.jpg", "type": "image"}])

# 检索：文本问 → 文本向量 → 命中图像（图文互搜）
text_emb = openclip.encode_text("夕阳下的湖泊")
hits = collection.query(query_embeddings=[text_emb], n_results=5)
# hits 的 metadatas 里带 uri，应用层渲染图片
```

多模态的工程注意点：CLIP 向量维度与文本模型不同，**多模态集合与纯文本集合分开建**（维度约束，见 02）；图片元数据里存 URI 而非二进制，Chroma 只负责"向量 + 指针"。
- **Context-1（2026）**：Chroma 发布的 200B 开源 agentic search 模型（Apache 2.0），专攻**多轮检索**场景（相比单轮相似度搜索，能结合对话历史理解检索意图）——定位是"检索模型"，与向量库配套而非替代。实验性能力，生产落地前先跑评测。

## 5. 完整 RAG 链路实战

端到端的最小生产链路（Java 视角）：

```text
文档源（PDF/MD/DB）→ 切分（语义分块）→ Embedding 模型 → Chroma VectorStore
                                                              ↓
用户问题 → Embedding 模型 → similaritySearch(SearchRequest, topK) → 上下文
                                                              ↓
                                                      LLM 生成回答（引用溯源）
```

五个关键决策点（对应本体系各篇）：**切分策略**决定召回粒度（分块过大噪声多、过小上下文碎片化，配合 `02-RAG检索增强生成` 体系）；**embedding 模型**选择决定语义质量且影响集合维度（写读必须一致）；**topK 与过滤**决定上下文质量（`where` 过滤 + `n_results` 钳制，防 post-filter 吞结果）；**混合检索**（稠密+稀疏）补关键词精确召回；**评估闭环**用 Forking 对比模型/参数迭代（见 06）。

检索端到端的 Java 实现（Spring AI）：

```java
@Autowired VectorStore vectorStore;

List<Document> retrieve(String question) {
    return vectorStore.similaritySearch(
        SearchRequest.builder()
            .query(question)
            .topK(5)
            .similarityThreshold(0.6)     // 低于阈值的噪声结果直接丢弃
            .filterExpression("category == 'docs'")  // 元数据过滤（见 06 where 语义）
            .build());
}
// 拿到的上下文拼进 Prompt 交 LLM 生成，答案附带 source 溯源
```

注意 `similarityThreshold` 与 Chroma 的 distance 是两套口径——Spring AI 内部按 space 语义换算，配置前确认 collection 的 space（cosine 阈值在 0-1 区间才有意义）。

评估闭环落在可量化的指标上，别停留在"看起来不错"：**命中率**（标注好的问答对里，正确答案是否出现在 top-5 结果中——召回质量的底线指标）与 **MRR**（正确答案的排名倒数均值——衡量排序质量）。评估流程三步：准备 100-200 条标注问答对 → 用同一批 query_texts 跑检索，统计命中率与 MRR → 用 Forking 建新集合对比（换 embedding 模型 / 调 HNSW 参数 / 改切分策略），指标提升才上线。**指标低于预期时先查数据问题**（embedding 写读不一致、脏数据、切分粒度），再动模型与参数——顺序反了会在错误方向调很久。

> 🎯 **核心要点**：Java 接 Chroma = Spring AI 一条 starter + VectorStore 接口，换库零改动；集成铁律"embedding 写读同源、命名空间一致、distance 按 space 语义转换"；Sync 管同步入库、Context-1 管多轮检索意图、RAG 链路五决策点把前面各篇串成一条线。

---

**参考来源**：

- [Spring AI Chroma 集成 API 文档](https://docs.spring.io/spring-ai/docs/2.0.x-SNAPSHOT/api/org/springframework/ai/chroma/vectorstore/package-summary.html)
- [Spring AI Chroma 自动配置](https://docs.spring.io/spring-ai/docs/2.0.x-SNAPSHOT/api/org/springframework/ai/vectorstore/chroma/autoconfigure/package-summary.html)
- [Spring AI + SpringBoot + Chroma 智能客服实战](https://blog.csdn.net/zxchenpeng/article/details/159794440)
- [Spring AI 向量库选择与集成](https://blog.csdn.net/qq_20236937/article/details/162204513)
- [Chroma Cookbook](https://cookbook.chromadb.dev/)

---

**下一模块**：[09-选型对比与决策](./09-选型对比与决策.md) / **返回总览**：[00-Chroma知识体系总览](./00-Chroma知识体系总览.md)
