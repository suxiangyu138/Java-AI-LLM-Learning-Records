# 08-文档 ETL 管道
> RAG 的数据准备层：DocumentReader 抽取 → Transformer 分块/压缩 → DocumentWriter 写入向量库，附分块策略对比与生产建议

## 📚 目录
1. [ETL 管道模型](#1-etl-管道模型)
2. [Extract：DocumentReader 抽取](#2-extractdocumentreader-抽取)
3. [Transform：分块与处理策略](#3-transform分块与处理策略)
4. [Load：DocumentWriter 写入](#4-loaddocumentwriter-写入)
5. [管道组装实战](#5-管道组装实战)
6. [分块策略选型](#6-分块策略选型)
7. [生产化建议](#7-生产化建议)
8. [核心要点](#8-核心要点)
9. [参考来源](#9-参考来源)

## 1. ETL 管道模型

```text
文档源 ──Reader──▶ Document ──Transformer──▶ 分块 Document ──Writer──▶ 向量库
 (PDF/网页/DB)     (原始文本+元数据)          (可检索的小块)           (+Embedding)
```

| 阶段 | 职责 | 输入 → 输出 |
|------|------|-------------|
| Extract | 从各种来源抽取文本 | 任意源 → `Document`（文本 + 元数据） |
| Transform | 分块/清洗/摘要，提升检索质量 | `Document` → `List<Document>` |
| Load | 向量化并写入存储 | `List<Document>` → 向量库/文件 |

> 🎯 **核心要点**：**检索质量的上限在 ETL，不在模型**。分块粒度、元数据完整性、文档清洗直接决定召回精度——RAG 效果差，90% 先查 ETL 环节。

## 2. Extract：DocumentReader 抽取

### 2.1 Reader 实现矩阵

| Reader | 来源 | 说明 |
|--------|------|------|
| `PageContentDocumentReader` | 网页 URL | 抓取并解析 HTML |
| `FileSystemDocumentReader` | 本地文件 | 基于 Tika 解析 |
| `GitHubDocumentReader` | GitHub 仓库/文件 | 直接读仓库内容 |
| S3 / Azure Blob Reader | 云存储 | 对象存储文档 |
| 数据库 Reader | JDBC 表数据 | 把行记录转文档 |

### 2.2 Apache Tika 格式支持

| 格式 | 解析 |
|------|------|
| PDF / DOCX / XLSX / PPTX | 文本提取（含表格/嵌入内容） |
| MD / TXT / HTML / CSV / RTF | 结构化文本 |
| 图片内文字（OCR 场景） | 需 Tika 扩展/外部 OCR |

```java
var reader = new FileSystemDocumentReader(
        "docs/", new TikaDocumentReaderConfig());
List<Document> docs = reader.get();
```

> ⚠️ **抽取陷阱**：PDF 扫描件（无文本层）Tika 提不出内容；多栏排版 PDF 文本顺序错乱——生产环境先做抽取质量抽检，再进管道。

## 3. Transform：分块与处理策略

### 3.1 分块器对比

| 分块策略 | 机制 | 优点 | 缺点 | 适用 |
|----------|------|------|------|------|
| 固定大小 | 按 token/字符切 | 简单可控 | 切碎语义 | 通用兜底 |
| Token 级分块 | 按模型 tokenizer 切 | 与模型上下文对齐 | 同上 | 成本敏感 |
| **语义分块** | 按语义边界切（常带重叠） | 块内语义连贯 | 计算成本略高 | **默认推荐** |
| **树状分块（Tree）** | 章-节-小节组装成树 | 保留文档结构层级 | 实现复杂度高 | 长文档（手册/规范） |
| **压缩/摘要分块** | LLM 提炼关键内容 | 噪声低、块密度高 | LLM 成本 | 海量低质文档 |

### 3.2 其他 Transformer

| 类型 | 作用 |
|------|------|
| 关键词提取 | 为块补充关键词元数据（提升检索） |
| 摘要生成 | 生成块摘要（用于重排/概览） |
| 元数据增强 | 注入来源、章节、时间、权限标签 |
| 清洗 | 去重、去页眉页脚、规范化空白 |

## 4. Load：DocumentWriter 写入

| Writer | 目标 | 场景 |
|--------|------|------|
| VectorStore Writer | 向量库 | 主路径（写入时自动 `EmbeddingModel` 向量化） |
| 文件 Writer | 输出文件 | 调试管道中间结果 |

```java
// 核心：写入向量库
vectorStore.add(transformedDocs);   // 内部：EmbeddingModel.embed → 入库
```

> 💡 **向量化时机**：默认"写入时向量化"（write-time embedding）。海量/冷启动场景评估批量化与增量更新策略，避免全量重嵌。

## 5. 管道组装实战

```java
@Configuration
public class IngestionPipelineConfig {

    @Bean
    ApplicationRunner ingestRunner(
            @Qualifier("pgVectorStore") VectorStore vectorStore,
            EmbeddingModel embeddingModel) {

        return args -> {
            // ① Extract：读 docs/ 目录（Tika 解析）
            List<Document> raw = new FileSystemDocumentReader("docs/").get();

            // ② Transform：语义分块 + 元数据
            List<Document> chunks = new TokenTextSplitter()   // 可换 SemanticTextSplitter
                    .apply(raw).stream()
                    .map(d -> Document.builder(d)
                            .metadata(Map.of("source", "wiki", "ts", Instant.now().toString()))
                            .build())
                    .toList();

            // ③ Load：写入向量库
            vectorStore.add(chunks);
            log.info("入库 {} 块", chunks.size());
        };
    }
}
```

```text
管道演进路径（生产）：
单次全量灌入 → 定时增量 → 事件驱动（文件上传/DB 变更触发 ETL）
```

## 6. 分块策略选型

| 文档类型 | 推荐策略 | 块大小参考 |
|----------|---------|-----------|
| 产品文档/手册 | 树状分块（保留章节层级） | 章-节粒度 |
| FAQ/短文档 | 固定或语义分块 | 200~500 token |
| 论文/长报告 | 语义分块 + 摘要 | 500~1000 token |
| 海量日志/公告 | 压缩摘要分块 | 摘要驱动 |

选型决策链：`文档结构（有无层级）→ 检索粒度（问答 vs 概览）→ 成本预算（LLM 分块开销）→ 实测召回率`

> ⚠️ **块大小与检索体验**：块太大→上下文混入噪声、token 成本高；块太小→语义残缺、召回碎片。用真实查询做召回率评测（见 [10-可观测性评估与生产避坑](10-可观测性评估与生产避坑.md)），不要拍脑袋定参数。

## 7. 生产化建议

| # | 建议 | 说明 |
|---|------|------|
| 1 | 增量更新 | 记录文档指纹（hash/mtime），只重灌变更文档 |
| 2 | 文档版本管理 | 向量库中保留版本字段，支持回滚 |
| 3 | 去重 | 文档级/块级去重，防重复灌入污染检索 |
| 4 | 权限标签 | 元数据打权限标签，检索后按用户过滤（防越权） |
| 5 | 管道可观测 | 记录每批抽取/分块/入库的数量与耗时 |
| 6 | 抽检机制 | 定期人工抽检入库质量（抽取乱码/分块错乱） |
| 7 | 幂等重跑 | 同批文档重复执行管道结果一致（ID 可重复） |

## 8. 核心要点

> 🎯 **核心要点**：
> - ETL 三阶段：Reader（抽取，Tika 多格式）→ Transformer（分块/摘要/元数据）→ Writer（向量化入库）；
> - 分块策略按"文档结构 + 检索粒度 + 成本"选型：语义分块默认、树状分块长文档、压缩摘要海量低质；
> - 检索质量上限在 ETL：先查抽取乱码、分块粒度、元数据，再谈模型与重排；
> - 生产化五件套：增量、版本、去重、权限标签、幂等。

## 9. 参考来源

- [Spring AI Reference：ETL Pipeline](https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html)
- [Spring AI Reference：Document Readers/Transformers/Writers](https://docs.spring.io/spring-ai/reference/api/document-readers.html)
- [Spring AI 2.0.0 GA 发布博客（ETL 框架）](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now)

---

**下一模块**：[09-记忆与Advisor链](09-记忆与Advisor链.md)　/　**返回总览**：[00-总览](00-Spring%20AI总览.md)
