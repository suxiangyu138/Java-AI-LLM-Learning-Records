# 00 - faiss-cpu 总览

> 定位：FAISS（faiss-cpu）——"Meta 开源的相似度检索算法库，亿级向量秒级检索的事实标准内核"——Chroma/Milvus 的底层引擎就是它——"数据库管存储，FAISS 管'找相似'本身"

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [与主体系的分工](#3-与主体系的分工)
4. [学习路线推荐](#4-学习路线推荐)
5. [核心概念速查](#5-核心概念速查)
6. [常见误区](#6-常见误区)
7. [一周学习计划示例](#7-一周学习计划示例)
8. [快速自测 10 题](#8-快速自测-10-题)
9. [参考来源](#9-参考来源)

---

## 1. 知识体系导图

```text
faiss-cpu（本体系 11 篇）
├── 定位层：01 faiss-cpu 是什么（算法库 vs 数据库/2026 基线）
│          02 安装与快速开始（float32 铁律/首个索引）
├── 核心层：03 索引类型与选型（Flat/IVF/PQ/HNSW 家族全景）
│          04 IndexFlat 精确检索（L2/IP/余弦三姿势）
│          05 IndexIVF 倒排索引（train-add 两阶段/nprobe）
│          06 IndexIVFPQ 内存压缩（乘积量化/压缩比）
│          07 IndexHNSWFlat 图索引（M/efConstruction/efSearch）
├── 应用层：08 ID 管理与持久化（IDMap/remove_ids/落盘）
│          09 性能优化与 GPU（批量/线程/内存账本/faiss-gpu 渠道）
└── 验收层：10 生产实战与自测（RAG 检索端 FAISS 方案 + 20 题）
```

## 2. 模块导航

| 篇 | 模块 | 核心内容 | 核心产出 |
|:---:|------|---------|---------|
| 00 | 总览 | 导图/分工/路线/速查/误区 | 学习计划 |
| 01 | faiss-cpu 是什么 | 算法库定位/2026 基线/替代品 | 认知 |
| 02 | 安装与快速开始 | 装包/IndexFlatL2 首个检索 | 会建索引 |
| 03 | 索引类型与选型 | 索引家族全景/选型决策 | 会选型 |
| 04 | Flat 精确检索 | L2/IP/归一化余弦/批量 | 会精确检索 |
| 05 | IVF 倒排索引 | train-add 两阶段/nlist/nprobe | 会近似检索 |
| 06 | PQ 内存压缩 | 乘积量化/压缩比/精度权衡 | 会压内存 |
| 07 | HNSW 图索引 | M/efConstruction/efSearch | 会建图索引 |
| 08 | ID 管理与持久化 | IDMap/remove_ids/落盘加载 | 会当库用 |
| 09 | 性能优化与 GPU | 批量/线程/内存/GPU 渠道 | 会调优 |
| 10 | 实战与自测 | RAG 检索端方案 + 20 题 | 毕业产出 |

## 3. 与主体系的分工

**与 RAG 体系（`../../../04-RAG检索增强生成/阶段%201：基础概念/00-阶段总览与学习路径.md`）的分工**：RAG 链路"加载 → 切分 → 向量化 → 检索 → 生成"，那个体系讲方法论，**本体系专讲"检索"环节的算法内核**——"检索靠什么找相似：FAISS 就是那个'找相似'的计算引擎"。

**与向量数据库主体系（`../../../08-向量数据库/向量数据库/00-向量数据库知识体系总览.md`）及其中 FAISS 篇（`../../../08-向量数据库/向量数据库/05-FAISS高性能索引.md`）的分工**：那套讲**原理与选型全景**（HNSW/IVF 原理、多库对比），本体系是 **13-Python 框架视角的实操篇**——"原理课讲索引为什么快，本体系讲 Python 里每个索引类怎么建、每个参数怎么调"。

**与 chromadb（`../chromadb/00-chromadb总览.md`）的分工**：Chroma 是**数据库**（向量+元数据+原文一体、过滤检索一体），FAISS 是**算法库**（只管'找相似'）——**"Chroma/Milvus 的 HNSW 内核就是 FAISS 的同类实现，FAISS 是'裸内核'：更快更省，但持久化、过滤、去重全要自己搭"**；两体系交叉阅读建立"库 vs 内核"完整心智。

**与 Milvus 主体系（`../../../08-向量数据库/Milvus/00-Milvus知识体系总览.md`）及 milvus‑python（`../../强烈推荐%EF%BC%88做项目必用%EF%BC%8C简历加分%EF%BC%89/milvus‑python/00-milvus‑python总览.md`）的分工**：**Milvus 内部用 FAISS 同族索引做引擎**——"FAISS 是引擎，Milvus 是整车"；引擎篇学算法本质，整车篇学分布式运维。

**与 LangChain（`../../强烈推荐%EF%BC%88做项目必用%EF%BC%8C简历加分%EF%BC%89/LangChain/00-LangChain总览.md`）、LlamaIndex（`../../强烈推荐%EF%BC%88做项目必用%EF%BC%8C简历加分%EF%BC%89/llama‑index/00-LlamaIndex总览.md`）的分工**：两个框架都有 FAISS vector store 封装——"框架管编排，FAISS 管内核——裸库是集成层的地基"（08 篇详讲 ID 管理与持久化，就是'裸 FAISS 当库用'的完整姿势）。

**2026-08 基线**：faiss-cpu **1.14.3**（2026-06-13 发布，GitHub 已出 v1.15.0 待 PyPI 同步）；**PyPI 周下载约 350 万**、0 漏洞；**关键认知**：faiss-cpu 是官方 CPU 轮子，**faiss-gpu 的 PyPI 包已停更**（卡在 1.7.x、Python ≤3.10）——GPU 版官方通道是 conda（`conda install faiss-gpu -c pytorch` / conda-forge），pip 用户走社区轮子 faiss-gpu-cu11/cu12（09 篇详讲）；1.14 新特性：Apple Silicon 默认启用 Metal GPU 后端、MetalIndexIVFFlat 等——**"FAISS 是向量检索的事实标准内核：Chroma 用 HNSW、Milvus 用 FAISS 同族，学 FAISS = 学一切向量库的底牌"**。

## 4. 学习路线推荐

**路线一：标准路线（3-5 天）**——01 → 10 逐篇 + 每篇动手——**毕业标准：独立实现"嵌入 → 归一化 → HNSW+IDMap 索引 → 持久化 → 批量检索"的 RAG 检索端 FAISS 方案**。

**路线二：速成路线（1-2 天）**——01 → 02 → 04 → 07 → 08 → 10（跳过 03/05/06/09 精读）——适合已有向量库经验、只想最快把 FAISS 用起来的人。

**路线三：原理驱动路线**——先读 03（索引家族）→ 按需深潜 04/05/06/07 单类索引——**"索引选型是 FAISS 的第一课，选对了类型，参数只是微调"**。

## 5. 核心概念速查

| 概念 | 一句话 | 对应篇 |
|------|--------|:---:|
| IndexFlatL2 | 精确检索：全量扫描 L2 距离，100% 召回 | 04 |
| IndexFlatIP | 精确检索：内积距离（归一化后 = 余弦） | 04 |
| IndexIVFFlat | 近似检索：先聚类分桶，只搜 nprobe 个桶 | 05 |
| IndexIVFPQ | 近似 + 压缩：乘积量化，内存降一个量级 | 06 |
| IndexHNSWFlat | 图索引：多层级图遍历，速度质量兼得 | 07 |
| IndexIDMap | ID 包装器：自定义 int64 ID + add_with_ids | 08 |
| train | 训练阶段：IVF/PQ 建索引前必须（用代表性子集） | 05/06 |
| nprobe | IVF 查询探测桶数：越大越准越慢 | 05 |
| efSearch / efConstruction | HNSW 查询/构建候选队列 | 07 |
| normalize_L2 | L2 归一化：cosine 的正确姿势（IP 前置） | 04 |
| remove_ids / reconstruct | 删除/还原向量 | 08 |
| write_index / read_index | 索引落盘与加载 | 08 |
| faiss-gpu | GPU 版：PyPI 停更，官方通道 conda | 09 |

## 6. 常见误区

**误区一：FAISS 是"向量数据库"**——它是**算法库**：只管"找相似"，**持久化、元数据、过滤、去重、增量全要自己搭**——"用 FAISS 的代价是这些都要自己写，收益是内核级的速度与控制力"（01/08 篇）。

**误区二：选一个"最好的索引"**——FAISS 哲学是**按场景组合**：小数据用 Flat、大数据近似用 IVF、质量优先用 HNSW、内存吃紧上 PQ——**"选型 = 数据量 × 精度要求 × 内存预算"**（03 篇决策表）。

**误区三：IVF 忘了 train**——IVF/PQ 建索引**必须先 train 后 add**，忘记 train 会得到垃圾结果（不报错！）——"不报错的错最危险"（05 篇）。

**误区四：直接搜"余弦相似度"**——FAISS 没有 cosine 索引类，**正确姿势是 L2 归一化 + IndexFlatIP**——"归一化一次入库、一次查询，两次缺一不可"（04 篇）。

**误区五：float64 直接喂**——FAISS 只吃 **float32**（dtype 不匹配直接报错或静默截断）——"float32 是 FAISS 的第一条铁律"（02 篇）。

**误区六：更新向量直接 add**——FAISS 的 add 是**追加**不是更新，重复 add = 重复向量污染结果；**正确姿势 remove_ids + 重新 add**（08 篇）。

**误区七：`pip install faiss-gpu` 就行**——PyPI 的 faiss-gpu **停更在 1.7.x**（Python ≤3.10 才装得上）；2026 的正确姿势：CPU 用 faiss-cpu，GPU 用 conda 或社区轮子（09 篇）。

## 7. 一周学习计划示例

| 天 | 内容 | 动手任务 |
|:---:|------|---------|
| 1 | 01 + 02 | 装 faiss-cpu，建 IndexFlatL2 检索 1000 条随机向量 |
| 2 | 03 + 04 | 对比 Flat/IVF/HNSW 三种索引的检索结果与耗时 |
| 3 | 05 + 06 | IVF train-add 全流程；PQ 压缩后对比精度与内存 |
| 4 | 07 | HNSW 调 M/efSearch，画出召回率-耗时曲线 |
| 5 | 08 + 09 | IDMap + 落盘加载；批量检索压测 10 万向量 |
| 6-7 | 10 自测 + 面试 | RAG 检索端 FAISS 方案全链路，跑 20 题 |

## 8. 快速自测 10 题

1. FAISS 与 Chroma/Milvus 的本质区别？"算法库 vs 数据库"意味着什么？
2. 为什么 float32 是第一条铁律？float64 会发生什么？
3. 四大索引家族（Flat/IVF/PQ/HNSW）各自解决什么问题？
4. cosine 在 FAISS 里的正确姿势？为什么不能直接搜余弦？
5. IVF 的 train 为什么必需？训练数据用什么？
6. nprobe 与召回率/延迟的关系？调优起点是多少？
7. PQ 压缩的原理？内存能降多少、代价是什么？
8. IndexIDMap 干什么？为什么更新要先 remove 再 add？
9. 索引怎么持久化？write_index/read_index 之外还缺什么？
10. faiss-gpu 的 2026 状态？正确安装渠道？

## 9. 参考来源

- [facebookresearch/faiss GitHub 官方仓库（Release 1.14.3/1.15.0）](https://github.com/facebookresearch/faiss)
- [faiss-cpu PyPI 页面（1.14.3 版本信息）](https://pypi.org/project/faiss-cpu/1.14.3/)
- [Faiss indexes 官方 Wiki（索引家族全景）](https://github.com/facebookresearch/faiss/wiki/Faiss-indexes)
- [FAISS 官方文档（Getting started/Python API）](https://faiss.ai/)
- [faiss-gpu 停更讨论（Issue #3617）](https://github.com/facebookresearch/faiss/issues/3617)
- [faiss-gpu-cu12 社区轮子（kyamagu/faiss-wheels）](https://socket.dev/pypi/package/faiss-gpu-cu12/overview/1.10.0)
- [FAISS Python API 详解（IndexIDMap/remove_ids/批量检索）](https://www.golinuxcloud.com/faiss-python-api/)

---

**下一模块**：[01-faiss-cpu是什么.md](01-faiss-cpu是什么.md)
