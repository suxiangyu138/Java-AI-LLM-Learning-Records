# 🧠 Transformer、BERT、GPT、GLM 核心知识点

> Transformer 是 2017 年由 Google 提出的深度学习架构，彻底摒弃 RNN/LSTM 的序列依赖，以自注意力机制实现并行计算，成为 BERT、GPT、GLM 等大语言模型的基础架构。本文系统对比四大模型的核心架构、机制、优势与适用场景。

## 前置阅读

- [[深度学习核心知识点]]
- [[AI 大模型核心知识点]]

## 目录

1. [Transformer（核心架构）](#1-transformer核心架构)
2. [BERT（双向预训练模型）](#2-bert双向预训练模型)
3. [GPT（生成式预训练模型）](#3-gpt生成式预训练模型)
4. [GLM（通用语言模型）](#4-glm通用语言模型)
5. [四大模型核心对比总结](#5-四大模型核心对比总结)

## 1. Transformer（核心架构）

### 1.1 核心机制：自注意力（Self-Attention）

Transformer 的核心创新是**自注意力机制**，它让模型在处理每个 token 时，同时关注输入序列中所有其他 token 的关联程度，计算注意力权重，从而捕捉全局上下文信息。这与 RNN 仅能感知局部上下文有本质区别。

### 1.2 整体结构

Transformer 采用**编码器-解码器（Encoder-Decoder）**结构：

- **编码器（6层堆叠）**：由"多头注意力"和"前馈神经网络"组成，负责对输入序列进行特征提取，输出全局上下文表征。
- **解码器（6层堆叠）**：在编码器结构基础上，增加了"掩码多头注意力（Masked Multi-Head Attention）"，防止预测时提前看到后续 token（适用于生成任务）。

### 1.3 关键设计细节

- **位置编码（Positional Encoding）**：补充序列的位置信息，因为自注意力本身不具备顺序感知能力。
- **残差连接（Residual Connection）** 与 **层归一化（Layer Normalization）**：缓解梯度消失问题，支持深层网络训练。

### 1.4 核心优势与局限

| 维度 | 说明 |
|------|------|
| **优势** | 并行计算（无需逐词处理）、捕捉长距离依赖、特征提取能力强 |
| **局限** | 计算复杂度高（与序列长度的平方成正比 $O(n^2)$） |

## 2. BERT（双向预训练模型）

BERT（Bidirectional Encoder Representations from Transformers）是 2018 年由 Google 提出的预训练语言模型，基于 Transformer 的编码器构建。

### 2.1 核心架构

仅使用 Transformer 的**编码器**（无解码器），分为两个版本：

| 版本 | 编码器层数 | 注意力头数 | 隐藏层维度 |
|------|-----------|-----------|-----------|
| BERT-base | 12 | 12 | 768 |
| BERT-large | 24 | 16 | 1024 |

### 2.2 预训练任务（核心创新）

1. **掩码语言模型（MLM, Masked Language Model）**：随机掩盖输入序列中 15% 的 token，让模型预测被掩盖的 token，迫使模型学习**双向上下文关联**。
2. **下一句预测（NSP, Next Sentence Prediction）**：输入两个句子，让模型判断第二句是否是第一句的下一句，用于学习句子级别的语义关联。

### 2.3 核心特点与局限

| 维度 | 说明 |
|------|------|
| **特点** | 双向上下文感知、预训练+微调范式，适配自然语言理解（NLU）任务（情感分析、文本分类、问答、NER） |
| **局限** | 仅基于编码器，不具备生成能力；MLM 存在"掩码偏差"（训练时用 [MASK]，推理时无 [MASK]，导致分布不一致） |

## 3. GPT（生成式预训练模型）

GPT（Generative Pre-trained Transformer）是 2018 年由 OpenAI 提出的生成式语言模型，基于 Transformer 的解码器构建，专注于自然语言生成（NLG）任务。

### 3.1 核心架构

仅使用 Transformer 的**解码器**（无编码器），采用 **Decoder-only** 结构，通过掩码注意力确保生成时只能看到当前及之前的 token。

### 3.2 预训练任务

**单向语言模型（Causal Language Model, CLM）**：给定前序 token，预测下一个 token 的概率。本质是自回归生成，让模型学习语言的序列规律和上下文关联。

### 3.3 核心特点与局限

| 维度 | 说明 |
|------|------|
| **特点** | 自回归生成、擅长长文本生成；GPT-3 后具备 Few-Shot / Zero-Shot 能力，打破预训练+微调范式 |
| **局限** | 单向上下文，无法利用后续信息；自回归生成速度慢（逐词生成）；易产生重复文本 |

### 3.4 与 BERT 的核心区别

- BERT 是**双向编码器**（擅长 NLU），GPT 是**单向解码器**（擅长 NLG）
- BERT 依赖微调，GPT 擅长零样本/少样本生成

## 4. GLM（通用语言模型）

GLM（General Language Model）是 2022 年由清华大学提出的通用语言模型，融合 BERT 的双向上下文和 GPT 的自回归生成优势，是中文场景下表现优异的开源大模型。

### 4.1 核心架构

基于 Transformer 编码器，创新引入**自回归填充（Autoregressive Blank Infilling）**机制，本质是"双向上下文 + 单向生成"的结合，兼具 BERT 的双向语义理解能力和 GPT 的生成能力。

### 4.2 预训练任务（核心创新）

**自回归填充任务**：将输入序列中的连续 token 替换为一个空白标记，让模型自回归地生成空白部分的内容。模型既学习了双向上下文（理解空白前后的信息），又训练了自回归生成能力（生成空白内容）。

### 4.3 核心特点

- **统一理解与生成**：无需分别适配 NLU 和 NLG 任务
- **中文优化**：针对中文语义、分词、文化场景深度优化
- **开源可定制**：相较于 GPT 的闭源，GLM 系列开源，可本地部署

### 4.4 与 BERT、GPT 的区别

| 对比 | 差异 |
|------|------|
| **GLM vs BERT** | GLM 具备生成能力，解决了 BERT 无法生成的问题 |
| **GLM vs GPT** | GLM 采用双向上下文，解决了 GPT 单向上下文的局限，在中文章景下生成质量和语义连贯性更优 |

### 4.5 代表模型

- **ChatGLM-6B**：轻量开源，适配中文对话
- **GLM-4**：性能接近 GPT-4，支持多模态

## 5. 四大模型核心对比总结

| 模型 | 核心结构 | 核心机制 | 核心优势 | 核心应用场景 |
|------|---------|---------|---------|-------------|
| **Transformer** | 编码器+解码器 | 自注意力、多头注意力 | 并行计算、捕捉长距离依赖 | 所有大语言模型的基础架构 |
| **BERT** | 仅编码器 | 双向注意力、MLM/NSP | 语义理解能力强 | 文本分类、问答、NER（NLU 任务） |
| **GPT** | 仅解码器 | 单向自回归、CLM | 长文本生成能力强 | 文章生成、对话、代码生成（NLG 任务） |
| **GLM** | 仅编码器（改进） | 双向自回归、空白填充 | 理解+生成双适配、中文优化 | 中文对话、多任务适配（NLU+NLG） |

## 核心要点回顾

- Transformer 是 LLM 的架构基石，核心创新是自注意力机制与并行计算
- BERT 使用编码器，擅长 NLU 任务；GPT 使用解码器，擅长 NLG 任务
- GLM 融合双向上下文与自回归生成，是中文场景的优秀开源方案
- 架构选择取决于任务类型：理解优先选 BERT/编码器，生成优先选 GPT/解码器，兼顾选 GLM

## 参考资料

1. Vaswani et al., "Attention Is All You Need", NeurIPS 2017
2. Devlin et al., "BERT: Pre-training of Deep Bidirectional Transformers for Language Understanding", NAACL 2019
3. Radford et al., "Improving Language Understanding by Generative Pre-Training", OpenAI 2018
4. Du et al., "GLM: General Language Model Pretraining with Autoregressive Blank Infilling", ACL 2022
