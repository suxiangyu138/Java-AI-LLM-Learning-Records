# 02 - 向量索引原理：HNSW 与 IVF

> 🎯 暴力检索 100% 准确但太慢，ANN 牺牲 <5% 精度换 100-1000× 速度 — HNSW 是当前最佳平衡点

---

## 目录

1. [索引演进路线](#1-索引演进路线)
2. [Flat 暴力检索](#2-flat-暴力检索)
3. [IVF 倒排索引](#3-ivf-倒排索引)
4. [HNSW 图索引](#4-hnsw-图索引)
5. [PQ 乘积量化](#5-pq-乘积量化)
6. [索引选择指南](#6-索引选择指南)

---

## 1. 索引演进路线

```text
Flat (暴力) → IVF (聚类) → HNSW (图) → IVF+PQ (压缩)

  Flat：    100% 精度，O(N) 速度
  IVF：     ~95% 精度，搜索部分簇
  HNSW：    ~98% 精度，图导航跳转
  IVF+PQ：  ~90% 精度，极致压缩
```

---

## 2. Flat 暴力检索

```text
原理：查询向量与所有候选向量逐个计算相似度 → 排序 → Top-K

  ✅ 精度 100%
  ❌ O(N×d) 复杂度 → 百万级向量 → ~100ms

适用：<10 万向量，精度要求极高的场景
```

---

## 3. IVF 倒排索引

```text
IVF (Inverted File) = 先聚类，只搜最近的几个簇

  ① 训练阶段：用 K-Means 将所有向量聚为 nlist 个簇
  ② 查询阶段：找到最近的 nprobe 个簇 → 只搜这些簇内的向量

  nlist=100：分成 100 个簇
  nprobe=10：搜最近的 10 个簇 → 只搜 10% 的向量 → ~10× 加速

  nprobe ↑ → 精度 ↑、速度 ↓（可动态调整）
```

```python
import faiss

dim = 1024
quantizer = faiss.IndexFlatIP(dim)  # 聚类中心用 Flat
index = faiss.IndexIVFFlat(quantizer, dim, nlist=100)  # 100 个簇

index.train(vectors)   # 必须先训练
index.add(vectors)
index.nprobe = 10      # 搜 10 个簇
```

---

## 4. HNSW 图索引

```text
HNSW (Hierarchical Navigable Small World) = 多层图导航

  底层：密集连接 → 精确搜索
  上层：稀疏连接 → 快速跳转到目标区域

  类比：高速公路（上层）→ 快速到达目标城市
        城市道路（中层）→ 到达目标区域
        社区小路（底层）→ 精确找到目标
  
  搜索过程：从顶层开始 → 贪心导航 → 逐层下降 → 底层精确搜索

参数 M：每个节点的最大连接数
  → M 大：精度高、构建慢、内存大
  → M 小：速度快、内存小、精度略降
  → 推荐 M=16~64
```

```python
# HNSW 索引
index = faiss.IndexHNSWFlat(dim, M=32)  # 32 连接
index.add(vectors)
# 直接可用，无需训练！
```

---

## 5. PQ 乘积量化

```text
PQ (Product Quantization) = 将向量分段压缩

  1024 维向量 → 分成 64 段 × 16 维
  每段用 256 个码本值近似 → 用 8bit 索引替代 512bit 原始值
  → 压缩比 = 512/8 = 64×

IVF+PQ 组合：
  ① IVF 粗筛 → 快速缩小候选范围
  ② PQ 精排 → 在压缩域计算近似距离
  → 亿级向量检索成为可能
```

---

## 6. 索引选择指南

| 索引 | 精度 | 速度 | 内存 | 需训练 | 适用规模 |
|------|:---:|:---:|:---:|:---:|:---:|
| **IndexFlat** | 100% | ★☆ | 高 | ❌ | <10万 |
| **IndexIVF** | ~95% | ★★★ | 中 | ✅ | 10万~千万 |
| **IndexHNSW** | ~98% | ★★★★★ | 高 | ❌ | <百万 |
| **IndexIVF+PQ** | ~90% | ★★★★ | 低 | ✅ | 百万~亿 |

```text
选型口诀：
  → 原型/demo → Flat (最简单)
  → 生产/高精度 → HNSW (最快最准)
  → 亿级向量 → IVF+PQ (压缩极致)
  → GPU → IndexFlat(暴力但有GPU加速)
```

---

## 核心要点回顾

- Flat=暴力(100%) → IVF=聚类(~95%) → HNSW=图导航(~98%) → PQ=压缩(~90%)
- HNSW 是当前最佳平衡（精度 98% + 速度最快、无需训练）
- IVF 的 nprobe 越大精度越高速度越慢（可动态调）
- 亿级向量用 IVF+PQ 压缩（64× 压缩比）
