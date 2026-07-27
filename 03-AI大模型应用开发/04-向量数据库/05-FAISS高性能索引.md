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

## 核心要点回顾

- FAISS = C++ 底层 + Python 接口 → 嵌入式极致性能
- GPU 加速一行代码 → 100× 提升
- IVF+PQ 压缩 → 内存减少 10-30×
- FAISS 不支持增量更新 → 全量重建 + 原子切换
