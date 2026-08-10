# 07 - 知识图谱：PropertyGraphIndex

> 本体系第七课：关系推理——向量检索答"内容在哪"，图检索答"实体怎么连"——"答案跨文档时，图检索是唯一路径"

---

## 📚 目录

1. [一句话定位](#1-一句话定位)
2. [为什么需要图](#2-为什么需要图)
3. [属性图模型](#3-属性图模型)
4. [构建图谱](#4-构建图谱)
5. [检索图谱](#5-检索图谱)
6. [GraphRAG 选型](#6-graphrag-选型)
7. [Schema 设计与图向量融合](#7-schema-设计与图向量融合)
8. [练习 5 题](#8-练习-5-题)
9. [本节验收](#9-本节验收)

---

## 1. 一句话定位

**PropertyGraphIndex 用 LLM 从文档抽取"实体 + 关系"存成图，支持跨文档多跳推理——"向量检索找'包含答案的段落'，图检索找'实体之间的联系'"**：

```text
图谱链路
├── 抽取：LLM 从文本抽实体/关系（SchemaLLMPathExtractor）
├── 存储：属性图（EntityNode + Relation + ChunkNode 回链原文）
├── 检索：向量找种子节点 → 图遍历扩展（VectorContextRetriever）
└── 合成：路径上的实体上下文拼进 LLM（或 TextToCypher 查库）
    ——"图 = 实体网络；ChunkNode = 每个实体回链的原文证据"
```

**定位心智**：**"图不是替代向量，是补向量做不到的——'多跳关系'与'实体扇出'——答案是向量检索的盲区"**——"**问题需要'连接事实'时用图：A 与 B 什么关系？谁投资了谁？影响链怎么走？——'单块内容能答的问题，向量就够'"**。

## 2. 为什么需要图

**两类查询向量检索天然答不出**：

```text
向量盲区
├── 多跳推理："X 公司的创始团队现在在哪家公司任职？"——事实分散在
│   多篇文档，单段都不含完整答案
└── 实体扇出："小明认识的所有人里，谁负责过支付系统？"——需要沿
    "认识"关系遍历再过滤
    ——"向量答'这段文字包含什么'；图答'这些实体怎么连'"
```

**心智**：**"判断标准一句话：答案是否在一段话内——在，向量；跨段/跨文档，图"**——"**图的额外价值：可解释（推理路径可视化）+ 全局问答（实体级聚合统计）——GraphRAG 的'全局视角'是图独有"**。

## 3. 属性图模型

**LPG（Labeled Property Graph）——节点和关系都带标签与属性**：

```text
三种节点/关系
├── EntityNode：实体（人/公司/技术——label: 类型，properties: 属性）
├── Relation：关系（WORKS_AT/INVESTS_IN——带方向与属性）
└── ChunkNode：原文块（把每个图节点回链到证据段落）
    ——"ChunkNode 是图与原文之间的'桥'——溯源答案全靠它"
```

**心智**：**"记三个类名：EntityNode/Relation/ChunkNode——'实体 + 关系 + 证据'"**——"**关系带方向（A WORKS_AT B ≠ B WORKS_AT A）——TextToCypher 查询时方向是语义的一部分"**。

## 4. 构建图谱

**0.13+ 需显式 mode——两种抽取器**：

```python
from llama_index.core.indices.property_graph import (
    PropertyGraphIndex, SchemaLLMPathExtractor, DynamicLLMPathExtractor,
)

# mode="llm"：LLM 抽取（Schema 预定义实体/关系类型——输出可控）
kg_index = PropertyGraphIndex.from_documents(
    docs,
    property_graph_store=SimplePropertyGraphStore(),  # 内存/JSON
    kg_extractors=[
        SchemaLLMPathExtractor(
            llm=Settings.llm,
            possible_entities=["Person", "Company", "Technology"],
            possible_relations=["WORKS_AT", "INVESTS_IN", "USES"],
            strict=True,   # 只抽 schema 内的类型
        ),
    ],
    mode="llm",   # 0.13+ 必填：llm 或 custom
)
```

**要点**：① `SchemaLLMPathExtractor` 预定义类型（可控、省钱），`DynamicLLMPathExtractor` 动态推断（开放域）；② 图存储可换 Neo4j/FalkorDB（`neo4j_property_graph_store`）——**"原型内存，生产 Neo4j"**；③ 抽取是 LLM 调用——**成本与文档量线性相关，先小语料验证 schema 再全量**。

## 5. 检索图谱

**两种检索策略——按图存储能力选**：

```python
# 方式一：VectorContextRetriever——向量找种子，图遍历扩展
from llama_index.core.indices.property_graph import VectorContextRetriever

kg_index.as_retriever(  # PropertyGraphIndex 默认用 VectorContextRetriever
    similarity_top_k=3,     # 种子实体数
    path_depth=2,           # 从种子向外走几步
)

# 方式二：TextToCypherRetriever——LLM 写 Cypher 查图库（Neo4j 专用）
from llama_index.core.indices.property_graph import TextToCypherRetriever

retriever = TextToCypherRetriever(
    property_graph_store=neo4j_store,   # Neo4j 才支持
    llm=Settings.llm,
)
nodes = retriever.retrieve("哪些公司投资了 Qdrant？")
```

**要点**：① **向量找种子 + 图遍历扩展**是默认组合——`path_depth` 是"推理深度"旋钮（越深越全越贵）；② TextToCypher 把自然语言转 Cypher 查库——**"精确但依赖图库（Neo4j）与 LLM 的 Cypher 能力"**；③ 图检索结果可直接与向量检索融合（`QueryFusionRetriever` 装两个检索器——06 篇的融合思路）。

**TextToCypher 示例**（查 Neo4j 的等价 Cypher——理解"LLM 在做什么"）：

```cypher
// 问题："哪些公司投资了 Qdrant？"  →  LLM 生成：
MATCH (c:Company)-[r:INVESTS_IN]->(t:Company {name: "Qdrant"})
RETURN c.name AS company, r.amount AS amount
```

**心智**：Cypher 是图库的查询语言——**"TextToCypher 的质量 = LLM 的 Cypher 能力 × schema 的清晰度——schema 命名规范（实体/关系命名一致）直接决定查询准确率"**（Neo4j 的 Cypher 语法与存储原理见本仓库 02-后端核心 目录的 Neo4j 体系）。

## 6. GraphRAG 选型

| 维度 | 向量 RAG | GraphRAG |
|------|---------|----------|
| 查询类型 | 单块内容问答 | 多跳/关系/全局 |
| 构建成本 | 低（切分 + 嵌入） | 高（LLM 抽取） |
| 可解释性 | 弱 | 强（路径可视化） |
| 增量更新 | 简单 | 需重新抽取 |
| 适用 | 知识库问答（约 8 成） | 关系分析/合规审计/全局洞察 |

**选型心智**：**"先用向量把 8 成问题做掉，图是'补盲区'的进阶——'图 + 向量混合（图推理、向量上下文）是 2026 GraphRAG 的主流姿势'"**——"**构建成本是主要门槛：抽取要 LLM、schema 要设计——小语料起步验证收益再上规模"**。

**图的三个局限（别神化）**：① **抽取噪声**——LLM 抽关系会漏抽/错抽，图不完整时"没查出来"≠"不存在"；② **更新成本**——文档改版后相关实体/关系要重新抽取（比向量增量重嵌贵）；③ **中文实体**——人名/组织名识别受 embedding 与 LLM 能力影响——"**图是'高成本高收益'的选项——先算清收益再上（与 04-RAG 体系 GraphRAG 的成本论述一致）"**。

## 7. Schema 设计与图向量融合

**Schema 设计三原则（决定抽取质量）**：

```text
Schema 三原则
├── ① 实体粒度适中：Person/Company/Technology 级别——别细到"房间号"级
│    ——类型越细，抽取越易错、覆盖越窄
├── ② 关系方向明确：WORKS_AT（人→公司）方向即语义——倒过来查询全错
└── ③ strict 起步：先 strict=True 只抽 schema 类型（可控）
    ——跑一批看漏抽/错抽，再放宽（DynamicLLMPathExtractor 开放域兜底）
```

**构建成本控制**：抽取按文档量线性消耗 LLM 调用——**"小语料（百篇内）验证 schema → 全量构建 → 增量更新时只对新增文档抽取"**；生产上抽取失败/超时要有重试——"一次抽取失败丢一个实体关系，图就不完整"。

**图 + 向量融合（2026 主流姿势）**——图推理、向量上下文：

```python
from llama_index.core.retrievers import QueryFusionRetriever

# 图检索器 + 向量检索器融合（06 篇的融合思路直接复用）
fusion = QueryFusionRetriever(
    retrievers=[kg_index.as_retriever(similarity_top_k=3, path_depth=2),
                VectorIndexRetriever(index=vector_index, similarity_top_k=5)],
    similarity_top_k=5,
    mode="relative_score",
)
query_engine = RetrieverQueryEngine.from_args(fusion, llm=Settings.llm)
```

**心智**：**"图管'关系路径'，向量管'上下文证据'——融合后回答既懂'谁连着谁'又有'原文支撑'"**——"**与 04-RAG 体系的 GraphRAG 篇同源：原理看那边，LlamaIndex 落地看这边**"。

## 8. 练习 5 题

1. 图解决什么向量答不出的问题？（多跳推理/实体扇出）
2. LPG 三个核心类？（EntityNode/Relation/ChunkNode）
3. 0.13+ 构建要什么参数？（mode="llm"/"custom" 显式）
4. 两种检索策略？（向量找种子 + 图遍历 / TextToCypher）
5. 怎么判断要不要图？（答案是否在一段话内）

## 9. 本节验收

**验收动作**：① 用 20 篇人物/公司关系类语料建 PropertyGraphIndex；② 跑"多跳关系"问题验证向量答不出、图能答；③ 打印检索路径看推理链——**"图 = 向量的盲区补充——会建会查即可"**。

> 🎯 **核心要点**：**图解决多跳与实体扇出（向量盲区）**；**三个类（EntityNode/Relation/ChunkNode）**；**0.13+ mode 必填**；**默认检索 = 向量找种子 + 图遍历**；**图 + 向量混合是 2026 主流**。

---

**上一模块**：[06-高级检索：混合检索与重排.md](./06-高级检索：混合检索与重排.md) / **下一模块**：[08-Agent与事件驱动工作流.md](./08-Agent与事件驱动工作流.md)
