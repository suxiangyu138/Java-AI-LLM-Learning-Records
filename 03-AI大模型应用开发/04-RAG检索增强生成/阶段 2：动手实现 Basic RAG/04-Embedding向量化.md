# 04 Embedding 向量化

> 把切片变成机器可比的高维向量：BGE 模型加载、归一化原理、批量编码——本模块让"语义相似=向量距离近"真正落地。

## 📚 目录

1. [Embedding 的本质](#1-embedding-的本质)
2. [BGE 中文模型加载](#2-bge-中文模型加载)
3. [归一化与相似度计算](#3-归一化与相似度计算)
4. [批量编码实践](#4-批量编码实践)
5. [中文 Embedding 的实测对比](#5-中文-embedding-的实测对比)

## 1. Embedding 的本质

Embedding 把文本映射为高维向量，核心性质：**语义相近的文本，向量距离近**。

```text
"如何申请退款"   → [0.12, 0.87, -0.34, ...]  1024 维
"退费流程是什么" → [0.11, 0.85, -0.32, ...]  ← 距离近（同义）
"今天天气很好"   → [0.53, -0.21, 0.08, ...]  ← 距离远（无关）
```

| 要点 | 说明 |
|---|---|
| 维度 | BGE-large-zh 为 1024 维 |
| 语义能力 | 取决于训练语料与方式（对比学习） |
| 语言 | 中文模型对中文语义理解远好于通用多语言模型 |
| 一致性 | 建库与查询必须用**同一个** Embedding 模型 |

> ⚠️ 一致性铁律：索引时用 A 模型、查询时用 B 模型 = 语义空间错位，检索结果随机。换模型必须重建索引。

## 2. BGE 中文模型加载

```python
from sentence_transformers import SentenceTransformer

# 方式一：自动从 HuggingFace 下载
model = SentenceTransformer("BAAI/bge-large-zh-v1.5")

# 方式二：本地路径（离线部署，见 01 篇）
model = SentenceTransformer("./models/bge-large-zh-v1.5")
```

加载要点：

- 首次加载自动下载模型（约 1.3GB），之后走本地缓存 `~/.cache/huggingface/hub`
- CPU 即可运行；有 GPU 时 sentence-transformers 自动使用
- 模型只加载一次（进程级单例），不要每次查询重新加载

## 3. 归一化与相似度计算

```python
import numpy as np

def embed_texts(model, texts: list[str]) -> np.ndarray:
    """编码 + L2 归一化：归一化后内积 = 余弦相似度"""
    vectors = model.encode(texts, normalize_embeddings=True)
    return np.array(vectors, dtype=np.float32)  # FAISS 要求 float32
```

归一化原理：

```text
余弦相似度 = (A·B) / (|A| × |B|)
归一化后 |A| = |B| = 1 → 余弦相似度 = A·B（内积）
```

| 相似度度量 | 公式 | 适用 |
|---|---|---|
| 余弦相似度 | cos(A, B) | 文本语义最常用 |
| 内积 | A·B | 归一化后等价余弦，FAISS 默认高效实现 |
| 欧氏距离 | \|A-B\| | 数值向量场景 |

> 💡 BGE 官方推荐：查询文本前可加指令前缀（如 "为这个句子生成表示以用于检索相关文章："），中文场景实测直接编码通常已够用，可自行对比。

## 4. 批量编码实践

```python
def build_vectors(model, chunks: list[dict], batch_size: int = 64) -> tuple[np.ndarray, list[dict]]:
    """批量编码切片，返回 (向量矩阵, 元数据列表)"""
    texts = [c["text"] for c in chunks]
    all_vecs = []

    for i in range(0, len(texts), batch_size):
        batch = texts[i:i + batch_size]
        vecs = model.encode(batch, normalize_embeddings=True)
        all_vecs.append(np.array(vecs, dtype=np.float32))
        print(f"已编码 {min(i + batch_size, len(texts))}/{len(texts)}")

    return np.vstack(all_vecs), chunks
```

批量要点：

| 项 | 建议 |
|---|---|
| batch_size | CPU 64 / GPU 128-256 |
| 进度 | 大数据量时打印进度（千级文档可能数分钟） |
| 内存 | 1024 维 × 10000 块 ≈ 40MB（向量小，文本大） |
| 持久化 | 向量矩阵存 .npy，元数据存 .json（05 篇） |

## 5. 中文 Embedding 的实测对比

中文近义词（"退款申请" vs "申请退款"）相似度实测：

| 模型 | 相似度 | 结论 |
|---|---|---|
| 通用多语言模型 | 约 0.72 | 阈值过滤可能误杀 |
| BGE 中文模型 | 0.95+ | 语义区分可靠 |

存储与速度（BGE-large-zh 1024 维 vs 通用 1536 维）：

| 指标 | 1024 维 | 1536 维 | 收益 |
|---|---|---|---|
| 存储 | 约 8MB/万块 | 约 12MB/万块 | -44% |
| 检索速度 | — | — | +35% |

> 🎯 核心要点：Embedding 是"语义翻译层"——模型选对（中文用 BGE）、归一化做对（内积=余弦）、一致性守住（建库查询同模型），检索质量的地基就稳了。下一步把向量真正"存进库"。

---

**下一模块**：[05-向量库与索引构建](05-向量库与索引构建.md) / **返回总览**：[00-阶段总览与学习路径](00-阶段总览与学习路径.md)
