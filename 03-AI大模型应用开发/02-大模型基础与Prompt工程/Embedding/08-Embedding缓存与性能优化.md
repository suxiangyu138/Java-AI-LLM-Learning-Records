# 08 - Embedding 缓存与性能优化

> 🎯 重复计算 Embedding 是 RAG 系统的隐形性能杀手 — 缓存 + 批处理 + 异步并发，三个优化让吞吐量提升 10 倍以上

---

## 目录

1. [为什么需要缓存](#1-为什么需要缓存)
2. [内存缓存实现](#2-内存缓存实现)
3. [持久化缓存方案](#3-持久化缓存方案)
4. [批处理优化](#4-批处理优化)
5. [并发与异步](#5-并发与异步)
6. [性能基准与调优清单](#6-性能基准与调优清单)

---

## 1. 为什么需要缓存

```text
RAG 系统中 Embedding 的重复计算场景：

  ① 相同的文档片段 — 系统启动时 Embedding 一次，之后永久缓存
  ② 相似的 query — 用户反复问类似问题，"怎么配置数据库"出现了 100 次
  ③ 开发/测试 — 同一个文档集反复实验不同参数

无缓存：
  每次查询：文档 Embedding（1000 条 × 50ms = 50s） + 检索（<10ms）
  → 50 秒等待 → 不可接受

有缓存：
  首次：文档 Embedding（50s） → 写入缓存
  之后：读缓存（<1ms） + 检索（<10ms）
  → <10ms → 实时体验
```

---

## 2. 内存缓存实现

### 2.1 基于哈希的简单缓存

```python
import hashlib

class EmbeddingCache:
    """哈希去重缓存"""
    
    def __init__(self, embed_fn, max_size=100000):
        self.embed_fn = embed_fn     # Embedding 函数
        self.cache = {}              # hash → vector
        self.max_size = max_size
    
    def _hash(self, text: str) -> str:
        """文本 → MD5 哈希"""
        return hashlib.md5(text.encode('utf-8')).hexdigest()
    
    def get_or_compute(self, text: str):
        """缓存命中 → 返回；未命中 → 计算并缓存"""
        key = self._hash(text)
        if key in self.cache:
            return self.cache[key]
        
        # 缓存淘汰（简单 LRU 近似）
        if len(self.cache) >= self.max_size:
            oldest = next(iter(self.cache))
            del self.cache[oldest]
        
        vec = self.embed_fn(text)
        self.cache[key] = vec
        return vec
    
    def get_or_compute_batch(self, texts: list[str]):
        """批量 — 缓存命中的跳过，未命中的批量计算"""
        results = []
        misses = []  # (index, text) 缓存未命中
        
        for i, text in enumerate(texts):
            key = self._hash(text)
            if key in self.cache:
                results.append((i, self.cache[key]))
            else:
                misses.append((i, text, key))
        
        # 批量计算未命中的
        if misses:
            miss_texts = [t for _, t, _ in misses]
            miss_vecs = self.embed_fn(miss_texts)  # 批量 API 调用
            
            for (idx, _, key), vec in zip(misses, miss_vecs):
                self.cache[key] = vec
                results.append((idx, vec))
        
        # 按原始顺序排序
        results.sort()
        return [v for _, v in results]
```

### 2.2 LRU 缓存

```python
from functools import lru_cache

@lru_cache(maxsize=10000)
def cached_embed(text: str) -> tuple:
    """LRU 缓存 — 自动淘汰最少使用的条目
    注意：返回 tuple 因为 lru_cache 要求可哈希
    """
    vec = embed_fn(text)  # 你的 Embedding 函数
    return tuple(vec)     # list→tuple 使可哈希

# 使用
vec = list(cached_embed("Spring Boot 配置"))  # 首次计算
vec = list(cached_embed("Spring Boot 配置"))  # 缓存命中，瞬时
```

---

## 3. 持久化缓存方案

### 3.1 JSON 文件缓存（简单场景）

```python
import json
import os

class FileEmbeddingCache:
    """JSON 文件持久化缓存"""
    
    def __init__(self, cache_file="embedding_cache.json"):
        self.cache_file = cache_file
        self.cache = self._load()
    
    def _load(self):
        if os.path.exists(self.cache_file):
            with open(self.cache_file, 'r', encoding='utf-8') as f:
                return json.load(f)
        return {}
    
    def _save(self):
        with open(self.cache_file, 'w', encoding='utf-8') as f:
            json.dump(self.cache, f, ensure_ascii=False)
    
    def get(self, text_hash):
        return self.cache.get(text_hash)
    
    def put(self, text_hash, vector):
        self.cache[text_hash] = vector
    
    def flush(self):
        """批量写入磁盘"""
        self._save()
```

### 3.2 SQLite 缓存（生产推荐）

```python
import sqlite3
import pickle

class SQLiteEmbeddingCache:
    """SQLite 持久化 — 支持百万级条目"""
    
    def __init__(self, db_path="embeddings.db"):
        self.conn = sqlite3.connect(db_path)
        self.conn.execute("""
            CREATE TABLE IF NOT EXISTS embeddings (
                text_hash TEXT PRIMARY KEY,
                vector BLOB,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)
        self.conn.execute("CREATE INDEX IF NOT EXISTS idx_hash ON embeddings(text_hash)")
    
    def get(self, text_hash):
        row = self.conn.execute(
            "SELECT vector FROM embeddings WHERE text_hash = ?",
            (text_hash,)
        ).fetchone()
        if row:
            return pickle.loads(row[0])
        return None
    
    def put_batch(self, items: list[tuple[str, list[float]]]):
        """批量写入"""
        data = [(h, pickle.dumps(v)) for h, v in items]
        self.conn.executemany(
            "INSERT OR REPLACE INTO embeddings (text_hash, vector) VALUES (?, ?)",
            data
        )
        self.conn.commit()
    
    def size(self):
        return self.conn.execute("SELECT COUNT(*) FROM embeddings").fetchone()[0]
```

---

## 4. 批处理优化

### 4.1 批处理 vs 逐条处理

```python
import time

def benchmark(texts, embed_fn, embed_batch_fn):
    """对比逐条 vs 批量"""
    
    # 逐条
    start = time.time()
    for t in texts:
        embed_fn(t)
    single_time = time.time() - start
    
    # 批量
    start = time.time()
    for i in range(0, len(texts), 100):
        batch = texts[i:i+100]
        embed_batch_fn(batch)
    batch_time = time.time() - start
    
    print(f"逐条: {single_time:.2f}s")
    print(f"批量: {batch_time:.2f}s")
    print(f"加速: {single_time/batch_time:.1f}×")

# 典型结果 (1000 条文本, OpenAI API):
# 逐条: 45.2s
# 批量: 8.3s
# 加速: 5.4×
```

### 4.2 动态批处理

```python
import asyncio

class DynamicBatcher:
    """动态批处理器 — 积攒请求成 batch，减少 API 调用次数"""
    
    def __init__(self, embed_fn, max_batch_size=100, max_wait_ms=50):
        self.embed_fn = embed_fn
        self.max_batch_size = max_batch_size
        self.max_wait_ms = max_wait_ms / 1000
        self.queue = []
        self.futures = []
    
    async def embed(self, text):
        """异步提交 Embedding 请求"""
        future = asyncio.Future()
        self.queue.append((text, future))
        
        if len(self.queue) >= self.max_batch_size:
            await self._flush()
        
        return await future
    
    async def _flush(self):
        """批量处理队列中的请求"""
        if not self.queue:
            return
        
        texts = [t for t, _ in self.queue]
        futures = [f for _, f in self.queue]
        self.queue = []
        
        vectors = self.embed_fn(texts)
        for f, v in zip(futures, vectors):
            f.set_result(v)
```

---

## 5. 并发与异步

### 5.1 线程池并发

```python
from concurrent.futures import ThreadPoolExecutor, as_completed

def embed_concurrent(texts, embed_fn, max_workers=10):
    """线程池并发 Embedding"""
    results = [None] * len(texts)
    
    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        futures = {
            executor.submit(embed_fn, text): i
            for i, text in enumerate(texts)
        }
        for future in as_completed(futures):
            idx = futures[future]
            results[idx] = future.result()
    
    return results
```

### 5.2 异步 + 信号量限流

```python
import asyncio
from openai import AsyncOpenAI

async def embed_with_ratelimit(texts, semaphore_limit=20):
    """异步 Embedding + 信号量控制并发"""
    client = AsyncOpenAI()
    semaphore = asyncio.Semaphore(semaphore_limit)
    
    async def embed_one(text):
        async with semaphore:
            resp = await client.embeddings.create(
                model="text-embedding-3-small",
                input=[text]
            )
            return resp.data[0].embedding
    
    tasks = [embed_one(t) for t in texts]
    return await asyncio.gather(*tasks)
```

---

## 6. 性能基准与调优清单

### 6.1 典型性能数据（1000 条文本，1024 维）

| 方案 | 耗时 | 提速 |
|------|:---:|:---:|
| 逐条 API（无缓存） | ~50s | 基准 |
| 批量 API（batch=100） | ~10s | 5× |
| 批量 + 缓存命中 | ~0.5s | 100× |
| 本地 Ollama + GPU batch | ~2s | 25× |
| 异步并发（concurrency=50） | ~3s | 17× |

### 6.2 优化清单

| 优化项 | 预期提升 | 实现难度 |
|--------|:---:|:---:|
| 增加 batch_size | 3-10× | ⭐ |
| 添加缓存层 | 10-100×（命中时） | ⭐⭐ |
| 异步并发 | 5-20× | ⭐⭐ |
| 换用本地模型 | 延迟→0，无网络开销 | ⭐⭐ |
| GPU 批处理推理 | 5-10× vs CPU | ⭐⭐⭐ |
| 预计算 + 预加载 | 首次延迟→0 | ⭐ |

---

## 核心要点回顾

- 缓存是 Embedding 性能优化的第一手段 — 相同文本绝不重复计算
- 生产推荐 SQLite 持久化缓存（支持百万级、跨进程共享）
- 批量 API 调用比逐条快 5-10×
- 本地 Ollama + GPU batch 可做到每条约 2ms
- 优化顺序：先加缓存 → 再调批处理 → 最后考虑异步/本地部署
