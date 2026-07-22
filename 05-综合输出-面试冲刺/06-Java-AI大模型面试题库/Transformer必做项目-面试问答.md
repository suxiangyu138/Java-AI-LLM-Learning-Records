# Transformer 面试问答清单
> 🎯 基于 Transformer 实战项目清单，涵盖面试高频问题与完美解答方案，帮助你在大模型核心架构理解上脱颖而出。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理

> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：Transformer 的核心创新是什么？自注意力机制是如何计算的？请手写关键公式。
**面试官意图：** 考察你对 Transformer 最核心原理的理解深度——自注意力机制（Self-Attention）。

**完美解答：**
Transformer 的核心创新是**抛弃了 RNN/CNN，完全依赖自注意力机制（Self-Attention）来建模序列依赖关系**。它的革命性在于两点：第一，任意两个位置的 token 可以直接交互，解决了 RNN 的长距离依赖问题；第二，所有位置可以并行计算，解决了 RNN 的串行瓶颈。

**缩放点积注意力的计算过程**：

```
输入: Q(Query), K(Key), V(Value) — 三个矩阵，来自同一序列
计算:
  1. Q × K^T → 得到注意力分数矩阵 (相似度)
  2. ÷ √d_k → 缩放 (防止 softmax 梯度消失)
  3. Softmax → 归一化为注意力权重
  4. × V → 加权求和得到输出
公式: Attention(Q,K,V) = softmax(QK^T / √d_k) × V
```

**PyTorch 实现**：
```python
import torch
import torch.nn as nn
import math

class ScaledDotProductAttention(nn.Module):
    def __init__(self, d_k):
        super().__init__()
        self.d_k = d_k
    
    def forward(self, Q, K, V, mask=None):
        # Q, K, V: [batch_size, seq_len, d_k]
        scores = torch.matmul(Q, K.transpose(-2, -1)) / math.sqrt(self.d_k)
        # scores: [batch_size, seq_len, seq_len]
        
        if mask is not None:
            # Padding Mask: 填充位置设为负无穷
            # Sequence Mask: 未来位置设为负无穷（Decoder 中的因果掩码）
            scores = scores.masked_fill(mask == 0, -1e9)
        
        attn_weights = torch.softmax(scores, dim=-1)
        output = torch.matmul(attn_weights, V)
        return output, attn_weights
```

**为什么除 √d_k？**
当维度 d_k 较大时，点积结果的方差会变大（约为 d_k），导致 softmax 的梯度极端化。除以 √d_k 可以把方差归一化到 1，保持梯度稳定。

**延伸追问应对：** 如果问"为什么自注意力比 RNN 好"，回答三个维度：计算复杂度（并行 vs 串行）、长距离依赖（O(1) 距离 vs O(n) 距离）、梯度传播（直连 vs 链式）。

---

### Q2：多头注意力（Multi-Head Attention）为什么要用"多头"？多头到底学到了什么？
**面试官意图：** 考察对多头注意力机制动机的深度理解，而不是背诵公式。

**完美解答：**
"多头"的本质是**让模型从不同的表示子空间中学习不同类型的注意力模式**。单头注意力只能学习一种加权方式，而多头可以并行学习多种。

**多头的工作原理**：
```python
class MultiHeadAttention(nn.Module):
    def __init__(self, d_model, num_heads):
        super().__init__()
        self.num_heads = num_heads
        self.d_k = d_model // num_heads  # 每个头的维度
        
        # 线性投影：将 d_model 映射到 d_model
        self.W_Q = nn.Linear(d_model, d_model)
        self.W_K = nn.Linear(d_model, d_model)
        self.W_V = nn.Linear(d_model, d_model)
        self.W_O = nn.Linear(d_model, d_model)
    
    def forward(self, Q, K, V, mask=None):
        batch_size = Q.size(0)
        
        # 1. 线性投影 + 分头
        Q = self.W_Q(Q).view(batch_size, -1, self.num_heads, self.d_k).transpose(1, 2)
        K = self.W_K(K).view(batch_size, -1, self.num_heads, self.d_k).transpose(1, 2)
        V = self.W_V(V).view(batch_size, -1, self.num_heads, self.d_k).transpose(1, 2)
        # 形状: [batch, num_heads, seq_len, d_k]
        
        # 2. 每个头独立计算注意力
        attn_output, _ = ScaledDotProductAttention(self.d_k)(Q, K, V, mask)
        # 形状: [batch, num_heads, seq_len, d_k]
        
        # 3. 合并头
        attn_output = attn_output.transpose(1, 2).contiguous()
        attn_output = attn_output.view(batch_size, -1, self.num_heads * self.d_k)
        
        # 4. 输出投影
        return self.W_O(attn_output)
```

**每个头学到什么？**
在实际训练好的模型中，不同头会关注不同的模式：
- 有些头关注**语法依赖**（主谓关系、修饰关系）
- 有些头关注**位置邻近**（相邻词的关系）
- 有些头关注**语义关联**（同义词、指代关系）
- 在深层中，有些头学会**特殊的模式**（如句首句尾标记、特殊 token 聚焦）

> 💡 头数不是越多越好：d_model / num_heads = d_k 不能太小，否则每个头学到的信息太少。典型配置：d_model=512, num_heads=8, d_k=64。

---

### Q3：位置编码（Positional Encoding）为什么需要？正余弦位置编码和 RoPE 旋转位置编码有什么区别？
**面试官意图：** 考察对 Transformer 位置信息编码的理解，以及追踪最新技术演进的能力。

**完美解答：**
**为什么需要位置编码**：自注意力机制本身是**置换不变的**——它不知道 token 的先后顺序。对于"我爱你"和"你爱我"，如果不加位置信息，模型会认为是一样的。位置编码的作用就是把位置信息注入模型。

**正余弦位置编码（原始 Transformer）**：
```python
class SinusoidalPositionEncoding(nn.Module):
    def __init__(self, d_model, max_len=5000):
        super().__init__()
        pe = torch.zeros(max_len, d_model)  # [max_len, d_model]
        position = torch.arange(0, max_len).unsqueeze(1)  # [max_len, 1]
        div_term = torch.exp(
            torch.arange(0, d_model, 2) * -(math.log(10000.0) / d_model)
        )
        pe[:, 0::2] = torch.sin(position * div_term)  # 偶数维度用 sin
        pe[:, 1::2] = torch.cos(position * div_term)  # 奇数维度用 cos
        self.register_buffer('pe', pe)
    
    def forward(self, x):
        return x + self.pe[:x.size(1)]
```

**RoPE（旋转位置编码，Llama 系列使用）**：
RoPE 的核心思想是**通过旋转变换在注意力计算中隐式编码位置**，而不是简单地加到输入上。

| 对比维度 | Sinusoidal PE | RoPE |
|----------|---------------|------|
| 编码方式 | 加到输入向量上 | 通过旋转矩阵变换 Q/K |
| 相对位置感知 | 隐式（模型自己学习） | 显式（旋转矩阵包含相对位置关系） |
| 外推能力 | 有限（无法超过 max_len） | 强（任意长度，已有 2M 上下文） |
| 参数量 | 0（固定编码） | 0（固定变换） |
| 训练稳定性 | 好 | 好 |

**为什么 RoPE 更优**：RoPE 把位置信息编码到 Q 和 K 的点积中，使得注意力分数天然依赖相对位置——距离越近的 token 注意力值越大，这种归纳偏置更符合语言的自然规律。

---

## 2. 项目实战深度问答

> 💡 面试官会深挖你的项目细节，验证你"真的做过"而不是"背过"

### Q4：你手写过迷你 Transformer，请讲讲 Encoder 层和 Decoder 层的核心区别，以及在训练时和推理时的流程差异。
**面试官意图：** 考察对 Transformer Encoder-Decoder 架构的完整理解，以及训练推理差异的工程认知。

**完美解答：**
**Encoder 与 Decoder 的核心区别**：

| 维度 | Encoder | Decoder |
|------|---------|---------|
| 注意力类型 | 双向自注意力（看到全部 token） | 因果掩码自注意力（只能看到前面的 token） |
| 交叉注意力 | 无 | 有（用 Encoder 输出作为 K/V，Decoder 的 Q 去查询） |
| 掩码方式 | Padding Mask | Padding Mask + Sequence Mask |
| 应用 | BERT、分类、理解任务 | GPT、生成任务 |

**训练流程（以机器翻译为例）**：
```
输入: "I love you"  
Encoder: 双向注意力编码所有 token
Decoder 训练: 
  - 输入: "<sos> 我 爱" (教师强制，用真实上一步)
  - 因果掩码: 每个位置只能看到自己和前面
  - 交叉注意力: 查询 Encoder 的输出
  - 输出: 预测下一个词 "你"
损失: CrossEntropy(预测, 真实)
```

**推理流程（自回归生成）**：
```
Step 1: 输入 "<sos>" → 预测 "我"
Step 2: 输入 "<sos> 我" → 预测 "爱"
Step 3: 输入 "<sos> 我 爱" → 预测 "你"
Step 4: 输入 "<sos> 我 爱 你" → 预测 "<eos>"

优化: 使用 KV Cache 缓存之前层的 K/V，避免重复计算
```

**关键差异点**：
- 训练时 Teacher Forcing 并行计算所有位置
- 推理时只能一个一个 token 生成（串行）
- 训练时用 Sequence Mask 防止偷看未来，推理时自动满足因果

---

### Q5：你在 LoRA 微调项目中，怎么选择 target_modules？rank 和 alpha 怎么配置？为什么？
**面试官意图：** 考察 LoRA 高效微调的实战经验，特别是参数配置的工程决策。

**完美解答：**
LoRA（Low-Rank Adaptation）通过**低秩矩阵分解**冻结预训练权重，只训练小规模适配器，大幅降低训练显存需求。

**target_modules 选择**：
```yaml
# Qwen2.5 的 LoRA 配置
target_modules:
  - q_proj    # Query 投影（最重要，必选）
  - k_proj    # Key 投影（推荐）
  - v_proj    # Value 投影（推荐）
  - o_proj    # Output 投影（可选）
  - gate_proj # FFN 门控（可选，对能力提升大）
  - up_proj    # FFN 升维投影（可选）
  - down_proj  # FFN 降维投影（可选）
```

**选择原则**：
- **必选**：`q_proj` 和 `v_proj`（注意力机制的核心，影响最大）
- **推荐**：加上 `k_proj` 和 `o_proj`（平衡效果和参数量）
- **进阶**：加 FFN 层投影（门控模块，对领域适配帮助大）

**rank 和 alpha 配置**：
```yaml
# 推荐配置（通用场景）
lora_rank: 16      # 低秩矩阵的 rank
lora_alpha: 32     # 缩放因子
lora_dropout: 0.05 # 防止过拟合

# 参数解读
# - rank=16: 每层适配器参数量为 d_model × rank × 2
#   对于 7B 模型，d_model=4096，每层参数量 ≈ 4096×16×2 = 131K
#   LoRA 总参数量 ≈ 131K × 层数 × 目标模块数
# - alpha: 缩放系数，通常为 rank 的 2 倍
#   实际权重 = lora_weight × (alpha / rank)

# 不同场景的 rank 选择
场景:
  代码生成: rank=32  # 需要更多新知识
  通用对话: rank=16  # 默认配置
  简单分类: rank=8   # 任务容易，少参数防过拟合
  NL2SQL: rank=32    # 需要学习新的结构化知识
```

**为什么 rank 选 16 而不是更大？**
- rank=16 已经能捕获大部分领域知识，参数量约为原始模型的 0.1-0.5%
- rank 每翻一倍，参数量翻倍，显存和训练时间也线性增加
- 更大的 rank 在通用任务上收益递减，反而可能过拟合

**训练效果数据**：
> rank=16, target_modules=[q_proj, v_proj]：准确率提升 12%，训练显存 16GB（7B 模型）
> rank=32, target_modules=[q_proj, k_proj, v_proj, o_proj, gate_proj]：准确率提升 15%，训练显存 22GB
> 性价比最优的是前者。

---

### Q6：你在做 Transformer RAG 系统时，语义检索的"召回-重排-生成"全链路是怎么设计的？
**面试官意图：** 考察 RAG 全链路设计的完整性和工程经验。

**完美解答：**
RAG 的全链路设计是决定系统质量的核心，我的方案是**"粗召回 → 精重排 → 安全生成"三段式**：

**第一段：粗召回（多路召回）**
```java
public List<Document> recall(String query, int topK) {
    List<Document> results = new ArrayList<>();
    
    // 路径 1：向量检索（语义相似）
    float[] embedding = embedService.embed(query);
    results.addAll(vectorStore.search(embedding, topK * 2));
    
    // 路径 2：BM25 关键词检索（精确匹配）
    results.addAll(bm25Search.search(query, topK));
    
    // 路径 3：如果有属性过滤（时间、类型等）
    results.addAll(filterSearch(query, topK));
    
    return results;
}
```

**第二段：精重排（Cross-Encoder Reranker）**
召回阶段用的是双编码器（Bi-Encoder），速度快但精度不够。重排阶段用交叉编码器（Cross-Encoder），精度高但速度慢。
```java
public List<Document> rerank(String query, List<Document> candidates) {
    // Cross-Encoder 逐对打分
    return candidates.stream()
        .map(doc -> {
            double score = crossEncoder.score(query, doc.getContent());
            return new ScoredDoc(doc, score);
        })
        .sorted(Comparator.comparingDouble(ScoredDoc::getScore).reversed())
        .limit(TOP_K_FINAL)
        .collect(Collectors.toList());
}
```

**第三段：安全生成**
```python
# RAG Answer Prompt
rag_prompt = f"""基于以下参考内容回答问题：

## 参考内容
{context}

## 问题
{question}

要求：
1. 仅基于参考内容回答
2. 标注引用来源 [来源：{doc.title}]
3. 如果参考内容不足以回答，说"无法回答"
4. 不要添加参考内容之外的信息
"""
```

**关键优化**：
```yaml
召回优化:
  - 分块大小: 512 tokens + 128 overlap
  - TopK: 粗召回 20 条 → 重排后取 5 条
  - 混合检索权重: 向量 0.7 + 关键词 0.3

重排优化:
  - 使用 BGE-Reranker 模型
  - 批处理大小: 32 条/批
  - 重排让 Top-5 命中率从 70% 提升到 92%

生成优化:
  - Temperature: 0.1（保守）
  - 引用溯源: 强制标注来源
  - 幻觉检测: 输出后做一轮自检
```

---

## 3. 进阶与系统设计

> 💡 这是拉开差距的环节，考察架构思维

### Q7：LLaMA 系列使用的 RMSNorm 和 SwiGLU 激活函数与原始 Transformer 的 LayerNorm 和 ReLU 有什么本质区别？为什么 LLaMA 选它们？
**面试官意图：** 考察对现代 Transformer 架构演进的深度理解，以及架构设计选择的洞察力。

**完美解答：**
这是 LLaMA 架构对原始 Transformer 做的最重要的两个改进。

**RMSNorm vs LayerNorm**：

| 对比 | LayerNorm | RMSNorm |
|------|-----------|---------|
| 计算 | (x - mean) / std × γ + β | x / RMS(x) × γ |
| 参数量 | 2 个（γ, β） | 1 个（γ） |
| 计算量 | 需要均值和方差 | 只需要 RMS |
| 速度 | 慢（约 15-20% 开销） | 快 |
| 效果 | 标准 | 与 LN 相当 |

RMSNorm 的核心思想是：**归一化中最重要的是"缩放"而不是"平移"**，所以去掉了均值减法和 β 参数，只保留 RMS 缩放。

```python
class RMSNorm(nn.Module):
    def __init__(self, d_model, eps=1e-6):
        super().__init__()
        self.weight = nn.Parameter(torch.ones(d_model))
        self.eps = eps
    
    def forward(self, x):
        # RMS: Root Mean Square
        rms = torch.sqrt(torch.mean(x ** 2, dim=-1, keepdim=True) + self.eps)
        return x / rms * self.weight
```

**SwiGLU vs ReLU**：
SwiGLU = Swish × Gate，是一种门控激活函数：
```python
# SwiGLU 激活
def swiglu(x, gate):
    return x * torch.sigmoid(gate) * gate  # Swish(gate) * x
    # 实际实现通常是 x * sigmoid(x) * gate
```
SwiGLU 比 ReLU 好了约 2-3 个百分点的准确率，但代价是 FFN 层参数量增加了 1/3（因为多了一个 gate 矩阵）。

**为什么 LLaMA 做这些选择**：
- RMSNorm：减少计算量，在数百亿参数规模下显着节省算力
- SwiGLU：提升模型质量，特别是在推理和代码生成任务上
- RoPE 替代正余弦 PE：更好的长度外推能力
- 这些选择共同让 LLaMA 在 1/2 的训练 token 下达到了原始 GPT 的性能

---

### Q8：你了解 Transformer 的推理加速技术吗？请讲讲 KV Cache、PagedAttention、vLLM 的原理和关系。
**面试官意图：** 考察对推理优化技术的系统性理解，这是大模型工程化的核心技能。

**完美解答：**
这三个技术是递进关系：KV Cache 是基础优化，PagedAttention 是其内存管理的革新，vLLM 是将两者工程化的完整系统。

**KV Cache（缓存历史 Key/Value）**：
在自回归生成的每个 step，Decoder 需要计算当前 token 与所有历史 token 的注意力。如果不优化，每次都要重新计算所有历史 token 的 K 和 V，复杂度 O(N^2)。

KV Cache 的核心思想是**"算一次，存起来，下次复用"**：
```
Step 1: 输入 [t1] → 计算 K1, V1 → 缓存 → 输出 t2
Step 2: 输入 t2 → 复用 K1,V1, 只算 K2,V2 → 注意力用 [K1,K2],[V1,V2] → 输出 t3
Step 3: 输入 t3 → 复用 K1,K2,V1,V2, 只算 K3,V3 → ...
```
效果：从 O(N^2) 降为 O(N)，生成速度提升 10-100 倍。

**PagedAttention（分页注意力）**：
KV Cache 的痛点在于它的大小不确定（取决于序列长度），而且会不断增长。传统方案会造成大量内存碎片和浪费。

PagedAttention 借鉴了操作系统的**分页内存管理**思想：
```yaml
传统方案: 为每个请求预分配最大可能长度的连续内存
  - 内存浪费: 大部分请求不会达到最大长度
  - 碎片化: 内存释放后难以继续利用

PagedAttention:
  - 将 KV Cache 分成固定大小的"块/页"（Block）
  - 按需分配，不用预分配全部内存
  - 物理上不连续，通过页表映射
  - 内存利用率从 20-40% 提升到 90%+
```

**vLLM = PagedAttention + 高性能推理引擎**：
vLLM 是一个开源推理加速框架，它整合了 PagedAttention 和各种优化策略：

```yaml
vLLM 的核心优化:
  1. PagedAttention 内存管理 → 内存利用率 90%+
  2. 动态批处理（Continuous Batching）→ 高吞吐
  3. 异步调度 → 无阻塞推理
  4. OpenAI 兼容 API → 即插即用

效果对比:
  - vs HuggingFace Transformers: 吞吐量提升 5-10 倍
  - vs 原生推理: 内存节省 60-80%
  - 单张 A100 可以部署 13B 模型服务 100+ QPS
```

---

## 4. 场景题与故障排查

> 💡 考察实际解决问题的能力

### Q9：你训练的 GPT 迷你模型在推理时总是生成重复内容（循环），怎么解决？
**面试官意图：** 考察对自回归生成中常见问题的诊断和解决能力。

**完美解答：**
生成重复是自回归模型最常见的故障模式之一，根因是模型在高概率 token 上陷入局部循环。

**解决方案体系（从简单到复杂）**：

**1. 采样策略优化**（最常用，效果最好）：
```python
def generate_with_anti_repetition(model, prompt, max_len=100):
    generated = []
    for _ in range(max_len):
        logits = model(prompt, generated)
        
        # Top-K 采样：只从 top-K 个高概率 token 中采样
        top_k_logits, top_k_indices = torch.topk(logits, k=50)
        probs = torch.softmax(top_k_logits / temperature, dim=-1)
        next_token = top_k_indices[torch.multinomial(probs, 1)]
        
        # Repetition Penalty：对已出现 token 做惩罚
        for token in set(generated):
            logits[token] /= 1.2  # 出现过的 token 概率降低
        
        generated.append(next_token)
```

**2. 引入多样性参数**：
- **Temperature 调度**：生成长文时逐步提高 Temperature，后期增加多样性
- **Top-P 采样**：动态选择候选集，避免高概率 token 垄断
- **Frequency Penalty**：OpenAI 的解决方案，出现频率越高惩罚越大

**3. 模型层面的优化**：
- 增加训练数据的多样性（数据去重很关键）
- 调整 beam search 的 beam size（beam width 太大也可能导致重复）
- 加入重复检测的 trigger token

> 💡 实际经验：大多数情况下，**Repetition Penalty + Top-K 采样**就足够了。penalty=1.1-1.3 是比较安全的范围。

---

### Q10：你的项目中，微调后的模型出现了"灾难性遗忘"——学会了新任务但忘记了通用对话能力，怎么处理？
**面试官意图：** 考察微调过程中的灾难性遗忘（Catastrophic Forgetting）问题的解决经验。

**完美解答：**
灾难性遗忘是微调中最常见的陷阱，解决方法分为数据层面和训练层面：

**数据层面**：
```yaml
混合训练策略:
  通用数据: 30%  # 保持通用对话能力
  领域数据: 60%  # 注入领域知识
  指令数据: 10%  # 保持指令跟随能力

关键操作:
  1. 每个 batch 包含通用 + 领域 + 指令三种数据
  2. 通用数据从原模型训练集中采样（保持分布一致）
  3. 领域数据要多样化，避免单一模式过拟合
```

**训练层面**：
```yaml
优化策略:
  1. LoRA rank 不宜过大 (8-16 最佳)
     - rank 越大，可训练参数越多，越容易过拟合

  2. 学习率调度:
     - 初始学习率: 2e-4 (LoRA 标准)
     - 余弦退火衰减
     - 前 10% steps warmup

  3. 早停策略:
     - 在验证集上监控通用任务准确率
     - 如果通用任务准确率下降超过 5%，停止训练

  4. EWC (弹性权重巩固):
     - 对重要参数施加更大的正则化惩罚
     - 减少对通用知识的"改写"
```

**验证方法**：
```
微调前评估 → 通用对话评分 85/100
微调后评估 → 通用对话评分 82/100 ✓ (仅下降 3%)
              领域任务评分 90/100 ✓
```
如果通用评分下降超过 10%，说明遗忘严重，需要增加通用数据比例。

---

### Q11：你的 Transformer 服务在生产环境上 GPU 显存不足，怎么在不换硬件的情况下优化？
**面试官意图：** 考察部署优化和资源受限场景下的工程能力。

**完美解答：**
这是一个非常实际的问题。我从**模型层面、推理层面、系统层面**三个维度给出优化方案：

**模型层面**：
```yaml
1. 模型量化（效果最显著）:
   - FP16 → INT8: 显存减半，精度损失 < 1%
   - INT8 → INT4: 显存再减半，精度损失 2-3%
   - GGUF 格式: 支持 CPU 推理，GPU 辅助加速

2. 模型压缩:
   - 层数剪枝: 移除后几层（效果略降但显存节省明显）
   - 注意力头剪枝: 减少 num_heads
```

**推理层面**：
```yaml
3. KV Cache 优化:
   - 限制最大生成长度（max_new_tokens）
   - 启用 PagedAttention（vLLM 特性）
   - 设置较短的 keep_alive 时间

4. 批处理优化:
   - 使用 dynamic batching
   - 合并短请求为一个 batch
   - 控制 batch_size 不超过显存余量
```

**系统层面**：
```yaml
5. CPU Offloading:
   - 部分层放到 CPU 计算（慢但显存不足时的保底方案）
   - vLLM 支持将 KV Cache 放在 CPU

6. 模型分片:
   - 多 GPU 张量并行
   - vLLM 的 tensor_parallel 自动分片

7. 降级策略:
   - 切换为更小的模型（13B → 7B → 3B）
   - 单个请求占用显存过高时排队等待
```

> 💡 实践建议：优先做 INT4 量化 + 控制并发数。这两个改动可以在不修改代码的情况下解决 80% 的 OOM 问题。

---

### Q12：在训练 BERT/GPT 时，你的 Loss 曲线不下降或突然 Nan，怎么排查？
**面试官意图：** 考察训练过程中的故障排查经验。

**完美解答：**
Loss 不下降或 NaN 是训练中最常见也最令人头疼的问题。

**Loss 不下降（不收敛）排查**：
```yaml
1. 检查数据（最常见原因）:
   - 标签是否正确？有无错标？
   - 输入是否有大量空值？
   - 是否存在数据泄漏（训练/测试数据重叠）？

2. 检查学习率:
   - 学习率太大 → loss 振荡不降
   - 学习率太小 → loss 降得太慢
   建议: 先用 lr=3e-5（BERT 微调标准）测试

3. 检查模型结构:
   - 初始化是否正确？(权重不要全零初始化)
   - 残差连接是否正常？
   - 层归一化参数是否正确？
```

**Loss 突然 NaN 排查**：
```yaml
1. 梯度爆炸（最常⻅）:
   - 启用梯度裁剪: torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
   - 降低学习率: 从 5e-5 降到 2e-5

2. 数值不稳定性:
   - 检查 softmax 输入是否过大（用 -1e9 而不是 -1e9 以外的数值做 mask）
   - 检查 log 输入是否接近 0（加 epsilon: log(x + 1e-8)）
   - 使用混合精度训练时检查 fp16 溢出

3. 数据问题:
   - 输入中有 NaN 值
   - 标签中有越界值
   - batch 中全是 padding

快速修复:
  1. 在 loss.backward() 前检查 loss 值
  2. 在 optimizer.step() 前检查梯度
  3. 在每一步用 torch.isnan(x).any() 定位位置
```

---

## 💎 面试加分金句

- "Transformer 说到底是'用计算换记忆'——通过大量的矩阵运算替代了 RNN 的隐状态传递，换来的是并行效率和长距离建模能力。"
- "自注意力机制最优雅的设计是缩放因子 √d_k——一个简单的数学变换，解决了高维空间 softmax 梯度消失的大问题。"
- "LoRA 的精妙之处在于它用极小的参数（0.1% 的参数量）实现了 90% 以上的全参数微调效果，这是低秩假设在 NLP 中的一次完美验证。"
- "从原始的 Transformer 到 LLaMA，架构的演进本质是对'质量 vs 效率'的持续平衡——RMSNorm 省掉了 50% 的归一化计算，效果几乎不变。"
- "vLLM 的 PagedAttention 是我在 AI 工程领域见过最优雅的设计——把操作系统的内存管理智慧用在了 Transformer 推理中。"

## 📋 高频追问清单

| 追问方向 | 应对策略 |
|----------|----------|
| Transformer 的复杂度是多少？ | O(n²·d) 注意力 + O(n·d²) FFN，n 是序列长度，d 是维度 |
| 为什么 Transformer 需要残差连接？ | 解决深层网络梯度消失，让信息直连 |
| Batch Norm vs Layer Norm 怎么选？ | NLP 用 Layer Norm（序列长度不定），CV 用 Batch Norm |
| GELU 和 SwiGLU 的区别？ | GELU = x·Φ(x)，SwiGLU 是门控版本，效果更好但参数量多 1/3 |
| LLM 最大上下文窗口怎么突破？ | RoPE 外推、ALiBi 位置编码、窗口注意力 |
| Pre-LN 和 Post-LN 的区别？ | Pre-LN 更稳定（先归一化后计算），现代模型都用 Pre-LN |

## 🔗 关联知识点

- [大模型必做项目清单-面试问答.md](./大模型必做项目清单-面试问答.md)
- [大模型微调必做项目-面试问答.md](./大模型微调必做项目-面试问答.md)
- [大模型部署必做项目清单-面试问答.md](./大模型部署必做项目清单-面试问答.md)
