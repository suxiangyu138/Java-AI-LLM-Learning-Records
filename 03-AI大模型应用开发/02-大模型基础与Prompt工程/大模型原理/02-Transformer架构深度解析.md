# 02 - Transformer 架构深度解析

> 🎯 Transformer 是 2017 年提出的架构，用自注意力彻底替代了 RNN 的序列依赖。它是所有现代 LLM 的骨架 — 不理解 Transformer，就不可能真正理解大模型

---

## 目录

1. [为什么需要 Transformer](#1-为什么需要-transformer)
2. [整体架构](#2-整体架构)
3. [自注意力机制（核心）](#3-自注意力机制核心)
4. [多头注意力](#4-多头注意力)
5. [位置编码](#5-位置编码)
6. [前馈网络 FFN](#6-前馈网络-ffn)
7. [残差连接与层归一化](#7-残差连接与层归一化)
8. [Encoder vs Decoder 架构分支](#8-encoder-vs-decoder-架构分支)
9. [从公式到代码](#9-从公式到代码)

---

## 1. 为什么需要 Transformer

```text
RNN/LSTM 的痛点：
  ❌ 串行处理：第 N 步必须等第 N-1 步 → 无法并行
  ❌ 长距离依赖弱：梯度消失，100 步前的信息几乎丢失
  ❌ 训练慢：一个 1000 token 的句子要跑 1000 次循环

Transformer 的解法：
  ✅ 并行处理：所有 token 同时输入，矩阵运算一次完成
  ✅ 长距离依赖：自注意力直接建模任意两两 token 的关系
  ✅ 训练快：GPU 矩阵乘法高度并行

代价：
  ⚠️ 复杂度 O(n²)：序列越长，计算量暴增
  ⚠️ 位置信息需要显式注入（无内置时序感知）
```

---

## 2. 整体架构

```text
┌─────────────────────────────────────────────────────┐
│                  Transformer 架构                     │
│                                                      │
│  ┌────────────────────┐  ┌─────────────────────┐    │
│  │     Encoder × N     │  │    Decoder × N       │    │
│  │  ┌───────────────┐  │  │  ┌────────────────┐  │    │
│  │  │ Multi-Head    │  │  │  │ Masked Multi-  │  │    │
│  │  │ Attention     │  │  │  │ Head Attention │  │    │
│  │  ├───────────────┤  │  │  ├────────────────┤  │    │
│  │  │ Add & Norm    │  │  │  │ Add & Norm     │  │    │
│  │  ├───────────────┤  │  │  ├────────────────┤  │    │
│  │  │ Feed Forward  │  │  │  │ Cross-Attention│  │    │
│  │  ├───────────────┤  │  │  ├────────────────┤  │    │
│  │  │ Add & Norm    │  │  │  │ Add & Norm     │  │    │
│  │  └───────────────┘  │  │  ├────────────────┤  │    │
│  └────────────────────┘  │  │ Feed Forward   │  │    │
│                           │  │ Add & Norm     │  │    │
│                           │  └────────────────┘  │    │
│                           └─────────────────────┘    │
└─────────────────────────────────────────────────────┘

输入 → Input Embedding + Positional Encoding → Encoder × N → Decoder × N → Output
```

---

## 3. 自注意力机制（核心）

### 3.1 直观理解

```text
句子："The cat sat on the mat because it was tired."

问题："it" 指什么？→ cat 还是 mat？

自注意力的做法：
  对 "it" 计算与句中每个词的相关度：
    it ↔ The  : 0.02
    it ↔ cat  : 0.75  ← 最高！
    it ↔ mat  : 0.15
    it ↔ tired: 0.05
    ...

  → 模型"关注"到 cat → 理解 it = cat
```

### 3.2 数学公式

```text
                      QK^T
Attention(Q, K, V) = softmax(───) V
                      √d_k

三个矩阵：
  Q (Query)  → "我在找什么？"  —— 当前 token 的查询向量
  K (Key)    → "我有什么？"  —— 每个 token 的键向量
  V (Value)  → "我的内容是什么？" —— 每个 token 的值向量

计算步骤：
  Step 1: Score  = Q × K^T        → 计算每对 token 的相关性分数
  Step 2: Score  = Score / √d_k   → 缩放，防止内积过大
  Step 3: Weight = softmax(Score) → 归一化为概率分布
  Step 4: Output = Weight × V     → 加权求和得到输出
```

### 3.3 为什么除以 √d_k

```text
假设 Q, K 的每个元素独立同分布，均值为 0，方差为 1
则 Q·K 的方差 = d_k

不缩放：
  d_k=512 → 方差 512 → 内积值很大
  → softmax 后极度尖锐（≈ one-hot）
  → 梯度趋零 → 无法训练

除以 √d_k：
  d_k=512 → 方差 = 1 → 内积值稳定
  → softmax 平滑分布 → 梯度正常
```

---

## 4. 多头注意力

```text
为什么需要"多头"？

单头的局限：
  一个 attention 只能捕捉一种关系模式

多头的优势：
  → 头1 关注语法关系（主谓宾）
  → 头2 关注指代关系（代词→实体）
  → 头3 关注语义关系（同义/反义）
  → 头4 关注位置关系（前后文）

实现：
  将 Q/K/V 各自线性投影 h 次，每个头独立计算 Attention
  最后拼接所有头的输出 → 再线性变换

  MultiHead(Q,K,V) = Concat(head_1, ..., head_h) × W_O
  head_i = Attention(Q×W_Qi, K×W_Ki, V×W_Vi)
```

| 模型 | 头数 | 隐藏维度 | d_k（每头） |
|------|:---:|:---:|:---:|
| Transformer (base) | 8 | 512 | 64 |
| BERT-base | 12 | 768 | 64 |
| GPT-3 (175B) | 96 | 12288 | 128 |
| LLaMA-7B | 32 | 4096 | 128 |

> 💡 $d_{model} = h \times d_k$，如 768 = 12 × 64，这是为了多头拼接后维度不变。

---

## 5. 位置编码

### 5.1 为什么需要

```text
自注意力是"位置盲"的：
  → "我爱你" 和 "你爱我" 中的 "我" 获得完全相同的 attention 权重
  → 因为 Attention(Q,K,V) 公式中不包含位置信息

解决方案：
  在输入 Embedding 上加入位置信息 → 模型能区分不同位置
```

### 5.2 两种实现

| 方式 | 公式 | 使用情况 |
|------|------|:---:|
| **Sinusoidal（三角函数）** | $PE_{(pos,2i)} = \sin(pos/10000^{2i/d})$ | 原始 Transformer |
| **Learned（可学习）** | 随机初始化，训练中学习 | GPT/LLaMA/BERT 主流 |

```text
Sinusoidal 编码的特点：
  ✅ 可外推到训练时未见过的长度
  ✅ 相对位置信息：PE(pos+k) 可由 PE(pos) 线性表示
  ❌ 效果不如可学习编码（实践中）

目前主流 = Learned Positional Encoding
  → RoPE（旋转位置编码）：LLaMA/Qwen/DeepSeek 使用
  → ALiBi（注意力线性偏置）：BLOOM 使用
```

### 5.3 RoPE（旋转位置编码）

```text
RoPE 的核心思想：
  通过旋转矩阵将位置信息注入 Q 和 K

  Q_pos = Q × R(pos)    （旋转 Q）
  K_pos = K × R(pos)    （旋转 K）

  效果：Q_i · K_j 的值只依赖于相对位置 (i - j)

  优势：
  ✅ 天然支持相对位置
  ✅ 长文本外推能力强
  ✅ LLaMA、Qwen、DeepSeek、ChatGLM 全部使用
```

---

## 6. 前馈网络 FFN

```text
FFN(x) = GELU(xW_1 + b_1)W_2 + b_2

本质：两层全连接 + 激活函数

作用：
  → 对 Attention 输出的语义向量做非线性变换
  → 增强模型表达能力
  → 存储大量知识（研究表明 FFN 是 LLM 的"知识库"）

维度：
  → 输入/输出 = d_model（如 768/4096）
  → 中间层 = d_model × 4（如 3072/16384）

新一代变体 SwiGLU（LLaMA 系列使用）：
  FFN(x) = (SiLU(xW_1) ⊙ (xW_2))W_3
  → 门控机制，效果优于标准 FFN
```

---

## 7. 残差连接与层归一化

### 7.1 残差连接

```text
output = LayerNorm(x + SubLayer(x))
         ↑
    残差连接：把输入直接加到子层输出上

作用：
  ✅ 梯度直通：反向传播时梯度可以直接流过
  ✅ 解决深层网络退化问题
  ✅ 使得堆叠 80+ 层成为可能
```

### 7.2 Pre-LN vs Post-LN

```text
Post-LN（原始 Transformer）：
  x + Attention(LayerNorm(x)) → LayerNorm
  问题：深层网络训练不稳定

Pre-LN（现代 LLM 标配）：
  x + Attention(LayerNorm(x))  → 无最后的 LayerNorm
  优势：训练稳定，收敛更快
  使用：GPT、LLaMA、BLOOM 等现代 LLM

注意：Pre-LN 中 LayerNorm 在 Attention/FFN 之前！
```

---

## 8. Encoder vs Decoder 架构分支

```text
完整架构演变路线：

完整 Transformer（Encoder + Decoder）
  → 机器翻译、T5、BART
  → 输入和输出都使用 Attention

Encoder-Only（仅编码器）
  → BERT 系列
  → 双向注意力：每个 token 看所有 token
  → 擅长"理解"：分类、抽取、语义匹配
  → 不能生成文本

Decoder-Only（仅解码器）
  → GPT/LLaMA/Qwen/DeepSeek/GLM...
  → 因果注意力：每个 token 只能看前面的 token
  → 擅长"生成"：对话、续写、代码
  → ⭐ 2024 年绝对主流
```

| 维度 | Encoder-Only | Decoder-Only | Encoder-Decoder |
|------|:---:|:---:|:---:|
| **代表** | BERT | GPT/LLaMA | T5/BART |
| **注意力** | 双向 | 单向（因果掩码） | 编码双向 + 解码单向 |
| **生成** | ❌ | ✅ | ✅ |
| **理解** | ✅⭐ | ✅ | ✅ |
| **当前地位** | 退居二线 | 绝对主流 | 特定任务 |

---

## 9. 从公式到代码

```python
import numpy as np

def self_attention(X, W_Q, W_K, W_V, W_O, mask=None):
    """
    X: (seq_len, d_model) 输入序列
    W_Q, W_K, W_V: (d_model, d_k) 投影矩阵
    W_O: (h * d_k, d_model) 输出投影矩阵
    """
    d_k = W_Q.shape[-1]
    
    # Step 1: 线性投影
    Q = X @ W_Q  # (seq_len, d_k)
    K = X @ W_K
    V = X @ W_V
    
    # Step 2: 计算注意力分数
    scores = Q @ K.T / np.sqrt(d_k)  # (seq_len, seq_len)
    
    # Step 3: 因果掩码（Decoder-Only 模式）
    if mask is not None:
        scores = scores + mask  # mask 中 -inf 的位置会被 softmax 忽略
    
    # Step 4: softmax
    attn_weights = np.exp(scores) / np.exp(scores).sum(axis=-1, keepdims=True)
    
    # Step 5: 加权求和
    output = attn_weights @ V  # (seq_len, d_k)
    
    # Step 6: 输出投影
    return output @ W_O

# 因果掩码示例（Decoder-Only）
def causal_mask(seq_len):
    """生成下三角掩码，防止看到未来 token"""
    mask = np.triu(np.ones((seq_len, seq_len)) * -1e9, k=1)
    return mask
# [[0.,   -inf,  -inf],
#  [0.,   0.,    -inf],
#  [0.,   0.,    0.  ]]
```

---

## 核心要点回顾

- Transformer = 自注意力 + 多头 + FFN + 位置编码 + 残差 + LayerNorm
- 自注意力 = $\text{softmax}(QK^T/\sqrt{d_k})V$，O(n²) 复杂度
- 多头 = 拆分 h 组 Q/K/V，从不同角度关注
- Decoder-Only（GPT/LLaMA）= 因果掩码 → 只看到上文
- 现代 LLM 标配：Pre-LN + RoPE + SwiGLU FFN
