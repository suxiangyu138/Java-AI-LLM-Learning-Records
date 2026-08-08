# 00 - NLP 知识体系总览

> 🎯 NLP 是 LLM 的母学科 — 从分词到 Embedding、从 NER 到文本分类，理解 NLP 经典任务才能理解 LLM 解决了什么问题

---

## 目录

1. [知识全景](#1-知识全景)
2. [文件导航](#2-文件导航)
3. [学习路线](#3-学习路线)

---

## 1. 知识全景

```
NLP 知识体系（11个文件）
│
├── 🏗️ 基础篇（01-03）
│   ├── 01-NLP概述与核心任务.md              # NLU/NLG/文本分类/NER/情感分析/机器翻译
│   ├── 02-文本预处理与分词.md                # 分词/词干提取/词形还原/停用词/文本清洗
│   └── 03-文本表示-BoW-TF-IDF到Word2Vec.md    # One-Hot/BoW/TF-IDF/Word2Vec/GloVe/FastText
│
├── 🔧 任务篇（04-06）
│   ├── 04-命名实体识别与序列标注.md           # NER/POS标注/CRF/BiLSTM-CRF/IOB2标注
│   ├── 05-文本分类与情感分析.md               # 传统ML分类/TextCNN/LSTM/BERT微调
│   └── 06-主题模型与文本聚类.md               # LDA/NMF/TextRank/文本摘要
│
├── 🚀 演进篇（07-08）
│   ├── 07-传统NLP到深度学习的演进.md           # 规则→统计→神经网络→预训练→LLM
│   └── 08-LLM时代的NLP范式转变.md             # 从微调→Prompt/RAG/Agent、任务边界消失
│
├── 🛠️ 实战篇（09）
│   └── 09-NLP工具与库实战.md                  # NLTK/spaCy/jieba/HuggingFace pipeline
│
├── 📋 面试篇（10）
│   └── 10-NLP面试题精选.md                    # 高频NLP面试题
│
└── 📌 00-NLP知识体系总览.md                    # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | NLP知识体系总览 | 全景导航 + 学习路线 | — |
| 01 | NLP概述与核心任务 | NLU/NLG/分类/NER/情感/翻译/问答/摘要 | ⭐⭐⭐ |
| 02 | 文本预处理与分词 | 中英文分词/词干提取/词形还原/停用词/清洗管线 | ⭐⭐⭐ |
| 03 | 文本表示-BoW到Word2Vec | One-Hot/BoW/TF-IDF/Word2Vec/GloVe/FastText/Embedding | ⭐⭐⭐⭐ |
| 04 | 命名实体识别与序列标注 | NER/POS/CRF/BiLSTM-CRF/BERT-NER/IOB2标注 | ⭐⭐⭐ |
| 05 | 文本分类与情感分析 | 朴素贝叶斯/SVM/TextCNN/LSTM/BERT微调分类 | ⭐⭐⭐ |
| 06 | 主题模型与文本聚类 | LDA/NMF/TextRank/文本摘要/K-Means聚类 | ⭐⭐ |
| 07 | 传统NLP到深度学习的演进 | 规则→统计→神经网络→预训练→LLM 五阶段 | ⭐⭐⭐ |
| 08 | LLM时代的NLP范式转变 | Prompt替代微调/RAG替代检索/Agent替代管线 | ⭐⭐⭐⭐ |
| 09 | NLP工具与库实战 | NLTK/spaCy/jieba/HuggingFace pipeline/代码实战 | ⭐⭐ |
| 10 | NLP面试题精选 | TF-IDF原理/Word2Vec训练/NER方法演进/NLP+LLM关系 | ⭐⭐⭐ |

---

## 3. 学习路线

### 🟢 L1：NLP 全景（30分钟）

```
01-概述 → 02-预处理 → 03-文本表示
产出：理解 NLP 任务分类、掌握文本预处理管线、理解 TF-IDF 和 Word2Vec
```

### 🔵 L2：经典任务（1小时）

```
04-NER → 05-文本分类 → 06-主题模型
产出：能用 BiLSTM-CRF 做 NER、能用 BERT 微调做文本分类
```

### 🟣 L3：演进理解（30分钟）

```
07-演进史 → 08-LLM 范式转变
产出：理解 NLP 五阶段演进、知道 LLM 时代哪些传统 NLP 任务被重塑
```

### 🟡 L4：实战 + 面试（1小时）

```
09-工具实战 → 10-面试题 → 系统回顾
产出：能用 HuggingFace pipeline 跑 NLP 任务、覆盖 NLP 高频面试题
```
