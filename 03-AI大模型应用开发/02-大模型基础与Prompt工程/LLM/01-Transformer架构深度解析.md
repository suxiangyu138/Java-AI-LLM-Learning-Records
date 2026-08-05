# 01 - Transformer 架构深度解析

> 🎯 Transformer 是当今所有 LLM 的基石——GPT、Llama、Claude、Gemini 全部基于它。理解 Self-Attention、Multi-Head、位置编码三大核心，就理解了 LLM 的底层原理

---

## 目录

1. [Transformer 全景](#1-transformer-全景)
2. [Self-Attention 机制](#2-self-attention-机制)
3. [Multi-Head Attention](#3-multi-head-attention)
4. [位置编码](#4-位置编码)
5. [完整架构](#5-完整架构)

---

## 1. Transformer 全景

```text
Transformer = Encoder + Decoder（原始论文）
现代 LLM (GPT) = Decoder-Only

                    ┌──────────────┐
Input → Embedding → │ Multi-Head   │ → Add&Norm → FFN → Add&Norm → ... × N layers → Output
                    │ Attention    │
                    └──────────────┘
```

| 组件 | 作用 | 公式直觉 |
|------|------|---------|
| **Embedding** | Token → 向量 | 字典查表 |
| **Self-Attention** | 每个词关注相关词 | Q·Kᵀ → Softmax → ×V |
| **Add & Norm** | 残差连接 + 层归一化 | 稳定训练 |
| **FFN** | 非线性变换 | 升维 → ReLU → 降维 |

## 2. Self-Attention 机制

### 2.1 核心思想

```text
"我 爱 北京 天安门"

Self-Attention 计算每个词应该关注其他多少词：
"爱" 关注 "我"(主语) + "北京"(宾语) → 理解句法关系
```

### 2.2 计算步骤

```python
import numpy as np

def self_attention(X, W_Q, W_K, W_V):
    """
    X: (seq_len, d_model) — 输入序列
    W_Q, W_K, W_V: (d_model, d_k) — 投影矩阵
    """
    # 1. 计算 Q, K, V
    Q = X @ W_Q    # Query: "我在找什么？"
    K = X @ W_K    # Key:   "我有什么？"
    V = X @ W_V    # Value: "我的内容是什么？"

    # 2. 注意力分数
    d_k = Q.shape[-1]
    scores = Q @ K.T / np.sqrt(d_k)    # 缩放点积（防梯度消失）

    # 3. Softmax → 注意力权重
    attn_weights = np.exp(scores) / np.exp(scores).sum(axis=-1, keepdims=True)

    # 4. 加权求和
    output = attn_weights @ V
    return output, attn_weights

# 一句话理解：
# Self-Attention = "每个词对所有词的相关性打分 → 加权求和所有词的信息"
```

### 2.3 为什么需要 √d_k？

```text
如果 d_k 很大（如 64），Q·Kᵀ 的值会很大
→ Softmax 后几乎变成 one-hot（梯度 ≈ 0）
→ 除以 √d_k 保持方差稳定
```

## 3. Multi-Head Attention

```text
单头注意力：只关注一种关系（如"主语-谓语"）
多头注意力：并行关注多种关系

Head 1: 句法关系（主语-谓语-宾语）
Head 2: 语义关系（同义词、上下文）
Head 3: 位置关系（远近依赖）
...
Head n: 不同的"视角"

MultiHead(Q,K,V) = Concat(head_1, ..., head_h) · W_O
```

```python
def multi_head_attention(X, num_heads=8, d_model=512):
    d_k = d_model // num_heads  # 每个头的维度

    outputs = []
    for _ in range(num_heads):
        W_Q = np.random.randn(d_model, d_k) * 0.02
        W_K = np.random.randn(d_model, d_k) * 0.02
        W_V = np.random.randn(d_model, d_k) * 0.02
        head_output, _ = self_attention(X, W_Q, W_K, W_V)
        outputs.append(head_output)

    multi_head = np.concatenate(outputs, axis=-1)   # 拼接头
    return multi_head @ np.random.randn(d_model, d_model)  # 投影回 d_model
```

## 4. 位置编码

```text
问题：Self-Attention 不关心顺序
  "我爱你" 和 "你爱我" → Attention 权重可能非常相似

解决方案：位置编码（给每个位置加唯一信号）
```

### 4.1 正弦位置编码（原始 Transformer）

```python
def sinusoidal_position_encoding(seq_len, d_model):
    PE = np.zeros((seq_len, d_model))
    for pos in range(seq_len):
        for i in range(0, d_model, 2):
            PE[pos, i] = np.sin(pos / (10000 ** (i / d_model)))
            PE[pos, i + 1] = np.cos(pos / (10000 ** (i / d_model)))
    return PE
```

### 4.2 RoPE（现代 LLM 标配）

```text
旋转位置编码 (Rotary Position Embedding)：
  → Llama/Mistral/Qwen 全部采用
  → 通过旋转变换将位置信息融入 Q 和 K
  → 相对位置编码：只关心距离，不关心绝对位置
  → 优势：外推能力更强（训练 2K 可推理 8K+）
```

## 5. 完整架构

```text
GPT 结构（Decoder-Only）：

Token → Embedding + Position Encoding
   ↓
[Block] × N
   ├── LayerNorm
   ├── Masked Multi-Head Self-Attention  ← "masked" = 只能看左边（因果）
   ├── Residual Connection
   ├── LayerNorm
   ├── FFN (Linear → GELU → Linear)
   └── Residual Connection
   ↓
LayerNorm → Linear (vocab_size) → Softmax → Next Token
```

### 关键参数速查

| 模型 | 层数 | d_model | 注意力头 | FFN 维度 | 总参数量 |
|------|:---:|:---:|:---:|:---:|:---:|
| GPT-2 Small | 12 | 768 | 12 | 3072 | 124M |
| GPT-3 | 96 | 12288 | 96 | 49152 | 175B |
| Llama 3.1 8B | 32 | 4096 | 32 | 14336 | 8B |
| Llama 3.1 405B | 126 | 16384 | 128 | 53248 | 405B |

## 核心要点回顾

- Self-Attention 公式：`Softmax(Q·Kᵀ/√d_k) · V`
- Q="我在找什么", K="我有什么", V="我的内容"
- Multi-Head = 多个并行的注意力"视角"
- RoPE = 现代 LLM 标配位置编码，支持上下文外推
- GPT Decoder-Only = 带 Causal Mask 的 Transformer（只看左边）
- FFN = 升维(4x) → 激活 → 降维，占参数量的大头

## 参考资料

1. Attention Is All You Need (Vaswani et al., 2017)
2. RoPE 论文 (Su et al., 2021)
3. The Illustrated Transformer — jalammar.github.io
