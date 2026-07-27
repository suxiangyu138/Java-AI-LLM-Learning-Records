# 02 - Embedding 模型训练与对比学习

> 🎯 理解 Embedding 模型的训练方式才能理解为什么它有效 — 对比学习拉近相似文本、推远无关文本，这是向量语义空间的构建原理

---

## 目录

1. [Embedding 模型的训练范式](#1-embedding-模型的训练范式)
2. [对比学习详解](#2-对比学习详解)
3. [Triplet Loss 与 InfoNCE](#3-triplet-loss-与-infonce)
4. [训练数据构造](#4-训练数据构造)
5. [两阶段训练策略](#5-两阶段训练策略)
6. [评估指标](#6-评估指标)

---

## 1. Embedding 模型的训练范式

### 1.1 从语言模型到 Embedding 模型

```text
Embedding 模型通常不是从零训练的，而是基于预训练语言模型改造：

  BERT/RoBERTa → 加 Pooling 层 → 加对比学习训练 → Embedding 模型
  Qwen/LLaMA → 加双向注意力 → 加对比学习 → Embedding 模型

改造的关键：
  ① 将"逐 token 输出"改为"整句输出一个向量"
  ② 用对比学习替代 Next-Token-Prediction
```

### 1.2 三种训练范式对比

| 范式 | 训练方式 | 代表模型 | 特点 |
|------|----------|----------|------|
| **交叉编码器** | 两句话拼接输入 → 输出相似度分数 | MonoT5 | 精度高但慢，不适合检索 |
| **双编码器** | 两句话分别编码 → 向量点积得相似度 | **BGE、Sentence-BERT** | 可预计算向量 → RAG 标配 |
| **对比学习** | 正例拉近、负例推远 | **BGE-M3、E5** | 当前 SOTA 方法 |

```text
为什么双编码器 + 对比学习是主流？

  交叉编码器：query+doc 拼接 → 模型推理 → 相似度
    → 每个 query-doc 对都要推理一次 → O(N) 次推理 → 太慢

  双编码器：doc 提前编码存向量 → query 编码一次 → 向量检索
    → query 只需推理一次 → O(1) 推理！
```

---

## 2. 对比学习详解

### 2.1 核心思想

```text
对比学习（Contrastive Learning）的核心目标：

  → 相似样本的表示向量在空间中"拉近"
  → 不相似样本的表示向量在空间中"推远"

关键问题：如何定义"相似"和"不相似"？

  正例对（相似）：
    ("今天天气真好", "今日阳光明媚")  — 语义相同
    (query, 被点击的文档)              — 用户反馈
    (原文, 回译后的文本)               — 数据增强

  负例对（不相似）：
    ("今天天气真好", "数据库连接超时") — 语义无关
    batch 内的其他样本                  — In-Batch Negatives
```

### 2.2 负例采样策略

```text
负例的质量直接影响模型效果：

① 简单负例（Easy Negatives）
    → 随机采样 → 太容易区分 → 模型学到很少
    → 仅训练初期有用

② 批内负例（In-Batch Negatives）
    → 把同 batch 内其他样本当负例
    → 免费！batch_size 越大，负例越多 → 效果越好
    → 大 batch (2048+) 是 SOTA 的关键

③ 难负例（Hard Negatives）
    → 用当前模型检索 → 排名靠前但不相关的样本
    → 模型已经"有点混淆"的样本 → 训练价值最高
    → BGE-M3 的训练中大量使用

④ 跨批负例（Cross-Batch Negatives）
    → 在多个 batch 间共享负例 → 进一步扩大负例池
```

---

## 3. Triplet Loss 与 InfoNCE

### 3.1 Triplet Loss

```text
Triplet Loss：最早的对比学习损失

  L = max(0, d(a, p) - d(a, n) + margin)

  a (anchor): 锚点样本
  p (positive): 正例（应与锚点相似）
  n (negative): 负例（应与锚点不同）
  margin: 正负例距离的最小间隔

直觉：
  正例距离必须比负例距离小至少 margin
  → "好样本至少要比坏样本近这么多"
```

```python
import torch
import torch.nn.functional as F

def triplet_loss(anchor, positive, negative, margin=0.5):
    """Triplet Loss 实现"""
    pos_dist = F.pairwise_distance(anchor, positive)  # 锚点-正例距离
    neg_dist = F.pairwise_distance(anchor, negative)  # 锚点-负例距离
    loss = torch.clamp(pos_dist - neg_dist + margin, min=0)
    return loss.mean()
```

### 3.2 InfoNCE Loss（当前 SOTA）

```text
InfoNCE Loss = 多分类交叉熵形式

  L = -log( exp(sim(q, d⁺)/τ) / Σ exp(sim(q, dⱼ)/τ) )
               └── 正例的分数 ──┘   └── 所有候选(正例+负例)的分数和 ──┘

  τ (temperature): 温度参数，控制分布的"锐度"
  sim: 余弦相似度

优势：
  ✅ 同时考虑多个负例（而非 Triplet 的一个）
  ✅ 梯度来自正例和所有负例的竞争
  ✅ 天然适合 In-Batch Negatives
```

```python
def info_nce_loss(query_emb, doc_emb, temperature=0.05):
    """
    query_emb: (batch_size, dim) — batch 内每个作为 query
    doc_emb:   (batch_size, dim) — 对应的正例
    """
    # 相似度矩阵
    sim = torch.matmul(query_emb, doc_emb.T) / temperature  # (B, B)
    
    # 对角线 = 正例相似度；非对角线 = In-Batch Negatives
    labels = torch.arange(sim.size(0)).to(sim.device)
    
    # 双向 loss（query→doc 和 doc→query）
    loss_q2d = F.cross_entropy(sim, labels)
    loss_d2q = F.cross_entropy(sim.T, labels)
    
    return (loss_q2d + loss_d2q) / 2
```

---

## 4. 训练数据构造

### 4.1 数据来源

| 来源 | 说明 | 规模 |
|------|------|:---:|
| **搜索点击日志** | query + 点击的文档 = 天然正例对 | 亿万级 |
| **问答社区** | 问题 + 采纳回答 = 正例对 | 百万级 |
| **回译增强** | 原文→翻译→回译→构造成正例 | 千万级 |
| **同义改写** | LLM 生成同义句 → 与原句配对 | 灵活 |
| **弱监督** | 标题 vs 正文、摘要 vs 全文 | 亿万级 |

### 4.2 数据清洗要点

```text
Embedding 训练数据的清洗比 LLM 更严格：

① 去重：相同的 query-doc 对只保留一份
② 长度过滤：过短（<5 tokens）或过长（>512 tokens）的去掉
③ 语言检测：多语言模型需要保持语言分布均衡
④ 质量过滤：用现有模型打分 → 低于阈值的视为噪声 → 丢弃
```

---

## 5. 两阶段训练策略

```text
现代 Embedding 模型的训练通常分两个阶段：

阶段一：弱监督预训练（Pre-training）
  数据：亿万级弱标注对（标题-正文、query-点击）
  目标：学习通用的语义表示
  数据规模极大但噪声多
  Batch Size: 2048~16384（越大越好）
  
阶段二：高质量微调（Fine-tuning）
  数据：百万级高质量标注对（人工标注、精选）
  目标：精准对齐语义
  加入 Hard Negatives
  Batch Size: 256~1024

BGE-M3 就是典型的两阶段训练产物
```

---

## 6. 评估指标

### 6.1 检索任务评估

| 指标 | 含义 | 说明 |
|------|------|------|
| **NDCG@10** | 前 10 个结果的排序质量 | 最常用 |
| **Recall@K** | 前 K 个结果包含正确答案的比例 | 侧重查全 |
| **MRR** | 第一个正确答案排名的倒数均值 | 侧重首位 |
| **MAP** | 平均精度 | 综合指标 |

### 6.2 MTEB 排行榜

```text
MTEB（Massive Text Embedding Benchmark）— Embedding 模型的"高考"

包含 8 大类任务、58 个数据集：
  → 检索（Retrieval）
  → 聚类（Clustering）
  → 分类（Classification）
  → 语义文本相似度（STS）
  → 重排序（Reranking）
  → 摘要（Summarization）
  → 双文本分类（Pair Classification）
  → 比特检索（Bitext Mining）

中文 MTEB 排行榜（2024）：
  1. Qwen-Embedding (阿里)
  2. BGE-M3 (BAAI)
  3. Jina Embeddings v3
```

---

## 核心要点回顾

- 对比学习 = 正例拉近 + 负例推远，是 Embedding 模型的核心训练方法
- InfoNCE Loss 是目前 SOTA，天然适合 In-Batch Negatives
- 大 Batch Size 是关键（负例多 → 信号丰富）
- 难负例挖掘是效果提升的重要技巧
- 两阶段训练：弱监督预训练 → 高质量微调
- MTEB 是评估 Embedding 模型的权威基准
