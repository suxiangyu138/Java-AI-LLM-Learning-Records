# 04 - 开源 LLM 生态全景

> 🎯 开源 LLM 在 2024-2025 年迎来爆发——Llama 3.1 405B 对标 GPT-4、Qwen 2.5 中文称王、DeepSeek V3 极致性价比。了解开源生态，才能在成本、性能、合规之间做对选择

---

## 目录

1. [开源模型全景对比](#1-开源模型全景对比)
2. [Llama 系列](#2-llama-系列)
3. [Qwen 通义千问](#3-qwen-通义千问)
4. [DeepSeek 系列](#4-deepseek-系列)
5. [Mistral / Gemma / 其他](#5-mistral--gemma--其他)

---

## 1. 开源模型全景对比

| 模型 | 开发方 | 最大参数 | 上下文 | 许可证 | 中文 | 定位 |
|------|------|:---:|:---:|------|:---:|------|
| **Llama 3.1** | Meta | 405B | 128K | Llama3 Community | ⭐⭐⭐ | 开源标杆 |
| **Qwen 2.5** | 阿里 | 72B | 128K | **Apache 2.0** | ⭐⭐⭐⭐⭐ | 中文最强 |
| **DeepSeek V3** | DeepSeek | 671B(MoE) | 64K | MIT | ⭐⭐⭐⭐⭐ | 极致性价比 |
| **Mistral** | Mistral AI | 123B | 128K | Apache 2.0 | ⭐⭐ | 欧洲标杆 |
| **Gemma 2** | Google | 27B | 8K | Gemma | ⭐⭐ | 轻量级 |
| **Hermes 3** | Nous Research | 405B | 32K | Llama3 | ⭐⭐ | 无审查FC |
| **Yi** | 零一万物 | 34B | 200K | Apache 2.0 | ⭐⭐⭐⭐ | 长上下文 |
| **Phi-4** | Microsoft | 14B | 16K | MIT | ⭐⭐ | 小而精 |

## 2. Llama 系列

### 2.1 演进

```text
Llama 1 (2023.02): 7B/13B/33B/65B
  → 首次证明"小模型+大数据"胜过"大模型+小数据"

Llama 2 (2023.07): 7B/13B/70B
  → 开源商用（<700M 月活免费）
  → GQA + 2T tokens 训练

Llama 3 (2024.04): 8B/70B
  → 128K 词表 + 15T tokens
  → 大幅提升多语言

Llama 3.1 (2024.07): 8B/70B/405B
  → 405B = 开源首个对标 GPT-4 的模型
  → 128K 上下文
  → 但许可证限制 >700M 月活需申请
```

### 2.2 技术特性

| 特性 | Llama 2 | Llama 3 | Llama 3.1 |
|------|:---:|:---:|:---:|
| 注意力 | MHA → GQA (70B) | GQA | GQA |
| 词表大小 | 32K | **128K** | 128K |
| 位置编码 | RoPE | RoPE | RoPE |
| 激活函数 | SwiGLU | SwiGLU | SwiGLU |
| 训练数据 | 2T tokens | 15T tokens | 15T+ tokens |

## 3. Qwen 通义千问

```text
Qwen 2.5 — 阿里开源旗舰

核心优势：
├── 🇨🇳 中文最强：中文理解/生成/翻译远超 Llama
├── 📜 Apache 2.0：最宽松的商用许可证
├── 📏 全尺寸：0.5B → 1.5B → 3B → 7B → 14B → 32B → 72B
├── 📖 128K 上下文
├── 🔧 Function Calling 原生支持
└── 🎯 多语言：支持 29 种语言

选型建议：
  中文场景 → Qwen 2.5（不二之选）
  需要商用修改 → Qwen（Apache 2.0，比 Llama 宽松）
  需要小模型 → Qwen 2.5 0.5B/1.5B（移动端可用）
```

## 4. DeepSeek 系列

```text
DeepSeek V3 — 2024-2025 年最具性价比的模型

MoE 架构：
  ├── 总参数 671B，激活参数 37B
  ├── 训练成本 ~$5.6M（仅 GPT-4 的 1/20！）
  └── 推理成本 $0.27/1M tokens（GPT-4 的 1/50）

DeepSeek R1 — 推理模型：
  ├── 通过强化学习获得推理能力（不是 SFT）
  ├── 自动生成 CoT 推理链
  ├── MATH 500: 97.3%
  └── 开源权重 + MIT 许可证

核心启示：
  → MoE 可以极大降低训练成本
  → 强化学习（不依赖人工标注）+ 蒸馏 = 推理能力
```

## 5. Mistral / Gemma / 其他

| 模型 | 一句话定位 |
|------|-----------|
| **Mistral Large** | 欧洲最强，法语王者，开源 123B |
| **Gemma 2** | Google 轻量级，27B 跑 8B 性能 |
| **Phi-4** | 微软"小而精"，14B 参数达 70B 水平 |
| **Yi 1.5** | 200K 超长上下文，中文优秀 |
| **Command R+** | Cohere 企业级 RAG 优化模型 |
| **DBRX** | Databricks MoE，132B 总参 |

## 核心要点回顾

- Llama 3.1 405B = 开源标杆，首次对标 GPT-4
- Qwen 2.5 = 中文最强 + Apache 2.0 最宽松许可证
- DeepSeek V3 = MoE 极致性价比 + MIT 开源
- 开源三极：Llama（美国）+ Qwen（中国）+ Mistral（欧洲）
- 选择公式：中文→Qwen，成本→DeepSeek，生态→Llama

## 参考资料

1. Llama 3 技术报告 — ai.meta.com
2. Qwen 2.5 技术报告 — github.com/QwenLM
3. DeepSeek V3/R1 技术报告 — github.com/deepseek-ai
