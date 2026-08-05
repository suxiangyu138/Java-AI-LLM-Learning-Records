# 03 - 主流 Embedding 模型全景对比

> 🎯 选对 Embedding 模型，RAG 效果能差 30% 以上 — 从开源到商业、从中文到多语言、从轻量到旗舰，一文覆盖全场景选型

---

## 目录

1. [模型分类全景](#1-模型分类全景)
2. [中文 Embedding 模型](#2-中文-embedding-模型)
3. [英文与国际模型](#3-英文与国际模型)
4. [多语言模型](#4-多语言模型)
5. [代码 Embedding 模型](#5-代码-embedding-模型)
6. [多模态 Embedding 模型](#6-多模态-embedding-模型)
7. [选型决策树](#7-选型决策树)
8. [模型能力基准对比](#8-模型能力基准对比)

---

## 1. 模型分类全景

```text
Embedding 模型家族：

┌────────────────────────────────────────────────────────┐
│                  Embedding 模型                          │
├──────────┬──────────┬──────────┬──────────┬────────────┤
│ 中文专用  │ 英文通用  │ 多语言   │ 代码专用  │ 多模态     │
├──────────┼──────────┼──────────┼──────────┼────────────┤
│ m3e-base │ text-emb │ BGE-M3   │ CodeBERT │ CLIP       │
│ text2vec │ E5       │ Jina v3  │ UniXcoder│ BLIP       │
│ BGE-zh   │ GTE      │ Qwen-Emb │          │ Jina CLIP  │
└──────────┴──────────┴──────────┴──────────┴────────────┘
```

---

## 2. 中文 Embedding 模型

### 2.1 主流中文模型

| 模型 | 维度 | 最大长度 | 开发者 | 特点 |
|------|:---:|:---:|--------|------|
| **BGE-M3** | 1024 | 8192 | 智源 BAAI | **中文首选**，多语言，支持稠密+稀疏 |
| **Qwen-Embedding** | 1024~4096 | 32768 | 阿里 | 超长文本、Qwen 生态 |
| **m3e-base** | 768 | 512 | 社区 | 轻量中文，适合本地 |
| **text2vec-large-chinese** | 1024 | 512 | 社区 | 经典中文模型 |
| **BGE-zh** | 768 | 512 | 智源 | BGE-M3 的前身，轻量替代 |

### 2.2 BGE-M3 详解（中文首选）

```text
BGE-M3 = BAAI General Embedding - Multilingual, Multi-Granularity, Multi-Function

三大特性：
  ① Multi-Lingual：支持 100+ 种语言
  ② Multi-Granularity：支持不同粒度（词级/句级/段级）
  ③ Multi-Function：同时支持 Dense（稠密）和 Sparse（稀疏）检索

Dense 向量：1024 维浮点数 — 语义检索
Sparse 向量：词权重字典 — 关键词匹配
  → 混合检索 = Dense + Sparse → 精度大幅提升

部署：
  ollama pull bge-m3          # 一行命令本地部署
```

### 2.3 Qwen-Embedding 详解（长文本首选）

```text
Qwen-Embedding = 阿里通义千问系列 Embedding 模型

突出优势：
  ✅ 超长文本：支持 32K token 输入
  ✅ 多尺寸：从 0.5B 到 7B 参数可选
  ✅ 生态整合：与 Qwen LLM 配合效果最佳
  ✅ 多语言：中英文均优秀

部署：
  ollama pull qwen-embedding   # 需先配置 Ollama 模型源
```

---

## 3. 英文与国际模型

| 模型 | 维度 | 最大长度 | 开发者 | 特点 |
|------|:---:|:---:|--------|------|
| **text-embedding-3-small** | 1536 | 8192 | OpenAI | 性价比最高商业模型 |
| **text-embedding-3-large** | 3072 | 8192 | OpenAI | 精度最高的商业模型 |
| **E5-mistral-7b-instruct** | 4096 | 32768 | Microsoft | 基于 Mistral-7B，超强 |
| **GTE-large** | 1024 | 8192 | 阿里 | 英文通用场景 |
| **SFR-Embedding-Mistral** | 4096 | 32768 | Salesforce | 基于 Mistral，高精度 |

### 3.1 OpenAI Embedding 定价（2024）

| 模型 | 每 1M tokens | 说明 |
|------|:---:|------|
| text-embedding-3-small | $0.02 | 1536 维，够用 |
| text-embedding-3-large | $0.13 | 3072 维，精度最高 |
| Ada v2 (旧) | $0.10 | 1536 维，即将退役 |

> 💡 text-embedding-3-small 的性价比极高 — 100 万 token 仅 $0.02（约 ¥0.14），中小规模项目首选。

---

## 4. 多语言模型

| 模型 | 语言数 | 维度 | 特点 |
|------|:---:|:---:|------|
| **BGE-M3** | 100+ | 1024 | 中英文均衡、Dense+Sparse |
| **Jina Embeddings v3** | 89 | 1024 | 多语言、任务特定 LoRA |
| **multilingual-e5-large** | 100+ | 1024 | E5 系列多语言版本 |
| **LaBSE** | 109 | 768 | Google 出品 |

---

## 5. 代码 Embedding 模型

| 模型 | 维度 | 用途 |
|------|:---:|------|
| **CodeBERT** | 768 | 代码语义检索 |
| **UniXcoder** | 768 | 跨模态（代码+文本）检索 |
| **CodeT5+** | 256~1024 | 代码理解+生成 |
| **Voyage-code-2** | 1536 | 商业代码 Embedding 最强 |

```text
代码 Embedding 的应用：
  ✅ 代码搜索："找项目中所有处理 JWT token 的函数"
  ✅ Bug 定位：用错误日志匹配相似代码
  ✅ 代码审查：检索类似的 PR 和修复方案
```

---

## 6. 多模态 Embedding 模型

| 模型 | 模态 | 维度 | 特点 |
|------|:---:|:---:|------|
| **CLIP** | 文本↔图片 | 512/768 | 多模态鼻祖 |
| **BLIP-2** | 文本↔图片 | 768 | 更强的图文理解 |
| **Jina CLIP** | 文本↔图片 | 768/1024 | 商业级多模态 |
| **ImageBind** | 6 种模态 | 1024 | 文本/图片/音频/深度/热力/IMU |

---

## 7. 选型决策树

```text
需要 Embedding 模型 → 选哪个？

  ① 中文为主？
     ├── 需要长文本 + Qwen 生态 → Qwen-Embedding
     ├── 需要 Dense+Sparse 混合 → BGE-M3
     └── 轻量本地部署 → m3e-base

  ② 英文为主？
     ├── 追求精度 → text-embedding-3-large
     ├── 追求性价比 → text-embedding-3-small
     └── 开源本地 → E5-mistral-7b

  ③ 中英文混合？
     └── BGE-M3（免费）或 text-embedding-3（付费）

  ④ 代码场景？
     └── UniXcoder（开源）或 Voyage-code-2（商业）

  ⑤ 预算极度有限？
     └── m3e-base (768维) — 单 CPU 也能跑
```

---

## 8. 模型能力基准对比

### 8.1 中文检索（CMTEB 基准）

| 模型 | 检索 NDCG@10 | 语义 STS | 分类 Acc | 聚类 V-measure |
|------|:---:|:---:|:---:|:---:|
| BGE-M3 | 0.62 | 0.83 | 0.78 | 0.52 |
| Qwen-Embedding | 0.64 | 0.85 | 0.80 | 0.53 |
| m3e-base | 0.55 | 0.78 | 0.72 | 0.45 |
| text-embedding-3-small | 0.58 | 0.80 | 0.75 | 0.48 |

### 8.2 英文检索（MTEB 基准）

| 模型 | 检索 NDCG@10 | 语义 STS | 分类 Acc |
|------|:---:|:---:|:---:|
| text-embedding-3-large | 0.65 | 0.86 | 0.82 |
| E5-mistral-7b | 0.64 | 0.85 | 0.81 |
| BGE-M3 | 0.60 | 0.82 | 0.77 |
| text-embedding-3-small | 0.58 | 0.80 | 0.76 |

---

## 核心要点回顾

- 中文场景首选 BGE-M3（Dense+Sparse 混合）或 Qwen-Embedding（超长文本）
- 英文高精度用 text-embedding-3-large，性价比用 text-embedding-3-small
- 本地部署推荐 BGE-M3（Ollama 一行命令）
- 向量维度是精度和成本的平衡：768 轻量、1024 通用、3072 高精度
- 不同模型的向量不能混用（向量空间分布不同）
