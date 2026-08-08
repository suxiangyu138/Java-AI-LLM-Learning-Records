# 01 - Attention 机制数学推导

> 🎯 Attention 的核心思想："对于每个 token，计算它与所有其他 token 的相关性，然后按相关性加权聚合信息"。本章从向量内积一步步推导到完整公式

---

## 目录

1. [直觉理解](#1-直觉理解)
2. [逐步数学推导](#2-逐步数学推导)
3. [计算复杂度分析](#3-计算复杂度分析)
4. [为什么除以 √d_k](#4-为什么除以-d_k)

---

## 1. 直觉理解

```text
输入句子："我 爱 北京"

传统 RNN：只能从左到右，逐步处理（"我"→"爱"→"北京"）
Attention："北京"可以同时看到"我"和"爱"，直接计算相关性

关键问题：怎么量化"相关性"？

答案：用向量内积（点积）
  两个向量方向越接近 → 内积越大 → 越相关
```

## 2. 逐步数学推导

### Step 1: 输入表示

```text
输入：X ∈ R^{n × d_model}
  n   = 序列长度（token 数）
  d_model = 每个 token 的向量维度（如 512）

示例（n=3, d_model=4）：
X = [[1, 0, 1, 0],    ← "我"
     [0, 1, 1, 0],    ← "爱"
     [1, 1, 0, 1]]    ← "北京"
```

### Step 2: 线性投影（Q, K, V）

```text
为什么要分别投影？
  → 不同的"角色"需要不同的特征空间
  → Q："我在找什么"（查询者视角）
  → K："我有什么标签"（被查者视角）
  → V："我的实际内容"（传递的信息）

Q = X · W_Q    (n × d_k)
K = X · W_K    (n × d_k)
V = X · W_V    (n × d_v)

W_Q, W_K, W_V 是可学习的参数矩阵 (d_model × d_k)
d_k 通常 = d_model / num_heads
```

### Step 3: 计算注意力分数

```text
Scores = Q · Kᵀ    (n × n)

Scores[i][j] = Q_i · K_j  ← "第 i 个 token 对第 j 个 token 的原始相关度"

示例：
Q_1·K_1 = "我"对"我"的相关度 = 很高
Q_1·K_2 = "我"对"爱"的相关度 = 中等（主语-谓语关系）
Q_2·K_1 = "爱"对"我"的相关度 = 高（谓语需要主语）
```

### Step 4: 缩放

```text
Scaled_Scores = Scores / √d_k

原因见 §4（防止 Softmax 梯度消失）
```

### Step 5: Softmax 归一化

```text
Attention_Weights = Softmax(Scaled_Scores, dim=-1)    (n × n)

每行是一个概率分布（∑ = 1）：
  第 i 行 = token i 关注其他所有 token 的权重
```

### Step 6: 加权求和

```text
Output = Attention_Weights · V    (n × d_v)

每个 token 的输出 = 所有 token 的 V 的加权和
  权重 = Attention_Weights（相关性分数）
```

### 完整公式

```text
Attention(Q, K, V) = Softmax(Q·Kᵀ / √d_k) · V

维度变换：
  Q: (n × d_k)
  K: (n × d_k)        Q·Kᵀ: (n × n)  ← 注意力矩阵
  V: (n × d_v)        Output: (n × d_v)
```

### 代码实现

```python
import numpy as np

def scaled_dot_product_attention(Q, K, V, mask=None):
    """
    Q: (batch, n, d_k)
    K: (batch, n, d_k)
    V: (batch, n, d_v)
    """
    d_k = Q.shape[-1]

    # Step 3: 得分矩阵
    scores = np.matmul(Q, K.transpose(0, 2, 1))  # (batch, n, n)

    # Step 4: 缩放
    scores = scores / np.sqrt(d_k)

    # Mask (Decoder 中屏蔽未来 token)
    if mask is not None:
        scores = np.where(mask == 0, -1e9, scores)

    # Step 5: Softmax
    attn_weights = np.exp(scores - scores.max(axis=-1, keepdims=True))
    attn_weights = attn_weights / attn_weights.sum(axis=-1, keepdims=True)

    # Step 6: 加权求和
    output = np.matmul(attn_weights, V)

    return output, attn_weights
```

## 3. 计算复杂度分析

```text
朴素 Attention：
  Q·Kᵀ:          O(n² · d_k)   ← n² 是瓶颈
  Softmax:        O(n²)
  Weights·V:     O(n² · d_v)
  ───────────────────────────
  总复杂度:       O(n² · d)    ← 序列长度平方！

n=512:    262K 次运算（可接受）
n=2048:   4.2M 次运算（还行）
n=32768:  1.07B 次运算（开始吃力）
n=131072: 17.2B 次运算（需要优化）

→ 这就是为什么需要 Flash Attention / Sparse Attention
```

## 4. 为什么除以 √d_k

```text
假设 Q 和 K 的元素是独立的随机变量（均值为 0，方差为 1）

Q·Kᵀ = Σ Q_i · K_i    (d_k 个独立乘积的和)

每个乘积的方差 = Var(Q_i) · Var(K_i) = 1
和的方差 = d_k (方差可加性)

∴ Q·Kᵀ 的方差随 d_k 线性增长

当 d_k = 64 时，Q·Kᵀ 的标准差 ≈ 8
→ 某些值很大，Softmax 后接近 One-Hot
→ 梯度 ≈ 0，训练不动

除以 √d_k:
  Var(Q·Kᵀ / √d_k) = d_k / d_k = 1  ← 方差归一化

结果：Softmax 平滑，梯度健康
```

## 核心要点回顾

- Attention 六步：投影 → 点积 → 缩放 → Softmax → 加权 → 输出
- Q="查询者视角", K="被查者标签", V="实际内容"
- 复杂度 O(n²·d)，序列长度是瓶颈
- √d_k 的作用：方差归一化，防止 Softmax 饱和
- 数值稳定性：`exp(x - max(x))` 代替 `exp(x)`

## 参考资料

1. Attention Is All You Need (Vaswani et al., 2017)
2. The Annotated Transformer — Harvard NLP
