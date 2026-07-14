# 02 — 大模型架构与 Transformer 详解

> **目标**：深入理解 Transformer 架构的每一个组件，以及当前主流大模型架构变体。这是理解模型行为、调优 Prompt 和选型的基础。

---

## 1. 从 RNN 到 Transformer

### 1.1 演进史

```
2014: Seq2Seq + Attention (Bahdanau)
  → 2017: Transformer (Attention Is All You Need)
    → 2018: BERT (Encoder-Only) / GPT (Decoder-Only)
      → 2019: GPT-2, T5, BART
        → 2020: GPT-3 (175B, 涌现能力被发现)
          → 2022: ChatGPT (InstructGPT → RLHF)
            → 2023: GPT-4, LLaMA, Claude, Gemini
              → 2024: GPT-4o, Claude 3.5, Gemini 2.0, DeepSeek-V3
                → 2025: Claude 4, GPT-5, Llama 4, DeepSeek-R1
```

### 1.2 Transformer 解决了什么问题

| RNN 的痛点 | Transformer 的解法 |
|-----------|------------------|
| 串行处理，无法并行 | Self-Attention 并行计算所有位置 |
| 长距离依赖衰减 | 每个位置直接关注所有位置 |
| 训练慢 | 充分利用 GPU 并行计算 |
| 梯度消失/爆炸 | Residual Connection + Layer Norm |

---

## 2. Transformer 完整架构

```
                    ┌──────────────┐
                    │   Output     │
                    │ Probabilities│
                    └──────▲───────┘
                           │
                    ┌──────┴───────┐
                    │   Softmax    │
                    └──────▲───────┘
                           │
                    ┌──────┴───────┐
                    │   Linear     │    ← 映射到词表大小 (vocab_size)
                    └──────▲───────┘
                           │
              ┌────────────┴────────────┐
              │    Add & LayerNorm       │ ← 残差连接 + 层归一化
              │           ▲              │
              │  ┌────────┴────────┐     │
              │  │ Feed Forward     │     │ ← FFN: 两个线性层 + 激活函数
              │  │ Network (FFN)   │     │
              │  └────────▲────────┘     │
              └───────────┼──────────────┘
              ┌───────────┴──────────────┐
              │    Add & LayerNorm       │  ← × N 层重复
              │           ▲              │
              │  ┌────────┴────────┐     │
              │  │ Multi-Head       │     │
              │  │ Attention (MHA)  │     │ ← 核心：自注意力
              │  └────────▲────────┘     │
              └───────────┼──────────────┘
                          │
              ┌───────────┴──────────────┐
              │   Input Embedding         │ ← Token Embedding + Positional Encoding
              └──────────────────────────┘
```

**关键数字对照**：

| 模型 | 层数 (N) | 隐藏维度 d_model | 注意力头数 | 词表大小 |
|------|----------|-----------------|-----------|---------|
| LLaMA 7B | 32 | 4096 | 32 | 32000 |
| LLaMA 70B | 80 | 8192 | 64 | 32000 |
| GPT-3 175B | 96 | 12288 | 96 | 50257 |
| DeepSeek-V3 | ~60 | 7168 | — | 129280 |
| Qwen2.5-72B | 80 | 8192 | 64 | 152064 |

---

## 3. Self-Attention 机制 —— 核心中的核心

### 3.1 直觉理解

```
Self-Attention 的本质：让句子中每个词"看到"并"权衡"其他所有词。

例句："The cat sat on the mat because it was tired."

传统方法：无法确定 "it" 指的是 "cat" 还是 "mat"
Self-Attention：计算 "it" 与每个词的注意力分数，发现与 "cat" 相关度最高
```

### 3.2 数学过程（三步走）

#### Step 1：生成 Q、K、V 矩阵

```
输入: X ∈ ℝ^{n × d_model}   (n 个 token，每个 d_model 维)

Q = X · W_Q     (Query:  "我想查什么？")
K = X · W_K     (Key:    "我是什么内容？")
V = X · W_V     (Value:  "我携带什么信息？")

其中 W_Q, W_K, W_V ∈ ℝ^{d_model × d_k}
```

#### Step 2：计算注意力分数

```
Attention(Q, K, V) = softmax( Q·K^T / √d_k ) · V

解读：
  Q·K^T        → 计算每对 token 的"匹配度"（内积）
  / √d_k        → 缩放防止梯度消失（缩放点积注意力）
  softmax(...)  → 归一化为概率分布（每行加起来 = 1）
  · V           → 按注意力权重对 Value 加权求和
```

#### Step 3：代码模拟

```python
import numpy as np

def self_attention(X, W_Q, W_K, W_V):
    n, d_model = X.shape
    d_k = W_Q.shape[1]

    Q = X @ W_Q  # (n, d_k)
    K = X @ W_K  # (n, d_k)
    V = X @ W_V  # (n, d_k)

    scores = Q @ K.T / np.sqrt(d_k)  # (n, n)
    attention_weights = np.exp(scores) / np.exp(scores).sum(axis=-1, keepdims=True)
    output = attention_weights @ V  # (n, d_k)

    return output, attention_weights
```

### 3.3 Java 实现（教学用）

```java
public class SelfAttention {
    /**
     * 单头自注意力（教学实现，不用于生产）
     * @param X  输入矩阵 [n, d_model]
     * @param WQ Query 权重 [d_model, d_k]
     * @param WK Key 权重   [d_model, d_k]
     * @param WV Value 权重 [d_model, d_k]
     * @return 注意力输出 [n, d_k]
     */
    public static double[][] forward(double[][] X,
                                      double[][] WQ,
                                      double[][] WK,
                                      double[][] WV) {
        int n = X.length;
        int d_k = WQ[0].length;

        // Q = X · W_Q
        double[][] Q = matMul(X, WQ);
        double[][] K = matMul(X, WK);
        double[][] V = matMul(X, WV);

        // Scores = Q · K^T / sqrt(d_k)
        double[][] scores = matMul(Q, transpose(K));
        double scale = Math.sqrt(d_k);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                scores[i][j] /= scale;
            }
        }

        // Softmax row-wise
        double[][] attnWeights = softmax(scores);

        // Output = attnWeights · V
        return matMul(attnWeights, V);
    }
}
```

---

## 4. Multi-Head Attention (MHA)

### 4.1 为什么需要多头

```
单头：每个 token 只能从一种"角度"关注其他 token
多头：每个头关注不同的"方面"

举例（翻译 "bank"）：
  Head 1 → "金融"相关的上下文 → 与 "money, account" 相关
  Head 2 → "河岸"相关的上下文 → 与 "river, water" 相关
  Head 3 → 语法结构              → 与 "the, of" 相关
  ...
```

### 4.2 计算过程

```
Multi-Head Attention:

Input X  →  [Head_1] → output_1
          →  [Head_2] → output_2
          →  [Head_3] → output_3        ← 每个 Head 有独立的 W_Q/W_K/W_V
          →  ...
          →  [Head_h] → output_h

Concat [output_1, ..., output_h]
  → W_O · Concat  → Final Output

维度变化：
  d_k = d_model / h
  (GPT-3: d_model=12288, h=96, d_k=128)
```

### 4.3 注意力变体对比

| 变体 | 全称 | 机制 | KV 数量 | 代表模型 |
|------|------|------|---------|---------|
| **MHA** | Multi-Head Attention | 每个注意力头有独立 K、V | h 组 | GPT-3、原始 Transformer |
| **MQA** | Multi-Query Attention | 所有头共享同一组 K、V | 1 组 | PaLM |
| **GQA** | Grouped-Query Attention | 头分组，组内共享 K、V | g 组（1 < g < h） | Llama 2/3、Gemma |

```
内存消耗对比（以 h=8 为例）：

MHA: Q₁Q₂...Q₈ × K₁K₂...K₈   → KV Cache 大（最高质量）
GQA: Q₁Q₂ Q₃Q₄ Q₅Q₆ Q₇Q₈    → KV Cache 中等（平衡）
     × K_a  × K_b  × K_c × K_d

MQA: Q₁Q₂...Q₈ × 共享K        → KV Cache 小（最快推理）
```

> 💡 **企业选型启示**：MQA 推理快但质量可能略降；GQA 是工业界首选（Llama 全系采用）。

---

## 5. Feed-Forward Network (FFN)

### 5.1 标准 FFN

```
FFN(X) = σ( X·W₁ + b₁ ) · W₂ + b₂

其中：
  X       : [n, d_model]
  W₁      : [d_model, d_ff]       d_ff 通常是 d_model 的 2.5~4 倍
  W₂      : [d_ff, d_model]
  σ       : 激活函数

常见激活函数：
  ReLU    : max(0, x)              → 传统
  GELU    : x · Φ(x)               → BERT/GPT
  SwiGLU  : x · σ(xW_gate) · W_out → LLaMA 系列（SwiGLU 变体）
  SiLU    : x · σ(x)               → Swish 同义
```

### 5.2 为什么需要 FFN

> Self-Attention 负责 **混合 token 之间的信息**；FFN 负责 **在单个 token 内部做非线性变换**。两者交替工作 → 模型既能理解上下文关系，又能做模式匹配。

---

## 6. 位置编码 (Positional Encoding)

> Transformer 的自注意力机制本身是**位置无关**的——"A 喜欢 B"和"B 喜欢 A"会产生相同的注意力模式。必须显式注入位置信息。

### 6.1 方案对比

| 方案 | 全称 | 方式 | 代表模型 |
|------|------|------|---------|
| **Sinusoidal PE** | 正弦位置编码 | 固定公式：sin/cos 不同频率 | 原始 Transformer |
| **Learned PE** | 可学习位置编码 | 将位置当作 Embedding 训练 | BERT、GPT-1 |
| **RoPE** | Rotary Position Embedding | 将 Q、K 旋转一个与位置相关的角度 | **Llama 全系、Qwen、DeepSeek、Gemma** |
| **ALiBi** | Attention with Linear Biases | 在注意力分数上加线性偏置 | BLOOM、MosaicML 模型 |

### 6.2 RoPE 详解（当前最主流）

```
RoPE 的核心思想：
  不是在输入上加位置信息，而是在 Attention 计算时，
  通过旋转矩阵让 Q 和 K 的內积天然含有相对位置信息。

公式：
  Q^(m) = Q · R(m)     (m 是位置)
  K^(n) = K · R(n)

  Q^(m) · K^(n)^T = Q · R(m-n) · K^T
           ↑ 只依赖相对位置！

优势：
  ✅ 天然编码相对位置（更符合语言直觉）
  ✅ 可外推：训练 2K，推理 4K 乃至更长
  ✅ 高效实现：只需对 Q、K 逐对维度做旋转
```

---

## 7. 大模型架构变体

### 7.1 Decoder-Only（当前绝对主流）

```
GPT / LLaMA / Claude / DeepSeek 架构：

Token₁ Token₂ ... Tokenₙ
  │     │          │
  ▼     ▼          ▼
┌─────────────────────┐
│   Embedding         │
│ + RoPE (位置编码)   │
└─────────┬───────────┘
          ▼
┌─────────────────────┐
│  Masked MHA (GQA)  │   ← Causal Mask: 每个 token 只能看到它之前的 token
│  + Residual + Norm  │     不能"偷看"未来（自回归生成的关键）
└─────────┬───────────┘
          ▼
┌─────────────────────┐
│  FFN (SwiGLU)       │
│  + Residual + Norm  │
└─────────┬───────────┘
          ▼
         ... × N 层
          ▼
┌─────────────────────┐
│  LM Head → Vocab    │
└─────────────────────┘
```

**因果掩码 (Causal Mask)** 是 Decoder-Only 的关键：

```
注意力矩阵（掩码后，× 表示被屏蔽）：

Token  我  爱  AI
我     ✓   ×   ×     ← "我" 只能看到自己
爱     ✓   ✓   ×     ← "爱" 可以看到"我""爱"
AI     ✓   ✓   ✓     ← "AI" 可以看到所有前面的 token
```

### 7.2 MoE (Mixture of Experts)

```
MoE 架构核心思想：
  不是所有参数都在每次推理时激活，而是动态路由到部分"专家"。

传统 Dense 模型：每次推理激活 100% 参数
MoE 模型：      每次推理仅激活 ~10% 参数（通过 Router / Gate）

MoE 结构示意：
                    ┌─────→ Expert₁ ──┐
  Input → Router ───┼─────→ Expert₂ ──┼──→ Output
  (Gate)            └─────→ Expert₃ ──┘
                    仅激活 Top-K 个 Expert

代表模型：
  - Mixtral 8×7B       (总 46.7B，激活 12.9B)
  - DeepSeek-V3        (总 671B，激活 37B)
  - GPT-4 (传言)       (总 ~1.8T，激活 ~280B)
  - Qwen2.5-MoE        (总 57B，激活 14B)
```

**MoE 对企业的意义**：

| 优势 | 劣势 |
|------|------|
| 同等计算量下模型更大、效果更好 | 显存占用高（需加载所有 Expert） |
| 推理时激活参数少，速度相对快 | 路由可能不均衡（Load Balancing 问题） |
| 训练效率高 | 微调更复杂 |

### 7.3 Mamba / SSM 架构

```
传统 Transformer：计算复杂度 O(n²)——注意力矩阵 n×n
Mamba (SSM)：      计算复杂度 O(n) —— 状态空间模型

适用场景：
  ✅ 超长序列处理（DNA、音频、长文本）
  ✅ 低延迟推理
  ❌ 目前在通用能力上还不如同等规模的 Transformer
  ❌ 生态不成熟

代表：Mamba、Mamba-2、Jamba（Mamba + Transformer 混合）
```

---

## 8. 生成过程：自回归解码

### 8.1 解码策略

```
自回归生成流程：
  Input: "今天天气真"
    → Model 预测下一个 Token 的概率分布
    → 根据策略选择 Token
    → 追加到输入，重复

解码策略对比：

┌──────────────┬────────────────────────────────────┐
│ 策略          │ 行为                               │
├──────────────┼────────────────────────────────────┤
│ Greedy       │ 每步选概率最大的 Token              │
│ (贪心)       │ 快速但可能陷入重复循环              │
├──────────────┼────────────────────────────────────┤
│ Beam Search  │ 保留 Top-K 条候选路径，最终选最优    │
│ (束搜索)     │ 适合翻译等确定性任务                │
├──────────────┼────────────────────────────────────┤
│ Temperature  │ 控制随机性：                        │
│ Sampling     │ T→0 = Greedy，T=1 = 原始分布        │
│ (温度采样)   │ T>1 = 更随机/有创造力，T<1 = 更保守│
├──────────────┼────────────────────────────────────┤
│ Top-K        │ 只从概率最高的 K 个 Token 中采样    │
│ Sampling     │ K 越小越保守                        │
├──────────────┼────────────────────────────────────┤
│ Top-P        │ 从累积概率达到 p 的最小 Token 集合   │
│ (Nucleus)    │ 中采样                              │
└──────────────┴────────────────────────────────────┘
```

### 8.2 关键参数

```java
// Spring AI 配置示例
OpenAiChatOptions options = OpenAiChatOptions.builder()
    .withModel("gpt-4o")
    .withTemperature(0.7)    // 温度：0~2，默认 1
    .withTopP(0.9)           // Top-P 采样
    .withMaxTokens(4096)     // 最大输出 token 数
    .withFrequencyPenalty(0.0)  // 频率惩罚（-2~2）：抑制重复
    .withPresencePenalty(0.0)   // 存在惩罚（-2~2）：鼓励多样性
    .withStop(List.of("###"))   // 停止词
    .build();
```

| 参数 | 作用 | 建议值 |
|------|------|--------|
| **temperature** | 控制随机性。0=确定性的，1=标准，>1=更随机 | 创作 0.8-1.2，代码 0-0.3，对话 0.6-0.8 |
| **top_p** | 核采样阈值 | 0.9 是常用默认值 |
| **max_tokens** | 输出长度上限 | 按需设置，不要超过上下文窗口-输入长度 |
| **frequency_penalty** | 抑制已出现 token 的重复 | 长文本生成 0.3-0.5 |
| **presence_penalty** | 鼓励新话题/词汇 | 对话 0.3-0.5 |
| **stop** | 遇到这些字符串立即停止 | `["\n\n", "User:"]` 等 |

---

## 9. KV Cache —— 推理加速的关键

### 9.1 原理

```
无 KV Cache（每次都重新计算）：
  生成 "我" → 计算 Attention(我 → 我)
  生成 "爱" → 计算 Attention(我爱 → 我) + Attention(我爱 → 爱)  ← 上面重复计算了
  生成 "AI" → 计算 Attention(我爱AI → 我) + ...                ← 上面又重复了

有 KV Cache（缓存已计算的 K、V）：
  生成 "我" → 计算 Attention(我 → 我)，缓存 K₁, V₁
  生成 "爱" → 只需计算 Attention(爱 → 我,爱)，复用 K₁,V₁，缓存 K₂, V₂
  生成 "AI" → 只需计算 Attention(AI → 我,爱,AI)，复用 K₁,V₁,K₂,V₂
```

### 9.2 显存计算

```
KV Cache 显存占用（单个序列）：

每层 KV Cache = 2 × n_tokens × d_model × dtype_bytes
每层 KV Cache for GQA = 2 × n_tokens × (d_model/head) × g × dtype_bytes

示例（Llama 7B, FP16, GQA g=32, 4K context）:
KV Cache ≈ 80 层 × 2 × 4096 × (4096/32) × 32 × 2 bytes
        = 80 × 2 × 4096 × 128 × 2
        = 80 × 2,097,152
        ≈ 167 MB / 序列

高并发时 KV Cache 成为显存瓶颈！
```

---

## 10. 快速复习

```
□ Transformer 三大组件：Self-Attention / FFN / Layer Norm + Residual
□ Self-Attention 计算过程：QKV → Scores → Softmax → Weighted Sum
□ 缩放因子 √d_k 的作用：防止 softmax 进入饱和区
□ MHA / MQA / GQA 的区别和适用场景
□ Causal Mask 为何必须（Decoder-Only）
□ RoPE 为什么是当前主流位置编码
□ MoE 的"总参数 vs 激活参数"概念
□ KV Cache 原理和内存计算
□ Temperature / Top-P / Top-K 的作用
```

---

> **下一步**：[03 — 大模型训练与微调](./03-大模型训练与微调.md)
