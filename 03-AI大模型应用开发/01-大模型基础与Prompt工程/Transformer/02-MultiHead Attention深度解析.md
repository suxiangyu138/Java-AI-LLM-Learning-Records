# 02 - Multi-Head Attention 深度解析

> 🎯 单头注意力只能捕捉一种"关系模式"。Multi-Head Attention 通过并行计算多组 Q/K/V，从不同子空间关注不同关系——一句汇总：多个"视角"的信息融合

---

## 目录

1. [为什么需要多头](#1-为什么需要多头)
2. [计算流程与维度变化](#2-计算流程与维度变化)
3. [代码实现](#3-代码实现)
4. [头数的影响](#4-头数的影响)

---

## 1. 为什么需要多头

```text
单头注意力：
  "我爱北京天安门"
  → 只关注一种关系模式（如"主谓宾"）

多头注意力：
  Head 1: 句法关系（主语-谓语-宾语）
  Head 2: 语义关系（"爱"和"北京"的情感联系）
  Head 3: 位置关系（近处 token vs 远处 token）
  Head 4: 指代关系（代词→名词的关联）
  ...
  Head h: 不同的"关注视角"

直觉：
  一个人看问题只有一个角度
  8 个人从 8 个角度看 → 综合意见更全面
```

## 2. 计算流程与维度变化

```text
设置（GPT-2 Small）：
  n = 序列长度，d_model = 768，h = 12（头数）
  d_k = d_model / h = 64

Step 1: 每个头独立计算 Attention
  Q_i, K_i, V_i = X · W_Qi, X · W_Ki, X · W_Vi
  每个 W 的形状： (768, 64)

  head_i = Attention(Q_i, K_i, V_i)   (n, 64)

Step 2: 拼接所有头
  Concat(head_1, head_2, ..., head_12)   (n, 768)

Step 3: 线性投影回 d_model
  MultiHead = Concat · W_O   (n, 768)
  W_O 形状: (768, 768)

维度变化总览：
  X:          (n, 768)
  Q_i, K_i:   (n, 64)    ← 每个头维度 = 768/12
  head_i:     (n, 64)
  Concat:     (n, 768)   ← 12 × 64 = 768
  Output:     (n, 768)   ← 维度守恒
```

### 为什么每个头用 d_model/h 维而不是 d_model？

```text
方案 A（GPT 做法）：d_k = d_model / h = 64
  → 总计算量 ≈ h × O(n² × 64) = O(n² × 768)  ← 和单头差不多！

方案 B（如果每个头 d_model 维）：d_k = d_model = 768
  → 总计算量 = h × O(n² × 768) = h× 单头  ← 不可接受！

结论：head_dim = d_model / h 是计算量和表达能力的最优平衡
```

## 3. 代码实现

```python
import torch
import torch.nn as nn
import math

class MultiHeadAttention(nn.Module):
    def __init__(self, d_model=512, num_heads=8, dropout=0.1):
        super().__init__()
        assert d_model % num_heads == 0

        self.d_model = d_model
        self.num_heads = num_heads
        self.d_k = d_model // num_heads

        # 将 Q/K/V 投影和 W_O 合并到一个大矩阵（效率更高）
        self.W_Q = nn.Linear(d_model, d_model)
        self.W_K = nn.Linear(d_model, d_model)
        self.W_V = nn.Linear(d_model, d_model)
        self.W_O = nn.Linear(d_model, d_model)

        self.dropout = nn.Dropout(dropout)

    def forward(self, Q, K, V, mask=None):
        batch_size = Q.size(0)

        # 1. 线性投影 + 拆分成多头
        # (batch, n, d_model) → (batch, n, num_heads, d_k)
        Q = self.W_Q(Q).view(batch_size, -1, self.num_heads, self.d_k)
        K = self.W_K(K).view(batch_size, -1, self.num_heads, self.d_k)
        V = self.W_V(V).view(batch_size, -1, self.num_heads, self.d_k)

        # 2. 转置 → (batch, num_heads, n, d_k)
        Q = Q.transpose(1, 2)
        K = K.transpose(1, 2)
        V = V.transpose(1, 2)

        # 3. Scaled Dot-Product Attention (批量并行)
        scores = torch.matmul(Q, K.transpose(-2, -1)) / math.sqrt(self.d_k)

        if mask is not None:
            scores = scores.masked_fill(mask == 0, -1e9)

        attn = torch.softmax(scores, dim=-1)
        attn = self.dropout(attn)

        # 4. 加权求和
        out = torch.matmul(attn, V)  # (batch, num_heads, n, d_k)

        # 5. 合并多头 → (batch, n, d_model)
        out = out.transpose(1, 2).contiguous()
        out = out.view(batch_size, -1, self.d_model)

        # 6. 最终投影
        return self.W_O(out)
```

## 4. 头数的影响

| 头数 | d_k | 适合 | 注意 |
|---:|:---:|------|------|
| 2-4 | 128-256 | 小模型（<100M） | 头太少，表达能力不足 |
| **8-12** | **64** | **标准选择** | GPT-2/BERT 都用 |
| 32 | 128 | 大模型（>7B） | Llama 3.1 8B=32头 |
| 64 | 128 | 超大模型 | Llama 3.1 70B、GPT-3 |
| 128 | 128 | 超大模型 | Llama 3.1 405B=128头 |

> 🎯 头数随模型规模增长而增长，但 `d_k = d_model/h` 通常稳定在 64-128

## 核心要点回顾

- Multi-Head = 并行计算 h 组 Attention → 拼接 → 投影
- 维度守恒：输入 d_model → 每个头 d_model/h → 拼接回 d_model
- `d_k = d_model / h` 是计算量与表达力的最优平衡
- 实现技巧：Q/K/V 拼接为大矩阵一次投影，然后 view+transpose 拆分
- 头数 = 模型的"多视角能力"，大模型=多头

## 参考资料

1. Attention Is All You Need (Vaswani et al., 2017)
2. PyTorch nn.MultiheadAttention 源码
