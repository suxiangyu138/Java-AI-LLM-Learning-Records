# Milvus 向量数据库 & RAG 面试宝典

> 基于课程大纲全面覆盖面试高频考点 — Milvus 架构、RAG 优化、LLM 推理优化全解析

## 目录

1. [一、基础概念速答](#一基础概念速答)
2. [二、深度原理剖析](#二深度原理剖析)
3. [三、实战场景题](#三实战场景题)
4. [四、手写代码题](#四手写代码题)
5. [五、系统设计题](#五系统设计题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板（Top 5）](#七面试回答模板top-5)
8. [八、快速查漏补缺 Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答

### 1.1 Milvus 是什么？适合什么场景？

**Milvus** 是一个开源的高性能向量数据库，专为 **向量相似度搜索**（Vector Similarity Search）和 **AI 应用** 设计。它支持万亿级向量数据的存储、索引和检索。

> 适用场景：RAG（检索增强生成）、推荐系统、图片/视频/音频相似度搜索、自然语言处理、药物分子搜索等。

### 1.2 什么是向量（Vector）和向量化（Vectorization）？

**向量** 是数学中一组浮点数，在 AI 领域代表非结构化数据（文本、图片、音频）的 **语义特征表示**。**向量化** 指通过 Embedding 模型（如 BERT、text2vec、OpenAI Embedding API）将原始数据转换为向量。

```text
"我喜欢编程" → [0.123, -0.456, 0.789, ...]  // 768 维向量
```

### 1.3 Milvus 核心架构组件有哪些？

| 组件 | 作用 | 说明 |
|------|------|------|
| **Proxy** | 请求入口网关 | 处理客户端请求，解析、校验、转发 |
| **Root Coord** | 总协调节点 | 管理集群状态、DDL/DCL 操作、时间戳分配 |
| **Query Coord + Query Node** | 查询节点 | 执行向量检索和标量过滤，管理 Segment 的加载/释放 |
| **Data Coord + Data Node** | 数据节点 | 管理数据的持久化、 compaction、索引构建 |
| **Index Node** | 索引节点 | 专门负责构建向量索引（异步） |
| **Meta Store** (etcd) | 元数据存储 | 存储集合、分区、索引等元数据 |
| **Object Store** (MinIO/S3) | 对象存储 | 存储 Binlog、索引文件、Delta log |
| **Log Broker** (Pulsar/Kafka) | 日志代理 | 负责增量数据的发布与订阅，保证写日志顺序一致性 |

### 1.4 Milvus 一致性级别有哪些？

| 级别 | 说明 | 适用场景 |
|------|------|----------|
| **Strong** | 强一致性，写入立即可见 | 对一致性要求极高的场景 |
| **Eventually** | 最终一致性，性能最高 | 推荐系统等允许短时不一致的场景 |
| **Bounded Staleness** | 有界过期，容忍设定的时间差 | 默认级别，兼顾性能与一致性 |
| **Session** | 会话级一致性，同一 session 内强一致 | 单用户操作场景 |

### 1.5 支持哪些向量距离度量？

| 度量方式 | 公式说明 | 适用场景 |
|----------|----------|----------|
| **Cosine** | `1 - cos(θ)` | 文本语义相似度 |
| **Euclidean (L2)** | `sqrt(Σ(aᵢ - bᵢ)²)` | 图像特征匹配 |
| **Dot Product (IP)** | `Σ aᵢ · bᵢ` | 推荐系统、归一化向量场景 |

> 💡 选型建议：文本检索首选 Cosine，图像/音频推荐 L2，评分推荐用 IP。

### 1.6 什么是 Collection、Partition、Segment？

- **Collection**：对应关系数据库的表，包含 Schema 定义。
- **Partition**：Collection 内的水平分区，通过 `partition_key` 实现物理隔离，提升查询性能。
- **Segment**：数据存储的最小物理单元，Milvus 会自动合并小 Segment（Compaction）。

### 1.7 什么是 Partition Key？

Partition Key 是 Milvus 2.3+ 引入的功能，允许用户在写入时指定字段作为分区键，系统自动将数据路由到对应分区，避免手动管理分区。

```python
schema.add_field("user_id", DataType.INT64, is_partition_key=True)
```

### 1.8 RAG 是什么？包含哪几个核心步骤？

**RAG (Retrieval-Augmented Generation)** 检索增强生成 — 从外部知识库检索相关文档片段，注入 LLM 上下文以生成准确答案。

> 核心步骤：文档加载 → 文档切分（Chunking）→ 向量化（Embedding）→ 存入向量库 → 用户查询 → 语义检索 → 重排序（Rerank）→ LLM 生成答案。

### 1.9 什么是 Chunking（文档切分）策略？

| 策略 | 方法 | 优缺点 |
|------|------|--------|
| **Fixed-size Chunking** | 固定字符数切分（如 512 tokens） | 简单但可能切碎语义 |
| **Recursive Character Text Splitter** | 递归按分隔符切分 | LangChain 默认方案，平衡性好 |
| **Semantic Chunking** | 按语义边界切分 | 效果好但计算成本高 |
| **Document-specific** | 按 Markdown 标题/HTML 结构切分 | 保留文档结构，代码文档首选 |

### 1.10 什么是 Embedding 模型？

Embedding 模型将文本、图像等非结构化数据映射到低维向量空间。常见模型：

```text
- text2vec-large-chinese (中文)
- BAAI/bge-large-zh-v1.5 (中文)
- OpenAI text-embedding-3-small/large (通用)
- intfloat/multilingual-e5-large (多语言)
```

### 1.11 什么是 Rerank（重排序）？

Rerank 是对检索结果进行二次精排的过程。第一次检索（ANN 搜索）快速召回 Top-K 候选，Rerank 用更精确的交叉编码器模型重新评分。

> 典型方案：Cohere Rerank、BGE Reranker、ColBERT-v2 等。

### 1.12 什么是 Harness Engineering？

Harness Engineering（AI 安全工程）是围绕 LLM Agent 构建的 **安全控制框架**，核心关注 Agent 工具调用的安全性。

| 核心能力 | 说明 |
|----------|------|
| **安全沙箱（Sandbox）** | 隔离 Agent 执行环境，防止恶意代码影响宿主机 |
| **代码执行（Code Execution）** | 在沙箱中安全运行 Agent 生成的代码 |
| **工具权限控制** | 细粒度控制 Agent 可调用的工具和 API |
| **输入验证（Guardrails）** | 检测和过滤注入攻击、恶意指令 |
| **输出审查** | 检查 Agent 输出是否包含敏感信息 |
| **审计日志** | 记录所有 Agent 操作轨迹 |
| **资源限制** | 限制 Agent 的 CPU、内存、网络、磁盘使用 |

### 1.13 什么是 Multi-Agent 和 Agent Skills？

- **Multi-Agent**：多个 Agent 协作完成复杂任务，每个 Agent 负责一个子任务，通过消息传递协调。
- **Agent Skills**：Agent 可复用的功能模块，类似插件的概念。每个 Skill 封装特定能力（如搜索、计算、数据分析）。

> Agent Skills vs 传统函数调用 — Skills 是面向 Agent 的，包含描述、输入输出 schema 和错误处理；普通函数调用仅面向开发者。

### 1.14 Distance Metric 在代码中如何指定？

```python
# Milvus 中创建索引时指定度量方式
index_params = {
    "metric_type": "COSINE",   # 可选: L2, IP, COSINE
    "index_type": "IVF_FLAT",
    "params": {"nlist": 1024}
}
```

### 1.15 什么是 AutoIndex？

Milvus Cloud 和 Milvus 2.4+ 提供的自动索引功能，系统根据数据分布自动选择最优索引类型和参数，无需手动调参。

### 1.16 什么是 ScaNN 索引？

ScaNN (Scalable Nearest Neighbors) 是 Google 开发的向量索引算法，通过 **各向异性量化（Anisotropic Quantization）** 提升检索精度。Milvus 2.4+ 通过 Knowhere 集成。

### 1.17 什么是 Load Balancing 和 QueryNode 扩缩容？

Milvus 通过 Proxy 层实现请求负载均衡。QueryNode 支持动态扩缩容，当查询压力增大时可自动增加 QueryNode 节点，减小则释放。

### 1.18 什么是 Time Travel（时间旅行）？

Milvus 支持基于时间戳的查询，允许用户查询 **某个历史时刻** 的数据状态，用于数据回滚、审计等场景。

```python
search_params = {"time_travel": timestamp}  # 查询指定时间点的数据
```

---

## 二、深度原理剖析

### 2.1 Milvus 索引类型对比（面试高频）

| 索引类型 | 原理 | 适合场景 | 优点 | 缺点 |
|----------|------|----------|------|------|
| **Flat (Brute Force)** | 暴力全量比对 | 小数据集（<1万） | 100% 召回率 | 速度最慢 |
| **IVF_FLAT** | IVF + 无压缩 | 中等规模、高精度 | 精度高、简单 | 内存占用大 |
| **IVF_PQ** | IVF + 乘积量化压缩 | 大规模、内存敏感 | 大幅降低内存 | 精度损失 |
| **IVF_SQ8** | IVF + 标量量化 | 内存敏感场景 | 比 PQ 精度略高 | 不如 Flat 精确 |
| **HNSW** | 分层可导航小世界图 | 高 QPS、低延迟 | 极快检索速度 | 索引构建慢、内存大 |
| **DiskANN** | 基于磁盘的图索引 | 超大规模（内存不足） | 支持内存放不下的数据 | 延迟比 HNSW 高 |
| **BIN_FLAT/BIN_IVF** | 二值化向量索引 | 二值向量（图像哈希） | 内存极省 | 仅支持二值向量 |

> 🎯 面试高频回答：`数据量 < 1万用 Flat`，`1万-100万用 IVF_FLAT`，`> 100万且需要高 QPS 用 HNSW`，`内存不足用 DiskANN`。

### 2.2 IVF_FLAT 原理

IVF (Inverted File) 分两步：

1. **K-means 聚类**：将数据集聚类为 `nlist` 个簇，每个簇有一个中心点向量。
2. **检索**：计算查询向量与各簇中心的距离，选出最近的 `nprobe` 个簇，只在簇内进行精确搜索。

```text
搜索过程：
查询向量 → 找到最近的 nprobe 个簇中心 → 在这些簇中遍历所有向量 → 返回 Top-K
```

`nlist` 越大，索引构建越慢但搜索精度越高。`nprobe` 越大，召回率越高但延迟增加。

### 2.3 HNSW 原理详解

HNSW 构建多层图结构：

- **底层（Layer 0）**：包含所有向量，是完整的可导航小世界图。
- **上层**：通过指数衰减概率抽样，越往上节点越少。
- **搜索**：从顶层开始找到最近节点，逐层向下精化搜索。

> 关键参数：`M`（每个节点的最大连接数，默认 16，越大召回越高但内存越大），`efConstruction`（构建时的动态候选列表大小），`ef`（搜索时的动态候选列表大小）。

### 2.4 RAG 优化五大维度

| 优化维度 | 关键策略 | 效果 |
|----------|----------|------|
| **1. Chunking 优化** | 动态大小切分、重叠窗口、按语义边界切分、HyDE | 提高检索命中率 |
| **2. Embedding 优化** | 选择领域 Embedding 模型、微调 Embedding、多路召回 | 提升语义表征质量 |
| **3. Retrieval 优化** | 多路召回（稀疏+稠密）、混合搜索、HyDE、Query Rewriting、Multi-Query | 增强召回覆盖 |
| **4. Rerank 优化** | 交叉编码器重排、MMR（最大边缘相关性）、Cohere/BGE Reranker | 提升 Top-K 相关性 |
| **5. Generation 优化** | Prompt 指令优化、Few-shot、上下文窗口管理、幻觉检测 | 提高生成答案质量 |

#### HyDE（Hypothetical Document Embedding）

用户查询 → LLM 先"想象"一个理想文档 → 用这个虚构文档的向量去检索 → 提高检索命中。

```text
query: "如何部署 Milvus？"
  ↓ LLM
hypothetical_doc: "Milvus 部署指南：通过 Docker Compose 安装..."
  ↓ Embedding
vector → Milvus 检索 → 返回真实文档
```

#### Multi-Query Retrieval

将用户问题拆解为多个子问题分别检索，去重后汇总结果。

```text
原问题: "Milvus 和 Elasticsearch 选型对比"
  ↕ 拆解
子问题1: "Milvus 向量搜索性能如何？"
子问题2: "Elasticsearch 的向量搜索功能"
子问题3: "Milvus vs ES 性能对比"
```

### 2.5 LLM 推理原理：Prefill 与 Decode 阶段

| 阶段 | 操作 | 特点 |
|------|------|------|
| **Prefill（预填充）** | 并行计算输入 tokens，生成 Key-Value Cache | 计算密集型，GPU 利用率高 |
| **Decode（解码）** | 逐 token 自回归生成，每个 token 一次前向 | 访存密集型，GPU 利用率低 |

### 2.6 E2E LLM 推理步骤

```text
1. Tokenization: 将输入文本转换为 token IDs
2. Embedding: 将 token IDs 映射为向量
3. Prefill: 并行计算所有输入 token 的 hidden states，构建 KV Cache
4. Decode loop:
   a. 取最后一个 token 的 hidden state
   b. 通过 LM Head 计算 logits
   c. softmax + sampling → 生成下一个 token
   d. 更新 KV Cache
   e. 重复直到遇到 EOS token 或达到最大长度
5. Detokenization: 将 token IDs 转换回文本
```

### 2.7 KV Cache 与 Page Attention

**KV Cache**：缓存历史 tokens 的 Key 和 Value 矩阵，避免每步重复计算，极大提升 Decode 速度。

**Page Attention（vLLM）**：借鉴操作系统虚拟内存管理，将 KV Cache 分页管理，解决显存碎片化问题。

```text
传统 KV Cache 问题:
- 显存预留过大（OOM）或过小（频繁 OOM）
- 显存碎片化严重

Page Attention 解决:
- 按 Page 分配显存（类似 OS 分页）
- 逻辑上连续的 KV Cache 可映射到不连续的物理块
- Copy-on-Write 支持共享 prefix
```

### 2.8 Continuous Batching 连续批处理

对比传统 Static Batching：

| 特性 | Static Batching | Continuous Batching |
|------|-----------------|---------------------|
| 批处理方式 | 等一批全部完成才处理下一批 | 动态增减序列，完成的立即退出 |
| GPU 利用率 | 低（等待慢序列） | 高（持续填充新请求） |
| 延迟 | 受最大序列拖累 | 更稳定，尾部延迟低 |
| 实现复杂度 | 简单 | 复杂（需要 Paged Attention 配合） |

> 代表性实现：vLLM、TensorRT-LLM、Text Generation Inference (HuggingFace TGI)。

### 2.9 vLLM 核心原理

vLLM = Paged Attention + Continuous Batching + Async Serving

1. **Paged Attention**：减少显存浪费，支持更大 batch size。
2. **Continuous Batching**：提高 GPU 利用率。
3. **Async Serving**：基于 FastAPI + asyncio 实现高吞吐服务。

```python
# vLLM 简单使用示例
from vllm import LLM, SamplingParams

llm = LLM(model="Qwen/Qwen2.5-7B-Instruct")
sampling_params = SamplingParams(temperature=0.7, top_p=0.9)

outputs = llm.generate(["解释一下什么是 Page Attention"], sampling_params=sampling_params)
for output in outputs:
    print(output.outputs[0].text)
```

---

## 三、实战场景题

### 3.1 RAG 系统整体架构设计

**问题：** 构建一个面向企业知识库的 RAG 系统，如何设计？

**回答要点：**

```text
数据层 → 文档解析器 → Chunking → Embedding → Milvus 向量库
                                            ↕
用户查询 → Query Rewriting → Embedding → Milvus 检索 → Rerank → LLM 生成
```

1. **文档处理流水线**：PDF/Word/HTML → Unstructured 解析器 → 按文档结构切分
2. **向量化服务**：部署 Embedding 模型（BGE/text2vec），批量生成向量
3. **Milvus 存储**：创建 Collection 存储 text + vector + metadata
4. **检索服务**：Query → Embedding → 检索 Top-K → Rerank → 过滤
5. **生成服务**：拼接 Context + Prompt → LLM 生成答案

### 3.2 如何优化检索召回率低于预期的问题？

**排查路径：**

1. Chunking 不合理 → 调整切分大小（256-1024 tokens），增加重叠窗口
2. Embedding 模型不适合领域 → 替换为领域专用 Embedding 或微调
3. 索引参数不匹配 → 检查 `nprobe`、`ef` 参数是否过小
4. 查询太短 → 使用 HyDE 或 Query Rewriting 扩充查询
5. 仅用向量检索 → 增加 BM25 稀疏检索，混合检索提升召回

### 3.3 如何保证数据实时性（写入后秒级可见）？

```python
# 使用毫秒级 flush，强制落盘
collection.flush()

# 设置一致性级别为 Strong
search_params = {
    "consistency_level": ConsistencyLevel.STRONG
}
```

> 注意：高频 flush 会影响写入性能。推荐用 Bounded Staleness + 合理刷新间隔。

### 3.4 如何处理亿级向量数据的索引构建？

1. **批量构建**：先全量写入，再统一建索引（`index_type="HNSW"`），而非逐条插入。
2. **多索引节点**：增加 IndexNode 数量并行构建。
3. **DiskANN**：如果内存无法容纳全部向量，使用 DiskANN 索引，利用 SSD 存储。
4. **分区策略**：按业务维度分区（如时间、用户 ID），只对活跃分区建索引。

### 3.5 如何设计 Embedding 服务的高可用架构？

```text
负载均衡器 (Nginx/HAProxy)
    ↕
Embedding Service (多副本, GPU Pods)
    ↕
缓存层 (Redis: <query, embedding> 缓存)
    ↕
Milvus 向量库 (一主多从)
```

- 使用缓存减少重复调用 Embedding API
- Embedding 服务无状态，可水平扩展
- Milvus 部署多副本，Proxy 层自动负载均衡

### 3.6 混合搜索（Hybrid Search）如何实现？

```python
# Milvus 2.4+ 支持 Hybrid Search
from pymilvus import AnnSearchRequest, RRFRanker

# 向量检索请求
vector_req = AnnSearchRequest(
    data=[query_vector],
    anns_field="embedding",
    param={"metric_type": "COSINE", "params": {"nprobe": 10}},
    limit=100
)

# 稀疏向量 / BM25 加权检索（配合 full-text index）
# 合并结果使用 RRF (Reciprocal Rank Fusion)
hybrid_results = collection.hybrid_search(
    reqs=[vector_req],
    rerank=RRFRanker(),
    limit=100,
    output_fields=["text", "metadata"]
)
```

### 3.7 在 Agent Skills 中如何集成 RAG？

```text
Agent Skill: "知识库查询"
  - 描述: "从企业内部知识库检索相关信息"
  - 输入: query (string), top_k (int)
  - 输出: retrieved_chunks (list)
  - 实现:
    1. 调用 Embedding Service 将 query 转换为向量
    2. 查询 Milvus Collection 获取 Top-K
    3. 调用 Reranker 重排序
    4. 返回 Top-3 最相关片段
    5. 安全过滤（去除敏感信息）
```

### 3.8 如何保障 Agent 代码执行的安全？

```text
Harness Engineering 实践:
1. 沙箱隔离: 使用 Docker 容器或 gVisor 运行 Agent 代码
2. 权限限制: 禁止 Agent 代码访问宿主机文件系统、网络
3. 资源限制: CPU 2核、内存 4GB、磁盘 1GB、无网络
4. 执行超时: 单次代码执行不超过 30 秒
5. 输出过滤: 检查输出是否包含敏感信息（API Key、密码等）
6. 审计日志: 记录所有 Agent 执行的代码和结果
```

---

## 四、手写代码题

### 4.1 连接 Milvus 并创建 Collection

```python
from pymilvus import connections, Collection, CollectionSchema, FieldSchema, DataType

# 1. 连接 Milvus
connections.connect(host="localhost", port="19530")

# 2. 定义 Schema
fields = [
    FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=True),
    FieldSchema(name="text", dtype=DataType.VARCHAR, max_length=65535),
    FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=768),
    FieldSchema(name="source", dtype=DataType.VARCHAR, max_length=255),
    FieldSchema(name="create_time", dtype=DataType.INT64)
]
schema = CollectionSchema(fields, description="RAG 知识库集合")

# 3. 创建 Collection
collection = Collection(name="knowledge_base", schema=schema)

# 4. 创建索引
index_params = {
    "metric_type": "COSINE",
    "index_type": "IVF_FLAT",
    "params": {"nlist": 1024}
}
collection.create_index(field_name="embedding", index_params=index_params)

# 5. 加载集合到内存
collection.load()

print(f"Collection '{collection.name}' created with {collection.num_entities} entities")
```

### 4.2 插入向量数据

```python
import numpy as np
from pymilvus import Collection

collection = Collection("knowledge_base")

# 准备数据
texts = [
    "Milvus 是一个高性能向量数据库",
    "RAG 是检索增强生成的缩写",
    "向量化是将非结构化数据转换为向量的过程"
]

# 模拟生成 768 维向量（实际应使用 Embedding 模型）
embeddings = np.random.random((len(texts), 768)).tolist()

# 插入数据
insert_result = collection.insert([
    texts,              # text 字段
    embeddings,         # embedding 字段
    ["blog", "wiki", "tutorial"],  # source 字段
    [1700000000, 1700000001, 1700000002]  # create_time 字段
])

print(f"插入成功, 影响行数: {len(insert_result.primary_keys)}")
```

### 4.3 向量搜索

```python
from pymilvus import Collection, AnnSearchRequest, RRFRanker

collection = Collection("knowledge_base")
collection.load()  # 确保集合已加载

# 模拟查询向量（实际应使用相同的 Embedding 模型）
query_text = "什么是向量数据库"
query_vector = np.random.random(768).tolist()  # 实际: embedding_model.encode(query_text)

# 搜索参数
search_params = {
    "metric_type": "COSINE",
    "params": {"nprobe": 10}
}

# 执行搜索
results = collection.search(
    data=[query_vector],
    anns_field="embedding",
    param=search_params,
    limit=5,
    output_fields=["text", "source", "create_time"]
)

# 打印结果
for i, hits in enumerate(results):
    print(f"\n查询 {i+1} 的结果:")
    for j, hit in enumerate(hits):
        print(f"  Rank {j+1}: text={hit.entity.get('text')}, "
              f"score={hit.score:.4f}, source={hit.entity.get('source')}")
```

### 4.4 完整的 RAG Pipeline 实现

```python
from typing import List, Dict
import numpy as np
from pymilvus import Collection
from dataclasses import dataclass


@dataclass
class Document:
    text: str
    metadata: Dict


class EmbeddingService:
    """模拟 Embedding 服务"""
    def embed(self, texts: List[str]) -> List[List[float]]:
        # 实际应调用 Embedding 模型
        return np.random.random((len(texts), 768)).tolist()


class MilvusRetriever:
    def __init__(self, collection_name: str, embedding_dim: int = 768):
        self.collection = Collection(collection_name)
        self.embedding_service = EmbeddingService()

    def retrieve(self, query: str, top_k: int = 5) -> List[Document]:
        # 1. 查询向量化
        query_vector = self.embedding_service.embed([query])[0]

        # 2. 向量检索
        results = self.collection.search(
            data=[query_vector],
            anns_field="embedding",
            param={"metric_type": "COSINE", "params": {"nprobe": 10}},
            limit=top_k,
            output_fields=["text", "source", "create_time"]
        )

        # 3. 解析结果
        documents = []
        for hits in results:
            for hit in hits:
                documents.append(Document(
                    text=hit.entity.get("text"),
                    metadata={
                        "source": hit.entity.get("source"),
                        "score": hit.score,
                        "timestamp": hit.entity.get("create_time")
                    }
                ))
        return documents


class Reranker:
    """模拟重排序器"""
    def rerank(self, query: str, documents: List[Document]) -> List[Document]:
        # 实际应使用交叉编码器
        # 这里按分数降序排列作为模拟
        return sorted(documents, key=lambda d: d.metadata["score"], reverse=True)


class LLMService:
    """模拟 LLM 服务"""
    def generate(self, context: str, query: str) -> str:
        # 实际应调用 LLM API
        return f"基于检索到的 {len(context)} 字上下文，对 '{query}' 的答案是：..."


class RAGPipeline:
    def __init__(self, retriever: MilvusRetriever, reranker: Reranker, llm: LLMService):
        self.retriever = retriever
        self.reranker = reranker
        self.llm = llm

    def query(self, user_query: str) -> Dict:
        # 1. 检索
        docs = self.retriever.retrieve(user_query, top_k=10)

        # 2. 重排序
        ranked_docs = self.reranker.rerank(user_query, docs)
        top_docs = ranked_docs[:3]

        # 3. 构建上下文
        context = "\n\n".join([doc.text for doc in top_docs])

        # 4. LLM 生成
        answer = self.llm.generate(context, user_query)

        return {
            "answer": answer,
            "sources": [doc.metadata for doc in top_docs]
        }
```

### 4.5 使用 Partition Key 优化数据隔离

```python
# 创建带 Partition Key 的 Collection
fields = [
    FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=True),
    FieldSchema(name="text", dtype=DataType.VARCHAR, max_length=65535),
    FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=768),
    FieldSchema(name="tenant_id", dtype=DataType.INT64, is_partition_key=True),
    FieldSchema(name="doc_type", dtype=DataType.VARCHAR, max_length=64),
]

schema = CollectionSchema(fields, description="多租户知识库")

collection = Collection(name="multitenant_kb", schema=schema)

# 插入数据时自动根据 tenant_id 路由到对应分区
collection.insert([
    texts,
    embeddings,
    [1001, 1002, 1001],  # tenant_id
    ["pdf", "word", "pdf"]  # doc_type
])

# 查询时指定 partition key 过滤，自动只查对应分区的数据
collection.load()
results = collection.search(
    data=[query_vector],
    anns_field="embedding",
    param=search_params,
    limit=10,
    expr="tenant_id == 1001"
)
```

### 4.6 Milvus 批量写入与性能优化

```python
import numpy as np
from pymilvus import Collection, BulkInsertState

collection = Collection("knowledge_base")

# 批量生成数据（推荐一次 5000-10000 条）
BATCH_SIZE = 5000
TOTAL = 100000

for i in range(0, TOTAL, BATCH_SIZE):
    batch_texts = [f"文档内容 {j}" for j in range(i, i + BATCH_SIZE)]
    batch_embeddings = np.random.random((BATCH_SIZE, 768)).tolist()

    mr = collection.insert([batch_texts, batch_embeddings])
    print(f"已写入 {i + BATCH_SIZE}/{TOTAL} 条, 耗时: ...")

# 全部写入后统一建索引（比逐条建索引快百倍）
collection.create_index(
    field_name="embedding",
    index_params={"metric_type": "COSINE", "index_type": "IVF_FLAT", "params": {"nlist": 1024}}
)

collection.load()
print(f"最终数据量: {collection.num_entities}")
```

### 4.7 使用 vLLM 部署推理服务

```python
# vLLM 启动推理服务（命令行）
# vllm serve Qwen/Qwen2.5-7B-Instruct --port 8000 --gpu-memory-utilization 0.9

# Python 客户端调用
import requests
import json

url = "http://localhost:8000/v1/chat/completions"
payload = {
    "model": "Qwen/Qwen2.5-7B-Instruct",
    "messages": [
        {"role": "system", "content": "你是一个知识库助手"},
        {"role": "user", "content": "什么是 Page Attention？"}
    ],
    "temperature": 0.7,
    "max_tokens": 512,
    "stream": True
}

response = requests.post(url, json=payload, stream=True)
for line in response.iter_lines():
    if line:
        data = json.loads(line.decode("utf-8").lstrip("data: "))
        if "choices" in data:
            print(data["choices"][0]["delta"].get("content", ""), end="")
```

### 4.8 Embedding 批处理与缓存策略

```python
import redis
import numpy as np
from functools import lru_cache


class CachedEmbeddingService:
    def __init__(self, model_name: str = "BAAI/bge-large-zh-v1.5"):
        self.model_name = model_name
        self.cache = redis.Redis(host="localhost", port=6379, db=0)
        # self.model = SentenceTransformer(model_name)  # 实际加载模型

    def embed(self, texts: List[str], batch_size: int = 32) -> List[List[float]]:
        results = []
        uncached_texts = []
        uncached_indices = []

        for i, text in enumerate(texts):
            cached = self.cache.get(f"emb:{hash(text)}")
            if cached is not None:
                # 从缓存取
                results.append(np.frombuffer(cached, dtype=np.float32).tolist())
            else:
                uncached_texts.append(text)
                uncached_indices.append(i)
                results.append(None)  # placeholder

        # 批量 Embedding 未缓存的文本
        if uncached_texts:
            # 分批处理
            for start in range(0, len(uncached_texts), batch_size):
                batch = uncached_texts[start:start + batch_size]
                # batch_vectors = self.model.encode(batch)  # 实际执行
                batch_vectors = np.random.random((len(batch), 768)).astype(np.float32)

                for j, vec in enumerate(batch_vectors):
                    idx = uncached_indices[start + j]
                    results[idx] = vec.tolist()
                    # 写入缓存
                    self.cache.setex(
                        f"emb:{hash(uncached_texts[start + j])}",
                        3600,  # 1 小时过期
                        vec.tobytes()
                    )

        return results
```

---

## 五、系统设计题

### 5.1 设计一个支持千万级文档的 RAG 知识库系统

**核心架构：**

```text
┌─────────────┐    ┌──────────────┐    ┌─────────────┐
│ 文档处理服务 │───→│ Embedding     │───→│ Milvus 集群  │
│ (Unstructured) │    │ Service (GPU) │    │ (分片 + 副本) │
└─────────────┘    └──────────────┘    └─────────────┘
                                                ↕
┌─────────────┐    ┌──────────────┐    ┌─────────────┐
│ 用户请求     │───→│ RAG Orchestrator │───→│ Reranker    │
│ API Gateway │    │ (编排层)       │    │ Service     │
└─────────────┘    └──────────────┘    └─────────────┘
                          ↕
                    ┌──────────────┐
                    │ LLM Service   │
                    │ (vLLM + Qwen) │
                    └──────────────┘
```

**关键设计要点：**

1. **分片策略**：按文档类型/时间分 Collection，或使用 Partition Key（建议 50-200GB/分片）
2. **索引策略**：HNSW 索引，`M=16`, `efConstruction=200`, `ef=100`
3. **缓存层**：查询结果缓存（Redis），减少重复检索
4. **熔断与降级**：检索超时 500ms，降级到 BM25 全文搜索
5. **数据同步**：CDC 机制保证文档更新实时同步到 Milvus
6. **监控告警**：检索延迟 P99 > 1s 告警，写入 QPS 监控

### 5.2 设计一个面向多租户的向量检索平台

| 需求 | 解决方案 |
|------|----------|
| 租户隔离 | Partition Key = tenant_id，数据物理隔离 |
| 限流控制 | Proxy + Rate Limiter，每个租户 QPS 配额 |
| 资源隔离 | 每个租户独立 Collection，或共享 Collection + 表达式过滤 |
| 存储计算分离 | QueryNode 计算资源按需分配，数据统一存储 |
| 计费计量 | 记录每个租户的存储量 + 查询量 |

```python
# 多租户索引设计
fields = [
    FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=True),
    FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=768),
    FieldSchema(name="tenant_id", dtype=DataType.INT64, is_partition_key=True),
]
schema = CollectionSchema(fields, description="多租户向量库")
```

### 5.3 设计一个低延迟的 RAG 检索系统（P99 < 200ms）

**优化路径：**

1. **索引选型**：HNSW（M=16, ef=64） vs IVF_FLAT（nprobe=8）
2. **QueryNode 水平扩展**：增加 QueryNode 副本分散查询压力
3. **预热（Warm-up）**：系统启动时预加载全部数据
4. **缓存策略**：L1（内存热点缓存）+ L2（Redis 查询缓存）
5. **连接池优化**：Proxy 与 QueryNode 间维持长连接池
6. **Embedding 服务优化**：GPU 推理 + ONNX Runtime 加速

```text
目标配置参考:
- 100 万向量，768 维
- HNSW 索引，ef=64, M=16
- 2 个 QueryNode，各 32GB 内存
- P99 延迟 ~50ms
```

---

## 六、常见坑点与最佳实践

### 6.1 索引类型选择常见问题

| 坑点 | 现象 | 解决方案 |
|------|------|----------|
| IVF_FLAT 的 `nlist` 过小 | 召回率低 | `nlist ≈ 4*sqrt(N)`，如 100 万数据设 4000 |
| IVFFLAT 的 `nprobe` 过小 | 检索精度差 | 增大到 8-32，平衡延迟与精度 |
| HNSW 的 `M` 参数过大 | 内存爆满 | M 每增加 1，内存约增 10MB/100万向量 |
| 索引未 Load | 报错 `collection not loaded` | 搜索前必须调用 `collection.load()` |
| 索引类型与度量不匹配 | 建索引失败 | Cosine 不支持 IP 索引 |

### 6.2 RAG 系统常见问题

| 陷阱 | 表现 | 解决方法 |
|------|------|----------|
| Chunk 过大 | 上下文超出 LLM 窗口 | 限制 512-1024 tokens |
| Chunk 过小 | 语义不完整 | 增加重叠窗口（overlap=10%-20%） |
| Embedding 模型不匹配 | 检索结果相关性差 | 选同领域/语言的 Embedding 模型 |
| 检索 Top-K 全部喂给 LLM | Attention 稀释 | 限制 Top-K ≤ 5，配合 Rerank |
| 未过滤无关文档 | 干扰 LLM 判断 | 加相似度阈值（score > 0.7） |

### 6.3 性能优化最佳实践

| 场景 | 最佳实践 |
|------|----------|
| 写入数据量 > 100 万 | 先批量写入，最后统一建索引 |
| 需要实时写入 + 实时搜索 | 使用 Bounded Staleness 一致性 |
| 频繁查询 | 确保 `collection.load()` 在查询前执行 |
| 大规模部署 | 启用 Milvus 集群模式，分片数 ≥ 2 |
| 跨机房部署 | 使用消息队列同步数据（Pulsar/Kafka） |

### 6.4 LLM 推理部署坑点

| 坑点 | 说明 | 解决 |
|------|------|------|
| KV Cache OOM | 大 batch size + 长序列 | 启用 Page Attention，降低 max_batch_size |
| Prefill 慢 | 长输入预填充慢 | 使用 Flash Attention 加速 |
| Decode 慢 | 逐 token 生成 | 启用 speculative decoding（推测解码） |
| 显存碎片 | 分配/释放导致 | 使用 vLLM / TensorRT-LLM |

---

## 七、面试回答模板（Top 5）

### 7.1 "请介绍一下 Milvus 的架构"

> Milvus 采用 **存储计算分离** 的架构设计，核心分为四层：
>
> 第一层是 **接入层（Proxy）**，负责请求路由和认证。第二层是 **协调层（Coordinator）**，包括管理 DDL/DCL 的 Root Coord、管理查询的 Query Coord、管理数据的 Data Coord，它们是无状态的。第三层是 **工作节点层（Worker Node）**，包括执行查询的 QueryNode、写入数据的 DataNode、构建索引的 IndexNode，这些是有状态的。第四层是 **存储层**，元数据存在 etcd，日志存在 Pulsar，数据存在 MinIO/S3。
>
> 这种架构的优点是存储与计算解耦，支持独立扩缩容，并有良好的容灾能力。

### 7.2 "RAG 与传统 Fine-tuning 有什么区别？"

> RAG 和 Fine-tuning 解决的是不同的问题。
>
> RAG 是 **检索 + 生成** 的架构，从外部知识库检索相关文档注入 LLM 上下文。优点是 **无需训练**、知识可实时更新、可回溯来源。缺点是增加检索延迟、依赖检索质量。
>
> Fine-tuning 是 **参数微调**，让模型学习特定领域的知识或格式。优点是 **深度定制**、推理时无额外检索开销。缺点是需要训练数据、知识固化在模型参数中、无法实时更新。
>
> 在实际应用中，两者 **互补使用** 效果最好：Fine-tuning 教会模型回答的格式和风格，RAG 提供最新的业务知识。

### 7.3 "如何评估 RAG 系统的效果？"

> RAG 评估分两个维度：
>
> **检索评估：**
> - Recall@K：检索结果是否包含正确答案
> - MRR (Mean Reciprocal Rank)：正确答案的排名
> - NDCG (Normalized Discounted Cumulative Gain)：检索排序质量
>
> **生成评估：**
> - Faithfulness（忠实度）：生成是否基于检索结果，有无幻觉
> - Relevance（相关性）：回答是否与问题相关
> - Context Recall（上下文召回）：生成是否遗漏了关键上下文信息
>
> 推荐评估框架：RAGAS、TruLens、LangSmith。

### 7.4 "请说明 Milvus 向量搜索的原理"

> Milvus 的向量搜索本质是 **最近邻搜索（ANN, Approximate Nearest Neighbor）**。当处理海量数据时，精确搜索（kNN）不可行，因此采用近似算法。
>
> 以 IVF_FLAT 为例：建索引时用 K-means 对向量聚类，生成 nlist 个簇。搜索时，先找到距离最近的前 nprobe 个簇中心，然后在这几个簇内做精确的 Bruteforce 搜索。速度相比暴力搜索提升 100-1000 倍。
>
> HNSW 则是构建多层图，逐层搜索，速度更快，精度更高，适合高 QPS 场景。
>
> 关键点：搜索是 **平衡精度与速度** 的过程，通过调整 nprobe/ef 参数来控制。

### 7.5 "如何在生产环境部署 Milvus？"

> 生产环境推荐使用 **Milvus Operator on Kubernetes** 或 **Milvus Cloud**。部署要点：
>
> 1. **配置计算资源**：QueryNode 建议 16 核 32GB 起，IndexNode 按需（建索引时是 CPU 密集）
> 2. **存储选型**：独占 MinIO/S3 集群，etcd 集群 3 节点
> 3. **网络规划**：内部网络 10Gbps，节点间低延迟
> 4. **监控与告警**：配置 Prometheus + Grafana 监控面板
> 5. **备份策略**：定期通过 `milvus-backup` 工具备份
> 6. **扩缩容策略**：根据 QPS 监控自动扩缩 QueryNode，数据增长时扩展 DataNode

---

## 八、快速查漏补缺 Checklist

### 8.1 Milvus 核心

- [ ] 我能说出 Milvus 四大组件（Proxy, Coord, WorkerNode, Storage）
- [ ] 我知道 IVF_FLAT 和 HNSW 的区别与选型依据
- [ ] 我能解释 Cosine, L2, IP 三种度量方式的区别
- [ ] 我知道四种一致性级别及选用场景
- [ ] 我会写 Python 连接 Milvus 并执行 CRUD
- [ ] 我知道 Partition Key 的作用和用法
- [ ] 我知道存储计算分离架构的优势
- [ ] 我能解释 Milvus 如何保证高可用

### 8.2 RAG 核心

- [ ] 我能画出 RAG 系统架构图
- [ ] 我知道五种 Chunking 策略及选型
- [ ] 我能解释 Embedding 的作用和常见模型
- [ ] 我知道 Rerank 的作用和使用场景
- [ ] 我知道 RAG 五大优化维度
- [ ] 我能解释 HyDE 和 Multi-Query 的原理
- [ ] 我了解 RAGAS 评估框架
- [ ] 我会写完整的 RAG Pipeline 代码

### 8.3 LLM 推理优化

- [ ] 我能区分 Prefill 和 Decode 两个阶段
- [ ] 我知道 KV Cache 为什么重要
- [ ] 我能解释 Page Attention 的原理
- [ ] 我能解释 Continuous Batching 的优化点
- [ ] 我知道 vLLM 的核心优势
- [ ] 我会用 vLLM 部署推理服务

### 8.4 AI 工程实践

- [ ] 我知道 Harness Engineering 的七种核心能力
- [ ] 我能说明 Sandbox 在 Agent 安全中的作用
- [ ] 我知道 Multi-Agent 与 Agent Skills 的区别
- [ ] 我能设计多租户向量检索方案
- [ ] 我会优化 RAG 检索延迟
- [ ] 我知道如何保障 Agent 代码执行安全

---

> 🎯 **面试要点总结**：Milvus 面试核心围绕 **架构原理（35%）**+ **索引与搜索（30%）**+ **RAG 系统设计（25%）**+ **LLM 推理优化（10%）**。准备时先确保能徒手画出架构图，能写出完整的 RAG Pipeline 代码，然后深入理解索引原理和优化参数。对于高级岗位，LLM 推理优化的 Prefill/Decode、Page Attention 是区分度考点。
