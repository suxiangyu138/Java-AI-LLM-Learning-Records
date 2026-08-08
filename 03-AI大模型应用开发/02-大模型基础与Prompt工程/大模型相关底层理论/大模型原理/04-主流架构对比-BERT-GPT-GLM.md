# 04 - 主流架构对比：BERT、GPT、GLM

> 🎯 三大架构代表了 LLM 的三个方向 — 理解（BERT）、生成（GPT）、统一（GLM）。看清架构差异，才能选对模型做对事

---

## 目录

1. [架构分支总览](#1-架构分支总览)
2. [BERT：双向编码器](#2-bert双向编码器)
3. [GPT：单向解码器](#3-gpt单向解码器)
4. [GLM：统一架构](#4-glm统一架构)
5. [T5：Text-to-Text](#5-t5text-to-text)
6. [四者核心对比](#6-四者核心对比)
7. [架构选型指南](#7-架构选型指南)

---

## 1. 架构分支总览

```text
                    Transformer (2017)
                    Encoder + Decoder
                           │
          ┌────────────────┼────────────────┐
          │                │                │
    Encoder-Only      Decoder-Only     Encoder-Decoder
          │                │                │
       BERT              GPT               T5
     (2018, Google)   (2018, OpenAI)   (2019, Google)
          │                │                │
     RoBERTa/ALBERT    GPT-2/3/4        BART/mT5
          │           LLaMA/Qwen
          │           DeepSeek/GLM-4
          │
    改进：GLM (2022, 清华)
    融合双向+自回归

当前格局：Decoder-Only 统治，Encoder-Only 退守 Embedding
```

---

## 2. BERT：双向编码器

### 2.1 架构特点

```text
BERT = Transformer 的 Encoder 部分

关键设计：
  ✅ 双向注意力：每个 token 同时看到左右所有 token
  ✅ 无解码器，不生成文本
  ✅ 预训练 → 微调流程

规模：
  BERT-base:  12 层, 768 维, 12 头, 110M 参数
  BERT-large: 24 层, 1024 维, 16 头, 340M 参数
```

### 2.2 两大预训练任务

```text
① MLM（Masked Language Model）
   输入: "The [MASK] sat on the mat"
   预测: "cat"
   
   细节：
   - 随机选 15% token 做预测
   - 其中 80% → [MASK]
   - 其中 10% → 随机替换
   - 其中 10% → 不变
   原因：推理时没有 [MASK]，需要让模型学会处理真实词

② NSP（Next Sentence Prediction）
   输入: 句子A + 句子B
   判断: B 是否是 A 的下一句
   
   注：后续 RoBERTa 实验证明 NSP 作用不大，已弃用
```

### 2.3 优势与局限

| 优势 | 局限 |
|------|------|
| 双向理解 → NLU 极强 | 不能生成文本 |
| 预训练+微调范式 | MLM 有 MASK 偏差 |
| 参数量小（110M/340M）| 仅适合理解类任务 |

### 2.4 适用场景

```text
✅ 文本分类（情感分析、垃圾检测）
✅ 命名实体识别（NER）
✅ 语义匹配（搜索、推荐）
✅ Embedding 提取（作为 Encoder 输出向量）

❌ 对话生成
❌ 代码生成
❌ 文案创作
```

---

## 3. GPT：单向解码器

### 3.1 架构特点

```text
GPT = Transformer 的 Decoder 部分（去掉 Cross-Attention）

关键设计：
  ✅ 因果掩码（Causal Mask）：只能看到当前位置之前的 token
  ✅ 自回归生成：逐 token 生成，每次基于已生成的内容
  ✅ 纯生成模型

GPT 系列演进：
  GPT-1 (2018): 117M, 12层, 768维 — 证明预训练+微调有效
  GPT-2 (2019): 1.5B, 48层 — 证明 Zero-Shot 能力
  GPT-3 (2020): 175B, 96层 — 颠覆性，Few-Shot 学习者
  GPT-4 (2023): ~1.8T (MoE) — 多模态，接近人类水平
```

### 3.2 自回归生成流程

```text
输入: "今天天气"
Step 1: 模型处理 "今天天气" → 预测概率最高的下一个 token → "真"
Step 2: 模型处理 "今天天气真" → 预测下一个 → "好"
Step 3: "今天天气真好" → "，"
Step 4: "今天天气真好，" → "适合"
...直到生成 <EOS> 或达到 max_length
```

### 3.3 GPT 系列关键创新

| 版本 | 创新 | 影响 |
|------|------|------|
| GPT-1 | 预训练+微调范式 | 证明可行性 |
| GPT-2 | Zero-Shot、更大规模 | 安全性争议 |
| GPT-3 | In-Context Learning、Few-Shot | 不用微调也能用 |
| GPT-3.5/ChatGPT | RLHF | 会聊天了 |
| GPT-4 | MoE、多模态 | 质的飞跃 |

### 3.4 优势与局限

| 优势 | 局限 |
|------|------|
| 生成能力极强 | 单向上下文 |
| Scaling 友好（越大越好） | 自回归生成慢 |
| Few-Shot/Zero-Shot 学习 | 训练成本极高 |
| 生态最丰富 | 闭源（GPT-4） |

---

## 4. GLM：统一架构

### 4.1 核心创新

```text
GLM（General Language Model）是 2022 年清华提出的统一框架

核心创新：自回归空白填充（Autoregressive Blank Infilling）

  BERT 的 MLM：掩码后独立预测每个 [MASK]
  GPT 的 CLM：从前到后逐个生成
  GLM：将一段连续文本视为空白，自回归地生成这一段

示例：
  输入: "The cat [BLANK] because [BLANK] was tired."
  
  GLM 先生成 [BLANK1] = "sat on the mat"
  再基于已生成内容，生成 [BLANK2] = "it"
  
  → 既用了双向上下文（理解空白前后），又用了自回归（生成空白）
```

### 4.2 与 BERT/GPT 的差异

| 对比 | 差异 |
|------|------|
| **GLM vs BERT** | GLM 能生成，解决了 BERT "纯理解"的局限 |
| **GLM vs GPT** | GLM 用双向上下文，解决了 GPT "只能看前面"的局限 |
| **统一性** | 一个模型同时胜任 NLU（分类）和 NLG（对话/生成） |

### 4.3 代表模型

```text
ChatGLM-6B: 轻量级，单卡可跑，中文友好
ChatGLM-130B: 千亿级，性能接近 GPT-3.5
GLM-4: 最新一代，128K 上下文，多模态，性能对标 GPT-4
```

---

## 5. T5：Text-to-Text

### 5.1 统一框架

```text
T5（Text-to-Text Transfer Transformer, Google 2019）

核心思想：所有 NLP 任务统一为 "Text → Text"

  翻译:     "translate English to German: Hello" → "Hallo"
  分类:     "sentiment: I love this movie" → "positive"
  问答:     "question: What is AI? context: ..." → "Artificial Intelligence..."
  摘要:     "summarize: [long text]" → "[summary]"

  一个模型 + 任务前缀 → 完成所有任务
```

### 5.2 T5 vs GPT vs BERT

| 维度 | T5 | GPT | BERT |
|------|:---:|:---:|:---:|
| **架构** | Encoder-Decoder | Decoder-Only | Encoder-Only |
| **框架** | Text-to-Text | 自回归生成 | MLM |
| **理解** | ✅ | ✅ | ✅ |
| **生成** | ✅ | ✅ | ❌ |
| **主流度** | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |

---

## 6. 四者核心对比

| 维度 | BERT | GPT | GLM | T5 |
|------|------|-----|-----|-----|
| **提出者** | Google | OpenAI | 清华大学 | Google |
| **年份** | 2018 | 2018 | 2022 | 2019 |
| **架构** | Encoder-Only | Decoder-Only | Encoder(改进) | Encoder-Decoder |
| **注意力** | 双向 | 单向(因果) | 双向+自回归 | 编码双向+解码单向 |
| **预训练任务** | MLM+NSP | CLM(Next Token) | 自回归空白填充 | Span Corruption |
| **生成能力** | ❌ | ✅ | ✅ | ✅ |
| **理解能力** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **开源** | ✅ | GPT-4×/LLaMA✅ | ✅ | ✅ |
| **中文友好** | ⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| **当前地位** | Embedding专用 | 绝对主流 | 中文首选开源 | 特定任务 |

---

## 7. 架构选型指南

```text
选 BERT 类：
  ✅ 纯文本理解任务（分类、NER、语义匹配）
  ✅ 需要 Embedding 向量
  ✅ 资源受限（110M 参数即可）

选 GPT 类（LLaMA/Qwen/DeepSeek）：
  ✅ 对话、生成、代码编写
  ✅ 通用 AI 应用（RAG、Agent）
  ✅ 需要 Follow Instructions

选 GLM 类：
  ✅ 中文场景（语义理解优于 LLaMA）
  ✅ 同时需要理解和生成
  ✅ 开源本地部署

选 T5 类：
  ✅ 有明确输入-输出结构的任务
  ✅ 翻译、摘要等 Seq2Seq 场景
```

---

## 核心要点回顾

- BERT = 双向理解、Encoder-Only、MLM 训练 → NLU 之王
- GPT = 单向生成、Decoder-Only、CLM 训练 → NLG 之王（当前主流）
- GLM = 双向+自回归、自回归填空 → 统一架构，中文最强开源
- T5 = Text-to-Text、Encoder-Decoder → 任务统一框架
- 2024 格局：Decoder-Only 统治，Encoder-Only 退居 Embedding
