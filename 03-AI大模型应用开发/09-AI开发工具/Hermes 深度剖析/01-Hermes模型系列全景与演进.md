# 01 - Hermes 模型系列全景与演进

> 🎯 Hermes 是 Nous Research 打造的"无审查、高可控"开源 LLM 系列，从 2023 年 Hermes 1 起步，历经 Hermes 2 Pro → Hermes 3（2024）→ DeepHermes 3（2025），逐步成为开源社区 Agent 开发的首选基座模型之一

---

## 目录

1. [Hermes 系列发展时间线](#1-hermes-系列发展时间线)
2. [各代模型架构对比](#2-各代模型架构对比)
3. [Hermes 3 核心特性](#3-hermes-3-核心特性)
4. [DeepHermes 3：混合推理时代](#4-deephermes-3混合推理时代)
5. [训练方法演进](#5-训练方法演进)

---

## 1. Hermes 系列发展时间线

| 时间 | 模型 | 基座 | 参数量 | 核心突破 |
|------|------|------|--------|---------|
| 2023 Q2 | **Hermes 1** | Llama 2 | 7B/13B | 首个无审查开源模型，角色扮演能力 |
| 2024 Q1 | **Hermes 2 Pro** | Mistral / Llama 3 | 7B/8B | 引入 Function Calling + JSON Mode |
| 2024 Q3 | **Hermes 3** | Llama 3.1 | 3B/8B/70B/**405B** | 首个 405B 全参数微调，RLHF 加持 |
| 2024 Q4 | **Hermes 3 3B** | Llama 3.2 | 3B | 小模型 Function Calling 标杆 |
| 2025 Q1 | **DeepHermes 3** | Llama 3.1 / Llama 3.2 / Mistral | 3B/8B/24B | **一键切换**直觉/深度推理模式 |

> 💡 Hermes 3 405B 是**业界首个**对 Llama 3.1 405B 进行全参数微调的开源模型——在 LambdaLabs 8 节点 H100 集群上仅用数周完成训练

## 2. 各代模型架构对比

| 维度 | Hermes 1 | Hermes 2 Pro | Hermes 3 | DeepHermes 3 |
|------|----------|-------------|----------|-------------|
| 基座架构 | Llama 2 | Mistral/Llama 3 | Llama 3.1/3.2 | Llama 3.1/3.2/Mistral |
| 对话格式 | 无标准格式 | ChatML | ChatML | **Llama-Chat** |
| 上下文长度 | 4K | 32K | 32K | **32K** |
| Function Calling | ❌ | ✅ (初代) | ✅ (增强) | ✅ (继承) |
| JSON Mode | ❌ | ✅ | ✅ | ✅ |
| 推理模式 | ❌ | ❌ | ❌ | ✅ (`<think>`) |
| 量化支持 | 4bit/8bit | 4bit/8bit | FP8 (Neural Magic) | GGUF/FP8 |
| 训练数据 | 混合开源 | 合成数据 | 合成数据+RLHF | R1 蒸馏+合成 |

> ⚠️ **重要变更**：DeepHermes 3 从 ChatML 格式迁移到 **Llama-Chat 格式**（`<|start_header_id|>`），与 Hermes 3 不兼容，迁移时需更新模板

## 3. Hermes 3 核心特性

### 3.1 模型矩阵

| 型号 | 参数量 | 显存需求 (FP16) | 显存需求 (4bit) | 适用场景 |
|------|:---:|:---:|:---:|------|
| Hermes 3 3B | 3B | ~6GB | ~2GB | 边缘设备/嵌入式 |
| Hermes 3 8B | 8B | ~16GB | ~6GB | 消费级 GPU (RTX 3060+) |
| Hermes 3 70B | 70B | ~140GB | ~40GB | 企业级部署 |
| Hermes 3 405B | 405B | ~810GB | ~200GB | 云端集群 |

### 3.2 关键能力

```
Hermes 3 能力矩阵
├── 🧠 通用推理         ← 对标 Llama 3.1 Instruct，多基准持平或超越
├── 🔧 Function Calling  ← ChatML + <tool_call> XML，支持多工具递归调用
├── 📋 结构化输出        ← <schema> 标签 + Pydantic JSON 约束
├── 🎭 角色扮演          ← 内置内心独白能力（internal monologue）
├── 💬 多轮对话          ← 长上下文连贯性，32K 窗口
└── 🎯 用户可控性        ← 极强的 system prompt 遵从度（aggressively follows）
```

> 💡 "Aggressively follows the system prompt" 是 Hermes 3 的标志性特征——它在遵循用户指令方面极为激进，这也意味着一份好的 system prompt 能带来极致的可控性

## 4. DeepHermes 3：混合推理时代

### 4.1 核心创新

2025 年，Nous Research 发布 DeepHermes 3，首次将**直觉模式**与**深度推理模式**统一到单一模型中：

```text
直觉模式（Intuitive Mode）
  ┌─────────────────────────────────┐
  │ System: "You are Hermes"         │ → 标准对话，类似 Hermes 3
  │ 输出：直接回答，无思考过程         │
  └─────────────────────────────────┘

深度推理模式（Reasoning Mode）
  ┌─────────────────────────────────┐
  │ System: "You are a deep thinking  │
  │ AI, use extremely long chains of  │ → <think> 包裹推理过程
  │ thought..."                       │
  │ 输出：<think> 推理 </think> 最终答案│
  └─────────────────────────────────┘
```

### 4.2 推理数据构成

| 数据类型 | 占比 | 说明 |
|---------|:---:|------|
| 通用指令 | 60.6% | 非 CoT 的日常问答 |
| 领域专家 | 12.8% | 垂直领域知识 |
| 数学推理 | 6.7% | 数学题 CoT 训练 |
| 角色扮演/创意写作 | 6.1% | 创造力保留 |
| 编程 | 4.5% | 代码生成 |
| 工具使用/Agent | 4.3% | Function Calling 能力 |
| 内容生成 | 3.0% | 模板化输出 |
| 对齐/指令遵从 | 2.5% | 安全与可控 |

> ⚠️ **已知限制**：推理模式在首轮响应正常激活，但**长对话中可能失效**。变通方案：强制以 `<think>\n` 开头（类似 DeepSeek-R1）

### 4.3 推理 Benchmark

| 模型 | MATH Hard | 定位 |
|------|:---:|------|
| DeepSeek R1 (distilled) | 89.1% | 纯推理专家 |
| **DeepHermes 3** | **67%** | **通才+推理双模式** |

> 💡 DeepHermes 3 不走纯数学路线，而是追求"既能日常聊天，又能深度推理"的通才定位

## 5. 训练方法演进

| 阶段 | 方法 | 数据 | 特点 |
|------|------|------|------|
| Hermes 1 | SFT | 混合开源数据 | 基础能力 |
| Hermes 2 Pro | SFT | 合成数据为主 | Function Calling 专项训练 |
| Hermes 3 | SFT → RLHF | 近一年积累的合成数据 | 人类反馈强化学习 |
| DeepHermes 3 | **R1 蒸馏 + SFT** | 100万非CoT + 15万CoT | 推理能力蒸馏 |

```text
训练基础设施（Hermes 3 405B）
├── GPU：LambdaLabs 1-Click Cluster（8节点 × 8×H100）
├── 优化：Flash Attention 2
├── 量化：Neural Magic FP8（减少 50% VRAM/磁盘）
└── 框架：HuggingFace Transformers + DeepSpeed
```

## 核心要点回顾

- Hermes 系列 = 无审查 + 高可控 + 强 Function Calling 的开源 LLM 生态
- Hermes 3 405B 是业界首个 Llama 3.1 405B 全参数微调模型
- ChatML 格式是 Hermes 1-3 的通用对话协议
- DeepHermes 3 = 首个"直觉/推理"一键切换的开源模型
- 训练演进：SFT → SFT + RLHF → R1 蒸馏 + SFT
- 注意 DeepHermes 3 切换到 Llama-Chat 格式，与 Hermes 3 不兼容

## 参考资料

1. Nous Research 官方 - Hermes 3 技术报告
2. VentureBeat - DeepHermes 3 发布报道（2025）
3. HuggingFace - NousResearch/Hermes-3-Llama-3.1-8B
4. Lambda AI - Hermes 3 405B 训练基础设施揭秘
