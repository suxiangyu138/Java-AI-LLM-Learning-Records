# 02 - GPT 模型架构与训练范式

> 🎯 GPT = Decoder-Only + Next Token Prediction + RLHF。三阶段训练是 ChatGPT 从"会说话"到"说人话"的核心秘密

---

## 目录

1. [GPT 是什么](#1-gpt-是什么)
2. [Decoder-Only 架构详解](#2-decoder-only-架构详解)
3. [三阶段训练全流程](#3-三阶段训练全流程)
4. [RLHF 数学原理](#4-rlhf-数学原理)
5. [GPT vs BERT vs T5](#5-gpt-vs-bert-vs-t5)
6. [训练成本](#6-训练成本)

---

## 1. GPT 是什么

GPT（Generative Pre-trained Transformer）是纯 Decoder-Only 的自回归语言模型架构。通过海量文本预训练学习语言规律，再通过 SFT + RLHF 对齐人类偏好。所有 GPT 系列模型——从 GPT-1 到 GPT-4o——都基于这一架构。

**核心本质**：单方向（从左到右）、逐 Token 预测下一个词的概率模型。

---

## 2. Decoder-Only 架构详解

GPT 使用 Transformer 的 Decoder 部分，关键设计：

**① Masked Self-Attention（因果掩码）**：每个 token 只能看到自己及之前的 token，保证自回归生成的"从左到右"特性。实现方式：在 Attention 分数矩阵上加上三角掩码（未来位置设为 -inf）。

**② Pre-LayerNorm**：LayerNorm 放在 Attention/FFN 之前（而非之后），训练更稳定。GPT-2 后全面采用。

**③ GELU 激活函数**：处处可导的平滑 ReLU 变体。$GELU(x) = x \cdot \Phi(x)$（$\Phi$ 为标准正态 CDF）。比 ReLU 更适合深层网络。

```text
GPT 架构尺寸演进：

GPT-1: 12层, 768维, 12头, 117M参数
GPT-2: 48层, 1600维, 25头, 1.5B参数
GPT-3: 96层, 12288维, 96头, 175B参数
GPT-4: MoE 8×220B, 激活~280B (推测)
```

---

## 3. 三阶段训练全流程

**阶段一：预训练（Pre-training）— 学会"说话"**
- 数据：万亿 token 互联网文本（网页、书籍、代码、论文）
- 任务：Next Token Prediction（给定上文，预测下一个词）
- 目标函数：$L = -\frac{1}{N}\sum \log P(w_t|w_{<t})$
- 产出：Base Model（只会续写，不会对话）
- GPT-4 预训练成本估算：~$100M

**阶段二：SFT（Supervised Fine-Tuning）— 学会"听指令"**
- 数据：数万条人工标注的（指令 → 回答）对
- 目标：交叉熵最小化
- 产出：Instruct Model（能遵循指令，但风格不稳定）
- 成本：标注成本 ~$100K

**阶段三：RLHF — 学会"说人话"**
- 步骤 ①：SFT Model 对同一 prompt 生成多个回答 → 人类标注员排序
- 步骤 ②：用排序数据训练 Reward Model（学会"打分"）
- 步骤 ③：以 RM 为奖励信号 + KL 约束，用 PPO 算法优化 SFT Model
- 产出：Chat Model（安全、有用、符合人类偏好）

---

## 4. RLHF 数学原理

PPO 目标函数：
$$\max_{\pi}\ \mathbb{E}[RM(prompt, response)] - \beta \cdot KL(\pi_{RL} \|\| \pi_{SFT})$$

- 第一项：Reward Model 打分 → 回答质量越高越好
- 第二项：KL 散度约束 → 新策略不能偏离 SFT 太远
- $\beta$：控制约束强度（$\beta$ 大 = 更保守、$\beta$ 小 = 更激进）

**为什么需要 KL 约束？** 没有约束时，RL 优化器会"作弊"——生成乱码或重复文本骗 Reward Model 高分（Reward Hacking）。KL 约束保证回答仍然"像人话"。

**KL 散度的方向选择**：$KL(\pi_{RL} \|\| \pi_{SFT})$ = 用新策略去近似旧策略的代价。选择这个方向是因为它鼓励 $\pi_{RL}$ 覆盖 $\pi_{SFT}$ 的所有模式（mode-covering），而非只聚焦少数高奖励模式（mode-seeking）。

---

## 5. GPT vs BERT vs T5

| 维度 | GPT (Decoder-Only) | BERT (Encoder-Only) | T5 (Enc-Dec) |
|------|:---:|:---:|:---:|
| 注意力方向 | 单向（因果掩码） | 双向 | 双向+单向 |
| 预训练任务 | Next Token Prediction | MLM + NSP | Span Corruption |
| 能否生成文本 | ✅ | ❌ | ✅ |
| 当前地位 | **绝对主流** | Embedding 专用 | 特定翻译/摘要 |
| 代表模型 | GPT-4o, LLaMA, Qwen | BERT, RoBERTa | T5, BART |

Decoder-Only 成为主流的原因：① 自回归天然适合对话生成；② 架构更简单、Scaling 友好；③ 统一预训练任务（Next Token Prediction）；④ GPT-4 的成功形成生态效应。

---

## 6. 训练成本

| 模型 | GPU | 训练时长 | 预估成本 |
|------|------|:---:|------|
| GPT-3 175B | 10,000 V100 | 数月 | ~$4.6M |
| GPT-4 | ~25,000 A100 | 90-100天 | ~$100M+ |

> 🎯 GPT = Decoder-Only + 三阶段训练。预训练学说话 → SFT 学听指令 → RLHF 学说人话。KL 约束是 RLHF 不掉链子的关键。
