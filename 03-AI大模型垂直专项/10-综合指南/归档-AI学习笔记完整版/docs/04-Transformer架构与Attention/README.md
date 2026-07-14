# 第4步：Transformer架构与Attention机制

> **阶段目标：** 深入理解Attention机制的本质，掌握BERT和GPT的核心原理及区别，为使用大模型打下坚实的理论基础  
> **预计学时：** 2-3周（每天3-4小时）  
> **前置要求：** Python + ML基础 + NLP基础（Tokenization, Embedding）  

---

## 📚 目录

- [4.1 为什么Transformer是分水岭](#41-为什么transformer是分水岭)
- [4.2 Attention机制深度解析](#42-attention机制深度解析)
- [4.3 Transformer完整架构](#43-transformer完整架构)
- [4.4 BERT：编码器-only架构](#44-bert编码器-only架构)
- [4.5 GPT：解码器-only架构](#45-gpt解码器-only架构)
- [4.6 现代大模型架构演进](#46-现代大模型架构演进)
- [4.7 从零实现Mini Transformer](#47-从零实现mini-transformer)
- [4.8 阶段练习](#48-阶段练习)
- [4.9 常见问题](#49-常见问题)

---

## 4.1 为什么Transformer是分水岭

### 4.1.1 前Transformer时代 vs 后Transformer时代

```
Pre-2017 (RNN/LSTM 时代):
  "我 喜欢 吃 苹果" → RNN → RNN → RNN → RNN
                         顺序处理，无法并行
                         长序列梯度消失
                         每步依赖前一步

Post-2017 (Transformer 时代):
  "我 喜欢 吃 苹果" → Attention (同时看所有词)
                         完全并行化
                         直接建模任意距离的依赖
                         Scale! Scale! Scale!
```

### 4.1.2 一句话理解Transformer

> **Transformer = Self-Attention + Feed-Forward Network**  
> Self-Attention让每个词看到其他所有词，FFN进行非线性变换。  
> 把这个模块堆叠N次，就是Transformer。

---

## 4.2 Attention机制深度解析

### 4.2.1 直观理解

```
句子: "The cat sat on the mat because it was tired."

问题: "it" 指的是什么？
人类: 看一眼就知道是 "cat"
RNN: 需要一步步读过去，中间经历了 "sat on the mat because..."
     到了 "it" 时，"cat" 的信息已经衰减了
Attention: "it" 可以直接 "看向" "cat"
          不管它们之间隔了多少个词！

Attention就是你给我一个词(query)，我帮你找出它应该关注哪些词(key)，
以及该多关注它们(attention weight)，然后聚合它们的值(value)。
```

### 4.2.2 Self-Attention的数学

```
输入: X ∈ R^{seq_len × d_model}  (例如: 10个词, 每个512维)

Step 1: 线性投影 → Q, K, V
  Q = X @ W_Q    (Query:  我要查什么)
  K = X @ W_K    (Key:    我有什么标签)
  V = X @ W_V    (Value:  我的实际内容)

Step 2: 计算Attention Score
  Scores = Q @ K^T / √d_k    (除以√d_k防止梯度消失)

Step 3: Softmax → 归一化为权重
  Weights = softmax(Scores)

Step 4: 加权求和
  Output = Weights @ V

完整公式:
  Attention(Q, K, V) = softmax(QK^T / √d_k) V
```

### 4.2.3 从零实现Self-Attention

```python
import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F

class SelfAttention(nn.Module):
    """从零实现 Scaled Dot-Product Self-Attention"""
    
    def __init__(self, d_model: int = 512, d_k: int = 64, d_v: int = 64):
        super().__init__()
        self.d_k = d_k
        
        # Q, K, V 的投影矩阵
        self.W_Q = nn.Linear(d_model, d_k, bias=False)
        self.W_K = nn.Linear(d_model, d_k, bias=False)
        self.W_V = nn.Linear(d_model, d_v, bias=False)
        
        # 输出投影
        self.W_O = nn.Linear(d_v, d_model, bias=False)
    
    def forward(self, x: torch.Tensor, 
                mask: torch.Tensor = None) -> torch.Tensor:
        """
        x: (batch_size, seq_len, d_model)
        mask: (batch_size, seq_len, seq_len) or None
        """
        # 投影
        Q = self.W_Q(x)  # (B, seq_len, d_k)
        K = self.W_K(x)  # (B, seq_len, d_k)
        V = self.W_V(x)  # (B, seq_len, d_v)
        
        # 计算Attention Score
        scores = torch.matmul(Q, K.transpose(-2, -1)) / np.sqrt(self.d_k)
        
        # 应用Mask（Decoder中遮挡未来位置）
        if mask is not None:
            scores = scores.masked_fill(mask == 0, float('-inf'))
        
        # Softmax + 加权求和
        attn_weights = F.softmax(scores, dim=-1)       # (B, seq_len, seq_len)
        output = torch.matmul(attn_weights, V)          # (B, seq_len, d_v)
        
        # 输出投影
        output = self.W_O(output)                       # (B, seq_len, d_model)
        
        return output


# ========== 验证 ==========
batch_size, seq_len, d_model = 2, 5, 512
x = torch.randn(batch_size, seq_len, d_model)

attn = SelfAttention(d_model=d_model)
out = attn(x)

print(f"输入shape: {x.shape}")      # (2, 5, 512)
print(f"输出shape: {out.shape}")    # (2, 5, 512)
print("✓ Self-Attention输入输出维度一致")
```

### 4.2.4 Multi-Head Attention

```python
class MultiHeadAttention(nn.Module):
    """
    多头注意力 = 多个Self-Attention并行，然后拼接
    
    为什么需要多头？
    - 不同头关注不同的语义关系
    - 头1可能关注语法关系（主语-谓语）
    - 头2可能关注指代关系（代词-先行词）
    - 头3可能关注位置关系（相邻词）
    """
    
    def __init__(self, d_model: int = 512, n_heads: int = 8):
        super().__init__()
        assert d_model % n_heads == 0
        
        self.d_model = d_model
        self.n_heads = n_heads
        self.d_k = d_model // n_heads
        
        # 联合投影 Q, K, V → 比分别投影更高效
        self.W_Q = nn.Linear(d_model, d_model, bias=False)
        self.W_K = nn.Linear(d_model, d_model, bias=False)
        self.W_V = nn.Linear(d_model, d_model, bias=False)
        self.W_O = nn.Linear(d_model, d_model, bias=False)
    
    def forward(self, x: torch.Tensor, mask: torch.Tensor = None):
        B, seq_len, _ = x.shape
        
        # 线性投影
        Q = self.W_Q(x)
        K = self.W_K(x)
        V = self.W_V(x)
        
        # 拆分为多头: (B, seq_len, d_model) → (B, n_heads, seq_len, d_k)
        Q = Q.view(B, seq_len, self.n_heads, self.d_k).transpose(1, 2)
        K = K.view(B, seq_len, self.n_heads, self.d_k).transpose(1, 2)
        V = V.view(B, seq_len, self.n_heads, self.d_k).transpose(1, 2)
        
        # Scaled Dot-Product Attention
        scores = torch.matmul(Q, K.transpose(-2, -1)) / np.sqrt(self.d_k)
        if mask is not None:
            scores = scores.masked_fill(mask == 0, float('-inf'))
        attn_weights = F.softmax(scores, dim=-1)
        attn_output = torch.matmul(attn_weights, V)
        
        # 合并多头: (B, n_heads, seq_len, d_k) → (B, seq_len, d_model)
        attn_output = attn_output.transpose(1, 2).contiguous()
        attn_output = attn_output.view(B, seq_len, self.d_model)
        
        # 输出投影
        return self.W_O(attn_output)
```

---

## 4.3 Transformer完整架构

### 4.3.1 架构总览

```
Transformer 原始架构 (2017, "Attention Is All You Need")

                     Encoder (编码器)                  Decoder (解码器)
                   ════════════════                ════════════════
    输入            "I love AI"                      (已生成的) "我 爱"
      ↓                  ↓                                  ↓
Embedding       词向量 + 位置编码                    词向量 + 位置编码
      ↓                  ↓                                  ↓
Block 1     ┌─ Multi-Head Self-Attn ─┐      ┌─ Masked Multi-Head Self-Attn ─┐
            ├─ Add & LayerNorm       ┤      ├─ Add & LayerNorm              ┤
            ├─ Feed Forward Network  ┤      ├─ Cross-Attention (Encoder→)   ┤
            └─ Add & LayerNorm       ┘      ├─ Add & LayerNorm              ┤
      ↓                  ↓                   ├─ Feed Forward Network         ┤
Block N     ... 重复N次 ...                  └─ Add & LayerNorm              ┘
      ↓                  ↓                                  ↓
输出         上下文表示 (双向)                  Linear + Softmax → 预测下一个词
```

### 4.3.2 位置编码

```python
class SinusoidalPositionalEncoding(nn.Module):
    """
    原始Transformer使用正弦位置编码
    
    为什么需要？Attention没有顺序概念——
     "我爱你"和"你爱我"的Attention输出完全一样
     → 必须注入位置信息
    """
    
    def __init__(self, d_model: int, max_len: int = 5000):
        super().__init__()
        
        pe = torch.zeros(max_len, d_model)
        position = torch.arange(0, max_len).unsqueeze(1).float()
        div_term = torch.exp(
            torch.arange(0, d_model, 2).float() * 
            (-np.log(10000.0) / d_model)
        )
        
        pe[:, 0::2] = torch.sin(position * div_term)  # 偶数维度用sin
        pe[:, 1::2] = torch.cos(position * div_term)  # 奇数维度用cos
        
        self.register_buffer('pe', pe.unsqueeze(0))  # (1, max_len, d_model)
    
    def forward(self, x: torch.Tensor) -> torch.Tensor:
        """x: (batch_size, seq_len, d_model)"""
        return x + self.pe[:, :x.size(1), :]


# 现代模型更多使用可学习的位置编码（如GPT的Learnable Position Embedding）
# 或旋转位置编码 RoPE (Rotary Position Embedding) — LLaMA, Qwen等使用
```

### 4.3.3 Feed-Forward Network

```python
class FeedForward(nn.Module):
    """
    FFN = 两个线性层 + 一个激活函数
    
    FFN(x) = W₂ · GELU(W₁ · x + b₁) + b₂
    
    通常 d_ff = 4 × d_model
    例如：GPT-3中 d_model=12288, d_ff=49152
    
    FFN存储了模型大部分的知识（参数量的2/3在FFN中）
    """
    
    def __init__(self, d_model: int = 512, d_ff: int = 2048, dropout: float = 0.1):
        super().__init__()
        self.linear1 = nn.Linear(d_model, d_ff)
        self.linear2 = nn.Linear(d_ff, d_model)
        self.dropout = nn.Dropout(dropout)
    
    def forward(self, x: torch.Tensor) -> torch.Tensor:
        return self.linear2(self.dropout(F.gelu(self.linear1(x))))
```

---

## 4.4 BERT：编码器-only架构

### 4.4.1 核心特点

```
BERT (Bidirectional Encoder Representations from Transformers)
Google, 2018

关键特征:
├── 双向: 每个词同时看到左边和右边的上下文
├── 预训练任务: MLM (Masked Language Model) + NSP (Next Sentence Prediction)
├── 用途: 理解任务（分类、NER、QA、相似度）
├── 不能生成: BERT不是生成模型，不能"写"文本
└── 输入格式: [CLS] 句子A [SEP] 句子B [SEP]

训练:  "我 [MASK] 吃苹果" → 预测 [MASK] 处是 "喜欢"
      利用了双向上下文（"我"和"吃苹果"）来预测中间的词
```

### 4.4.2 BERT实战

```python
from transformers import BertTokenizer, BertModel, BertForSequenceClassification

# 加载预训练BERT
tokenizer = BertTokenizer.from_pretrained('bert-base-chinese')
model = BertModel.from_pretrained('bert-base-chinese')

# 编码文本
text = "人工智能正在改变世界"
inputs = tokenizer(
    text,
    return_tensors='pt',
    padding=True,
    truncation=True,
    max_length=512,
)

# 前向传播
with torch.no_grad():
    outputs = model(**inputs)

# outputs包含:
# - last_hidden_state: (1, seq_len, 768) 每个Token的上下文表示
# - pooler_output:     (1, 768)          [CLS] token的表示（句子向量）

print(f"最后一层hidden state: {outputs.last_hidden_state.shape}")
print(f"Pooler输出(句子向量):  {outputs.pooler_output.shape}")

# [CLS]向量常用于分类任务
sentence_embedding = outputs.pooler_output  # (1, 768)
```

### 4.4.3 BERT vs GPT的核心区别

```
              BERT (Encoder)              GPT (Decoder)
              ════════════════            ════════════════
阅读方向:      双向 ← [MASK] →             单向 → → →
Attention:     看到所有词                  只能看到前面的词(Causal Mask)
训练目标:      预测被Mask的词              预测下一个词
输出:          每个词的表示                 下一个Token的概率
典型用途:      理解(分类/问答/NER)          生成(写作/对话/代码)
代表模型:      BERT, RoBERTa, DeBERTa      GPT-3, GPT-4, LLaMA, Claude
```

---

## 4.5 GPT：解码器-only架构

### 4.5.1 核心特点

```python
"""
GPT = Generative Pre-trained Transformer

核心原理: 自回归语言模型
  P(w₁, w₂, ..., wₙ) = P(w₁) × P(w₂|w₁) × ... × P(wₙ|w₁...wₙ₋₁)

训练方式: "预测下一个Token"
  输入:  "我 喜欢 吃"
  目标:  预测 "苹果"

Causal Mask (因果遮罩):
  当预测位置i的词时，只能看到位置1到i-1的词
  这就是为什么GPT不能"往后看"——它故意被限制为单向

这个"限制"恰恰让它能生成文本！
因为你永远只能基于已生成的Token来预测下一个。
"""
```

### 4.5.2 Causal Mask实现

```python
def create_causal_mask(seq_len: int) -> torch.Tensor:
    """
    创建因果遮罩（下三角矩阵）
    
    例: seq_len=4
    [[1, 0, 0, 0],     # 位置0只能看到位置0
     [1, 1, 0, 0],     # 位置1能看到0,1
     [1, 1, 1, 0],     # 位置2能看到0,1,2
     [1, 1, 1, 1]]     # 位置3能看到0,1,2,3
    """
    mask = torch.tril(torch.ones(seq_len, seq_len))
    return mask


class GPTAttention(nn.Module):
    """带Causal Mask的自注意力"""
    
    def forward(self, x: torch.Tensor) -> torch.Tensor:
        B, seq_len, d_model = x.shape
        
        Q = self.W_Q(x)
        K = self.W_K(x)
        V = self.W_V(x)
        
        scores = torch.matmul(Q, K.transpose(-2, -1)) / np.sqrt(self.d_k)
        
        # 关键：应用Causal Mask
        causal_mask = torch.tril(torch.ones(seq_len, seq_len)).to(x.device)
        scores = scores.masked_fill(causal_mask == 0, float('-inf'))
        
        attn_weights = F.softmax(scores, dim=-1)
        return torch.matmul(attn_weights, V)
```

---

## 4.6 现代大模型架构演进

```
GPT (2018) — 117M参数
  ↓ Decoder-only + 预训练+微调范式
GPT-2 (2019) — 1.5B参数
  ↓ Zero-shot能力出现，引发伦理讨论
GPT-3 (2020) — 175B参数
  ↓ In-Context Learning，Few-shot Prompting
InstructGPT/GPT-3.5 (2022)
  ↓ RLHF对齐，遵循指令
GPT-4 (2023) — 未公开
  ↓ 多模态，更强的推理
GPT-4o (2024)

LLaMA路线:
LLaMA → LLaMA 2 → LLaMA 3 (开源标杆)
  ├── RoPE位置编码
  ├── SwiGLU激活函数
  ├── RMSNorm (替代LayerNorm)
  └── Pre-Norm结构

关键技术改进:
├── RoPE: 旋转位置编码，外推性好
├── GQA: 分组查询注意力，推理更快
├── SwiGLU: 更好的激活函数
└── Flash Attention: IO感知的快速注意力计算
```

---

## 4.7 从零实现Mini Transformer

```python
class TransformerBlock(nn.Module):
    """一个Transformer层"""
    
    def __init__(self, d_model: int = 512, n_heads: int = 8, 
                 d_ff: int = 2048, dropout: float = 0.1):
        super().__init__()
        self.attention = MultiHeadAttention(d_model, n_heads)
        self.norm1 = nn.LayerNorm(d_model)
        self.ffn = FeedForward(d_model, d_ff, dropout)
        self.norm2 = nn.LayerNorm(d_model)
        self.dropout = nn.Dropout(dropout)
    
    def forward(self, x: torch.Tensor, mask: torch.Tensor = None):
        # Self-Attention + Residual + LayerNorm
        attn_out = self.attention(x, mask)
        x = self.norm1(x + self.dropout(attn_out))
        
        # FFN + Residual + LayerNorm
        ffn_out = self.ffn(x)
        x = self.norm2(x + self.dropout(ffn_out))
        
        return x


class MiniGPT(nn.Module):
    """迷你GPT — 理解大模型本质"""
    
    def __init__(self, vocab_size: int = 10000, d_model: int = 256,
                 n_heads: int = 8, n_layers: int = 6, max_seq_len: int = 512):
        super().__init__()
        
        self.token_embedding = nn.Embedding(vocab_size, d_model)
        self.position_embedding = nn.Embedding(max_seq_len, d_model)
        
        self.blocks = nn.ModuleList([
            TransformerBlock(d_model, n_heads, d_model * 4) 
            for _ in range(n_layers)
        ])
        
        self.ln_final = nn.LayerNorm(d_model)
        self.lm_head = nn.Linear(d_model, vocab_size, bias=False)
        
        # 权重共享：Embedding和LM Head共用权重
        self.lm_head.weight = self.token_embedding.weight
        
        self.max_seq_len = max_seq_len
    
    def forward(self, token_ids: torch.Tensor) -> torch.Tensor:
        """token_ids: (batch_size, seq_len)"""
        B, seq_len = token_ids.shape
        
        # Token + Position Embedding
        positions = torch.arange(0, seq_len, device=token_ids.device).unsqueeze(0)
        x = self.token_embedding(token_ids) + self.position_embedding(positions)
        
        # Causal Mask
        causal_mask = torch.tril(torch.ones(seq_len, seq_len)).to(token_ids.device)
        
        # 通过所有Transformer层
        for block in self.blocks:
            x = block(x, causal_mask)
        
        # 最终的LayerNorm + LM Head
        x = self.ln_final(x)
        logits = self.lm_head(x)  # (B, seq_len, vocab_size)
        
        return logits
    
    def generate(self, start_tokens: torch.Tensor, max_new_tokens: int = 50,
                 temperature: float = 0.8) -> torch.Tensor:
        """自回归生成"""
        self.eval()
        tokens = start_tokens.clone()
        
        for _ in range(max_new_tokens):
            # 截断过长序列
            input_tokens = tokens[:, -self.max_seq_len:]
            
            with torch.no_grad():
                logits = self.forward(input_tokens)
                next_logits = logits[:, -1, :] / temperature  # 只看最后一个位置
            
            # 采样下一个Token
            probs = F.softmax(next_logits, dim=-1)
            next_token = torch.multinomial(probs, num_samples=1)
            
            tokens = torch.cat([tokens, next_token], dim=1)
        
        return tokens


# ========== 使用 ==========
model = MiniGPT(vocab_size=10000, d_model=256, n_heads=8, n_layers=6)
print(f"模型参数量: {sum(p.numel() for p in model.parameters()):,}")

# 模拟输入
dummy_input = torch.randint(0, 10000, (1, 10))
logits = model(dummy_input)
print(f"输入: {dummy_input.shape} → 输出: {logits.shape}")
# (1, 10) → (1, 10, 10000)
# 每个位置都输出一个词汇表大小的概率分布
```

---

## 4.8 阶段练习

### 练习1：可视化Attention
用matplotlib画出不同头的Attention权重热力图，观察多头注意力的不同模式。

### 练习2：Mini Transformer训练
在一个小语料库上训练MiniGPT，体验训练过程和Loss下降曲线。

### 练习3：BERT vs GPT对比
用同一个句子分别输入BERT和GPT，比较它们的输出向量的不同。

---

## 4.9 常见问题

### Q1: 一定要看懂Attention的数学公式吗？

**答：** 核心直觉（Query查、Key匹配、Value聚合）比公式重要。但如果你想调模型或做研究，数学是绕不开的。建议用代码实现一遍——代码比公式容易理解。

### Q2: BERT现在还有用吗？

**答：** BERT本身在大模型时代用得少了，但BERT的思想无处不在：
- Embedding模型（BGE, E5）都是BERT-like架构
- RAG中的检索模型通常是BERT变体
- 分类/NER等任务仍用BERT变体（更省资源）

### Q3: 为什么现在的LLM都是Decoder-only？

**答：** 几个原因：
- 训练效率：Decoder-only的Causal LM训练效率最高
- 生成能力：自回归天生适合文本生成
- 规模效应：Decoder-only结构在scale up时表现最稳定
- In-Context Learning：Decoder-only天然支持

### Q4: 需要看懂Transformer的每一行代码吗？

**答：** 作为应用开发者，理解架构图和数据流就够了。如果你想做模型训练/微调，则需要深入理解每个组件。

---

> **✅ 阶段完成检查清单：**
> - [ ] 能画出Transformer的完整架构图
> - [ ] 能手写Self-Attention的代码实现
> - [ ] 理解Multi-Head Attention的"多头"含义
> - [ ] 能说清BERT(Encoder)和GPT(Decoder)的核心区别
> - [ ] 知道Causal Mask的作用和实现
> - [ ] 完成了3个阶段练习
>
> **下一步：** [第5步：HuggingFace与开源模型库](../05-HuggingFace与开源模型库/README.md)
