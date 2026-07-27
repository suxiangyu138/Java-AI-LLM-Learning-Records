# 00 - Embedding 知识体系总览

> 🎯 Embedding 是 RAG 的基石 — 文本转向量、语义相似度、向量检索，三个环节缺一不可。理解 Embedding 才算真正入门 AI 应用开发

---

## 目录

1. [知识全景](#1-知识全景)
2. [文件导航](#2-文件导航)
3. [精通级学习路线](#3-精通级学习路线)

---

## 1. 知识全景

```
Embedding 精通体系（12个文件）
│
├── 🔧 原理篇（01-02）
│   ├── 01-Embedding原理与向量语义.md          # 定义/语义空间/余弦相似度/LLM vs Embedding
│   └── 02-Embedding模型训练与对比学习.md       # InfoNCE Loss/正负例构造/两阶段训练/MTEB
│
├── 📊 模型篇（03-05）
│   ├── 03-主流Embedding模型全景对比.md         # 中英文/多语言/代码/多模态 + 选型决策树
│   ├── 04-OpenAI-Embedding-API实战.md         # API调用/批量异步/Matryoshka缩维/成本/重试
│   └── 05-本地Embedding部署-Ollama与BGE-M3.md  # Ollama/Dense+Sparse/LangChain/云端vs本地
│
├── 🏗️ 工程篇（06-08）
│   ├── 06-文本预处理与切片策略.md              # 固定/语义/递归切片/overlap/预处理管线
│   ├── 07-向量相似度检索实战.md               # NumPy→FAISS→HNSW/ANN原理/生产级封装
│   └── 08-Embedding缓存与性能优化.md           # 内存/SQLite缓存/批处理/异步并发/性能基准
│
├── 🚀 应用篇（09-10）
│   ├── 09-Embedding在RAG中的集成.md            # 离线索引/在线检索/后处理/完整RAG代码
│   └── 10-混合检索-关键词与向量融合.md          # Dense+Sparse/BM25/RRF融合/完整示例
│
├── 📋 面试篇（11）
│   └── 11-Embedding生产避坑与面试题.md         # TOP10避坑/性能陷阱/15道面试题
│
└── 📌 00-Embedding知识体系总览.md               # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | Embedding知识体系总览 | 全景导航 + 学习路线 | — |
| 01 | Embedding原理与向量语义 | 定义/语义空间/余弦相似度/欧氏距离/LLM vs Embedding | ⭐⭐⭐ |
| 02 | Embedding模型训练与对比学习 | InfoNCE Loss/正负例构造/Hard Negatives/两阶段训练/MTEB | ⭐⭐⭐⭐ |
| 03 | 主流Embedding模型全景对比 | BGE-M3/Qwen/text-embedding-3/代码/多模态 + 选型决策树 | ⭐⭐⭐ |
| 04 | OpenAI-Embedding-API实战 | API调用/批量异步/Matryoshka缩维/成本计算/指数退避重试 | ⭐⭐⭐ |
| 05 | 本地Embedding部署-Ollama与BGE-M3 | Ollama部署/Dense+Sparse双模式/LangChain/云端vs本地抉择 | ⭐⭐⭐ |
| 06 | 文本预处理与切片策略 | 固定/语义/递归切片/overlap/预处理管线/不同文档类型 | ⭐⭐⭐ |
| 07 | 向量相似度检索实战 | NumPy向量化/FAISS索引/HNSW/ANN原理/生产级检索封装 | ⭐⭐⭐⭐ |
| 08 | Embedding缓存与性能优化 | 内存/SQLite缓存/批处理/异步并发/动态Batching/性能基准 | ⭐⭐⭐ |
| 09 | Embedding在RAG中的集成 | 离线索引/在线检索/检索后处理/完整MiniRAG代码/常见问题 | ⭐⭐⭐⭐ |
| 10 | 混合检索-关键词与向量融合 | Dense+Sparse/BM25/RRF融合/加权融合/完整HybridSearch代码 | ⭐⭐⭐⭐ |
| 11 | Embedding生产避坑与面试题 | TOP10避坑/性能陷阱/数据质量/模型陷阱/15道高频面试题 | ⭐⭐⭐ |

---

## 3. 精通级学习路线

### 🟢 L1：理解概念（30分钟）

```
01-Embedding原理与向量语义
产出：能解释"Embedding 是什么"、"为什么语义相近向量距离近"
```

### 🔵 L2：上手编码（1小时）

```
03-模型全景 → 04-OpenAI API 或 05-本地Ollama
产出：能调用 API/本地模型生成 Embedding、写相似度检索代码
```

### 🟣 L3：理解底层（1.5小时）

```
02-模型训练原理 → 07-向量检索实战 → 06-文本切片策略
产出：理解对比学习、能用 FAISS 做万级检索、掌握切片最佳实践
```

### 🟡 L4：工程落地（1.5小时）

```
08-缓存与性能优化 → 09-RAG集成 → 10-混合检索
产出：能搭建生产级 RAG 检索系统，缓存+批处理+混合检索全链路
```

### 🔴 L5：面试冲刺（30分钟）

```
11-生产避坑与面试题 → 系统回顾 01-10
产出：覆盖 15 道高频面试题，掌握 10 大生产避坑要点
```
