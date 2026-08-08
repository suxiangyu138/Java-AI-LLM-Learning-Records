# 04 - LayerNorm 与残差连接

> 🎯 训练 100 层的 Transformer 不崩溃，全靠 LayerNorm 和残差连接。Pre-Norm vs Post-Norm 的选择、DeepNorm 的改进——这些归一化策略直接影响训练稳定性和最终效果

---

## 目录

1. [残差连接](#1-残差连接)
2. [LayerNorm 详解](#2-layernorm-详解)
3. [Pre-Norm vs Post-Norm](#3-pre-norm-vs-post-norm)
4. [RMSNorm 与 DeepNorm](#4-rmsnorm-与-deepnorm)

---

## 1. 残差连接

```text
残差连接 = 输入直接加到输出

标准:   Output = f(x)
残差:   Output = x + f(x)

为什么需要？
  1. 梯度高速公路：梯度可以直接流过加法（梯度=1），不会衰减
  2. 恒等映射：最差情况 f(x)≈0 → Output≈x（至少不退化）
  3. 训练深层网络：没有残差，100+ 层根本训练不动
```

```text
Transformer 块中的两处残差：

x = x + MultiHeadAttention(LayerNorm(x))
x = x + FFN(LayerNorm(x))
```

## 2. LayerNorm 详解

### 2.1 为什么是 LayerNorm 而不是 BatchNorm

```text
BatchNorm: 对同一特征的不同样本做归一化
  → 依赖 batch 中的其他样本
  → NLP 中句子长度不一 → padding 干扰 → 不适用

LayerNorm: 对同一样本的不同特征做归一化
  → 每个样本独立归一化
  → 不受 batch size 和序列长度影响
  → NLP 最佳选择
```

### 2.2 计算过程

```python
def layer_norm(x, gamma, beta, eps=1e-5):
    # x: (batch, seq_len, d_model)

    # 1. 计算均值（沿最后一个维度）
    mean = x.mean(dim=-1, keepdim=True)

    # 2. 计算方差
    var = x.var(dim=-1, keepdim=True, unbiased=False)

    # 3. 标准化
    x_norm = (x - mean) / np.sqrt(var + eps)

    # 4. 可学习的缩放和平移
    return gamma * x_norm + beta

# gamma, beta: 可学习参数 (d_model,)
# LayerNorm 让每层的输出分布稳定 → 训练更平稳
```

## 3. Pre-Norm vs Post-Norm

```text
Post-Norm（原始 Transformer）：
  x = LayerNorm(x + Attention(x))     ← 残差后归一化
  x = LayerNorm(x + FFN(x))

Pre-Norm（现代 LLM 标配）：
  x = x + Attention(LayerNorm(x))     ← 残差前归一化
  x = x + FFN(LayerNorm(x))
```

| 维度 | Post-Norm | **Pre-Norm** |
|------|:---:|:---:|
| 训练稳定性 | 需要 warmup | 不需要 warmup |
| 梯度流 | 可能消失 | **更好** |
| 最终效果 | 理论上略好 | 实践几乎相同 |
| 深层网络 | 不稳定 | **稳定** |
| 使用模型 | 原版 Transformer | **GPT/Llama/Qwen** |

```text
为什么 Pre-Norm 更稳定？

Post-Norm:
  残差分支的梯度 = 1 × ∂L/∂x  ← 经过 LayerNorm 的梯度可能很小
  → 深层网络梯度消失

Pre-Norm:
  主干道的梯度 = 1 × ∂L/∂x  ← 不经过 LayerNorm！
  → 梯度始终有一条"高速公路"直达底层
```

## 4. RMSNorm 与 DeepNorm

### 4.1 RMSNorm

```text
RMSNorm = LayerNorm 的简化版，只做方差归一化（不做均值中心化）

RMSNorm(x) = x / sqrt(mean(x²)) * gamma

计算：
  RMS = sqrt(mean(x²))    ← 只算均方根，不减去均值
  return x / RMS * gamma

为什么用 RMSNorm？
  ✅ 计算量少 ~30%（不用算均值）
  ✅ 效果和 LayerNorm 几乎相同
  → Llama/Mistral/Qwen 全部使用 RMSNorm
```

### 4.2 DeepNorm

```text
DeepNorm = 专门为超深层 Transformer (>100 层) 设计的归一化

核心改进：
  → 在残差分支上加一个缩放因子 α
  → 初始化时让 α 很小 → 训练初期接近恒等映射 → 极其稳定

x = x + α · Attention(LayerNorm(x))

效果：
  → 成功训练 1000 层 Transformer
  → 不需要 learning rate warmup
```

## 核心要点回顾

- 残差 = 梯度高速公路 + 恒等映射兜底
- LayerNorm > BatchNorm：NLP 序列长度不一，逐样本归一化
- **Pre-Norm > Post-Norm：现代 LLM 标配，训练更稳定**
- RMSNorm = LayerNorm 轻量版（省 30% 计算），Llama/Qwen 使用
- DeepNorm = 超深层 Transformer 专用（1000 层可训）

## 参考资料

1. Layer Normalization 论文 (Ba et al., 2016)
2. Pre-Norm vs Post-Norm (Xiong et al., 2020)
3. DeepNorm 论文 (Wang et al., 2022)
