# 00 - RAG 知识体系总览

> 🎯 RAG = 检索 + 生成，是 LLM 落地最成熟的范式 — 解决幻觉、注入私域知识、成本可控。这是 AI 应用开发者的必修课

---

## 目录

1. [知识全景](#1-知识全景)
2. [文件导航](#2-文件导航)
3. [学习路线](#3-学习路线)

---

## 1. 知识全景

```
RAG 精通体系（12个文件）
│
├── 🏗️ 基础篇（01-02）
│   ├── 01-RAG概述与核心原理.md              # 什么是RAG/为什么需要/RAG vs 微调/核心链路
│   └── 02-RAG技术栈全景.md                  # LangChain/LlamaIndex/向量数据库/Embedding/LLM
│
├── 📥 数据篇（03-04）
│   ├── 03-文档加载与解析.md                 # PDF/HTML/Markdown解析/多格式处理/元数据
│   └── 04-文本切片策略.md                   # 固定/语义/递归切片/Chunk Size/Overlap
│
├── 🔍 检索篇（05-07）
│   ├── 05-Embedding与向量索引.md             # Embedding模型选型/向量入库/FAISS-Milvus
│   ├── 06-检索策略与优化.md                  # 相似度检索/Top-K/阈值过滤/查询改写
│   └── 07-混合检索与Rerank.md               # BM25+Dense/RRF融合/Cross-Encoder重排序
│
├── 🚀 进阶篇（08-10）
│   ├── 08-RAG评估与质量保障.md               # RAGAS/忠实度/相关性/检索精度评估
│   ├── 09-高级RAG范式.md                     # Self-RAG/Corrective-RAG/GraphRAG/Agentic-RAG
│   └── 10-RAG与Agent结合.md                  # ReAct RAG/Tool-RAG/多步检索Agent
│
├── 📋 面试篇（11）
│   └── 11-RAG生产实战与面试题.md             # 生产架构/避坑指南/高频面试题
│
└── 📌 00-RAG知识体系总览.md                    # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | RAG知识体系总览 | 全景 + 路线 | — |
| 01 | RAG概述与核心原理 | 定义/链路/RAG vs 微调 vs 长上下文/适用场景 | ⭐⭐⭐⭐ |
| 02 | RAG技术栈全景 | LangChain/LlamaIndex/Haystack/向量库/Embedding选型 | ⭐⭐⭐ |
| 03 | 文档加载与解析 | PDF解析/HTML清洗/多格式处理/元数据提取 | ⭐⭐⭐ |
| 04 | 文本切片策略 | 递归切片/语义切片/Chunk Size/Overlap/多粒度索引 | ⭐⭐⭐⭐ |
| 05 | Embedding与向量索引 | 模型选型/向量入库/FAISS/Milvus/Chroma/Pinecone | ⭐⭐⭐ |
| 06 | 检索策略与优化 | 相似度检索/Top-K/阈值过滤/查询改写/多路召回 | ⭐⭐⭐⭐ |
| 07 | 混合检索与Rerank | BM25+Dense融合/RRF/Cross-Encoder重排序 | ⭐⭐⭐⭐ |
| 08 | RAG评估与质量保障 | RAGAS框架/忠实度/答案相关性/检索精度/端到端评估 | ⭐⭐⭐ |
| 09 | 高级RAG范式 | Self-RAG/CRAG/GraphRAG/Agentic RAG/多模态RAG | ⭐⭐⭐ |
| 10 | RAG与Agent结合 | ReAct RAG/Tool-Augmented RAG/多步检索 | ⭐⭐⭐ |
| 11 | RAG生产实战与面试题 | 生产架构/避坑TOP10/15道面试题 | ⭐⭐⭐ |

---

## 3. 学习路线

### 🟢 L1：理解 RAG（30分钟）

```
01-概述 → 02-技术栈
产出：理解 RAG 链路、知道 LangChain + 向量库 + Embedding + LLM 四件套
```

### 🔵 L2：搭建 RAG（1.5小时）

```
03-文档解析 → 04-切片 → 05-Embedding+索引 → 06-检索
产出：能从零搭建一个基础 RAG 系统
```

### 🟣 L3：优化 RAG（1小时）

```
07-混合检索+Rerank → 08-评估
产出：能诊断 RAG 问题、用混合检索和 Rerank 提升精度
```

### 🟡 L4：高级范式+面试（1小时）

```
09-高级RAG → 10-Agent → 11-实战面试
产出：了解前沿 RAG 范式、覆盖面试题
```
