# 生态集成与 RAG 实战

> Spring AI 官方 starter、LangChain4j、JdbcTemplate 直连、Spring Data JPA 实体层——Java 接入 PgVector 的全部路径

## 1. Spring AI：官方 starter

Spring AI 官方提供 `spring-ai-pgvector-store-spring-boot-starter`，一条依赖获得向量库自动配置：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>
</dependency>
```

```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        initialize-schema: true        # 自动建表
        dimensions: 1536               # 与 embedding 模型一致（硬约束）
        index-type: HNSW
        distance-type: COSINE_DISTANCE # 与建表时的操作符一致
        table-name: documents
```

核心类 `PgVectorStore` 实现 Spring AI 统一 `VectorStore` 接口——`add(List<Document>)` 入库、`similaritySearch(SearchRequest)` 检索，业务代码与 Chroma/Milvus 完全一致，**换向量库只换依赖与配置**。自动建表逻辑默认生成 `vector(1536)` 列 + HNSW + cosine 索引，与手写 SQL 等价；生产环境建议 `initialize-schema: false`，用 Flyway 管 schema（下表）。

```java
@Configuration
public class VectorStoreConfig {
    @Bean
    public VectorStore vectorStore(JdbcTemplate jdbc, EmbeddingModel embeddingModel) {
        return new PgVectorStore(jdbc, embeddingModel,
                new PgVectorStore.PgVectorStoreConfig()
                        .withTableName("documents")
                        .withDimensions(1536)
                        .withIndexType(PgVectorStore.PgIndexType.HNSW)
                        .withDistanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE));
    }
}
```

与 embedding 模型的配套注意：**dimensions 必须与模型输出一致**（text-embedding-3-small 是 1536、BGE-M3 是 1024）；**换模型 = 换列或换表**（维度变了向量存不进去），生产上把"模型版本"与"表结构"绑定记录，避免换了模型忘了迁移表——这是所有向量方案共有的坑，PgVector 因为表是显式的，反而更容易审查。

## 2. 直连 SQL：JdbcTemplate 最小路径

不引入框架时，PgVector 的最小接入就是一条 `<->` 查询——**这是所有向量方案里 Java 成本最低的**：

```java
@Repository
public class DocumentRepository {
    private final JdbcTemplate jdbc;

    public List<DocHit> search(String queryEmbedding, int topK) {
        // embedding 序列化为 pgvector 的文本格式 '[0.1,0.2,...]'
        return jdbc.query("""
            SELECT id, content, 1 - (embedding <=> ?::vector) AS similarity
            FROM documents
            ORDER BY embedding <=> ?::vector
            LIMIT ?
            """, (rs, i) -> new DocHit(rs.getLong("id"), rs.getString("content"),
                                       rs.getDouble("similarity")),
            queryEmbedding, queryEmbedding, topK);
    }
}
```

注意两个工程点：**embedding 序列化格式**——pgvector 的 JDBC 驱动（pgvector-java）提供 `Pgvector` 类型直接绑定参数，避免手拼字符串；**`?::vector` 显式类型转换**——PostgreSQL 无法推断字符串参数是 vector 类型，不写转换会报"operator does not exist"。pgvector-java 驱动同时提供 `Vector`/`Halfvec`/`Sparsevec` 类型与 JPA 支持。

```xml
<!-- pgvector-java：JDBC 类型绑定 + JPA 映射 -->
<dependency>
    <groupId>com.pgvector</groupId>
    <artifactId>pgvector</artifactId>
    <version>0.1.x</version>   <!-- 与 pgvector 扩展版本解耦，独立演进 -->
</dependency>
```

驱动用法：`new Pgvector.Vector(new float[]{0.1f, 0.2f})` 绑定参数（Spring 的 `JdbcTemplate` 直接传对象即可，驱动自动序列化为 `'[0.1,0.2]'` 文本）；JPA 实体字段用 `Pgvector.Vector` 类型 + `@Column(columnDefinition = "vector(1536)")`——**注意驱动版本与 PG 服务器扩展版本独立**，驱动 0.1.x 兼容扩展 0.8.x，升级任一侧前查兼容说明。

## 3. LangChain4j 与 Spring Data JPA

**LangChain4j**：`langchain4j-pgvector` 模块提供 `PgVectorEmbeddingStore`，配合 `EmbeddingStoreIngestor`（切分 → 向量化 → 入库）走组件化管线——适合"框架即全流程"的团队；与 Spring AI 并存（一个管检索、一个管切分）是常见组合。

```java
// LangChain4j：文档 → 切分 → 向量化 → pgvector
EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder().apiKey("...").build();
PgVectorEmbeddingStore store = PgVectorEmbeddingStore.builder()
        .host("localhost").port(5432).database("app")
        .user("app").password("...").dimension(1536)
        .createTable(true)                  // 自动建表
        .build();
EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
        .documentSplitter(new DocumentByParagraphSplitter(500, 50))
        .embeddingModel(embeddingModel)
        .embeddingStore(store)
        .build();
ingestor.ingest(Document.from("...内容..."));
```

LangChain4j 侧两个注意：**`dimension` 必须与模型一致**（与 Spring AI 同规则）；`createTable(true)` 只适合原型，生产表结构交给 Flyway（用 `createTable(false)` 指向既有表）——两条路都走通后，**"原型用自动建表、生产用迁移脚本"**是统一的工程纪律。

**Spring Data JPA**：向量列作为实体字段需要类型映射（pgvector-jpa 支持 `Vector` 字段），查询走 `@Query` 原生 SQL（`ORDER BY embedding <=> ?1`）——**实体层与检索层分离**是生产推荐形态：

```java
@Entity
@Table(name = "documents")
public class Document {
    @Id private Long id;
    private String content;
    private Vector embedding;      // pgvector-jpa 的类型映射
}
```

JPA 侧三个注意：**实体读写向量走驱动类型**（`Pgvector.Vector` 构造与读取），不要手拼字符串；**相似度查询不走 JPQL 走 @Query 原生 SQL**（JPQL 无 `<=>` 操作符概念）；**实体不参与检索排序**——排序与 topK 在 Repository 的原生查询里，实体只做 CRUD 与展示。生产组合是"Spring AI 管向量检索 + LangChain4j 管文档处理 + JPA 管实体"，各司其职——三条 Java 路径（08 篇第 1-3 节）不是三选一，而是按层分工。

生产架构三件套：**Flyway 管 schema**（建表 + 索引 + partial index 全部进迁移脚本，版本化可回滚）、**JPA 管实体**（业务字段）、**JdbcTemplate/VectorStore 管检索**（向量查询走原生 SQL，不走 JPQL——JPQL 无法表达 `<=>`）。

Flyway 迁移脚本的标准形态（`V1__vector_schema.sql`）：

```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    metadata JSONB DEFAULT '{}',
    embedding vector(1536)              -- 维度与模型绑定，写死并注释模型版本
);

CREATE INDEX idx_documents_hnsw
    ON documents USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- 热门分类的部分索引（见 05 篇）
CREATE INDEX idx_documents_jvm
    ON documents USING hnsw (embedding vector_cosine_ops)
    WHERE metadata->>'category' = 'jvm';
```

两条规范：**`CREATE EXTENSION` 放迁移脚本首条**（新环境一条命令建全）；**索引与查询操作符在注释里对齐**（`-- 配合 ORDER BY embedding <=> ?` 使用），防止后人改了索引忘了查询——团队里"EXPLAIN 出 Seq Scan 才发现索引错配"的排障基本都是这样留下的。

## 4. 完整 RAG 链路实战

```text
文档源 → 切分（Spring AI TokenTextSplitter）→ Embedding 模型 → PgVectorStore
                                                              ↓
用户问题 → Embedding → similaritySearch(SearchRequest) → 上下文
                                                              ↓
                                            ChatClient 生成回答（引用溯源）
```

关键决策点：**dimensions 与 embedding 模型硬绑定**（改模型 = 新建表/列）；**混合检索落地**——向量 + FTS 用 RRF 时，FTS 部分走 JdbcTemplate 原生查询，两路结果在服务层融合（06 篇的 SQL 模板）；**post-filter 防御**——Spring AI 的 `filterExpression` 传进查询后仍是 post-filter，热门分类建 partial index（05 篇）；**评估闭环**——命中率 + MRR 指标对比（对齐 Chroma 体系 08 篇的评估方法论）。

混合检索的 Java 落地（向量走 VectorStore、FTS 走 JdbcTemplate、服务层 RRF 融合）：

```java
public List<DocHit> hybridSearch(String question, String qvec, String tsQuery, int topK) {
    // 语义路：Spring AI VectorStore
    List<Document> semantic = vectorStore.similaritySearch(
            SearchRequest.builder().query(question).topK(topK * 2).build());
    // 关键词路：FTS 原生 SQL
    List<DocHit> lexical = jdbc.query("""
        SELECT id, ROW_NUMBER() OVER (ORDER BY ts_rank_cd(content_tsv, q) DESC) r
        FROM documents, to_tsquery('english', ?) q
        WHERE content_tsv @@ q LIMIT ?""", (rs, i) -> ...);
    // 服务层 RRF 融合：1/(60 + rank) 求和排序
    Map<Long, Double> scores = new HashMap<>();
    semantic.forEach(d -> scores.merge(d.getId(), 1.0 / 61, Double::sum));
    lexical.forEach(h -> scores.merge(h.getId(), 1.0 / (60 + h.rank()), Double::sum));
    return scores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(topK).map(...).toList();
}
```

融合的工程注意：**两路 topK 各取目标的 2 倍**（融合后才有富余）；**id 键类型统一**（语义路是 Long、FTS 路也是 Long，先对齐再 merge）；候选集与融合逻辑抽成独立方法，方便 A/B 单路 vs 混合（评估闭环见 06 篇）。

评估闭环的落地（对齐 Chroma 体系 08 篇的方法论）：准备 100-200 条标注问答对（问题 → 期望命中的文档 id）；跑三种检索——纯语义、纯 FTS、混合 RRF——统计命中率（top-5 是否含期望 id）与 MRR（期望 id 的排名倒数均值）；**指标对比决定是否上线混合检索**（实测混合比纯语义 +10-20% 时值得投入）。评估脚本进 CI（每日跑），参数变更（ef_search、RRF k、权重）触发重跑——**检索质量是可回归的工程资产，不是一次性的验收动作**。

与切分策略的联动（配合 `02-RAG检索增强生成` 体系）：切分粒度决定召回单元——**分块太小（<200 字）语义碎片化、FTS 词项稀疏；太大（>1000 字）向量被稀释、相似度区分度下降**；PgVector 的独特优势是**切分元数据可以结构化落库**（chunk 序号、来源文档 id、层级路径存进 JSONB 列）——检索后用 SQL 直接按元数据聚合（如"同一来源文档的多个 chunk 合并去重"），这是专用向量库做不到的灵活性，也是"同库"红利在 RAG 场景最实用的兑现。切分参数（chunk_size/overlap）进评估闭环（见上段）一并调优——切分与检索是两个耦合变量，分开调等于调了一半。

```java
List<Document> hits = vectorStore.similaritySearch(
    SearchRequest.builder()
        .query(question)
        .topK(5)
        .similarityThreshold(0.7)
        .filterExpression("category = 'docs'")   // 注意：post-filter 语义
        .build());
```

阈值与过滤的配合注意：**`similarityThreshold` 与 post-filter 是两套独立机制，会叠加吞结果**——阈值筛掉低分候选、过滤再吞掉不匹配候选，两者叠加后 topK 可能只剩一两条；调参时先放宽一个（阈值设 0 或去掉过滤）确认另一路的召回，再逐步收紧。阈值与过滤参数都进评估闭环（见上段）定值，不拍脑袋。

> 🎯 **核心要点**：Java 接 PgVector 三路径——Spring AI starter（统一抽象）、JdbcTemplate + `<->`（最小成本）、JPA + Flyway（生产形态）；`?::vector` 类型转换与 pgvector-java 驱动是两个必踩点；混合检索的 FTS 部分走原生 SQL，服务层 RRF 融合。

---

**参考来源**：

- [Spring AI pgvector starter（Maven）](https://libraries.io/maven/org.springframework.ai:spring-ai-pgvector-store-spring-boot-starter)
- [Spring AI pgvector 示例仓库](https://github.com/JavaAIDev/pgvector-sample)
- [RAG 知识库实战（Spring Boot + Spring AI + pgvector）](https://github.com/rahul-ghadge/rag-knowledge-base)
- [LangChain4j + Spring AI RAG（阿里云开发者）](https://developer.aliyun.com/article/1683341)

---

**下一模块**：[09-选型对比与决策](./09-选型对比与决策.md) / **返回总览**：[00-PgVector知识体系总览](./00-PgVector知识体系总览.md)
