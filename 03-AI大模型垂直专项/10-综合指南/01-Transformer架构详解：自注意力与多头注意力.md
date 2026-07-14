# 🏗️ Transformer 架构详解：自注意力与多头注意力

> Transformer 是当今几乎所有大语言模型（GPT、BERT、Claude 等）的基石架构。理解其核心机制——自注意力（Self-Attention）如何捕捉长距离语义依赖，以及多头注意力（Multi-Head Attention）如何并行学习多维特征，是区分 AI 开发与传统软件开发的关键认知门槛。

## 前置阅读

- [[AI 大模型核心知识点]]
- [[深度学习核心知识点]]
- [[大模型基础核心知识点：语言模型基础]]

## 目录

1. [为什么需要 Transformer](#1-为什么需要-transformer)
2. [自注意力机制：核心引擎](#2-自注意力机制核心引擎)
3. [多头注意力：并行学习多维特征](#3-多头注意力并行学习多维特征)
4. [位置编码：注入顺序信息](#4-位置编码注入顺序信息)
5. [前馈神经网络与残差连接](#5-前馈神经网络与残差连接)
6. [Encoder 与 Decoder 的分工](#6-encoder-与-decoder-的分工)
7. [代码实现：从零构建 Self-Attention](#7-代码实现从零构建-self-attention)

---

## 1. 为什么需要 Transformer

在 Transformer 出现之前，自然语言处理领域的霸主是 **RNN（循环神经网络）** 和 **LSTM（长短期记忆网络）**。它们有一个根本性缺陷：**顺序计算**——必须按时间步逐个处理单词，无法并行化，导致训练速度极慢，且长距离依赖容易丢失。

> **重点**：Transformer 的核心创新在于**彻底抛弃了循环结构**，代之以**自注意力机制**，使模型能够一次性看到整个输入序列中的所有单词，并行计算任意两个位置之间的关联度。

这一设计带来了三个革命性变化：

| 特性 | RNN/LSTM | Transformer |
|------|----------|-------------|
| 计算方式 | 顺序计算，无法并行 | 完全并行计算 |
| 长距离依赖 | 梯度消失，远距信息丢失 | 自注意力直达任意位置 |
| 训练速度 | 慢 | 快（GPU 友好） |

---

## 2. 自注意力机制：核心引擎

### 2.1 直觉理解

假设句子是：**"银行提高了贷款利率"**

当你读到"利率"这个词时，你自然知道这里的"银行"指的是"金融机构"而非"河岸"。你的大脑自动将"银行"与上下文中的"贷款""利率"建立了强关联——这就是注意力机制的直觉来源。

自注意力机制做的正是这件事：为句子中的**每个词**，计算它与其他**所有词**的关联强度，然后据此生成一个包含上下文信息的表示。

### 2.2 数学原理

自注意力机制的核心操作是 **Scaled Dot-Product Attention**：

```
Attention(Q, K, V) = softmax(QK^T / √d_k) × V
```

三个关键矩阵：

| 矩阵 | 全称 | 含义 |
|------|------|------|
| **Q（Query）** | 查询向量 | "我在找什么？"——当前词想要关注什么信息 |
| **K（Key）** | 键向量 | "我有什么？"——每个词可以提供什么信息 |
| **V（Value）** | 值向量 | "我的内容是什么？"——每个词实际携带的信息 |

计算过程可以类比为**搜索引擎**：
1. **Query** 是你的搜索关键词
2. **Key** 是数据库中每个文档的标题
3. 计算 Query 与每个 Key 的相似度（QK^T）
4. 用相似度作为权重，对 **Value**（文档内容）做加权求和
5. 除以 √d_k 防止点积过大导致 softmax 梯度消失

### 2.3 直观示例

```python
# 句子："我 爱 北京"
# 计算"北京"与所有词（包括自己）的注意力分数

注意力分数矩阵（softmax 归一化后）：
          我      爱     北京
我      0.15    0.10    0.75   ← "我"最关注"北京"
爱      0.05    0.20    0.75   ← "爱"最关注"北京"
北京    0.30    0.30    0.40   ← "北京"关注自己最多

# 每个词的输出 = 所有词 value 向量的加权和
# 权重 = 注意力分数
```

---

## 3. 多头注意力：并行学习多维特征

### 3.1 为什么需要"多头"

单头注意力只能学到一种"关系模式"。但在语言中，同一个词在不同维度上与其他词有不同关系：

- **语法维度**：主语-谓语关系（"我"←→"爱"）
- **语义维度**：对象-属性关系（"北京"←→"首都"）
- **共指维度**：代词-指代关系（"它"←→"北京"）

**多头注意力**就是并行运行多套独立的 Q/K/V 映射，让每个"头"专注于学习一种类型的关系。

### 3.2 计算流程

```
输入向量 X (d_model = 512)
     │
     ├── 头1: Q₁=X·Wq₁, K₁=X·Wk₁, V₁=X·Wv₁  → Attention₁
     ├── 头2: Q₂=X·Wq₂, K₂=X·Wk₂, V₂=X·Wv₂  → Attention₂
     ├── ...
     └── 头8: Q₈=X·Wq₈, K₈=X·Wk₈, V₈=X·Wv₈  → Attention₈

Concat(Attention₁, ..., Attention₈) × Wᴼ → 最终输出
```

> **重点**：每个头使用降维的 Q/K/V（d_k = d_model / h），所以多头注意力的总计算量与单头（全维度）相当，但表征能力大幅提升。

### 3.3 各头的典型分工（以 BERT 为例）

| 头 | 可能学到的模式 | 示例 |
|----|-------------|------|
| 头1 | 相邻词关注 | 关注前一个/后一个词 |
| 头2 | 语法依存 | 动词←→主语，形容词←→名词 |
| 头3 | 分隔符关注 | 关注句号、逗号等标点 |
| 头8 | 长距离共指 | "它"与10个词前的"北京"关联 |

---

## 4. 位置编码：注入顺序信息

Transformer 没有循环结构，天生不知道词的顺序。"我爱你"和"你爱我"在它看来是完全相同的一组词。**位置编码（Positional Encoding）** 就是给每个位置注入唯一的"身份信息"。

### 4.1 正弦位置编码（原始方案）

```python
PE(pos, 2i)   = sin(pos / 10000^(2i/d_model))
PE(pos, 2i+1) = cos(pos / 10000^(2i/d_model))
```

这种设计的精妙之处：
- 每个位置有唯一的编码向量
- 相对位置关系可通过三角函数性质自然推导：`PE(pos+k)` 可以表示为 `PE(pos)` 的线性函数
- 可以外推到训练时未见过的更长序列

### 4.2 可学习位置编码（现代方案）

GPT 系列采用**可学习的位置嵌入**（Learned Positional Embedding），直接将位置当作一个额外的嵌入表来训练：

```python
class LearnedPositionalEmbedding(nn.Module):
    def __init__(self, max_seq_len, d_model):
        super().__init__()
        self.embedding = nn.Embedding(max_seq_len, d_model)

    def forward(self, x):
        positions = torch.arange(x.size(1), device=x.device)
        return x + self.embedding(positions)
```

### 4.3 旋转位置编码（RoPE）

**RoPE（Rotary Position Embedding）** 是当前最先进的位置编码方式，被 LLaMA、Qwen 等主流模型采用。它通过旋转矩阵将位置信息直接融入 Q 和 K 的计算，使注意力分数天然包含相对位置信息。

---

## 5. 前馈神经网络与残差连接

### 5.1 前馈神经网络（FFN）

每个 Transformer 层的自注意力之后，都有一个**逐位置**的前馈网络：

```
FFN(x) = ReLU(x·W₁ + b₁)·W₂ + b₂
```

- W₁ 将维度从 d_model 扩展到 d_ff（通常 ×4，如 512 → 2048）
- W₂ 再压缩回 d_model
- 这个"扩展-压缩"结构提供了非线性变换能力

### 5.2 残差连接与层归一化

```
x → Self-Attention → +残差 → LayerNorm → FFN → +残差 → LayerNorm → 输出
```

- **残差连接**：让梯度直通底层，解决深层网络的退化问题
- **层归一化**：稳定训练，加速收敛

现代 Transformer 普遍采用 **Pre-Norm**（先归一化再计算）而非原始论文的 Post-Norm：

```
# Post-Norm（原始）
x = LayerNorm(x + SelfAttention(x))

# Pre-Norm（现代，更稳定）
x = x + SelfAttention(LayerNorm(x))
```

---

## 6. Encoder 与 Decoder 的分工

### 6.1 Encoder（编码器）

- 通过**双向**自注意力理解整个输入序列
- 每个位置都能看到前后的所有词
- 适用于：文本理解、分类、NER、情感分析
- 代表模型：**BERT**

### 6.2 Decoder（解码器）

- 通过**单向（因果）**自注意力逐词生成输出
- 每个位置只能看到之前的词（Masked Self-Attention）
- 适用于：文本生成、对话、代码补全
- 代表模型：**GPT 系列**

### 6.3 Encoder-Decoder

- Encoder 编码输入 → Decoder 交叉注意力融合 → 生成输出
- Decoder 的交叉注意力层同时关注 Encoder 输出和自身已生成内容
- 适用于：翻译、摘要、语音识别
- 代表模型：**T5、BART**

---

## 7. 代码实现：从零构建 Self-Attention

```python
import torch
import torch.nn as nn
import torch.nn.functional as F
import math

class MultiHeadSelfAttention(nn.Module):
    """多头自注意力机制的完整实现"""

    def __init__(self, d_model: int = 512, n_heads: int = 8, dropout: float = 0.1):
        super().__init__()
        assert d_model % n_heads == 0, "d_model 必须能被 n_heads 整除"

        self.d_model = d_model
        self.n_heads = n_heads
        self.d_k = d_model // n_heads  # 每个头的维度

        # Q、K、V 的线性投影（合并为一个大矩阵以提升效率）
        self.qkv_proj = nn.Linear(d_model, 3 * d_model)
        self.out_proj = nn.Linear(d_model, d_model)
        self.dropout = nn.Dropout(dropout)

    def forward(self, x: torch.Tensor, mask: torch.Tensor = None) -> torch.Tensor:
        """
        Args:
            x: (batch_size, seq_len, d_model) 输入序列
            mask: (batch_size, seq_len, seq_len) 可选的注意力掩码
        Returns:
            (batch_size, seq_len, d_model) 输出序列
        """
        batch_size, seq_len, _ = x.shape

        # 1. 线性投影 → 拆分为多头
        qkv = self.qkv_proj(x)  # (batch, seq, 3*d_model)
        qkv = qkv.reshape(batch_size, seq_len, 3, self.n_heads, self.d_k)
        qkv = qkv.permute(2, 0, 3, 1, 4)  # (3, batch, heads, seq, d_k)
        q, k, v = qkv[0], qkv[1], qkv[2]

        # 2. Scaled Dot-Product Attention
        scores = torch.matmul(q, k.transpose(-2, -1)) / math.sqrt(self.d_k)

        if mask is not None:
            scores = scores.masked_fill(mask == 0, float('-inf'))

        attn_weights = F.softmax(scores, dim=-1)
        attn_weights = self.dropout(attn_weights)

        # 3. 加权聚合 Value
        context = torch.matmul(attn_weights, v)  # (batch, heads, seq, d_k)

        # 4. 合并多头 → 最终投影
        context = context.transpose(1, 2).contiguous()  # (batch, seq, heads, d_k)
        context = context.reshape(batch_size, seq_len, self.d_model)

        return self.out_proj(context)


# ========== 使用演示 ==========
if __name__ == "__main__":
    # 模拟输入：batch=2，序列长度=10，嵌入维度=512
    x = torch.randn(2, 10, 512)

    attention = MultiHeadSelfAttention(d_model=512, n_heads=8)
    output = attention(x)

    print(f"输入形状: {x.shape}")      # (2, 10, 512)
    print(f"输出形状: {output.shape}") # (2, 10, 512)
    print("✅ 自注意力计算完成")
```

---

## 核心要点回顾

- Transformer 用**自注意力**替代 RNN 的循环结构，实现完全并行化计算
- 自注意力的本质是"搜索引擎"：Query 查询，Key 匹配，Value 聚合
- **多头注意力**让模型并行学习语法、语义、共指等多种关系模式
- **位置编码**为无序模型注入顺序信息，RoPE 是当前最先进方案
- Encoder 负责**理解**（双向），Decoder 负责**生成**（单向因果）
- 残差连接 + Pre-Norm 是深层 Transformer 稳定训练的标配

## 参考资料

1. Vaswani et al., "Attention Is All You Need" (NeurIPS 2017)
2. Devlin et al., "BERT: Pre-training of Deep Bidirectional Transformers"
3. Su et al., "RoFormer: Enhanced Transformer with Rotary Position Embedding"
4. [[Transformer、BERT、GPT、GLM 核心知识点]]
5. [[深度学习核心知识点]]
