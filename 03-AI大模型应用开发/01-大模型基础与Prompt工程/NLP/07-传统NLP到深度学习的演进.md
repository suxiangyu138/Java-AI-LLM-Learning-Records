# 07 - 传统 NLP 到深度学习的演进

> 🎯 NLP 五十年进化 — 从手写规则到统计模型、从 Word2Vec 到 Transformer、从专用模型到通用 LLM。理解演进史才能理解"为什么 LLM 是必然"

---

## 目录

1. [五阶段演进全景](#1-五阶段演进全景)
2. [规则时代 1950s-1990s](#2-规则时代-1950s-1990s)
3. [统计时代 1990s-2013](#3-统计时代-1990s-2013)
4. [神经网络时代 2013-2017](#4-神经网络时代-2013-2017)
5. [预训练时代 2018-2022](#5-预训练时代-2018-2022)
6. [LLM 时代 2022-至今](#6-llm-时代-2022-至今)

---

## 1. 五阶段演进全景

```text
NLP 演进的五大阶段：

  1950s       1990s      2013       2018       2022
   │           │          │          │          │
   规则 → 统计 → 神经网络 → 预训练 → LLM
   │           │          │          │          │
  手写规则     TF-IDF    Word2Vec   BERT/GPT   ChatGPT
  语法分析     HMM       LSTM       微调范式    Prompt
  专家系统     CRF       Seq2Seq               RAG/Agent
```

---

## 2. 规则时代（1950s-1990s）

```text
方法：语言学家手写语法规则 + 词典
代表：Eliza (1966, 第一个聊天机器人)

示例规则：
  IF 句子包含 "我" + "感到" + X
  THEN 回复 "为什么你感到 X？"

局限：
  ❌ 规则写不完（语言无穷无尽）
  ❌ 无法处理歧义和例外
  ❌ 换个语言全部重写
  ❌ 对拼写错误、口语化完全无法应对
```

---

## 3. 统计时代（1990s-2013）

### 3.1 核心范式

```text
核心理念：从数据中统计规律，而非人工写规则

代表技术：
  → N-gram 语言模型：P(wₜ|wₜ₋₂, wₜ₋₁) = count(前三词) / count(前两词)
  → TF-IDF：词的重要性 = 词频 × 逆文档频率
  → HMM (隐马尔可夫模型)：序列标注（分词、POS）
  → CRF (条件随机场)：NER、序列标注
  → IBM Model：统计机器翻译

优势：
  ✅ 数据驱动，无需人工规则
  ✅ 数学理论扎实（概率论+统计学）

局限：
  ❌ 特征工程繁重（人工设计模板特征）
  ❌ 数据稀疏（N-gram 的致命问题）
  ❌ 无法捕捉长距离依赖
```

---

## 4. 神经网络时代（2013-2017）

### 4.1 Word2Vec 革命（2013）

```text
Word2Vec 的突破：
  → 首次用神经网络学习词向量
  → 语义相近 → 向量距离近
  → "国王 - 男人 + 女人 ≈ 王后"

影响：
  NLP 从"符号"进入"语义"时代
  → 预训练词向量 + 下游任务微调 → 成为标准流程
```

### 4.2 Seq2Seq + Attention（2014-2016）

```text
Seq2Seq (Sutskever et al., 2014)：
  Encoder(编码器) → 语义向量 → Decoder(解码器)
  机器翻译：输入任意长度 → 输出任意长度

Attention (Bahdanau et al., 2015)：
  解码时动态关注编码器的不同位置
  → 解决了 Seq2Seq 的"信息瓶颈"问题
  → 长句翻译质量大幅提升
```

### 4.3 LSTM 时代

| 任务 | 模型 | 效果 |
|------|------|:---:|
| 文本分类 | TextCNN / BiLSTM | ⭐⭐⭐ |
| NER | BiLSTM-CRF | ⭐⭐⭐⭐ |
| 机器翻译 | LSTM Seq2Seq + Attention | ⭐⭐⭐⭐ |
| 文本生成 | LSTM LM | ⭐⭐⭐ |

---

## 5. 预训练时代（2018-2022）

### 5.1 Transformer 诞生（2017）

```text
《Attention Is All You Need》— 彻底改变了 NLP

核心创新：
  → 抛弃 RNN/LSTM → 纯 Attention → 可并行训练
  → 自注意力：每个 token 同时关注所有 token
  → 训练速度：LSTM 的 10-100×

这一年被称为 "ImageNet Moment for NLP"
```

### 5.2 BERT + GPT 双雄（2018）

```text
BERT (Google)：双向 Encoder
  → 预训练：Masked LM + Next Sentence Prediction
  → 微调：加分类头 → 各种 NLU 任务 SOTA
  → "预训练 + 微调" 范式统治 NLP

GPT (OpenAI)：单向 Decoder
  → 预训练：Next Token Prediction（自回归）
  → GPT-2/3 → Zero-Shot / Few-Shot 能力
  → 证明了"规模就是力量"
```

---

## 6. LLM 时代（2022-至今）

### 6.1 ChatGPT 的范式转变

```text
ChatGPT (2022.11) — NLP 的"iPhone 时刻"

传统 NLP 范式：
  数据收集 → 标注 → 训练专用模型 → 部署
  每个任务一个模型

LLM 新范式：
  Prompt → LLM → 结果
  一个模型做所有任务

根本变化：
  → 不再需要为每个任务训练模型
  → 不再需要大量标注数据
  → "软件 3.0"：Prompt Engineering 取代传统编程
```

### 6.2 技术栈演变

```text
传统 NLP 技术栈               LLM 时代技术栈
───────────────              ──────────────
分词 (jieba)                  Tokenizer (BPE)
TF-IDF / Word2Vec             Embedding (LLM 自带)
BiLSTM-CRF (NER)              Prompt → LLM
TextCNN (分类)                Prompt → LLM
SMT / NMT (翻译)              Prompt → LLM
Seq2Seq (摘要)                Prompt → LLM
```

---

## 核心要点回顾

- NLP 五阶段：规则 → 统计 → 神经网络 → 预训练 → LLM
- Word2Vec (2013) 让 NLP 从符号进入语义
- Transformer (2017) 让并行训练成为可能
- BERT+GPT (2018) 建立"预训练+微调"范式
- ChatGPT (2022) 统一所有 NLP 任务 → Prompt 替代模型训练
