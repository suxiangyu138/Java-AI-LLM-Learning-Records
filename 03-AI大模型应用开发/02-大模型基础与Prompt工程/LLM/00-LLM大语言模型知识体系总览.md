# 00 - LLM 大语言模型 知识体系总览

> 🎯 LLM（Large Language Model）是当代 AI 的核心引擎——从 Transformer 架构到 GPT 系列、从预训练到 RLHF、从 Prompt 工程到推理优化。本系列构建 LLM 的完整理论地基

> 🎯 共 **12 篇**，从 Transformer 原理到模型演进、从训练方法论到推理优化、从 Prompt 工程到面试冲刺

---

## 1. 知识全景

```
LLM 大语言模型体系（12个文件）
│
├── 🏗️ 基础篇（01-03）
│   ├── 01-Transformer架构深度解析.md        # 注意力机制/多头/位置编码/残差
│   ├── 02-Tokenization分词技术.md            # BPE/WordPiece/SentencePiece/词表
│   └── 03-GPT系列模型演进.md                # GPT-1→4→5.5 架构变化与能力跃迁
│
├── 🔧 训练篇（04-06）
│   ├── 04-开源LLM生态全景.md               # Llama/Qwen/DeepSeek/Mistral/Hermes
│   ├── 05-预训练与微调技术.md               # Pre-training/SFT/LoRA/QLoRA
│   └── 06-RLHF与对齐技术.md                 # RLHF/DPO/Constitutional AI
│
├── 🚀 应用篇（07-09）
│   ├── 07-Prompt Engineering核心技法.md      # Few-shot/CoT/ReAct/System Prompt
│   ├── 08-模型评估与Benchmark.md            # MMLU/HumanEval/GSM8K/评估方法论
│   └── 09-推理优化技术.md                   # 量化/KV Cache/FlashAttn/Speculative
│
├── 📋 架构篇（10）
│   └── 10-LLM应用架构模式.md               # RAG/Agent/FunctionCalling/多模态
│
└── 📌 冲刺篇（11）
    └── 11-面试高频考点与总结.md              # 面试题/架构理解/技术深度
```

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | 知识体系总览 | 全景+路线 | — |
| 01 | Transformer架构 | 注意力/多头/位置编码 | ⭐⭐⭐⭐⭐ |
| 02 | Tokenization | BPE/词表/特殊Token | ⭐⭐⭐⭐ |
| 03 | GPT模型演进 | GPT-1→4 架构/能力跃迁 | ⭐⭐⭐⭐⭐ |
| 04 | 开源LLM生态 | Llama/Qwen/DeepSeek | ⭐⭐⭐⭐ |
| 05 | 预训练与微调 | SFT/LoRA/QLoRA/全参 | ⭐⭐⭐⭐⭐ |
| 06 | RLHF与对齐 | RLHF/DPO/偏好对齐 | ⭐⭐⭐⭐⭐ |
| 07 | Prompt工程 | Few-shot/CoT/ReAct | ⭐⭐⭐⭐⭐ |
| 08 | 模型评估 | MMLU/HumanEval/Benchmark | ⭐⭐⭐⭐ |
| 09 | 推理优化 | 量化/KV Cache/FlashAttn | ⭐⭐⭐⭐ |
| 10 | 应用架构 | RAG/Agent/FC/多模态 | ⭐⭐⭐⭐⭐ |
| 11 | 面试考点 | 高频题+架构深问 | ⭐⭐⭐⭐⭐ |

## 3. 学习路线

```text
🟢 基础（1h）：01-Transformer → 02-Tokenization
🔵 理解（1.5h）：03-GPT演进 → 04-开源生态 → 05-预训练微调
🟣 深入（1h）：06-RLHF → 07-Prompt → 08-评估
🟡 实战（45min）：09-推理优化 → 10-应用架构
🔴 冲刺（30min）：11-面试
```

## 4. 核心术语速查

| 术语 | 含义 |
|------|------|
| **Self-Attention** | 自注意力，计算序列中每个 token 与其他 token 的关系 |
| **Multi-Head Attention** | 多头注意力，并行计算多组注意力，捕捉不同子空间信息 |
| **BPE** | Byte Pair Encoding，最常见分词算法 |
| **SFT** | Supervised Fine-Tuning，监督微调 |
| **RLHF** | RL from Human Feedback，人类反馈强化学习 |
| **LoRA** | Low-Rank Adaptation，轻量级微调方法 |
| **CoT** | Chain-of-Thought，思维链推理 |
| **RAG** | Retrieval-Augmented Generation，检索增强生成 |
| **MoE** | Mixture of Experts，混合专家架构 |
