# 生态集成与 RAG 实战

> Java 三条路径（官方客户端/LangChain4j/Spring AI）、Python 生态（LangChain/FastEmbed）、完整 RAG 链路与评估闭环

## 1. Java 生态：三条路径

**路径一：官方客户端**（`io.qdrant:client`，gRPC 实现，03 篇已演示）——最底层、最可控，适合性能敏感与自定义查询场景。

**路径二：LangChain4j** `QdrantEmbeddingStore`（RAG 首选，组件化管线）：

```java
QdrantEmbeddingStore store = QdrantEmbeddingStore.builder()。
        .collectionName("rag-collection")。
        .host("localhost").port(6334)。
        .apiKey("...")                       // 自建可省略；Cloud 必填。
        .build();。

EmbeddingModel embeddingModel = AllMiniLmL6V2EmbeddingModel.builder().build();。

// 入库：切分 → 向量化 → 存储。
EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()。
        .documentSplitter(new DocumentByParagraphSplitter(500, 50))。
        .embeddingModel(embeddingModel)。
        .embeddingStore(store)。
        .build();。
ingestor.ingest(textSegmenter.split(Document.from("...")));。

// 检索：内容检索器接入 AI 助手。
EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()。
        .queryEmbedding(embeddingModel.embed("向量数据库怎么选型").content())。
        .maxResults(5).minScore(0.6)。
        .build();。
List<EmbeddingMatch<TextSegment>> matches = store.search(request);。
```

**路径三：Spring AI 官方 starter**（`spring-ai-starter-vector-store-qdrant`）——统一 `VectorStore` 接口（与 Chroma/PgVector 体系的接入同构，换库只换依赖与配置）：

```yaml
spring:。
  ai:。
    vectorstore:。
      qdrant:。
        host: localhost。
        port: 6334。
        collection-name: knowledge_base。
```

```java
@Bean。
public VectorStore vectorStore(QdrantApi qdrantApi, EmbeddingModel embeddingModel) {。
    return QdrantVectorStore.builder(qdrantApi, embeddingModel)。
            .collectionName("knowledge_base")。
            .initializeSchema(true)      // 原型自动建集合；生产关掉用脚本管。
            .build();。
}。

// 业务代码只面向 VectorStore：add / delete / similaritySearch。
List<Document> hits = vectorStore.similaritySearch(。
    SearchRequest.builder()。
        .query(question)。
        .topK(5)。
        .similarityThreshold(0.6)。
        .filterExpression("category == 'docs'")   // 转成 Qdrant filter（过滤感知）。
        .build());。
```

三条路径的选择：**LangChain4j**（RAG 组件最全：切分/检索/助手）、**Spring AI**（Spring 生态深度集成、统一抽象）、**官方客户端**（底层性能与控制）。

生产常见组合：Spring AI 管向量检索 + LangChain4j 管文档处理——各司其职（与 PgVector 体系 08 篇的结论一致）。

换库成本对比：Spring AI 的 `VectorStore` 抽象让 Qdrant ↔ Chroma ↔ PgVector ↔ Milvus 切换只改依赖与配置——**评估与迁移的保险丝，新项目一律从统一抽象起步**。

## 2. Python 生态

- **官方客户端 qdrant-client**：全 API 覆盖（03 篇）。
- **LangChain**：`QdrantVectorStore`（配合 OpenAIEmbeddings 一行接入，`QdrantVectorStore.from_documents`）。
- **LlamaIndex**：`QdrantVectorStore` 支持向量存储 + 过滤 + 混合检索（`query_mode="hybrid"`）。
- **FastEmbed**（Qdrant 自家 embedding 库）：本地小模型快速向量化，配合 qdrant-client 免外部 API——原型与内网场景实用。

FastEmbed 的定位与注意：**ONNX 运行时 + 量化小模型**（如 BGE-small 系列，单条向量化毫秒级），无需 GPU、无网络依赖——内网部署与离线环境的省心选择。

**质量上限低于云端大模型**（如 text-embedding-3-large），检索质量敏感场景先对比再定。

**维度随模型变**（如 384 维小模型），建集合前确认维度——FastEmbed 适合"够用就好"的场景，质量优先时仍用专用 embedding 服务。

Python 侧的混合检索与评估闭环直接复用 06 篇的 prefetch + RRF 代码；LangChain 的 `QdrantVectorStore` 支持 `retrieval_mode` 切换（dense/sparse/hybrid），生产切换前先用标注集对比三种模式的命中率——**框架给的默认值不一定最优，评估是唯一裁判**（与 Chroma/PgVector 体系的评估方法论一致）。

Python 侧三条注意：**embedding 维度与集合配置一致**（跨模型复用集合报错）；**Qdrant 官方的 embedding 管道是"应用侧"职责**（Qdrant 不像 Milvus 提供内置 pipeline，向量化在应用完成）。

**批量 upsert 用 `client.upsert(points=[...], batch_size=64)`**（客户端自动分批，防大请求超时）。

## 3. 完整 RAG 链路实战（Java 视角）

```text
文档源 → 切分（段落/递归）→ Embedding 模型 → Qdrant upsert（payload 带来源/分类）
                                                          ↓
用户问题 → Embedding → query_points（filter + dense + sparse + RRF）
                                                          ↓
                                            上下文（payload 溯源）→ LLM 生成
```

五个关键决策点：**切分粒度**（500-800 字 + 50 重叠是常见起点，配合 `02-RAG检索增强生成` 体系调优）；**payload 设计**（source/title/category/updated_at 是四件套，为过滤与溯源服务）。

**过滤感知利用**（业务过滤直接进 filter——这是 Qdrant 相对 post-filter 派系的优势兑现点）；**混合检索**（dense+sparse+RRF 一次查询，06 篇代码）；**评估闭环**（下节）。

## 4. 评估闭环（对齐向量库体系方法论）

检索质量是可回归的工程资产：**100-200 条标注问答对**（问题 → 期望命中的点 ID）→ 跑检索统计**命中率**（top-5 是否含期望点）与 **MRR**（期望点排名倒数均值）→ 对比基线（纯 dense vs 混合 RRF vs 加 filter）→ 参数变更（ef/量化/候选量/权重）触发重跑。

评估脚本进 CI，每日跑——**"混合检索提升 10-20%"要有自己的数据支撑，而不是听信宣传**（与 Chroma/PgVector 体系的评估方法论完全一致，三套体系可共用一套标注集）。

评估脚本的最小形态（Python，进 CI）：

```python
def evaluate(collection, queries, expected_ids, mode):。
    hits = mrr = 0。
    for q, exp in zip(queries, expected_ids):。
        top = query_top5(collection, q, mode)          # dense / sparse / hybrid。
        if exp in top:。
            hits += 1。
            mrr += 1 / (top.index(exp) + 1)。
    return hits / len(queries), mrr / len(queries)。

for mode in ["dense", "sparse", "hybrid"]:。
    print(mode, evaluate(...))    # 用指标决策，不用感觉决策。
```

评估的工程注意：**标注集覆盖长尾**（专有名词、型号、口语改写各占一部分——否则评估偏向语义路）；**模式对比固定其余变量**（同一批数据、同一 ef——只改检索模式）；**指标进告警**（命中率跌破阈值触发排查——检索质量与基础设施同等重要地进监控）。

## 5. 集成避坑五条

（注：本体系避坑清单的完整版见 10 篇 Top 12——与集成相关的坑在两端重复强调，是因为"客户端配置"与"服务端架构"是两道防线，各自都值得单列。）。

1. **gRPC 端口混淆**：客户端默认 6334，REST 调试用 6333——用错端口连接被拒。
2. **维度不匹配**：upsert 报 wrong number of coordinates——核对模型与集合配置。
3. **未建 payload 索引的过滤**：功能正常但慢且部分操作符不可用（text/geo 必须建索引）——建集合时同步声明 schema。
4. **多租户不用 tenant 索引**：租户过滤场景建 tenant 索引（06 篇），否则基数估算与局域化优势全丢。
5. **Spring AI 集合名与初始化**：`initialize-schema` 类配置（自动建集合）生产环境关掉，用 Flyway/脚本管集合 schema——集合配置也是 schema，版本化管理。

集成排错的分层心智（与 03 篇排错一致）：**连接层**（端口 6333/6334、TLS、api_key）→ **集合层**（命名、维度、度量）→ **数据层**（ID、payload schema）→ **检索层**（filter 语法、score 语义）——每层报错特征不同（连接拒绝/维度报错/过滤无结果/分数反常），按层排查比乱试快一个量级。

排查入口统一从 `GET /collections/{name}` 看配置与 `GET /healthz` 看状态开始——**先看全局状态，再钻问题细节**。

三套向量库体系的集成避坑高度重合（gRPC/REST 端口、维度一致、索引先行、幂等 ID）——**这套"客户端四层排错 + 索引先行 + 模型一致性"的纪律是通用的**，换库时排错经验直接迁移，这也是本体系与 Chroma/PgVector 体系共用"评估/避坑/面试"框架的原因。

> 🎯 **核心要点**：Java 三路径（官方客户端/LangChain4j/Spring AI）按场景选，生产组合"Spring AI 管检索 + LangChain4j 管切分"；Python 侧 FastEmbed 本地向量化是内网利器；RAG 五决策点（切分/payload/过滤感知/混合/评估）串起全链路；评估闭环进 CI，指标说话。

---

**参考来源**：

- [Qdrant Java 客户端与 LangChain4j 集成（开发技能库）](https://www.skills.sh/giuseppe-trisciuoglio/developer-kit/qdrant)
- [LangChain4j 与 Spring AI RAG 对比（CSDN）](https://blog.csdn.net/badao_liumang_qizhi/article/details/160214877)
- [Qdrant 官方文档](https://qdrant.tech/documentation/)
- [Qdrant 集成技能库（SkillMD）](https://skillmd.ai/skills/qdrant-vector-database-integration/SKILL.md)

---

**下一模块**：[09-选型对比与决策](./09-选型对比与决策.md) / **返回总览**：[00-Qdrant知识体系总览](./00-Qdrant知识体系总览.md)
