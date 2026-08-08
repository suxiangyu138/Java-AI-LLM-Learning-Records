# 07 - Encoder-Decoder 与 T5

> 🎯 原版 Transformer 是 Encoder-Decoder 架构——Encoder 理解输入，Decoder 生成输出。T5 将一切 NLP 任务统一为 "Text-to-Text" 范式。虽然今天 Decoder-Only 统治了 LLM，但理解 Encoder-Decoder 才能理解 Transformer 的完整设计

---

## 目录

1. [Encoder-Decoder 架构](#1-encoder-decoder-架构)
2. [Cross-Attention 机制](#2-cross-attention-机制)
3. [T5：Text-to-Text 统一范式](#3-t5text-to-text-统一范式)
4. [三架构对比与现状](#4-三架构对比与现状)

---

## 1. Encoder-Decoder 架构

```text
原版 Transformer 是 Encoder-Decoder（用于机器翻译）：

Encoder（双向注意力）：
  "我 爱 北京" → 编码为上下文表示
  → 每个 token 融合了整句话的信息

Decoder（因果注意力 + Cross-Attention）：
  生成 "I love Beijing"（逐 token）
  → 自注意力（因果） + 交叉注意力（看 Encoder 输出）
```

### 计算流程

```text
Encoder:
  x = "我爱北京"
  encoder_out = Encoder(x)   # (3, d_model) — 整句的上下文表示

Decoder:
  第 1 步: "<s>" + encoder_out → "I"
  第 2 步: "<s> I" + encoder_out → "love"
  第 3 步: "<s> I love" + encoder_out → "Beijing"
  第 4 步: "<s> I love Beijing" + encoder_out → "</s>"
```

## 2. Cross-Attention 机制

```text
Cross-Attention = Decoder 关注 Encoder 输出

与 Self-Attention 的唯一区别：Q/K/V 的来源

Self-Attention:  Q, K, V 都来自同一个序列
Cross-Attention: Q 来自 Decoder，K,V 来自 Encoder

直觉：
  Decoder 说："我现在要生成一个词，请 Encoder（源语言）给我信息"
  Query = Decoder 当前的生成状态
  Key, Value = Encoder 编码的源语言信息

计算：
  Q = Decoder_Output · W_Q
  K = Encoder_Output · W_K    ← 来自 Encoder！
  V = Encoder_Output · W_V    ← 来自 Encoder！
  Output = Softmax(Q·Kᵀ/√d_k) · V
```

## 3. T5：Text-to-Text 统一范式

### 3.1 核心思想

```text
T5 (Text-to-Text Transfer Transformer)：
  → 一切 NLP 任务统一为 "文本输入 → 文本输出"

翻译：
  输入: "translate English to German: Hello"
  输出: "Hallo"

分类：
  输入: "sentiment: This movie is great!"
  输出: "positive"

问答：
  输入: "question: What is the capital of France?"
  输出: "Paris"

→ 同一个模型、同一个 loss、同一个训练流程！
```

### 3.2 T5 架构特点

| 特性 | T5 |
|------|-----|
| 架构 | Encoder-Decoder |
| 参数量 | 60M → 11B |
| 注意力 | 相对位置编码（非绝对） |
| 激活函数 | ReLU（非 GELU） |
| 预训练 | Span Corruption（类似 MLM） |
| 范式 | Text-to-Text |

## 4. 三架构对比与现状

| 维度 | Encoder-Only (BERT) | Decoder-Only (GPT) | Encoder-Decoder (T5) |
|------|:---:|:---:|:---:|
| 代表 | BERT, RoBERTa | GPT, Llama, Qwen | T5, BART |
| 注意力 | 双向 Self-Attn | 因果 Self-Attn | 双向 + 因果 + Cross |
| 参数效率 | 高（理解任务） | 中 | 低（两层） |
| 理解能力 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 生成能力 | ❌ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 统一能力 | ❌ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 当前地位 | 被 LLM 取代 | **统治** | 小众但存在 |

```text
为什么 Decoder-Only 最终胜出？

1. 统一性：一个模型既理解又生成
2. 简单性：没有 Cross-Attention，架构最简洁
3. Scaling：GPT-3 证明足够大就能涌现 In-Context Learning
4. 生态：GPT 系列 + Llama 系列形成了最大的开源生态

Encoder-Decoder 仍有特定场景优势：
  → 翻译（天然的序列到序列）
  → 长输入短输出（Encoder 可并行处理长输入）
```

## 核心要点回顾

- Encoder-Decoder = 双向 Encoder + 因果 Decoder + Cross-Attention 桥接
- Cross-Attention = Decoder 的 Q 查询 Encoder 的 K,V（信息桥接）
- T5 = "一切任务都是 Text-to-Text" 的优雅统一范式
- Decoder-Only 统一了理解和生成 → 当前统治地位
- 翻译/摘要等 Seq2Seq 任务，Encoder-Decoder 仍有优势

## 参考资料

1. T5 论文 (Raffel et al., 2020)
2. BART 论文 (Lewis et al., 2019)
