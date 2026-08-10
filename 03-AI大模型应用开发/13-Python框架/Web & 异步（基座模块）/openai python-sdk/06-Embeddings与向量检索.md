# 06 - Embeddings 与向量检索

> 定位：RAG 的向量化环节——"embeddings.create 一行出向量"——模型选型、批量姿势、与向量库的完整链路——"SDK 管'文本→向量'，向量库管'向量→相似'"

---

## 📚 目录

1. [嵌入 API：一行出向量](#1-嵌入-api一行出向量)
2. [模型与维度选型](#2-模型与维度选型)
3. [批量嵌入三原则](#3-批量嵌入三原则)
4. [与向量库的完整链路](#4-与向量库的完整链路)
5. [嵌入一致性纪律](#5-嵌入一致性纪律)
6. [常见坑](#6-常见坑)
7. [练习 5 题](#7-练习-5-题)

---

## 1. 嵌入 API：一行出向量

```python
client = OpenAI()
resp = client.embeddings.create(
    model="text-embedding-3-small",
    input="MySQL 的 InnoDB 使用 B+ 树索引",   # 文本（或文本列表）
)
vector = resp.data[0].embedding   # [0.0123, -0.0456, ...] 浮点列表
print(len(vector))                # 1536（默认维度）
```

**嵌入是什么**：文本 → 向量（浮点数组），**语义相近的文本向量也相近**（余弦/内积距离小）——这是 RAG 检索的数学基础。**返回结构**：`resp.data[0].embedding` 是向量、`resp.usage` 是 token 账单（嵌入也按 token 计费）。

**注意数据类型**：embedding 是 **Python 浮点列表**——喂给向量库（numpy/faiss）时转 float32 数组：`np.array(vector, dtype='float32')`（faiss 铁律，chromadb 自动处理）——"**SDK 出列表，向量库吃数组，中间一次转换**"。

## 2. 模型与维度选型

| 模型 | 默认维度 | 特性 |
|------|:---:|------|
| text-embedding-3-small | 1536 | 默认选择：性价比高 |
| text-embedding-3-large | 3072 | 质量最高、成本高 |
| text-embedding-ada-002（旧） | 1536 | 老模型，新项目不用 |

**text-embedding-3 系列的杀手锏：`dimensions` 参数**——创建时指定任意维度（≤ 最大维度），**服务端降维**（Matryoshka 机制）：

```python
resp = client.embeddings.create(
    model="text-embedding-3-small",
    input=texts,
    dimensions=384,        # 降维：更省内存，质量略降
)
```

**"dimensions=384 与向量库默认维度（chromadb 的 ONNX 384/faiss 自定义）对齐"**——降维让存储内存下降（384 vs 1536，4 倍差距），检索质量损失通常可接受——"**先测后降：384 维够用就别上 1536**"（与 chromadb 04 篇/faiss 09 篇内存账本呼应）。

**选型直觉**：**small 是默认（性价比），large 是质量敏感场景（检索精度要求极高）**；中文效果两者都依赖文本质量——"**嵌入模型的选型先看性价比，再看评测**"。

**评测方法**（与向量库体系同源）：取 100 个真实问答对，嵌入后用向量库检索，算 **top-5 命中率**（正确答案是否在召回内）——small vs large 各跑一遍对比——"**差一个点要不要多花 5 倍钱，评测数字说了算**"；同一批测试集固定，换模型/调参都回测——"**嵌入选型是评测驱动，不是价格驱动**"。

## 3. 批量嵌入三原则

`input` 支持**文本列表**一次嵌入多条：

```python
texts = [c.text for c in chunks]           # 几千条分块
resp = client.embeddings.create(model="text-embedding-3-small", input=texts)
vectors = [d.embedding for d in resp.data]
```

**批量三原则**：其一，**一次一批（几百到几千条）**——单条循环调用是"逐条计费 + 逐条延迟"的差姿势；其二，**分批控制与限速**——大批量分片（如每批 1000 条）+ 重试兜底（09 篇限流）；其三，**批量嵌入的返回顺序与输入一一对应**（`resp.data[i]` 对应 `input[i]`）——"**顺序对应是批量的契约，排序别乱**"。

**嵌入是 RAG 管道的第一成本项**（按 token 计费），批量姿势省的不只是时间还有重试成本——"**嵌入批次与向量库批量入库（chromadb 05 篇）对齐，一次管道两段批量**"。

**批量 + 异步的叠加**：离线建库场景（几十万条分块）用 AsyncOpenAI + 信号量并发嵌入（07 篇），再分批 upsert 进向量库——"**嵌入并发 + 入库批量 = 建库管道的吞吐组合拳**"；并发嵌入注意限流（07 篇信号量纪律），别把 TPM 撞穿——"**建库是离线任务，但要按在线额度规划，撞墙只会更慢**"。

## 4. 与向量库的完整链路

**SDK 与向量库的分工**：SDK 管"文本 → 向量"，向量库管"向量 → 相似检索"。完整链路：

```python
import numpy as np
import chromadb
from openai import OpenAI

client = OpenAI()
chroma = chromadb.PersistentClient(path="./db")
collection = chroma.get_or_create_collection(
    name="kb", metadata={"hnsw:space": "cosine"})

# ① 批量嵌入（SDK）
texts = ["MySQL 用 B+ 树索引", "Redis 是内存数据库"]
resp = client.embeddings.create(model="text-embedding-3-small", input=texts)
vectors = [d.embedding for d in resp.data]

# ② 向量 + 原文 + 元数据入库（向量库，自带向量绕过 chroma 嵌入函数）
collection.add(ids=["k1", "k2"], documents=texts,
               embeddings=vectors, metadatas=[{"topic": "数据库"}] * 2)

# ③ 查询：先嵌入查询文本，再检索
qr = client.embeddings.create(model="text-embedding-3-small", input="什么数据库用 B+ 树")
res = collection.query(query_embeddings=qr.data[0].embedding, n_results=1)
print(res["documents"])    # [['MySQL 用 B+ 树索引']]
```

**链路三要点**：其一，**手传 embeddings 时向量库跳过自己的嵌入函数**——chromadb 用 `query_embeddings`、faiss 直接 `search(np.array(vec, 'float32'))`；其二，**模型一致性**——入库与查询必须同一嵌入模型（chromadb 04 篇"嵌入一致性"纪律）；其三，**cosine 语义**——chromadb 配 `hnsw:space: cosine`；faiss 场景要先归一化 + IP（faiss 04 篇）——"**SDK 出向量，向量库管相似——中间唯一的契约是'同一个模型'"**。

## 5. 嵌入一致性纪律

1. **入库与查询同模型**——模型 A 入库、模型 B 查询 = 向量空间不同，检索全是噪声——"**选模型是选'语义母语'，换了母语全库重灌**"；
2. **同模型同维度**——`dimensions` 参数一致性；维度不同向量库直接报错（InvalidDimension）；
3. **归一化纪律**——faiss 场景入库/查询都归一化；chromadb 内置处理；
4. **缓存嵌入**——同一文本重复嵌入浪费 token；**嵌入结果落缓存**（文本哈希 → 向量），增量更新只嵌新文本——"**嵌入是 RAG 的第一成本，缓存是第一省钱**"（09 篇成本治理）。

## 6. 常见坑

1. **单条循环嵌入**——逐条延迟 + 逐条计费；批量 input 列表。
2. **embedding 是列表不是数组**——喂 faiss 要 `np.array(dtype='float32')`。
3. **dimensions 乱降**——与向量库已建集合维度不一致；建集合前定好。
4. **入库查询不同模型**——检索噪声；一致性铁律。
5. **批量顺序错位**——`resp.data[i]` 与 `input[i]` 一一对应，别用 dict 去重搞乱。
6. **不缓存重复嵌入**——重复文本反复计费；文本哈希缓存。
7. **超长文本一刀切嵌入**——超过模型 token 上限报错；先分块再嵌入（unstructured 分块，07 篇）。

## 7. 练习 5 题

1. 嵌入返回结构？为什么向量要转 float32？
2. dimensions 参数的原理与价值？"先测后降"？
3. 批量嵌入三原则？顺序对应为什么是契约？
4. 完整链路三步？"中间唯一的契约是同一个模型"？
5. 一致性四条纪律？为什么嵌入是 RAG 第一成本？

> 🎯 **核心要点**：Embeddings = **embeddings.create 一行（input 列表批量 + dimensions 降维）+ 向量库链路（同模型 + 手传向量）+ 一致性纪律四条**——"SDK 管'文本→向量'，向量库管'向量→相似'——模型一致是契约，缓存是第一省钱"。

---

**下一模块**：[07-异步与并发.md](07-异步与并发.md) / **返回总览**：[00-openai-python-sdk总览.md](00-openai-python-sdk总览.md)
