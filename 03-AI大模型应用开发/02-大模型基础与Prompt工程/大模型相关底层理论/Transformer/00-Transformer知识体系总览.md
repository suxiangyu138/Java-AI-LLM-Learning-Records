# 00 - Transformer 知识体系总览

> 🎯 Transformer 是深度学习史上最重要的架构创新——从 NLP 到 CV、从 BERT 到 GPT、从 65M 到 1.8T 参数。本系列从数学推导到架构变体、从训练技巧到面试冲刺，构建 Transformer 的完整知识体系

> 🎯 共 **12 篇**，与 `LLM/` 系列互补：那边讲"怎么用"，这边讲"为什么这样设计"

---

## 1. 知识全景

```
Transformer 深度体系（12个文件）
│
├── 🏗️ 核心篇（01-04）
│   ├── 01-Attention机制数学推导.md          # QKV/点积/缩放/Softmax 完整推导
│   ├── 02-Multi-Head Attention深度解析.md    # 多头并行/头维度/信息融合
│   ├── 03-位置编码方法对比.md               # Sinusoidal/Learned/Relative/RoPE/ALiBi
│   └── 04-LayerNorm与残差连接.md            # Pre-Norm/Post-Norm/DeepNorm
│
├── 🔧 架构变体（05-08）
│   ├── 05-BERT：Encoder-Only架构.md         # MLM预训练/双向注意力/下游微调
│   ├── 06-GPT：Decoder-Only架构.md          # 因果注意力/自回归/ChatGPT的根
│   ├── 07-Encoder-Decoder与T5.md            # 跨注意力/Seq2Seq/Text-to-Text
│   └── 08-Vision Transformer与多模态.md     # ViT/CLIP/DALL-E/图文融合
│
├── 🚀 优化篇（09-10）
│   ├── 09-Transformer训练技巧.md            # 学习率调度/初始化/正则化/混合精度
│   └── 10-高效Transformer变体.md            # Linformer/Reformer/Sparse Attention
│
└── 📌 冲刺篇（11）
    └── 11-面试高频考点与总结.md              # 数学推导/架构对比/训练技巧
```

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | 知识体系总览 | 全景+路线 | — |
| 01 | Attention推导 | QKV矩阵/点积/Softmax/梯度 | ⭐⭐⭐⭐⭐ |
| 02 | Multi-Head | 头维度/信息融合/代码实现 | ⭐⭐⭐⭐⭐ |
| 03 | 位置编码 | 4种方法+RoPE推导+外推能力 | ⭐⭐⭐⭐ |
| 04 | LayerNorm与残差 | Pre-Norm/Post-Norm/DeepNorm | ⭐⭐⭐⭐ |
| 05 | BERT架构 | MLM/NSP/双向/微调范式 | ⭐⭐⭐⭐ |
| 06 | GPT架构 | 因果注意力/自回归/Scaling Law | ⭐⭐⭐⭐⭐ |
| 07 | Encoder-Decoder | 跨注意力/T5/Seq2Seq | ⭐⭐⭐ |
| 08 | Vision Transformer | ViT/CLIP/DALL-E/多模态 | ⭐⭐⭐⭐ |
| 09 | 训练技巧 | Warmup/Label Smoothing/混合精度 | ⭐⭐⭐⭐ |
| 10 | 高效变体 | Linformer/Reformer/Sparse | ⭐⭐⭐ |
| 11 | 面试考点 | 手推公式/架构对比/创新点 | ⭐⭐⭐⭐⭐ |

## 3. 学习路线

```text
🟢 基础（1h）：01-数学推导 → 02-MultiHead → 03-位置编码
🔵 架构（1h）：04-LayerNorm → 05-BERT → 06-GPT → 07-T5
🟣 扩展（45min）：08-ViT → 09-训练技巧 → 10-高效变体
🔴 冲刺（30min）：11-面试
```
