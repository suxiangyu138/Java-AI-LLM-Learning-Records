# Embedding 向量化：从原理到代码实践

> **核心摘要**：本文从 Embedding 的核心原理出发，通过完整的 Python 代码示例，覆盖 API 调用、相似度计算、批量处理以及最小向量检索系统的实现，帮助读者快速掌握 Embedding 的工程实践能力。

> **前置阅读**：[[AI-Embedding-快速吃透]]、[[Python基础]]

---

## 目录

1. [什么是 Embedding](#1-什么是-embedding)
2. [核心原理](#2-核心原理)
3. [主流 Embedding 模型](#3-主流-embedding-模型)
4. [API 调用实战](#4-api-调用实战)
5. [相似度检索最小实践](#5-相似度检索最小实践)
6. [关键实践要点](#6-关键实践要点)
7. [常见问题](#7-常见问题)
8. [核心要点回顾](#8-核心要点回顾)
9. [参考资料](#9-参考资料)

---

## 1. 什么是 Embedding

**Embedding（嵌入）** 是将文本、图像等非结构化数据映射为**固定长度的浮点数向量**，使得语义相近的内容在向量空间中距离也相近。

```
"今天天气真好"  →  [0.12, -0.34, 0.56, ..., 0.78]  (1536维)
"今日阳光明媚"  →  [0.11, -0.33, 0.58, ..., 0.77]  (向量接近)
"数据库连接超时" →  [-0.45, 0.67, -0.23, ..., -0.12] (向量远离)
```

---

## 2. 核心原理

### 2.1 向量空间中的语义

Embedding 模型将语义相近的文本映射到向量空间中相邻的区域，形成语义簇：

- 天气相关文本聚集在一个区域
- 数据库相关文本聚集在另一个区域
- 两个区域之间的距离反映了语义差异

### 2.2 相似度度量

```python
import numpy as np

def cosine_similarity(a, b):
    """余弦相似度 — 最常用的向量相似度量"""
    return np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b))

def euclidean_distance(a, b):
    """欧几里得距离"""
    return np.linalg.norm(np.array(a) - np.array(b))

# 示例
weather_1 = [0.12, -0.34, 0.56]
weather_2 = [0.11, -0.33, 0.58]
db_error   = [-0.45, 0.67, -0.23]

print(cosine_similarity(weather_1, weather_2))  # 0.998 (高相似)
print(cosine_similarity(weather_1, db_error))    # -0.342 (低相似)
```

> **重点**：余弦相似度取值范围为 [-1, 1]，值越接近 1 表示语义越相似。它是 RAG 和向量检索中最常用的度量方式。

---

## 3. 主流 Embedding 模型

| 模型 | 维度 | 中文支持 | 特点 |
|------|------|----------|------|
| OpenAI text-embedding-3-small | 1536 | 一般 | 商业模型，效果好 |
| BGE-M3 (BAAI) | 1024 | 优秀 | 开源，多语言支持 |
| text2vec-large-chinese | 1024 | 优秀 | 中文专用 |
| m3e-base | 768 | 优秀 | 中文轻量，适合本地部署 |

---

## 4. API 调用实战

### 4.1 OpenAI Embedding API

```python
from openai import OpenAI

client = OpenAI(api_key="sk-xxx")

def get_embedding(text, model="text-embedding-3-small"):
    text = text.replace("\n", " ")
    return client.embeddings.create(
        input=[text], model=model
    ).data[0].embedding

# 使用
vec = get_embedding("Spring Boot 自动配置原理")
print(len(vec))  # 1536
```

### 4.2 本地模型（Ollama）

```python
# 先拉取模型：ollama pull bge-m3
import requests

def get_ollama_embedding(text, model="bge-m3"):
    resp = requests.post("http://localhost:11434/api/embeddings", json={
        "model": model,
        "prompt": text
    })
    return resp.json()["embedding"]
```

### 4.3 批量处理

```python
def batch_embed(texts, batch_size=100):
    """批量生成 Embedding，减少 API 调用次数"""
    embeddings = []
    for i in range(0, len(texts), batch_size):
        batch = texts[i:i + batch_size]
        resp = client.embeddings.create(
            input=batch, model="text-embedding-3-small"
        )
        embeddings.extend([d.embedding for d in resp.data])
    return embeddings
```

---

## 5. 相似度检索最小实践

以下实现一个最简的向量存储与检索系统，无需依赖第三方向量数据库：

```python
import numpy as np

class SimpleVectorStore:
    """最简向量存储与检索"""

    def __init__(self):
        self.texts = []
        self.vectors = []

    def add(self, text, vector):
        self.texts.append(text)
        self.vectors.append(vector)

    def search(self, query_vector, top_k=5):
        """余弦相似度检索 Top-K"""
        if not self.vectors:
            return []
        matrix = np.array(self.vectors)
        query = np.array(query_vector)
        # 归一化后点积 = 余弦相似度
        matrix_norm = matrix / np.linalg.norm(matrix, axis=1, keepdims=True)
        query_norm = query / np.linalg.norm(query)
        scores = np.dot(matrix_norm, query_norm)
        top_indices = np.argsort(scores)[-top_k:][::-1]
        return [(self.texts[i], scores[i]) for i in top_indices]

# 使用
store = SimpleVectorStore()
store.add("Spring IoC 控制反转", get_embedding("Spring IoC 控制反转"))
store.add("MySQL 索引优化", get_embedding("MySQL 索引优化"))
store.add("依赖注入的原理", get_embedding("依赖注入的原理"))

results = store.search(get_embedding("什么是控制反转"))
for text, score in results:
    print(f"[{score:.3f}] {text}")
# [0.956] Spring IoC 控制反转
# [0.891] 依赖注入的原理
# [0.234] MySQL 索引优化
```

---

## 6. 关键实践要点

### 6.1 文本预处理

```python
def preprocess(text):
    """Embedding 前的文本清理"""
    text = text.replace("\n", " ")      # 合并换行
    text = " ".join(text.split())       # 合并多余空格
    return text[:8000]                  # 截断到模型限制
```

### 6.2 Embedding 缓存

避免重复计算相同的文本 Embedding：

```python
import hashlib
import json
import os

class EmbeddingCache:
    def __init__(self, cache_file="embedding_cache.json"):
        self.cache_file = cache_file
        self.cache = self._load()

    def _hash(self, text):
        return hashlib.md5(text.encode()).hexdigest()

    def _load(self):
        if os.path.exists(self.cache_file):
            with open(self.cache_file) as f:
                return json.load(f)
        return {}

    def get_or_compute(self, text, compute_fn):
        key = self._hash(text)
        if key not in self.cache:
            self.cache[key] = compute_fn(text)
            with open(self.cache_file, "w") as f:
                json.dump(self.cache, f)
        return self.cache[key]
```

---

## 7. 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 相似度都很高 | 向量未归一化 | 使用余弦相似度代替内积 |
| 检索不准确 | 模型不适配语言或领域 | 换用 BGE-M3 等中文模型 |
| 内存不足 | 向量维度 x 数量过大 | 使用 FAISS 等索引压缩工具 |
| API 调用慢 | 单条请求延迟高 | 使用批量 API 合并请求 |

---

## 8. 核心要点回顾

- Embedding 将文本映射为固定长度向量，语义相似的内容向量距离更近
- 余弦相似度是衡量向量相似度的最常用方法
- 生产环境需注意文本预处理、向量归一化和缓存策略
- 批量 API 调用可显著提升 Embedding 生成效率
- 最简向量检索系统用 NumPy 即可实现，无需依赖第三方数据库

---

## 9. 参考资料

1. OpenAI Embeddings API 官方文档
2. BAAI BGE-M3 模型论文
3. FAISS 向量检索库官方文档
4. LangChain Embeddings 模块文档
5. NumPy 线性代数模块文档
