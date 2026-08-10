# 05 - IVF 索引与倒排

> 定位：近似检索的入门课——"先聚类分桶，只搜 nprobe 个桶"——train-add 两阶段是 IVF 的灵魂，nprobe 是精度的旋钮——"10 万-1 亿向量的速度担当"

---

## 📚 目录

1. [倒排思想：先粗筛再精算](#1-倒排思想先粗筛再精算)
2. [两阶段：train 与 add](#2-两阶段train-与-add)
3. [nlist 与 nprobe：两个旋钮](#3-nlist-与-nprobe两个旋钮)
4. [召回率 95-99%：调参验证闭环](#4-召回率-95-99调参验证闭环)
5. [IVF 与业务 ID](#5-ivf-与业务-id)
6. [常见坑](#6-常见坑)
7. [练习 5 题](#7-练习-5-题)

---

## 1. 倒排思想：先粗筛再精算

IVF（Inverted File，倒排文件）的思想一句话：**"把空间切成格子，查询只进附近的格子找"**——用 k-means 把全部向量聚成 nlist 个簇（Voronoi 单元），每条向量进它最近的簇；查询时计算查询向量到各簇中心的距离，**只探测最近的 nprobe 个簇**，在簇内做精确比较。

**为什么快**：全量扫描 O(N) 变成"簇中心比较 O(nlist) + 簇内扫描 O(N·nprobe/nlist)"——**"从'检查所有人'变成'先问'你在哪个街区'，再在街区里找人'"**。

**召回率从哪丢**：真正的最近邻可能不在被探测的簇里（聚类边界效应）——**"召回损失 = 探测的簇没盖住真邻居"**；召回率典型 **95-99%**，nprobe 越大盖住概率越高（nprobe = nlist 时退化为全量扫描，召回 100%）。

## 2. 两阶段：train 与 add

IVF 是 FAISS 中**"重型"索引的代表**：建索引不是"add 就完"，而是两个阶段：

```python
quantizer = faiss.IndexFlatL2(d)        # 量词：簇中心用什么索引（Flat 即可）
nlist = 100                             # 簇数
index = faiss.IndexIVFFlat(quantizer, d, nlist)

index.train(training_vectors)           # 阶段一：k-means 聚类，学簇中心
index.add(vectors)                      # 阶段二：向量进簇（此前的 add 无效！）
index.nprobe = 10                       # 查询旋钮：探测 10 个簇（默认 1！）
D, I = index.search(queries, k)
```

**train 的语义**：用**代表性子集**跑 k-means 学出 nlist 个簇中心（存于量词索引）——**"train 学的是'空间怎么切'，add 用的是'切好的格子'"**。三条纪律（03 篇）：训练集与数据同分布、子集即可（几万条）、train 一次落盘复用。

**quantizer 是什么**：构造时第一个参数是"量词"——决定**簇中心之间怎么比距离**：`faiss.IndexFlatL2(d)` 当量词（精确比簇中心距离）是标准姿势；生产上可换 `IndexHNSWFlat`（簇很多时量词检索快）——"**量词是'格子怎么找'的引擎，量级小用 Flat、量大上 HNSW**"，新手直接用 Flat 量词即可。

**最危险的坑**：**先 add 后 train 或忘 train，不报错但结果垃圾**（簇中心未学，向量全挤进错误格子）——"**IVF 的一切静默失败都源于 train 纪律**"。

## 3. nlist 与 nprobe：两个旋钮

| 参数 | 作用 | 默认 | 调优方向 |
|------|------|:---:|---------|
| nlist | 簇数（空间切多细） | 构造时定 | 约 4√N（N=数据量）经验值 |
| nprobe | 查询探测几个簇 | **1** | 越大召回越高、延迟越高 |

**nlist 是"建库粒度"**：经验公式 **nlist ≈ 4√N**——10 万条约 1000 簇、1000 万条约 12000 簇；簇太少（每簇太大）近似无意义，簇太多（每簇太小）簇中心比较成本上升。

**nprobe 是"查询精度旋钮"**：**默认 1 只探测最近一个簇，召回可能很差**——**"nprobe=1 是新手默认，nprobe=10~50 是生产常态"**；它运行时可改（`index.nprobe = 20`），**"数据量涨了、召回降了，第一个动作就是加大 nprobe"**。

**调优节奏**：先定 nlist（经验公式）→ nprobe 从 10 起步逐步加大 → 每次加大测召回率-延迟 → **"找到'召回达标（如 98%）时延迟最小'的 nprobe，记录在案"**。

```python
for nprobe in [10, 25, 50, 100]:
    index.nprobe = nprobe                    # 查询前随时改，无需重建索引
    t0 = time.perf_counter()
    _, approx = index.search(queries, k)
    dt = (time.perf_counter() - t0) / len(queries)
    print(f"nprobe={nprobe} 延迟={dt*1000:.2f}ms")   # 与召回率对照选点
```

**nprobe 是"运行时可改"的**——不像 nlist 建库定死，它是**零成本实验**的旋钮："**先加 nprobe 看召回能不能救，救不了再回头动 nlist/换索引**"——这是近似索引调参的第一直觉。

## 4. 召回率 95-99%：调参验证闭环

IVF 的参数调优不能凭感觉，**必须用 Flat 做基准验证**：

```python
flat = faiss.IndexFlatL2(d); flat.add(vectors)      # 基准：精确答案
ivf = faiss.IndexIVFFlat(faiss.IndexFlatL2(d), d, 100)
ivf.train(vectors[:50000]); ivf.add(vectors)

_, exact = flat.search(queries, k=10)
for nprobe in [1, 10, 50, 100]:
    ivf.nprobe = nprobe
    _, approx = ivf.search(queries, k=10)
    recall = (np.array([set(a) & set(e) for a, e in zip(approx, exact)]).mean(axis=1).mean())
    print(f"nprobe={nprobe} 召回={recall:.3f}")
```

**闭环三件事**：其一，**基准先行**——Flat 答案是一切召回率的分子；其二，**量化对比**——"nprobe=10 召回 96%，50 召回 99%"的数字决策；其三，**延迟同测**——召回与延迟画曲线，**取达标点**——"**参数不是抄来的，是你的数据与硬件测出来的**"（09 篇压测方法同源）。

## 5. IVF 与业务 ID

**IVF 原生支持自定义 ID**（它有自己的 `add_with_ids`），不需要再包 IndexIDMap：

```python
index = faiss.IndexIVFFlat(quantizer, d, nlist)
index.train(training)
ids = np.arange(100000, dtype='int64')          # 业务 ID（int64！）
index.add_with_ids(vectors, ids)                # 直接带 ID 入库
D, I = index.search(queries, k)                 # I 返回业务 ID
```

**"IVF 自带 ID 管理，Flat 系才需要 IndexIDMap 包装"**（08 篇详讲）——选型时这一条影响代码结构。配合 `IndexIDMap2` 的 IVF 变体还支持按 ID 高效删除与还原——"**带删除/更新需求的 IVF 场景，直接选 IDMap2 包装**"。

**IVF 的删除语义也优于顺序索引**：remove_ids 按 ID 精确删、**不影响其他 ID 的行号**（簇内记录按 ID 定位）——"**要频繁增删的库，IVF（或 IDMap2）比 Flat 少一个'行号漂移'的坑**"（08 篇详讲）。**增量注意**：IVF 的簇是 train 时定死的，新数据分布偏移过大要重训——"**增量友好的答案是 HNSW（图动态生长），分布稳定的大库用 IVF**"。

## 6. 常见坑

1. **忘 train / 先 add 后 train**——不报错但结果垃圾；train 必须在 add 前。
2. **nprobe 默认 1 忘调**——召回惨不忍睹；生产从 10 起步。
3. **nlist 拍脑袋**——经验公式 4√N；太小近似无效，太大查询成本上升。
4. **训练集与数据不同分布**——簇中心学偏，边界召回崩塌。
5. **全量数据训练**——几万条子集足够，全量纯浪费。
6. **不测召回就上线**——"感觉还行"不是证据；Flat 基准 + nprobe 扫描。
7. **nprobe=nlist 当高性能配置**——那是退化为全量扫描，不如直接 Flat。

## 7. 练习 5 题

1. 倒排思想的直觉？召回率从哪丢？
2. train 与 add 两阶段各自做什么？为什么"忘 train 的错最危险"？
3. nlist 与 nprobe 的分工？经验公式与调优起点？
4. 召回率验证闭环怎么做？为什么 Flat 是基准？
5. IVF 与业务 ID 的关系？什么场景不需要 IndexIDMap？

> 🎯 **核心要点**：IVF = **k-means 分簇 + 两阶段（train 学空间切法、add 进簇）+ 双旋钮（nlist 建库粒度、nprobe 查询精度）+ Flat 基准验证闭环**——"10 万-1 亿向量的速度担当：先粗筛后精算，召回 95-99% 靠 nprobe 买到"。

---

**下一模块**：[06-PQ与内存压缩.md](06-PQ与内存压缩.md) / **返回总览**：[00-faiss-cpu总览.md](00-faiss-cpu总览.md)
