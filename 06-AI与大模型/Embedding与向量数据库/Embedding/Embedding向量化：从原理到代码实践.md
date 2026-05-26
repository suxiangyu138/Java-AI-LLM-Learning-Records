# Embedding 向量化：从原理到代码实践

> **所属阶段**：阶段二 — RAG 知识库应用开发
> **前置知识**：Python 基础、大模型 API 调用
> **核心问题**：如何让计算机"理解"文本的语义相似度？

---

## 1. 什么是 Embedding

Embedding（嵌入）是将文本、图像等非结构化数据映射为**固定长度的浮点数向量**，使得语义相近的内容在向量空间中距离也相近。

```
"今天天气真好"  →  [0.12, -0.34, 0.56, ..., 0.78]  (1536维)
"今日阳光明媚"  →  [0.11, -0.33, 0.58, ..., 0.77]  (向量接近)
"数据库连接超时" →  [-0.45, 0.67, -0.23, ..., -0.12] (向量远离)
```

---

## 2. 核心原理

### 2.1 向量空间中的语义

```
         ┌──────────────────────────┐
         │    "今天天气真好"          │
         │    "今日阳光明媚"  ← 聚集   │
         │                          │
         │          "数据库连接超时"   │
         │          "MySQL报错"      │ ← 另一个簇
         └──────────────────────────┘
```

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

---

## 3. 主流 Embedding 模型

| 模型 | 维度 | 中文支持 | 特点 |
|------|------|----------|------|
| OpenAI text-embedding-3-small | 1536 | 一般 | 商业，效果好 |
| BGE-M3 (BAAI) | 1024 | 优秀 | 开源，多语言 |
| text2vec-large-chinese | 1024 | 优秀 | 中文专用 |
| m3e-base | 768 | 优秀 | 中文轻量 |

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

| 问题 | 原因 | 解决 |
|------|------|------|
| 相似度都很高 | 向量未归一化 | 使用余弦相似度 |
| 检索不准确 | 模型不适配语言/领域 | 换用 BGE-M3 等中文模型 |
| 内存不足 | 向量维度 × 数量过大 | 使用 FAISS 索引压缩 |
| API 调用慢 | 单条请求延迟高 | 使用批量 API |
