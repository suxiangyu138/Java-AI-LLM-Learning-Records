# DeepSeek 知识体系总览

> 🎯 全方位深度解读 DeepSeek 大模型技术体系 —— 从 V3 到 V4 Pro（1.6T MoE），从架构原理到工程落地，从模型家族到开源生态，构建完整的 DeepSeek 认知框架 | 更新至 2026年7月

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [技术关键词索引](#5-技术关键词索引)

---

## 1. 知识体系导图

```
DeepSeek 知识体系全景
│
├── 🏢 基础认知层
│   ├── 01-概览与模型家族    → 公司背景、发展历程、全系列模型（含 V4）
│   └── 08-生态对比与选型    → 横向对比（含 V4 Pro vs GPT-5.4 等）
│
├── 🧠 核心技术层（V3 时代）
│   ├── 02-MoE与MLA架构      → Mixture of Experts、Multi-head Latent Attention
│   ├── 03-V3训练与优化       → FP8训练、MTP、多Token预测、成本优化
│   └── 04-R1推理模型        → GRPO、冷启动、推理链、蒸馏
│
├── 🚀 前沿架构层（V4 时代）⭐ NEW
│   └── 09-V4与V4-Pro        → 1.6T MoE、CSA+HCA混合注意力、mHC、Muon、OPD+GRM
│
├── 🎯 垂直能力层
│   ├── 05-Coder代码模型     → FIM、代码生成、仓库级理解
│   └── 06-多模态与前沿探索  → Janus、VL2、统一多模态
│
└── 🔧 工程实践层
    └── 07-API与工程落地      → API调用、Prompt工程、本地部署、框架集成
```

---

## 2. 模块导航

| 序号 | 模块名称 | 核心内容 | 适合人群 |
|:----:|---------|---------|---------|
| 01 | [DeepSeek 概览与模型家族](./01-DeepSeek概览与模型家族.md) | 公司背景、发展里程碑、完整模型谱系（含V4） | 所有人 |
| 02 | [核心架构-MoE与MLA](./02-DeepSeek核心架构-MoE与MLA.md) | MoE原理、DeepSeekMoE、MLA注意力机制（V2/V3基础） | 算法工程师/研究者 |
| 03 | [V3 训练与优化策略](./03-DeepSeek-V3训练与优化策略.md) | FP8训练、MTP、HAI-LLM框架、成本分析 | ML工程师/架构师 |
| 04 | [R1 推理模型深度解析](./04-DeepSeek-R1推理模型深度解析.md) | 推理范式、GRPO、冷启动RL、蒸馏 | 研究者/高级工程师 |
| 05 | [Coder 代码模型](./05-DeepSeek-Coder与代码能力.md) | FIM训练、代码理解、仓库级补全 | 软件开发者 |
| 06 | [多模态与前沿探索](./06-DeepSeek多模态与前沿探索.md) | Janus、DeepSeek-VL2、统一理解生成 | 多模态研究者 |
| 07 | [API与工程落地实践](./07-DeepSeek-API与工程落地实践.md) | API调用、Prompt技巧、本地部署、框架集成 | 应用开发者 |
| 08 | [生态对比与选型指南](./08-DeepSeek生态对比与选型指南.md) | 横向对比GPT/Claude/Qwen、选型决策 | 技术管理者/架构师 |
| 09 | [⭐ V4与V4-Pro深度解析](./09-DeepSeek-V4与V4-Pro深度解析.md) | 1.6T MoE、CSA+HCA、mHC、Muon、OPD、DSpark | 全体（2026年必读） |

---

## 3. 学习路线推荐

### 🟢 入门路线（1-2天）

```
01-概览与模型家族  →  07-API与工程落地实践  →  08-生态对比与选型指南
```

> 💡 **目标**：理解 DeepSeek 是什么、能做什么、怎么用、什么时候选它。

### 🟡 进阶路线（3-5天）

```
01-概览  →  02-MoE与MLA架构  →  03-V3训练优化  →  05-Coder  →  07-API实践
```

> 💡 **目标**：掌握核心技术原理，具备调优和集成能力。

### 🔴 深度研究路线（1-2周）

```
01 → 02 → 03 → 04-R1推理 → 05-Coder → 06-多模态 → 07-API → 08-对比
```

> 💡 **目标**：全面掌握 DeepSeek 技术栈，可进行学术研究或高级工程实践。

### 🚀 最新前沿路线（2026年必读）

```
01-概览 → 02-MoE基础 → 09-V4/V4-Pro深度解析 → 08-对比（V4版）
```

> 💡 **目标**：快速了解 DeepSeek V4 的范式级架构革新，把握最新技术趋势。

---

## 4. 核心概念速查

| 概念 | 英文全称 | 简要说明 | 详见模块 |
|------|---------|---------|:-------:|
| **MoE** | Mixture of Experts | 混合专家模型，稀疏激活子网络 | 02 |
| **MLA** | Multi-head Latent Attention | 多头潜在注意力，大幅压缩KV Cache（V2/V3） | 02 |
| **CSA** | Compressed Sparse Attention | 压缩稀疏注意力，V4 混合注意力组件 | 09 |
| **HCA** | Heavily Compressed Attention | 重度压缩注意力，V4 全局语境感知 | 09 |
| **mHC** | Manifold-Constrained Hyper-Connections | 流形约束超连接，V4 稳定深层梯度 | 09 |
| **Muon** | - | 矩阵级优化器，V4 替代 AdamW | 09 |
| **DeepSeekMoE** | - | DeepSeek 自研的细粒度 MoE 架构 | 02 |
| **MTP** | Multi-Token Prediction | 多 Token 预测（V3），后被 DSpark 替代 | 03 |
| **DSpark** | - | V4 推测性解码框架，推理加速 60-85% | 09 |
| **FP8/FP4** | 8-bit/4-bit Floating Point | V3 FP8 / V4 FP4+FP8 混合精度训练 | 03/09 |
| **GRPO** | Group Relative Policy Optimization | 群组相对策略优化，R1/V4 的核心 RL 算法 | 04 |
| **OPD** | On-Policy Distillation | 在线策略蒸馏，V4 后训练核心方法 | 09 |
| **GRM** | Generative Reward Model | 生成式奖励模型，V4 细粒度反馈 | 09 |
| **CoT** | Chain of Thought | 思维链推理 | 04 |
| **FIM** | Fill-in-the-Middle | 中间填充训练，代码补全的核心范式 | 05 |
| **Janus** | - | 统一理解与生成的多模态架构 | 06 |
| **SFT** | Supervised Fine-Tuning | 监督微调 | 03/04 |
| **RLHF** | RL from Human Feedback | 基于人类反馈的强化学习 | 03 |
| **DPO** | Direct Preference Optimization | 直接偏好优化 | 03 |

---

## 5. 技术关键词索引

### 架构相关
`Mixture of Experts` `MoE` `DeepSeekMoE` `Fine-grained Expert Segmentation` `Shared Expert Isolation` `Multi-head Latent Attention` `MLA` `Compressed Sparse Attention` `CSA` `Heavily Compressed Attention` `HCA` `Hybrid Attention` `RoPE` `KV Cache Compression` `Load Balancing` `Auxiliary-Loss-Free` `Device-Limited Routing` `Token-Choice Routing` `mHC` `Manifold-Constrained Hyper-Connections`

### 训练相关
`FP8 Mixed Precision` `FP4+FP8` `Multi-Token Prediction` `MTP` `HAI-LLM` `Pipeline Parallelism` `Expert Parallelism` `DualPipe` `cross-node MoE training` `Muon Optimizer` `Newton-Schulz Iteration` `On-Policy Distillation` `OPD` `GRM` `Generative Reward Model` `Interleaved Thinking`

### 推理/对齐
`GRPO` `Group Relative Policy Optimization` `Chain of Thought` `CoT` `cold-start data` `reasoning distillation` `Rejection Sampling` `RL Stage` `Aha Moment` `Self-Verification` `Reflection` `Non-think` `Think High` `Think Max` `DSpark` `Speculative Decoding` `半自回归生成`

### 模型家族
`DeepSeek-V2` `DeepSeek-V3` `DeepSeek-V4` `DeepSeek-V4-Pro` `DeepSeek-V4-Flash` `DeepSeek-R1` `DeepSeek-R1-Zero` `DeepSeek-Coder` `DeepSeek-Coder-V2` `DeepSeek-VL` `DeepSeek-VL2` `Janus` `Janus-Pro` `DeepSeek-LLM`

### 工程/生态
`Ollama` `vLLM` `SGLang` `OpenAI-Compatible API` `Function Calling` `JSON Mode` `Context Caching` `Prompt Caching` `LangChain` `Spring AI` `Ascend NPU` `DeepSpec`

---

> 🎯 **开始学习**：建议从 [01-DeepSeek概览与模型家族](./01-DeepSeek概览与模型家族.md) 开始，建立对 DeepSeek 的整体认知。

---

*最后更新：2026年7月*
