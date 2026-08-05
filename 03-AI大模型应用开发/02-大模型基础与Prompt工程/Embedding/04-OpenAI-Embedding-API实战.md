# 04 - OpenAI Embedding API 实战

> 🎯 text-embedding-3 系列是目前最成熟的商业 Embedding API — 接入即用、精度高、批量处理、多维度可选

---

## 目录

1. [API 基础调用](#1-api-基础调用)
2. [批量处理与性能优化](#2-批量处理与性能优化)
3. [维度缩减特性](#3-维度缩减特性)
4. [错误处理与重试](#4-错误处理与重试)
5. [成本计算与优化](#5-成本计算与优化)
6. [完整封装示例](#6-完整封装示例)

---

## 1. API 基础调用

### 1.1 最简调用

```python
from openai import OpenAI

client = OpenAI(api_key="sk-xxx")

def get_embedding(text: str, model="text-embedding-3-small") -> list[float]:
    """单条文本生成 Embedding"""
    text = text.replace("\n", " ")
    response = client.embeddings.create(
        model=model,
        input=[text]
    )
    return response.data[0].embedding

# 使用
vec = get_embedding("Spring Boot 自动配置原理")
print(f"维度: {len(vec)}")   # 1536
print(f"前5个值: {vec[:5]}") # [0.012, -0.034, ...]
```

### 1.2 指定维度（text-embedding-3 特性）

```python
# text-embedding-3-small 支持任意小于等于 1536 的维度
# 维度越小 → 存储越小 → 检索越快 → 精度轻微下降

vec_256 = client.embeddings.create(
    model="text-embedding-3-small",
    input=["test"],
    dimensions=256       # 指定 256 维
).data[0].embedding
print(len(vec_256))     # 256

# 不指定 dimensions → 默认最大维度
vec_full = client.embeddings.create(
    model="text-embedding-3-small",
    input=["test"]
).data[0].embedding
print(len(vec_full))    # 1536
```

### 1.3 两个模型的对比

| 特性 | text-embedding-3-small | text-embedding-3-large |
|------|----------------------|----------------------|
| 最大维度 | 1536 | 3072 |
| 价格 | $0.02/1M tokens | $0.13/1M tokens |
| 精度 (MTEB) | 62.3% | 64.6% |
| 推荐场景 | 通用、高性价比 | 高精度要求 |

---

## 2. 批量处理与性能优化

### 2.1 批量调用

```python
def batch_embed(texts: list[str], model="text-embedding-3-small",
                batch_size=100) -> list[list[float]]:
    """批量生成 Embedding，减少网络往返"""
    all_embeddings = []
    
    for i in range(0, len(texts), batch_size):
        batch = texts[i:i + batch_size]
        response = client.embeddings.create(
            model=model,
            input=batch
        )
        batch_embs = [d.embedding for d in response.data]
        all_embeddings.extend(batch_embs)
        
        print(f"已处理 {min(i + batch_size, len(texts))}/{len(texts)}")
    
    return all_embeddings

# 性能对比：
# 1000 条文本
#   单条模式：1000 次 API 调用 → ~100s（受 Rate Limit 限制）
#   批量模式（batch=100）：10 次 API 调用 → ~15s
#   ✅ 速度提升 6-7×
```

### 2.2 异步并发（更高吞吐）

```python
import asyncio
from openai import AsyncOpenAI

async_client = AsyncOpenAI(api_key="sk-xxx")

async def embed_one(text: str) -> list[float]:
    resp = await async_client.embeddings.create(
        model="text-embedding-3-small",
        input=[text]
    )
    return resp.data[0].embedding

async def embed_concurrent(texts: list[str], concurrency=20):
    """并发处理多条文本"""
    semaphore = asyncio.Semaphore(concurrency)
    
    async def bounded_embed(text):
        async with semaphore:
            return await embed_one(text)
    
    tasks = [bounded_embed(t) for t in texts]
    return await asyncio.gather(*tasks)

# 使用
embeddings = asyncio.run(embed_concurrent(texts, concurrency=20))
```

---

## 3. 维度缩减特性

### 3.1 为什么可以缩维

```text
text-embedding-3 系列在训练时使用了 Matryoshka Representation Learning

传统 Embedding：768 维 → 只能用 768 维
Matryoshka：1536 维 → 前 256 维也有效 → 前 512 维也有效 → ...

就像一个俄罗斯套娃：
  外层 (1536d) 包含全部信息
  中层 (768d) 包含主要信息
  内层 (256d) 包含核心信息

即：截取前 N 维仍能保持合理的精度！
```

### 3.2 不同维度的精度对比

```python
import numpy as np

def test_dimension_impact(base_embedding_1536d):
    """测试不同截取维度的精度保留率"""
    for dim in [256, 512, 768, 1024, 1536]:
        truncated = base_embedding_1536d[:dim]
        # 精度保留率 ≈ 与全维度向量的余弦相似度
        retention = np.dot(truncated, base_embedding_1536d[:dim])
        print(f"dim={dim:4d}: 精度保留率 ≈ {retention:.4f}")

# 典型结果：
# dim= 256: 精度保留率 ≈ 0.91
# dim= 512: 精度保留率 ≈ 0.96
# dim= 768: 精度保留率 ≈ 0.98
# dim=1024: 精度保留率 ≈ 0.99
# dim=1536: 精度保留率 ≈ 1.00

# 推荐：如果不追求极致精度，512 维通常是性价比最优
```

---

## 4. 错误处理与重试

```python
import time
from openai import RateLimitError, APIError

def embed_with_retry(texts, model="text-embedding-3-small",
                     max_retries=5, batch_size=100):
    """带指数退避重试的 Embedding 生成"""
    for attempt in range(max_retries):
        try:
            response = client.embeddings.create(
                model=model,
                input=texts[:batch_size]
            )
            return [d.embedding for d in response.data]
            
        except RateLimitError:
            wait = 2 ** attempt  # 指数退避：1s, 2s, 4s, 8s, 16s
            print(f"Rate Limit，{wait}s 后重试...")
            time.sleep(wait)
            
        except APIError as e:
            if e.status_code >= 500:  # 服务端错误
                wait = 2 ** attempt
                print(f"服务端错误 {e.status_code}，{wait}s 后重试...")
                time.sleep(wait)
            else:
                raise  # 4xx 错误不重试
    
    raise Exception(f"重试 {max_retries} 次后仍失败")
```

---

## 5. 成本计算与优化

### 5.1 Token 计数

```python
import tiktoken

def count_tokens(text: str, model="text-embedding-3-small") -> int:
    """计算文本的 token 数"""
    encoding = tiktoken.encoding_for_model(model)
    return len(encoding.encode(text))

# 示例
text = "Spring Boot 是一个基于 Spring 框架的快速开发工具"
print(f"Token 数: {count_tokens(text)}")  # ~25 tokens

# 中文：1 个汉字 ≈ 1.5 ~ 2 tokens
# 英文：1 个单词 ≈ 1 ~ 1.5 tokens
```

### 5.2 成本估算

```python
def estimate_cost(texts: list[str], model="text-embedding-3-small") -> float:
    """估算 Embedding 成本"""
    total_tokens = sum(count_tokens(t) for t in texts)
    
    prices = {
        "text-embedding-3-small": 0.02 / 1_000_000,  # $0.02 per 1M
        "text-embedding-3-large": 0.13 / 1_000_000,
    }
    
    return total_tokens * prices[model]

# 示例：1000 篇文章，每篇 1000 tokens
texts = ["文章内容" * 500] * 1000  # 每条约 1000 tokens
cost = estimate_cost(texts, "text-embedding-3-small")
print(f"预估成本: ${cost:.4f}")  # ~$0.02

# 结论：Embedding 成本极低，通常不是瓶颈
```

---

## 6. 完整封装示例

```python
import numpy as np
from openai import OpenAI
from typing import Optional

class OpenAIEmbedder:
    """生产级 OpenAI Embedding 封装"""
    
    def __init__(self, api_key: str, model="text-embedding-3-small",
                 dimensions: Optional[int] = None):
        self.client = OpenAI(api_key=api_key)
        self.model = model
        self.dimensions = dimensions
    
    def embed(self, texts):
        """单条或批量生成 Embedding"""
        is_single = isinstance(texts, str)
        texts = [texts] if is_single else texts
        
        # 文本预处理
        texts = [t.replace("\n", " ").strip() for t in texts]
        
        kwargs = {"model": self.model, "input": texts}
        if self.dimensions:
            kwargs["dimensions"] = self.dimensions
        
        resp = self.client.embeddings.create(**kwargs)
        vectors = [d.embedding for d in resp.data]
        
        return vectors[0] if is_single else vectors
    
    def similarity(self, text_a: str, text_b: str) -> float:
        """计算两个文本的余弦相似度"""
        a, b = self.embed([text_a, text_b])
        a, b = np.array(a), np.array(b)
        return float(np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b)))


# 使用
embedder = OpenAIEmbedder(api_key="sk-xxx", dimensions=512)

# 单条
vec = embedder.embed("Hello World")

# 批量
vecs = embedder.embed(["text1", "text2", "text3"])

# 相似度
sim = embedder.similarity("今天天气真好", "今日阳光明媚")
print(f"相似度: {sim:.4f}")  # 接近 1.0
```

---

## 核心要点回顾

- text-embedding-3-small 性价比最高（$0.02/1M tokens）
- 批量处理可提速 6-7×，异步并发可突破 Rate Limit
- Matryoshka 维度缩减：512 维通常是性价比最优
- 指数退避重试是生产必备的错误处理
- Embedding 成本极低，通常不是项目的主要开销
