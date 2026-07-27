# 06 - GPT：Decoder-Only 架构

> 🎯 GPT 架构定义了整个 LLM 时代——Decoder-Only + 自回归生成 + Scaling Law。理解因果注意力、自回归推理、以及为什么 Decoder-Only 最终统治了 NLP，是 LLM 工程师的必修课

---

## 目录

1. [GPT 架构全景](#1-gpt-架构全景)
2. [因果注意力 (Causal Attention)](#2-因果注意力-causal-attention)
3. [自回归生成](#3-自回归生成)
4. [Scaling Law](#4-scaling-law)

---

## 1. GPT 架构全景

```text
GPT 架构 = Transformer Decoder Stack（去掉 Cross-Attention）

为什么不要 Cross-Attention？
  → 原版 Transformer 的 Cross-Attention 用于 Encoder→Decoder 信息传递
  → GPT 没有 Encoder，不需要 Cross-Attention
  → 纯 Self-Attention + Causal Mask

GPT Block：
  x = x + MaskedMultiHeadAttention(LayerNorm(x))
  x = x + FFN(LayerNorm(x))
```

### GPT-1 vs GPT-2 vs GPT-3 架构变化

| 组件 | GPT-1 | GPT-2 | GPT-3 |
|------|:---:|:---:|:---:|
| 层数 | 12 | 48 | 96 |
| d_model | 768 | 1600 | 12288 |
| 头数 | 12 | 25 | 96 |
| 参数 | 117M | 1.5B | 175B |
| LayerNorm | Post-Norm | Pre-Norm | Pre-Norm |
| 位置编码 | Learned | Learned | Learned |
| 激活函数 | GELU | GELU | GELU |

## 2. 因果注意力 (Causal Attention)

### 2.1 Causal Mask

```python
def create_causal_mask(seq_len):
    """创建因果掩码 — 上三角矩阵（除对角线）"""
    mask = torch.tril(torch.ones(seq_len, seq_len))
    # [[1, 0, 0, 0],
    #  [1, 1, 0, 0],
    #  [1, 1, 1, 0],
    #  [1, 1, 1, 1]]
    return mask

# 在 Attention 中使用：
scores = Q @ K.T / math.sqrt(d_k)
scores = scores.masked_fill(mask == 0, float('-inf'))  # 不可见的 token → -inf
attn = F.softmax(scores, dim=-1)  # -inf → Softmax → 0
```

### 2.2 为什么只需要 Causal Attention？

```text
自回归语言模型的目标：
  P(w₁, w₂, ..., wₙ) = P(w₁) · P(w₂|w₁) · P(w₃|w₁,w₂) · ...

即：预测 wₙ 时，只能依赖 w₁...wₙ₋₁（之前的 token）
→ Causal Mask 完美满足这个约束

训练效率：
  → 一次前向传播可以并行计算所有位置的 loss
  → 因为 Causal Mask 保证了预测第 i 个 token 时不泄露 i 之后的信息
```

## 3. 自回归生成

```text
自回归 (Autoregressive) = 一个接一个生成

推理过程（无法并行，只能串行）：
  输入: "我"
  输出: "爱"
  输入: "我 爱"
  输出: "北"
  输入: "我 爱 北"
  输出: "京"
  ...

→ 这就是为什么 LLM 推理慢（串行生成）
→ 也是为什么需要 KV Cache（避免重算历史）
```

### 生成策略

| 策略 | 原理 | 特点 |
|------|------|------|
| **Greedy** | 每次选概率最高的 token | 确定性强，但容易重复 |
| **Temperature** | Softmax(x/T) — T>1 更随机 | 控制"创意度" |
| **Top-K** | 只从概率最高的 K 个中选 | 过滤低概率 token |
| **Top-P (Nucleus)** | 累积概率达到 P 就停止 | 动态调整候选集 |
| **Beam Search** | 保留 N 条候选序列 | 质量高但慢 |

## 4. Scaling Law

```text
Scaling Law (Kaplan et al., 2020)：
  LLM 的能力 = f(参数量, 数据量, 计算量)
  三者满足幂律关系 (Power Law)

核心发现：
  1. Loss 随参数量/数据量/计算量的增加而平滑下降
  2. 关系可以用简单的幂律公式描述 → 可预测！
  3. 模型规模比模型架构细节更重要

Chinchilla Scaling Law (DeepMind, 2022)：
  最优配置：每个参数约 20 个 token 的训练数据
  → 之前很多模型训练不够（token 太少）
  → Llama 3 用 15T tokens 训 405B → ~37 tokens/参数（数据充足）

为什么叫 Scaling "Law"（定律）？
  → 可以在小模型上训练 → 预测大模型的效果
  → 不用真去训练 405B 就能估计它的大概能力
  → 指导模型研发的资源分配
```

## 核心要点回顾

- GPT = Decoder-Only = 去掉 Cross-Attention 的 Transformer Decoder
- Causal Mask = 上三角 -inf → 每个 token 只能看到左边
- 推理过程 = 串行自回归生成（慢，需要 KV Cache 优化）
- 生成策略：Temperature（调节创意）、Top-P（动态阈值）、Beam Search（质量）
- Scaling Law = 模型能力随规模可预测地提升；Chinchilla = 数据也要跟上

## 参考资料

1. GPT-1/2/3 论文 — OpenAI
2. Scaling Laws 论文 (Kaplan et al., 2020)
3. Chinchilla 论文 (Hoffmann et al., 2022)
