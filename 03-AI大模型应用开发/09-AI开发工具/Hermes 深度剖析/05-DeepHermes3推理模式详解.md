# 05 - DeepHermes 3 推理模式详解

> 🎯 DeepHermes 3 是 2025 年开源社区最受关注的创新之一——首次将"直觉回答"与"深度链式推理"统一到一个模型，通过 system prompt 一键切换模式，无需部署两个模型

---

## 目录

1. [混合推理架构](#1-混合推理架构)
2. [推理模式激活与调控](#2-推理模式激活与调控)
3. [`<think>` 标签机制深入](#3-think-标签机制深入)
4. [训练数据与蒸馏策略](#4-训练数据与蒸馏策略)
5. [已知限制与规避方案](#5-已知限制与规避方案)
6. [性能 Benchmark](#6-性能-benchmark)

---

## 1. 混合推理架构

### 1.1 双模式设计

```
DeepHermes 3 架构
│
├── 模式一：直觉模式 (Intuitive Mode)
│   ├── System: "You are Hermes, an AI assistant"
│   ├── 行为：标准 LLM 对话，直接输出答案
│   └── 适用：日常问答、代码生成、工具调用
│
└── 模式二：深度推理模式 (Reasoning Mode)
    ├── System: "You are a deep thinking AI..."
    ├── 行为：<think> 标签包裹推理链 → 最终答案
    ├── 最大推理长度：~13,000 tokens
    └── 适用：数学题、逻辑题、复杂分析
```

### 1.2 模型矩阵

| 模型 | 基座 | 参数量 | 上下文 | License |
|------|------|:---:|:---:|------|
| DeepHermes 3 3B Preview | Llama 3.2 | 3B | 32K | Llama 3 Community |
| DeepHermes 3 8B Preview | Llama 3.1 | 8B | 32K | Llama 3 Community |
| DeepHermes 3 24B Preview | Mistral Small | 24B | 32K | Mistral Research |

## 2. 推理模式激活与调控

### 2.1 激活推理模式

**System Prompt（完整复制即可激活）：**

```
You are a deep thinking AI, you may use extremely long chains of thought
to deeply consider the problem and deliberate with yourself via systematic
reasoning processes to help come to a correct solution prior to answering.
You should enclose your thoughts and internal monologue inside <think>
</think> tags, and then provide your solution or response to the problem.
```

### 2.2 关闭推理模式（切换回直觉模式）

```
You are Hermes, an AI assistant.
```

> 💡 **切换成本为零**——同一个模型，只改 system prompt，无需重新加载权重

### 2.3 推理长度控制

```python
# 推理模式下 max_new_tokens 需要大幅增加
model.generate(
    inputs,
    max_new_tokens=2500,    # ← 直觉模式 500 就够了，推理模式至少 2500
    temperature=0.6,        # ← 推理模式建议 0.5-0.7
    top_p=0.95,
    do_sample=True
)
```

| 参数 | 直觉模式 | 推理模式 |
|------|:---:|:---:|
| `max_new_tokens` | 500-1000 | **2500+**（最长可达 13000） |
| `temperature` | 0.7-0.9 | 0.5-0.7 |
| `top_p` | 0.9-0.95 | 0.95 |

## 3. `<think>` 标签机制深入

### 3.1 标准输出格式

```
[模型接收到数学题]

<think>
让我逐步分析这个问题。

第一步：理解题意...
题目要求计算 x² + 4x + 4 = 0 的根。

第二步：选择方法...
这是一个标准的一元二次方程，可以用求根公式：x = [-b ± √(b²-4ac)] / 2a
其中 a=1, b=4, c=4

第三步：计算判别式...
Δ = b²-4ac = 16-16 = 0
判别式为 0，说明方程有两个相等的实根。

第四步：代入公式...
x = [-4 ± √0] / 2 = -4/2 = -2

验证：(-2)² + 4(-2) + 4 = 4-8+4 = 0 ✓
</think>

方程 x² + 4x + 4 = 0 的解为 **x = -2**（重根）。
判别式为 0，因此方程有两个相等的实数根。
```

### 3.2 推理过程中的关键行为

```text
<think> 内部的典型推理流程：
├── 1. 问题重述        ← 用自己的话复述问题
├── 2. 方法选择        ← 列出可能的解题路径，选择最优
├── 3. 逐步计算        ← 分步骤执行，每步验证
├── 4. 自我纠错        ← 发现错误时回溯修正
├── 5. 最终验证        ← 代入验算，确认正确性
└── 6. 生成答案        ← 将推理压缩为清晰的最终输出
```

### 3.3 推理 vs 直觉输出对比

| 问题 | 直觉模式输出 | 推理模式 `<think>` 输出 |
|------|-----------|---------------------|
| "why is sky blue" | 直接解释瑞利散射 | 先分析光的性质 → 波长与散射的关系 → 得出瑞利散射 → 给出解释 |
| "solve 3x+5=20" | `x=5` | 列方程 → 移项 3x=15 → x=5 → 验算 |
| "compare Kafka and RabbitMQ" | 直接给对比表 | 各分析特性 → 找差异点 → 归纳使用场景 → 给出结论 |

## 4. 训练数据与蒸馏策略

### 4.1 数据构成（总训练集 ~390M tokens）

```text
DeepHermes 3 训练数据分布
├── 非 CoT 数据（100万条）
│   ├── 通用指令      ████████████████████████████████ 60.6%
│   ├── 领域专家      ██████ 12.8%
│   ├── 数学推理      ███ 6.7%
│   ├── 角色扮演      ███ 6.1%
│   ├── 编程          ██ 4.5%
│   ├── Agent/工具    ██ 4.3%
│   ├── 内容生成      █ 3.0%
│   └── 对齐/遵从     █ 2.5%
│
└── CoT 数据（15万条）
    └── 从 DeepSeek R1 蒸馏的推理链数据
```

### 4.2 蒸馏策略

```
DeepSeek R1（教师模型）
    │
    ├── 选择适合推理的数学/逻辑任务
    ├── 提取 R1 的 CoT 推理链
    ├── 清洗/去重/格式标准化
    │
    ▼
SFT 微调（学生模型 = Llama 3.1/Mistral）
    │
    ▼
DeepHermes 3 = 直觉 + 推理 混合模型
```

> 🎯 关键设计：CoT 数据仅占 15/115 ≈ 13%，但模型学会了"按需推理"——不是每道题都长篇大论

## 5. 已知限制与规避方案

### 5.1 限制清单

| 问题 | 表现 | 影响 | 规避 |
|------|------|:---:|------|
| 推理模式不持久 | 长对话中 `<think>` 逐渐消失 | 中 | 每轮强制 `assistant: <think>\n` |
| 推理+工具冲突 | 同时启用推理和 Function Calling 结果不稳定 | 高 | **不同时使用** |
| 8B/24B 推理差异 | 24B 版本推理更长更准确 | 低 | 复杂任务选 24B |
| 幻觉推理 | `<think>` 中可能出现错误推理过程 | 中 | 关注最终答案，不盲信推理过程 |

### 5.2 强制推理模式持续

```python
# 多轮对话中维持推理模式
def build_messages_with_think(history, new_question):
    messages = [
        {"role": "system", "content": DEEP_THINK_SYSTEM_PROMPT}
    ]
    for turn in history:
        messages.append({"role": "user", "content": turn["user"]})
        # 强制以 <think> 开头
        messages.append({
            "role": "assistant",
            "content": "<think>\n" + turn["assistant"]
        })
    messages.append({"role": "user", "content": new_question})
    return messages
```

> ⚠️ **强制 `<think>\n` 的副作用**：可能影响模型正常对话风格，建议只在需要推理的场景使用

## 6. 性能 Benchmark

### 6.1 数学推理

| 模型 | MATH Hard | GSM8K | 定位 |
|------|:---:|:---:|------|
| DeepSeek R1 (70B distil) | 89.1% | 95.2% | 纯推理专家 |
| **DeepHermes 3 24B** | **67.0%** | **~88%** | 通才+推理 |
| DeepHermes 3 8B | ~55% | ~82% | 入门推理 |
| Hermes 3 8B | ~35% | ~70% | 无推理能力 |

### 6.2 综合能力（推理模式下）

```text
推理模式下的能力变化（相对直觉模式）
├── 数学        ↑↑↑ (+30-40%)    ← 最大增益
├── 逻辑推理    ↑↑  (+15-25%)
├── 代码调试    ↑   (+10-15%)
├── 写作        →   (基本不变)
├── 翻译        →   (基本不变)
└── 角色扮演    ↓   (-5-10%)     ← 推理链打断自然对话流
```

## 核心要点回顾

- DeepHermes 3 = 同一模型 + 不同 System Prompt = 双模式切换
- 推理模式：system prompt 要求 `<think>` 包裹推理链 → 复杂题目效果飞跃
- `<think>` 最长可达 13,000 tokens，务必调大 `max_new_tokens`
- 蒸馏自 DeepSeek R1，CoT 数据占训练集约 13%
- **两大限制**：推理模式多轮不持久 + 不同时支持 Function Calling
- 性能：数学 +30-40%，代码 +10-15%，写作/翻译基本不变

## 参考资料

1. HuggingFace - NousResearch/DeepHermes-3-Mistral-24B-Preview Model Card
2. VentureBeat - "Nous Research launches first toggle-on reasoning model: DeepHermes-3" (2025)
3. OpenRouter - DeepHermes 3 API 快速入门
