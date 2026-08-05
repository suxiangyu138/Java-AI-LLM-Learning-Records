# 05 - FAISS 高性能索引

> 🎯 FAISS = Meta 出品的高性能向量检索库 — C++ 底层 + GPU 加速 + 多种索引类型，是嵌入式高性能场景的标配

---

## 目录

1. [FAISS 定位与优势](#1-faiss-定位与优势)
2. [GPU 加速](#2-gpu-加速)
3. [索引选择实战](#3-索引选择实战)
4. [内存优化技巧](#4-内存优化技巧)
5. [索引持久化](#5-索引持久化)

---

## 1. FAISS 定位与优势

```text
FAISS vs Chroma vs Milvus：

  FAISS：嵌入式库（C++底层/Python接口）
    → 场景：嵌入到 Python 进程，极致性能
    → 不是数据库（无 CRUD/持久化/分布式）

  Chroma：嵌入式向量数据库
  Milvus：独立服务型向量数据库

选 FAISS 的场景：
  → 需要 GPU 加速
  → 需要自定义索引策略
  → 作为其他向量库的底层引擎（Chroma/Milvus 底层都用 FAISS）
```

---

## 2. GPU 加速

```python
import faiss

dim = 1024
vectors = np.random.randn(1000000, dim).astype('float32')

# CPU 索引
cpu_index = faiss.IndexFlatIP(dim)
cpu_index.add(vectors)

# GPU 索引 — 一行代码转 GPU
gpu_res = faiss.StandardGpuResources()
gpu_index = faiss.index_cpu_to_gpu(gpu_res, 0, cpu_index)  # GPU 0

# 多 GPU
gpu_index = faiss.index_cpu_to_all_gpus(cpu_index)

# 速度对比（100万 × 1024维, Top-10）：
#   CPU Flat: ~500ms
#   GPU Flat: ~5ms → 100× 加速
```

---

## 3. 索引选择实战

```python
def create_index(vectors, scenario):
    """根据场景选索引"""
    N, dim = vectors.shape
    
    if scenario == "prototype" or N < 10000:
        return faiss.IndexFlatIP(dim)  # 精确
    
    elif scenario == "high_precision" or N < 500000:
        index = faiss.IndexHNSWFlat(dim, 32)
        index.add(vectors)
        return index
    
    elif scenario == "large_scale":
        nlist = int(4 * np.sqrt(N))  # 聚类中心数
        quantizer = faiss.IndexFlatIP(dim)
        index = faiss.IndexIVFPQ(quantizer, dim, nlist, 64, 8)
        #                          聚类中心     子向量数 每段bit
        index.train(vectors)
        index.add(vectors)
        index.nprobe = 10
        return index
    
    elif scenario == "gpu_speed":
        res = faiss.StandardGpuResources()
        index = faiss.index_cpu_to_gpu(res, 0, faiss.IndexFlatIP(dim))
        index.add(vectors)
        return index
```

---

## 4. 内存优化技巧

```python
# ① 使用 IDMap 包装（分离 ID 和向量）
base_index = faiss.IndexHNSWFlat(dim, 32)
index = faiss.IndexIDMap(base_index)
index.add_with_ids(vectors, ids)  # 自定义 ID

# ② IVF+PQ 压缩（内存减少 10-30×）
index = faiss.index_factory(dim, "IVF1000,PQ64x8")
#                          1000簇 64段×8bit=64字节/向量

# ③ 使用 DirectMap（快速 ID→向量 查找）
index = faiss.IndexIDMap(faiss.IndexFlatIP(dim))
```

---

## 5. 索引持久化

```python
# 保存
faiss.write_index(index, "index.faiss")

# 加载
index = faiss.read_index("index.faiss")

# 增量更新 — FAISS 不支持原地修改
# 方案：build_new_index → 原子切换
new_index = faiss.IndexHNSWFlat(dim, 32)
new_index.add(all_vectors)  # 全量重建
faiss.write_index(new_index, "index_v2.faiss")
os.rename("index_v2.faiss", "index.faiss")  # 原子切换
```

---

## 8. FAISS vs 向量数据库：2026 边界

**FAISS 的定位再确认（2026）**：

| 维度 | FAISS（索引库） | 向量数据库 |
|------|:--------------:|:----------:|
| 数据管理 | ❌（无 Schema/事务/持久化） | ✅ |
| 检索能力 | ✅（核心强项） | ✅ |
| 混合检索 | ❌（需自拼） | ✅（BM25+向量） |
| 多用户/并发 | ❌（进程内） | ✅ |
| 适用 | 单机/嵌入式/离线批处理 | 生产在线服务 |

**2026 的 FAISS 使用场景**：

```text
① 嵌入式应用：移动端/桌面端本地检索（无服务依赖）
② 离线批量：百万级向量聚类/去重/相似性分析（非在线）
③ 训练管线：Embedding 验证/样本检索（科研）
④ 混合架构：FAISS 做"批量索引构建" → 向量库做"在线服务"
   （构建与查询分离，两者互补而非竞争）
```

> 🎯 **核心要点**：FAISS 与向量库 2026 的边界 = "**进程内工具 vs 生产服务**"——**在线 RAG 用向量库、离线/嵌入式用 FAISS**；"FAISS 被向量库取代"的说法不成立，两者场景互补。

---

## 9. FAISS 使用速查

**FAISS 核心 API 速查**（高频操作）：

```python
import faiss
import numpy as np

# ① 索引创建（按规模选型）
dim = 768
index = faiss.IndexFlatL2(dim)              # 精确（小数据/验证）
index = faiss.IndexIVFFlat(quantizer, dim, nlist)   # IVF（百万级）
index = faiss.IndexHNSWFlat(dim, M)         # HNSW（默认首选）

# ② 训练与添加
index.train(vectors)                        # IVF 需要训练（聚类）
index.add(vectors)                          # 添加向量

# ③ 检索
D, I = index.search(query, k=10)            # 距离 + 索引

# ④ 保存与加载
faiss.write_index(index, "index.bin")
index = faiss.read_index("index.bin")

# ⑤ 索引 ID 映射（与业务 ID 关联）
index = faiss.IndexIDMap2(flat_index)
index.add_with_ids(vectors, ids)            # 业务 ID 直接入库
```

**FAISS vs 向量库的选择速查**：

```text
用 FAISS：单机/嵌入式、离线批量、训练管线、构建与查询分离
用向量库：在线服务、多用户、过滤/混合检索、数据管理
混合：FAISS 批量构建 → 导出 → 向量库在线服务（两者互补）
```

> 💡 FAISS 学习三件事：**索引选型（Flat/IVF/HNSW）+ ID 映射（业务关联）+ 序列化（持久化）**——掌握这三件即可上手（GPU 版 `faiss-gpu` 用于大吞吐）。

---

## 核心要点回顾

- FAISS = C++ 底层 + Python 接口 → 嵌入式极致性能
- GPU 加速一行代码 → 100× 提升
- IVF+PQ 压缩 → 内存减少 10-30×
- FAISS 不支持增量更新 → 全量重建 + 原子切换
